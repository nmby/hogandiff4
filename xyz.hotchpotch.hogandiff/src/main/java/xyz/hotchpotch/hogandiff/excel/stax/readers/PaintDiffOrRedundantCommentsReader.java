package xyz.hotchpotch.hogandiff.excel.stax.readers;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import xyz.hotchpotch.hogandiff.excel.CellsUtil;
import xyz.hotchpotch.hogandiff.excel.stax.StaxUtil;
import xyz.hotchpotch.hogandiff.excel.stax.StaxUtil.NONS_QNAME;
import xyz.hotchpotch.hogandiff.excel.stax.StaxUtil.V_QNAME;
import xyz.hotchpotch.hogandiff.excel.stax.StaxUtil.X_QNAME;

/**
 * 差分セルコメントおよび余剰セルコメントに色を付ける {@link XMLEventReader} の実装です。<br>
 * 具体的には、.xlsx/.xlsm 形式のExcelファイルの各ワークシートに対応する
 * xl/drawing/vmlDrawing?.vml エントリを処理対象とし、
 * 各種要素に対する操作を行います。<br>
 *
 * @author nmby
 */
public class PaintDiffOrRedundantCommentsReader extends BufferingReader {
    
    // [static members] ********************************************************
    
    private static final XMLEventFactory eventFactory = XMLEventFactory.newFactory();
    
    /**
     * 新しいリーダーを構成します。<br>
     * 
     * @param source ソースリーダー
     * @param diffCommentAddrs 差分セルコメントの位置
     * @param redundantCommentAddrs 余剰セルコメントの位置
     * @param diffCommentColor 差分セルコメントに適用する色
     * @param redundantCommentColor 余剰セルコメントに適用する色
     * @return 新しいリーダー
     * @throws NullPointerException パラメータが {@code null} の場合
     *@throws IllegalArgumentException
     *      {@code diffCommentAddrs}, {@code redundantCommentAddrs} がいずれも空の場合
     */
    public static XMLEventReader of(
            XMLEventReader source,
            Set<String> diffCommentAddrs,
            Set<String> redundantCommentAddrs,
            String diffCommentColor,
            String redundantCommentColor) {
        
        Objects.requireNonNull(source);
        Objects.requireNonNull(diffCommentAddrs);
        Objects.requireNonNull(redundantCommentAddrs);
        Objects.requireNonNull(diffCommentColor);
        Objects.requireNonNull(redundantCommentColor);
        if (diffCommentAddrs.isEmpty() && redundantCommentAddrs.isEmpty()) {
            throw new IllegalArgumentException("no target comments");
        }
        
        return new PaintDiffOrRedundantCommentsReader(
                source,
                diffCommentAddrs,
                redundantCommentAddrs,
                diffCommentColor,
                redundantCommentColor);
    }
    
    // [instance members] ******************************************************
    
    private final Set<String> diffCommentAddrs;
    private final Set<String> redundantCommentAddrs;
    private final String diffCommentColor;
    private final String redundantCommentColor;
    
    private int row;
    private int column;
    private boolean inRow;
    private boolean inColumn;
    
    private PaintDiffOrRedundantCommentsReader(
            XMLEventReader source,
            Set<String> diffCommentAddrs,
            Set<String> redundantCommentAddrs,
            String diffCommentColor,
            String redundantCommentColor) {
        
        super(source);
        
        assert diffCommentAddrs != null;
        assert redundantCommentAddrs != null;
        assert diffCommentColor != null;
        assert redundantCommentColor != null;
        assert !diffCommentAddrs.isEmpty() || !redundantCommentAddrs.isEmpty();
        
        this.diffCommentAddrs = diffCommentAddrs;
        this.redundantCommentAddrs = redundantCommentAddrs;
        this.diffCommentColor = diffCommentColor;
        this.redundantCommentColor = redundantCommentColor;
    }
    
    @Override
    protected void seekNext() throws XMLStreamException {
        if (!source.hasNext()) {
            return;
        }
        
        XMLEvent event = source.peek();
        if (StaxUtil.isStart(event, V_QNAME.SHAPE)
                && "#_x0000_t202".equals(
                        event.asStartElement().getAttributeByName(NONS_QNAME.TYPE).getValue())) {
            
            row = -1;
            column = -1;
            inRow = false;
            inColumn = false;
            Queue<XMLEvent> queue = new ArrayDeque<>();
            
            while (!StaxUtil.isEnd(source.peek(), V_QNAME.SHAPE)) {
                XMLEvent ev = source.nextEvent();
                queue.add(ev);
                
                if (StaxUtil.isStart(ev, X_QNAME.ROW)) {
                    inRow = true;
                    
                } else if (StaxUtil.isEnd(ev, X_QNAME.ROW)) {
                    inRow = false;
                    
                } else if (StaxUtil.isStart(ev, X_QNAME.COLUMN)) {
                    inColumn = true;
                    
                } else if (StaxUtil.isEnd(ev, X_QNAME.COLUMN)) {
                    inColumn = false;
                    
                } else if (ev.isCharacters()) {
                    Characters cs = ev.asCharacters();
                    
                    if (inRow) {
                        row = Integer.parseInt(cs.getData());
                    } else if (inColumn) {
                        column = Integer.parseInt(cs.getData());
                    }
                }
            }
            queue.add(source.nextEvent());
            
            if (row < 0 || column < 0) {
                throw new AssertionError("no row or column element.");
            }
            String address = CellsUtil.idxToAddress(row, column);
            
            if (diffCommentAddrs.contains(address)) {
                processCommentShape(queue, diffCommentColor);
            } else if (redundantCommentAddrs.contains(address)) {
                processCommentShape(queue, redundantCommentColor);
            } else {
                buffer.addAll(queue);
            }
        }
    }
    
    private void processCommentShape(Queue<XMLEvent> events, String color) {
        XMLEvent first = events.poll();
        assert StaxUtil.isStart(first, V_QNAME.SHAPE);
        buffer.add(createNewShape(first.asStartElement(), color));
        
        while (!StaxUtil.isEnd(events.peek(), X_QNAME.CLIENT_DATA)) {
            buffer.add(events.poll());
        }
        
        buffer.add(eventFactory.createStartElement(X_QNAME.VISIBLE, null, null));
        buffer.add(eventFactory.createEndElement(X_QNAME.VISIBLE, null));
        
        buffer.addAll(events);
    }
    
    private StartElement createNewShape(StartElement originalShape, String color) {
        Map<QName, Attribute> attrs = new HashMap<>();
        originalShape.getAttributes().forEachRemaining(attr -> attrs.put(attr.getName(), attr));
        
        String orgStyle = attrs.get(NONS_QNAME.STYLE).getValue();
        String newStyle = orgStyle.replace("visibility:hidden", "visibility:visible");
        attrs.put(NONS_QNAME.STYLE, eventFactory.createAttribute(NONS_QNAME.STYLE, newStyle));
        
        attrs.put(NONS_QNAME.FILL_COLOR, eventFactory.createAttribute(NONS_QNAME.FILL_COLOR, color));
        
        return eventFactory.createStartElement(V_QNAME.SHAPE, attrs.values().iterator(), null);
    }
}
