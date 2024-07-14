package xyz.hotchpotch.hogandiff.excel.stax.readers;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
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
import xyz.hotchpotch.hogandiff.excel.stax.XSSFBookPainterWithStax.StylesManager;
import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 余剰列に色を付ける {@link XMLEventReader} の実装です。<br>
 * 具体的には、.xlsx/.xlsm 形式のExcelファイルの各ワークシートに対応する
 * xl/worksheets/sheet?.xml エントリを処理対象とし、
 * {@code <cols>} 要素およびその子要素である {@code <col>} 要素に対する
 * 操作を行います。<br>
 *
 * @author nmby
 */
public class PaintColumnsReader extends BufferingReader {
    
    // [static members] ********************************************************
    
    private static final XMLEventFactory eventFactory = XMLEventFactory.newFactory();
    
    /**
     * 新しいリーダーを構成します。<br>
     * 
     * @param source ソースリーダー
     * @param stylesManager スタイルマネージャ
     * @param targetColumns 着色対象の列インデックス（0 開始）
     * @param colorIdx 着色する色のインデックス
     * @return 新しいリーダー
     * @throws NullPointerException パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code targetColumns} の長さが 0 の場合
     */
    public static XMLEventReader of(
            XMLEventReader source,
            StylesManager stylesManager,
            List<Integer> targetColumns,
            short colorIdx) {
        
        Objects.requireNonNull(source);
        Objects.requireNonNull(stylesManager);
        Objects.requireNonNull(targetColumns);
        if (targetColumns.isEmpty()) {
            throw new IllegalArgumentException("no target columns");
        }
        
        return new PaintColumnsReader(
                source,
                stylesManager,
                targetColumns,
                colorIdx);
    }
    
    // [instance members] ******************************************************
    
    private final StylesManager stylesManager;
    private final Deque<IntPair> targetRanges = new ArrayDeque<>();
    private final short colorIdx;
    private boolean auto = false;
    
