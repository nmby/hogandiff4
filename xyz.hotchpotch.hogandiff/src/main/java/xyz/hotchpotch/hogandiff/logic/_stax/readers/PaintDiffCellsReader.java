package xyz.hotchpotch.hogandiff.logic._stax.readers;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import xyz.hotchpotch.hogandiff.logic.CellData;
import xyz.hotchpotch.hogandiff.logic._plain.CellsUtil;
import xyz.hotchpotch.hogandiff.logic._stax.StaxUtil;
import xyz.hotchpotch.hogandiff.logic._stax.PainterWithStax.StylesManager;
import xyz.hotchpotch.hogandiff.logic._stax.StaxUtil.NONS_QNAME;
import xyz.hotchpotch.hogandiff.logic._stax.StaxUtil.QNAME;

/**
 * 差分セルに色を付ける {@link XMLEventReader} の実装です。<br>
 * 具体的には、.xlsx/.xlsm 形式のExcelファイルの各ワークシートに対応する
 * xl/worksheets/sheet?.xml エントリを処理対象とし、
 * {@code <c>} 要素に対する操作を行います。<br>
 *
 * @author nmby
 */
public class PaintDiffCellsReader extends BufferingReader {

    // [static members] ********************************************************

    private static final XMLEventFactory eventFactory = XMLEventFactory.newFactory();

    private static final Comparator<CellData> cellSorter = (c1, c2) -> {
        if (c1.row() != c2.row()) {
            return c1.row() < c2.row() ? -1 : 1;
        }
        if (c1.column() != c2.column()) {
            return c1.column() < c2.column() ? -1 : 1;
        }
        return 0;
    };

    /**
     * 新しいリーダーを構成します。<br>
     * 
     * @param source           ソースリーダー
     * @param stylesManager    スタイルマネージャ
     * @param diffCellContents 差分セル
     * @param colorIdx         着色する色のインデックス
     * @return 新しいリーダー
     * @throws NullPointerException     パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code diffCellContents} が空の場合
     */
    public static XMLEventReader of(
            XMLEventReader source,
            StylesManager stylesManager,
            List<CellData> diffCellContents,
            short colorIdx) {

        Objects.requireNonNull(source);
        Objects.requireNonNull(stylesManager);
        Objects.requireNonNull(diffCellContents);
        if (diffCellContents.isEmpty()) {
            throw new IllegalArgumentException("no target cells");
        }

        return new PaintDiffCellsReader(
                source,
                stylesManager,
                diffCellContents,
                colorIdx);
    }

    // [instance members] ******************************************************

    private final StylesManager stylesManager;
    private final Map<Integer, Queue<String>> diffAddresses;
    private final Queue<Integer> targetRows;
    private final short colorIdx;
    private boolean auto = false;

    private PaintDiffCellsReader(
            XMLEventReader source,
            StylesManager stylesManager,
            List<CellData> diffCellContents,
            short colorIdx) {

        super(source);

        assert stylesManager != null;
        assert diffCellContents != null;
        assert !diffCellContents.isEmpty();

        this.stylesManager = stylesManager;
        this.diffAddresses = diffCellContents.stream()
                .sorted(cellSorter)
                .collect(Collectors.groupingBy(
                        CellData::row,
                        Collectors.mapping(
                                CellData::address,
                                Collectors.toCollection(ArrayDeque::new))));
        this.targetRows = diffAddresses.keySet().stream()
                .sorted()
                .collect(Collectors.toCollection(ArrayDeque::new));
        this.colorIdx = colorIdx;
    }

