package xyz.hotchpotch.hogandiff.logic._stax.readers;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import xyz.hotchpotch.hogandiff.logic._stax.StaxUtil;
import xyz.hotchpotch.hogandiff.logic._stax.StaxUtil.NONS_QNAME;
import xyz.hotchpotch.hogandiff.logic._stax.StaxUtil.V_QNAME;
import xyz.hotchpotch.hogandiff.logic._stax.StaxUtil.X_QNAME;

/**
 * 全てのセルコメントを非表示にして色を消す {@link XMLEventReader} の実装です。<br>
 * 具体的には、.xlsx/.xlsm 形式のExcelファイルの各ワークシートに対応する
 * xl/drawing/vmlDrawing?.vml エントリを処理対象とし、
 * 各種要素に対する操作を行います。<br>
 *
 * @author nmby
 */
public class CloseAndUnpaintCommentsReader extends BufferingReader {

    // [static members] ********************************************************

    private static final XMLEventFactory eventFactory = XMLEventFactory.newFactory();

    /**
     * 新しいリーダーを構成します。<br>
     * 
     * @param source ソースリーダー
     * @return 新しいリーダー
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static XMLEventReader of(XMLEventReader source) {
        Objects.requireNonNull(source);

        return new CloseAndUnpaintCommentsReader(source);
    }

    // [instance members] ******************************************************

    private boolean inCommentShape;
    private boolean inNoteObject;

    private CloseAndUnpaintCommentsReader(XMLEventReader source) {
        super(source);
    }

    @Override
    protected void seekNext() throws XMLStreamException {
        if (!source.hasNext()) {
            return;
        }

        // 完全に手探り実装。これで正しいのかさっぱり分からん。

        XMLEvent event = source.peek();
        if (StaxUtil.isStart(event, V_QNAME.SHAPE)
                && "#_x0000_t202".equals(
                        event.asStartElement().getAttributeByName(NONS_QNAME.TYPE).getValue())) {

            inCommentShape = true;

            buffer.addLast(createNewShape(event.asStartElement()));
            source.nextEvent();

        } else if (inCommentShape
                && StaxUtil.isStart(event, V_QNAME.FILL)) {

            // v;fill 要素を消しても消さなくても影響ないっぽいが
            // 良く分からないので消しておく。orz
            while (!StaxUtil.isEnd(source.peek(), V_QNAME.FILL)) {
                source.nextEvent();
            }
            source.nextEvent();

        } else if (inCommentShape
                && StaxUtil.isStart(event, X_QNAME.CLIENT_DATA)
                && "Note".equals(
                        event.asStartElement().getAttributeByName(NONS_QNAME.OBJECT_TYPE).getValue())) {

            inNoteObject = true;

        } else if (inCommentShape && inNoteObject
                && StaxUtil.isStart(event, X_QNAME.VISIBLE)) {

            // Visible を読み飛ばすことによって、コメントを非表示にする。
            // ※実験の結果、この削除処理は結果に影響しないっぽい
            // （削除してもコメントが表示されてしまう）が、
            // 後の着色処理をしやすくするために実施しておく。
            while (!StaxUtil.isEnd(source.peek(), X_QNAME.VISIBLE)) {
                source.nextEvent();
            }
            source.nextEvent();

        } else if (inNoteObject && StaxUtil.isEnd(event, X_QNAME.CLIENT_DATA)) {
            inNoteObject = false;

        } else if (inCommentShape && StaxUtil.isEnd(event, V_QNAME.SHAPE)) {
            inCommentShape = false;
        }
    }

    private StartElement createNewShape(StartElement originalShape) {
        Map<QName, Attribute> attrs = new HashMap<>();
        originalShape.getAttributes().forEachRemaining(attr -> attrs.put(attr.getName(), attr));

        String orgStyle = attrs.get(NONS_QNAME.STYLE).getValue();
        String newStyle = orgStyle.replace("visibility:visible", "visibility:hidden");
        attrs.put(NONS_QNAME.STYLE, eventFactory.createAttribute(NONS_QNAME.STYLE, newStyle));

        attrs.remove(NONS_QNAME.FILL_COLOR);
        attrs.remove(NONS_QNAME.STROKE_COLOR);

        return eventFactory.createStartElement(V_QNAME.SHAPE, attrs.values().iterator(), null);
    }
}