    private PaintColumnsReader(
            XMLEventReader source,
            StylesManager stylesManager,
            List<Integer> targetColumns,
            short colorIdx) {
        
        super(source);
        
        assert stylesManager != null;
        assert targetColumns != null;
        assert !targetColumns.isEmpty();
        
        this.stylesManager = stylesManager;
        this.colorIdx = colorIdx;
        
        int start = -1;
        int end = -1;
        for (int i : targetColumns) {
            if (start == -1) {
                start = i;
                end = i;
            } else if (end + 1 == i) {
                end = i;
            } else if (end + 1 < i) {
                targetRanges.add(IntPair.of(start, end));
                start = i;
                end = i;
            } else {
                throw new AssertionError();
            }
        }
        targetRanges.add(IntPair.of(start, end));
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
        if (StaxUtil.isStart(event, QNAME.SHEET_DATA)) {
            // 元ファイルに cols 要素が存在しない場合は、
            // cols 要素を作成して着色列分の col 要素を追加する。
            buffer.add(eventFactory.createStartElement(QNAME.COLS, Collections.emptyIterator(), null));
            targetRanges.forEach(this::createCol);
            buffer.add(eventFactory.createEndElement(QNAME.COLS, null));
            auto = true;
            return;
        }
        if (!StaxUtil.isStart(event, QNAME.COL)) {
            // col 要素が現れるまで読み飛ばす。
            return;
        }
        
        Deque<XMLEvent> nextCol = new ArrayDeque<>();
        IntPair sourceRange = supplyCol(nextCol);
        IntPair targetRange = targetRanges.removeFirst();
        
        // 実装の容易さを優先し、cols 要素内の全 col 要素を一気に片付けてしまうことにする。
        // col 要素の数は高が知れているので、メモリ消費量は問題にならないはず。
        
        while (targetRange != null && sourceRange != null) {
            
            if (targetRange.b() < sourceRange.a()) {
                // targetRange  +-----+
                // sourceRange           +-----+ 
                
                createCol(targetRange);
                targetRange = targetRanges.pollFirst();
                
            } else if (sourceRange.b() < targetRange.a()) {
                // targetRange           +-----+
                // sourceRange  +-----+ 
                
                buffer.addAll(nextCol);
                nextCol.clear();
                
                event = source.peek();
                if (StaxUtil.isStart(event, QNAME.COL)) {
                    sourceRange = supplyCol(nextCol);
                } else {
                    sourceRange = null;
                }
                
            } else if (targetRange.a() < sourceRange.a()) {
                // targetRange  +------...
                // sourceRange      +--...
                
                createCol(targetRange.a(), sourceRange.a() - 1);
                targetRange = IntPair.of(sourceRange.a(), targetRange.b());
                
            } else if (sourceRange.a() < targetRange.a()) {
                // targetRange      +--...
                // sourceRange  +------...
                
                StartElement start = nextCol.remove().asStartElement();
                Queue<XMLEvent> copyCol = new ArrayDeque<>(nextCol);
                
                buffer.add(modifyCol(start, sourceRange.a(), targetRange.a() - 1, false));
                buffer.addAll(copyCol);
                nextCol.addFirst(modifyCol(start, targetRange.a(), sourceRange.b(), false));
                
                sourceRange = IntPair.of(targetRange.a(), sourceRange.b());
                
            } else if (targetRange.b() < sourceRange.b()) {
                // targetRange  +---+
                // sourceRange  +------+
                
                StartElement start = nextCol.remove().asStartElement();
                Queue<XMLEvent> copyCol = new ArrayDeque<>(nextCol);
                
                buffer.add(modifyCol(start, targetRange.a(), targetRange.b(), true));
                buffer.addAll(copyCol);
                nextCol.addFirst(modifyCol(start, targetRange.b() + 1, sourceRange.b(), false));
                
                sourceRange = IntPair.of(targetRange.b() + 1, sourceRange.b());
                targetRange = targetRanges.pollFirst();
                
            } else if (sourceRange.b() < targetRange.b()) {
                // targetRange  +------+
                // sourceRange  +---+
                
                StartElement start = nextCol.remove().asStartElement();
                
                buffer.add(modifyCol(start, sourceRange.a(), sourceRange.b(), true));
                buffer.addAll(nextCol);
                nextCol.clear();
                
                targetRange = IntPair.of(sourceRange.b() + 1, targetRange.b());
                event = source.peek();
                if (StaxUtil.isStart(event, QNAME.COL)) {
                    sourceRange = supplyCol(nextCol);
                } else {
                    sourceRange = null;
                }
                
            } else {
                // targetRange  +------+
                // sourceRange  +------+
                
                StartElement start = nextCol.remove().asStartElement();
                
                buffer.add(modifyCol(start, sourceRange.a(), sourceRange.b(), true));
                buffer.addAll(nextCol);
                nextCol.clear();
                
                event = source.peek();
                if (StaxUtil.isStart(event, QNAME.COL)) {
                    sourceRange = supplyCol(nextCol);
                } else {
                    sourceRange = null;
                }
                targetRange = targetRanges.pollFirst();
            }
        }
        
        if (targetRange != null) {
            createCol(targetRange);
            targetRanges.forEach(this::createCol);
        }
        if (sourceRange != null) {
            buffer.addAll(nextCol);
        }
        auto = true;
    }
    
    // ↓こーいう関数チックじゃない副作用もりもりのメソッドは嫌なんだけど・・・
    // ↓良くないのは分かってるんだけど・・・許して・・・
    
    /**
     * ソースリーダーから次の col 要素を構成する一連のイベントを読み取り、
     * 引数で渡されたデックに格納します。読み取ったイベントはソースから削除します。<br>
     * 加えて、次の col 要素の範囲を表すペアを戻り値として返します。<br>
     * <br>
     * このメソッドは、ソースリーダーの次のイベントが col の開始要素であることを
     * 前提としています。<br>
     * 
     * @param nextCol 次の col 要素を構成する一連のイベントを格納するためのデック
     * @return 次の col 要素の範囲を表すペア
     * @throws XMLStreamException XMLイベントの解析に失敗した場合
     */
    private IntPair supplyCol(Deque<XMLEvent> nextCol) throws XMLStreamException {
        XMLEvent event;
        do {
            event = source.nextEvent();
            nextCol.add(event);
        } while (!StaxUtil.isEnd(event, QNAME.COL));
        
        return IntPair.of(
                Integer.parseInt(nextCol.getFirst().asStartElement()
                        .getAttributeByName(NONS_QNAME.MIN).getValue()) - 1,
                Integer.parseInt(nextCol.getFirst().asStartElement()
                        .getAttributeByName(NONS_QNAME.MAX).getValue()) - 1);
    }
    