    @Override
    protected void seekNext() throws XMLStreamException {
        // FIXME: [No.X 内部実装改善] 我ながらチョー読みにくいのでどうにかしたい

        if (auto || !source.hasNext()) {
            return;
        }
        XMLEvent event = source.peek();
        if (!StaxUtil.isStart(event, QNAME.ROW)) {
            return;
        }

        int sourceRow = Integer.parseInt(
                event.asStartElement().getAttributeByName(NONS_QNAME.R).getValue()) - 1;
        int targetRow = targetRows.peek();

        if (sourceRow < targetRow) {
            // nop
            return;
        }

        Queue<String> addrs = diffAddresses.get(targetRow);

        if (targetRow < sourceRow) {
            createRowStart(targetRow);
            addrs.forEach(this::createCell);
            createRowEnd();

            targetRows.remove();
            if (targetRows.isEmpty()) {
                auto = true;
            }
            return;
        }

        // row 要素開始イベントをバッファに逃がす
        buffer.add(source.nextEvent());

        Queue<Queue<XMLEvent>> sourceCs = groupingCEvents();
        Queue<XMLEvent> nextC = sourceCs.poll();
        String addr = addrs.poll();

        while (nextC != null && addr != null) {
            int sourceColumn = CellsUtil.addressToIdx(
                    nextC.peek().asStartElement().getAttributeByName(NONS_QNAME.R).getValue()).b();
            int targetColumn = CellsUtil.addressToIdx(addr).b();

            if (targetColumn < sourceColumn) {
                createCell(addr);
                addr = addrs.poll();

            } else if (sourceColumn < targetColumn) {
                buffer.addAll(nextC);
                nextC = sourceCs.poll();

            } else {
                buffer.add(paintCell(nextC.remove().asStartElement()));
                buffer.addAll(nextC);
                addr = addrs.poll();
                nextC = sourceCs.poll();
            }
        }

        if (nextC != null) {
            buffer.addAll(nextC);
            sourceCs.forEach(buffer::addAll);

        } else if (addr != null) {
            createCell(addr);
            addrs.forEach(this::createCell);
        }

        targetRows.remove();
        if (targetRows.isEmpty()) {
            auto = true;
        }
    }

    /**
     * ひとつの r 要素に含まれる c 要素ほか一連の子要素を、
     * c 要素ごとにグルーピングして返します。<br>
     * 
     * @return c 要素ごとにグルーピングされたイベント
     * @throws XMLStreamException
     */
    private Queue<Queue<XMLEvent>> groupingCEvents() throws XMLStreamException {
        Queue<Queue<XMLEvent>> sourceCs = new ArrayDeque<>();

        XMLEvent event = source.peek();
        while (StaxUtil.isStart(event, QNAME.C)) {
            Queue<XMLEvent> events = new ArrayDeque<>();

            while (!StaxUtil.isEnd(event, QNAME.C)) {
                event = source.nextEvent();
                events.add(event);
            }
            sourceCs.add(events);
            event = source.peek();
        }
        return sourceCs;
    }

    /**
     * row 要素開始イベントを作成してバッファに追加します。<br>
     * 
     * @param r 行インデックス（0 開始）
     */
    private void createRowStart(int r) {
        Set<Attribute> attrs = new HashSet<>();
        attrs.add(eventFactory.createAttribute(NONS_QNAME.R, Integer.toString(r + 1)));
        buffer.add(eventFactory.createStartElement(QNAME.ROW, attrs.iterator(), null));
    }

    /**
     * row 要素終了イベントを作成してバッファに追加します。<br>
     */
    private void createRowEnd() {
        buffer.add(eventFactory.createEndElement(QNAME.ROW, null));
    }

    /**
     * c 要素開始／終了イベントを作成してバッファに追加します。<br>
     * 
     * @param addr
     */
    private void createCell(String addr) {
        int newStyle = stylesManager.getPaintedStyle(0, colorIdx);

        Set<Attribute> attrs = new HashSet<>();
        attrs.add(eventFactory.createAttribute(NONS_QNAME.R, addr));
        attrs.add(eventFactory.createAttribute(NONS_QNAME.S, Integer.toString(newStyle)));

        buffer.add(eventFactory.createStartElement(QNAME.C, attrs.iterator(), null));
        buffer.add(eventFactory.createEndElement(QNAME.C, null));
    }

    /**
     * c 要素開始イベントを受け取り、適用するスタイルを着色スタイルに変更して返します。<br>
     * 
     * @param original c 要素開始イベント
     * @return 適用するスタイルを着色スタイルに変更した c 要素開始イベント
     */
    private StartElement paintCell(StartElement original) {
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

        Iterator<Attribute> itr = original.getAttributes();
        while (itr.hasNext()) {
            Attribute attr = itr.next();
            if (!newAttrs.containsKey(attr.getName())) {
                newAttrs.put(attr.getName(), attr);
            }
        }

        return eventFactory.createStartElement(QNAME.C, newAttrs.values().iterator(), null);
    }
}
