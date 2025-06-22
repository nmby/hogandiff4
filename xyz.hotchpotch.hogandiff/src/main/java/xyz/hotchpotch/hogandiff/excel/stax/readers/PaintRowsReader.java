package xyz.hotchpotch.hogandiff.excel.stax.readers;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import xyz.hotchpotch.hogandiff.excel.stax.StaxUtil;
import xyz.hotchpotch.hogandiff.excel.stax.StaxUtil.NONS_QNAME;
import xyz.hotchpotch.hogandiff.excel.stax.StaxUtil.QNAME;
import xyz.hotchpotch.hogandiff.excel.stax.PainterWithStax.StylesManager;

/**
 * 余剰行に色を付ける {@link XMLEventReader} の実装です。<br>
 * 具体的には、.xlsx/.xlsm 形式のExcelファイルの各ワークシートに対応する
 * xl/worksheets/sheet?.xml エントリを処理対象とし、
 * {@code <row>} 要素に対する操作を行います。<br>
 *
 * @author nmby
 */
public class PaintRowsReader extends BufferingReader {

    // [static members] ********************************************************

    private static final XMLEventFactory eventFactory = XMLEventFactory.newFactory();

    /**
     * 新しいリーダーを構成します。<br>
     * 
     * @param source        ソースリーダー
     * @param stylesManager スタイルマネージャ
     * @param targetRows    着色対象の行インデックス（0 開始）
     * @param colorIdx      着色する色のインデックス
     * @return 新しいリーダー
     * @throws NullPointerException     パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code targetRows} の長さが 0 の場合
     */
    public static XMLEventReader of(
            XMLEventReader source,
            StylesManager stylesManager,
            List<Integer> targetRows,
            short colorIdx) {

        Objects.requireNonNull(source);
        Objects.requireNonNull(stylesManager);
        Objects.requireNonNull(targetRows);
        if (targetRows.isEmpty()) {
            throw new IllegalArgumentException("no target rows");
        }

        return new PaintRowsReader(
                source,
                stylesManager,
                targetRows,
                colorIdx);
    }

    // [instance members] ******************************************************

    private final StylesManager stylesManager;
    private final Queue<Integer> targetRows;
    private final short colorIdx;
    private boolean auto = false;

    private PaintRowsReader(
            XMLEventReader source,
            StylesManager stylesManager,
            List<Integer> targetRows,
            short colorIdx) {

        super(source);

        assert stylesManager != null;
        assert targetRows != null;
        assert !targetRows.isEmpty();

        this.stylesManager = stylesManager;
        this.targetRows = new ArrayDeque<>(targetRows);
        this.colorIdx = colorIdx;
    }

    @Override
    protected void seekNext() throws XMLStreamException {
        if (auto) {
            return;
        }
        if (!source.hasNext()) {
            throw new XMLStreamException("file may be corrupted");
        }

        XMLEvent event = source.peek();
        if (StaxUtil.isEnd(event, QNAME.SHEET_DATA)) {
            targetRows.forEach(this::createRow);
            auto = true;
            return;
        }
        if (!StaxUtil.isStart(event, QNAME.ROW)) {
            // row 要素が現れるまで読み飛ばす。
            return;
        }

        StartElement rowStart = event.asStartElement();
        int sourceRow = Integer.parseInt(
                rowStart.getAttributeByName(NONS_QNAME.R).getValue()) - 1;
        int targetRow = targetRows.isEmpty()
                ? Integer.MAX_VALUE
                : targetRows.peek();

        if (sourceRow < targetRow) {
            if (rowStart.getAttributeByName(NONS_QNAME.CUSTOM_FORMAT) != null) {
                buffer.add(removeCustomFormat(rowStart));
                source.nextEvent();
            }

        } else if (targetRow < sourceRow) {
            createRow(targetRow);
            targetRows.remove();

        } else {
            buffer.add(paintRow(rowStart));
            source.nextEvent();
            targetRows.remove();
        }
    }

    /**
     * 着色された新たな row 要素を生成してバッファに追加します。<br>
     * 
     * @param r 行インデックス（0 開始）
     */
    private void createRow(int r) {
        int newStyle = stylesManager.getPaintedStyle(0, colorIdx);

        Set<Attribute> attrs = new HashSet<>();
        attrs.add(eventFactory.createAttribute(NONS_QNAME.R, Integer.toString(r + 1)));
        attrs.add(eventFactory.createAttribute(NONS_QNAME.S, Integer.toString(newStyle)));
        attrs.add(eventFactory.createAttribute(NONS_QNAME.CUSTOM_FORMAT, "1"));

        buffer.add(eventFactory.createStartElement(QNAME.ROW, attrs.iterator(), null));
        buffer.add(eventFactory.createEndElement(QNAME.ROW, null));
    }

    /**
     * row 要素開始イベントを受け取り、適用するスタイルを着色スタイルに変更して返します。<br>
     * 
     * @param original row 要素開始イベント
     * @return 適用するスタイルを着色スタイルに変更した row 要素開始イベント
     */
    private StartElement paintRow(StartElement original) {
        int currStyleIdx = Optional
                .ofNullable(original.getAttributeByName(NONS_QNAME.S))
                .map(Attribute::getValue)
                .map(Integer::parseInt)
                .orElse(0);
        int newStyleIdx = stylesManager.getPaintedStyle(currStyleIdx, colorIdx);

        Map<QName, Attribute> newAttrs = new HashMap<>();
        newAttrs.put(
                NONS_QNAME.S,
                eventFactory.createAttribute(NONS_QNAME.S, Integer.toString(newStyleIdx)));
        newAttrs.put(
                NONS_QNAME.CUSTOM_FORMAT,
                eventFactory.createAttribute(NONS_QNAME.CUSTOM_FORMAT, "1"));

        Iterator<Attribute> itr = original.getAttributes();
        while (itr.hasNext()) {
            Attribute attr = itr.next();
            if (!newAttrs.containsKey(attr.getName())) {
                newAttrs.put(attr.getName(), attr);
            }
        }

        return eventFactory.createStartElement(QNAME.ROW, newAttrs.values().iterator(), null);
    }

    /**
     * row 要素開始イベントの customFormat 属性を削除したイベントを返します。<br>
     * 
     * @param original row 要素開始イベント
     * @return customFormat 属性を削除した row 要素開始イベント
     */
    private StartElement removeCustomFormat(StartElement original) {
        Set<Attribute> attrs = new HashSet<>();
        Iterator<Attribute> itr = original.getAttributes();
        while (itr.hasNext()) {
            Attribute attr = itr.next();
            if (!NONS_QNAME.CUSTOM_FORMAT.equals(attr.getName())) {
                attrs.add(attr);
            }
        }

        return eventFactory.createStartElement(QNAME.ROW, attrs.iterator(), null);
    }
}
