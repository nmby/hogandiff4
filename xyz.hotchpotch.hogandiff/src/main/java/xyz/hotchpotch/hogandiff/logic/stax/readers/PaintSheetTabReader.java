package xyz.hotchpotch.hogandiff.logic.stax.readers;

import java.awt.Color;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import xyz.hotchpotch.hogandiff.logic.stax.StaxUtil;
import xyz.hotchpotch.hogandiff.logic.stax.StaxUtil.NONS_QNAME;
import xyz.hotchpotch.hogandiff.logic.stax.StaxUtil.QNAME;

/**
 * シートの見出しに色を付ける {@link XMLEventReader} の実装です。<br>
 * 具体的には、.xlsx/.xlsm 形式のExcelファイルの各ワークシートに対応する
 * xl/worksheets/sheet?.xml エントリを処理対象とし、
 * {@code <sheetPr>} 要素を追加します。
 * （{@code <sheetPr>} 要素が予め取り除かれていることを前提とします。）<br>
 *
 * @author nmby
 */
public class PaintSheetTabReader extends BufferingReader {
    
    // [static members] ********************************************************
    
    /**
     * 新しいリーダーを構成します。<br>
     * 
     * @param source ソースリーダー
     * @param color  着色する色
     * @return 新しいリーダー
     * @throws XMLStreamException XMLイベントの解析に失敗した場合
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static XMLEventReader of(
            XMLEventReader source,
            Color color)
            throws XMLStreamException {
        
        Objects.requireNonNull(source);
        Objects.requireNonNull(color);
        
        return new PaintSheetTabReader(source, color);
    }
    
    // [instance members] ******************************************************
    
    private final String rgb;
    private boolean auto;
    
    private PaintSheetTabReader(
            XMLEventReader source,
            Color color)
            throws XMLStreamException {
        
        super(source);
        
        assert color != null;
        
        this.rgb = "FF%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue())
                .toUpperCase();
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
        if (!StaxUtil.isStart(event, QNAME.WORKSHEET)) {
            return;
        }
        
        buffer.add(source.nextEvent());
        buffer.add(createStartElement(QNAME.SHEET_PR, Collections.emptyIterator()));
        
        Set<Attribute> attrs = Set.of(eventFactory.createAttribute(NONS_QNAME.RGB, rgb));
        buffer.add(createStartElement(QNAME.TAB_COLOR, attrs.iterator()));
        buffer.add(eventFactory.createEndElement(QNAME.TAB_COLOR, null));
        
        buffer.add(eventFactory.createEndElement(QNAME.SHEET_PR, null));
        
        auto = true;
    }
}