    private void createCol(IntPair range) {
        createCol(range.a(), range.b());
    }
    
    /**
     * 新しい col 要素を構成するイベントを生成してバッファに追加します。<br>
     * 
     * @param start 開始列のインデックス（0 開始）
     * @param end 終了列のインデックス（0 開始）
     */
    private void createCol(int start, int end) {
        int newStyle = stylesManager.getPaintedStyle(0, colorIdx);
        
        Set<Attribute> attrs = new HashSet<>();
        attrs.add(eventFactory.createAttribute(NONS_QNAME.MIN, Integer.toString(start + 1)));
        attrs.add(eventFactory.createAttribute(NONS_QNAME.MAX, Integer.toString(end + 1)));
        attrs.add(eventFactory.createAttribute(NONS_QNAME.STYLE, Integer.toString(newStyle)));
        // Excelのデフォルトの列幅は 8.47 らしいが、"8.47" を指定するとより狭い幅になってしまい、
        // "9.1" を指定すると 8.47 になる。Apache POI は魔訶不識である。
        attrs.add(eventFactory.createAttribute(NONS_QNAME.WIDTH, "9.1"));
        
        buffer.add(eventFactory.createStartElement(QNAME.COL, attrs.iterator(), null));
        buffer.add(eventFactory.createEndElement(QNAME.COL, null));
    }
    
    /**
     * 指定された col 開始要素をコピーして新たな要素を生成し、戻り値として返します。<br>
     * 新しい要素の属性は、次の属性を除きオリジナルからコピーされます。<br>
     * <ul>
     *   <li>{@code min} : {@code start + 1} が設定されます。</li>
     *   <li>{@code max} : {@code end + 1} が設定されます。</li>
     *   <li>{@code style} : {@code paint} が {@code true} の場合、
     *                       オリジナルのスタイルにセル背景色を加えたスタイルが設定されます。</li>
     * </ul>
     * 
     * @param original コピー元の col 開始要素
     * @param start 開始列のインデックス（0 開始）
     * @param end 終了列のインデックス（0 開始）
     * @param paint 色を付ける場合は {@code true}
     * @return 新しい col 開始要素
     */
    private StartElement modifyCol(StartElement original, int start, int end, boolean paint) {
        Map<QName, Attribute> newAttrs = new HashMap<>();
        newAttrs.put(
                NONS_QNAME.MIN,
                eventFactory.createAttribute(NONS_QNAME.MIN, Integer.toString(start + 1)));
        newAttrs.put(
                NONS_QNAME.MAX,
                eventFactory.createAttribute(NONS_QNAME.MAX, Integer.toString(end + 1)));
        
        if (paint) {
            int currStyleIdx = Optional
                    .ofNullable(original.getAttributeByName(NONS_QNAME.STYLE))
                    .map(Attribute::getValue)
                    .map(Integer::parseInt)
                    .orElse(0);
            int newStyleIdx = stylesManager.getPaintedStyle(currStyleIdx, colorIdx);
            newAttrs.put(
                    NONS_QNAME.STYLE,
                    eventFactory.createAttribute(NONS_QNAME.STYLE, Integer.toString(newStyleIdx)));
        }
        
        Iterator<Attribute> itr = original.getAttributes();
        while (itr.hasNext()) {
            Attribute attr = itr.next();
            if (!newAttrs.containsKey(attr.getName())) {
                newAttrs.put(attr.getName(), attr);
            }
        }
        
        return eventFactory.createStartElement(QNAME.COL, newAttrs.values().iterator(), null);
    }
}
