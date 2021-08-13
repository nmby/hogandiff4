package xyz.hotchpotch.hogandiff.excel.common;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.CellsUtil;
import xyz.hotchpotch.hogandiff.excel.SComparator;
import xyz.hotchpotch.hogandiff.excel.SResult;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * {@link SComparator} の標準的な実装です。<br>
 *
 * @author nmby
 */
public class SComparatorImpl implements SComparator {
    
    // [static members] ********************************************************
    
    /**
     * 行同士または列同士の対応関係を決定するマッパーを表します。<br>
     * これは、{@link #makePairs(Set, Set)} を関数メソッドに持つ関数型インタフェースです。<br>
     *
     * @author nmby
     */
    @FunctionalInterface
    private static interface Mapper {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        /**
         * 行同士または列同士の対応関係を決定し、
         * インデックスのペアのリストとして返します、<br>
         * 
         * @param cells1 セルセット1
         * @param cells2 セルセット2
         * @return 行同士または列同士の対応関係
         */
        List<Pair<Integer>> makePairs(
                Set<CellData> cells1,
                Set<CellData> cells2);
    }
    
    /**
     *  縦方向の余剰／欠損を考慮しない場合のマッパーを返します。<br>
     * 
     * @param verticality 縦方向の座標を抽出する関数
     * @return 縦方向の要素同士を対応付けるマッパー
     */
    private static Mapper mapper(
            ToIntFunction<CellData> verticality) {
        
        assert verticality != null;
        
        return (cells1, cells2) -> {
            assert cells1 != null;
            assert cells2 != null;
            assert cells1 != cells2;
            
            Pair<Integer> range = range(cells1, cells2, verticality);
            return IntStream.rangeClosed(range.a(), range.b())
                    .mapToObj(n -> Pair.of(n, n))
                    .toList();
        };
    }
    
    /**
     * 縦方向の余剰／欠損を考慮する場合のマッパーを返します。<br>
     * 
     * @param <U> 横方向のソートキーの型
     * @param verticality 縦方向の座標を抽出する関数
     * @param comparator 横方向のソートキーをソートするための比較関数
     * @return 縦方向の要素同士を対応付けるマッパー
     */
    private static <U> Mapper mapper(
            ToIntFunction<CellData> verticality,
            Comparator<CellData> comparator) {
        
        assert verticality != null;
        assert comparator != null;
        
        return (cells1, cells2) -> {
            assert cells1 != null;
            assert cells2 != null;
            assert cells1 != cells2;
            
            int start = range(cells1, cells2, verticality).a();
            List<List<CellData>> cellsList1 = convert(
                    cells1, start, verticality, comparator);
            List<List<CellData>> cellsList2 = convert(
                    cells2, start, verticality, comparator);
            
            Matcher<List<CellData>> matcher = Matcher.minimumEditDistanceMatcherOf(
                    List::size,
                    (list1, list2) -> evaluateDiff(list1, list2, comparator));
            
            return matcher.makePairs(cellsList1, cellsList2).stream()
                    .map(p -> p.map(i -> i + start))
                    .toList();
        };
    }
    
    private static <U> int evaluateDiff(
            List<CellData> list1,
            List<CellData> list2,
            Comparator<CellData> comparator) {
        
        assert list1 != null;
        assert list2 != null;
        assert list1 != list2;
        assert comparator != null;
        
        Iterator<CellData> itr1 = list1.iterator();
        Iterator<CellData> itr2 = list2.iterator();
        
        int diff = 0;
        int comp = 0;
        CellData cell1 = null;
        CellData cell2 = null;
        
        while (itr1.hasNext() && itr2.hasNext()) {
            if (comp <= 0) {
                cell1 = itr1.next();
            }
            if (0 <= comp) {
                cell2 = itr2.next();
            }
            comp = comparator.compare(cell1, cell2);
            if (comp == 0 && !cell1.dataEquals(cell2)) {
                diff += 2;
            } else if (comp != 0) {
                diff++;
            }
        }
        while (itr1.hasNext()) {
            diff++;
            itr1.next();
        }
        while (itr2.hasNext()) {
            diff++;
            itr2.next();
        }
        
        return diff;
    }
    
    /**
     * セルのセットを、セルのリストのリストに変換します。<br>
     * 一次元目のリストは verticality でソートしたものであり、
     * 二次元目のリストは同一 verticality 値のものを
     * extractor と comparator でソートしたものです。<br>
     * 
     * @param <U> 横方向のソートキーの型
     * @param cells セルセット
     * @param start セルセットのリスト化を始める最小インデックス値
     * @param verticality 縦方向の値（行または列）の抽出関数
     * @param comparator 横方向のソートキーの比較関数
     * @return セルのリストのリスト
     */
    private static <U> List<List<CellData>> convert(
            Set<CellData> cells,
            int start,
            ToIntFunction<CellData> verticality,
            Comparator<CellData> comparator) {
        
        assert cells != null;
        assert 0 <= start;
        assert verticality != null;
        assert comparator != null;
        
        Map<Integer, List<CellData>> map = cells.stream()
                .collect(Collectors.groupingBy(verticality::applyAsInt));
        
        int end = range(cells, verticality).b();
        return IntStream.rangeClosed(start, end).parallel()
                .mapToObj(i -> {
                    if (map.containsKey(i)) {
                        List<CellData> list = map.get(i);
                        list.sort(comparator);
                        return list;
                    } else {
                        return List.<CellData> of();
                    }
                })
                .toList();
    }
    
    private static Pair<Integer> range(
            Set<CellData> cells,
            ToIntFunction<CellData> axis) {
        
        assert cells != null;
        assert axis != null;
        
        int min = cells.stream()
                .mapToInt(axis)
                .min().orElse(0);
        int max = cells.stream()
                .mapToInt(axis)
                .max().orElse(0);
        
        return Pair.of(min, max);
    }
    
    private static Pair<Integer> range(
            Set<CellData> cells1,
            Set<CellData> cells2,
            ToIntFunction<CellData> axis) {
        
        assert cells1 != null;
        assert cells2 != null;
        assert cells1 != cells2;
        assert axis != null;
        
        Pair<Integer> range1 = range(cells1, axis);
        Pair<Integer> range2 = range(cells2, axis);
        
        return Pair.of(
                Math.min(range1.a(), range2.a()),
                Math.max(range1.b(), range2.b()));
    }
    
    /**
     * 新しいコンパレータを返します。<br>
     * 
     * @param considerRowGaps 比較において行の余剰／欠損を考慮する場合は {@code true}
     * @param considerColumnGaps 比較において列の余剰／欠損を考慮する場合は {@code true}
     * @param compareCellContents 比較においてセル内容を比較する場合は {@code true}
     * @param compareCellComments 比較においてセルコメントを比較する場合は {@code true}
     * @param saveMemory 省メモリモードの場合は {@code true}
     * @return 新しいコンパレータ
     */
    public static SComparator of(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean compareCellContents,
            boolean compareCellComments,
            boolean saveMemory) {
        
        return new SComparatorImpl(
                considerRowGaps,
                considerColumnGaps,
                compareCellContents,
                compareCellComments,
                saveMemory);
    }
    
    // [instance members] ******************************************************
    
    private final boolean considerRowGaps;
    private final boolean considerColumnGaps;
    private final boolean compareCellContents;
    private final boolean compareCellComments;
    private final boolean saveMemory;
    private final Mapper rowsMapper;
    private final Mapper columnsMapper;
    
    private SComparatorImpl(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean compareCellContents,
            boolean compareCellComments,
            boolean saveMemory) {
        
        this.considerRowGaps = considerRowGaps;
        this.considerColumnGaps = considerColumnGaps;
        this.compareCellContents = compareCellContents;
        this.compareCellComments = compareCellComments;
        this.saveMemory = saveMemory;
        
        if (considerRowGaps && considerColumnGaps) {
            rowsMapper = mapper(CellData::row, CellData::dataCompareTo);
            columnsMapper = mapper(CellData::column, CellData::dataCompareTo);
        } else if (considerRowGaps) {
            rowsMapper = mapper(CellData::row, Comparator.comparingInt(CellData::column));
            columnsMapper = mapper(CellData::column);
        } else if (considerColumnGaps) {
            rowsMapper = mapper(CellData::row);
            columnsMapper = mapper(CellData::column, Comparator.comparingInt(CellData::row));
        } else {
            rowsMapper = mapper(CellData::row);
            columnsMapper = mapper(CellData::column);
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code cells1}, {@code cells2} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code cells1}, {@code cells2} が同一インスタンスの場合
     */
    @Override
    public SResult compare(
            Set<CellData> cells1,
            Set<CellData> cells2) {
        
        Objects.requireNonNull(cells1, "cells1");
        Objects.requireNonNull(cells2, "cells2");
        
        if (cells1 == cells2) {
            if (cells1.isEmpty()) {
                return new SResult(
                        considerRowGaps,
                        considerColumnGaps,
                        compareCellContents,
                        compareCellComments,
                        Pair.of(List.of(), List.of()),
                        Pair.of(List.of(), List.of()),
                        List.of());
            } else {
                throw new IllegalArgumentException("cells1 == cells2");
            }
        }
        
        List<Pair<Integer>> rowPairs = rowsMapper.makePairs(cells1, cells2);
        List<Pair<Integer>> columnPairs = columnsMapper.makePairs(cells1, cells2);
        
        // 余剰行の収集
        List<Integer> redundantRows1 = rowPairs.stream()
                .filter(Pair::isOnlyA).map(Pair::a).toList();
        List<Integer> redundantRows2 = rowPairs.stream()
                .filter(Pair::isOnlyB).map(Pair::b).toList();
        
        // 余剰列の収集
        List<Integer> redundantColumns1 = columnPairs.stream()
                .filter(Pair::isOnlyA).map(Pair::a).toList();
        List<Integer> redundantColumns2 = columnPairs.stream()
                .filter(Pair::isOnlyB).map(Pair::b).toList();
        
        // 差分セルの収集
        List<Pair<CellData>> diffCells = extractDiffs(
                cells1, cells2, rowPairs, columnPairs);
        
        return new SResult(
                considerRowGaps,
                considerColumnGaps,
                compareCellContents,
                compareCellComments,
                Pair.of(redundantRows1, redundantRows2),
                Pair.of(redundantColumns1, redundantColumns2),
                diffCells);
    }
    
    private List<Pair<CellData>> extractDiffs(
            Set<CellData> cells1,
            Set<CellData> cells2,
            List<Pair<Integer>> rowPairs,
            List<Pair<Integer>> columnPairs) {
        
        assert cells1 != null;
        assert cells2 != null;
        assert cells1 != cells2;
        assert rowPairs != null;
        assert columnPairs != null;
        
        // TODO: 要処理内容見直し
        
        Map<String, CellData> map1 = cells1.stream()
                .collect(Collectors.toMap(CellData::address, Function.identity()));
        Map<String, CellData> map2 = cells2.stream()
                .collect(Collectors.toMap(CellData::address, Function.identity()));
        
        return rowPairs.parallelStream().filter(Pair::isPaired).flatMap(rp -> {
            int row1 = rp.a();
            int row2 = rp.b();
            
            return columnPairs.stream().filter(Pair::isPaired).map(cp -> {
                int column1 = cp.a();
                int column2 = cp.b();
                String address1 = CellsUtil.idxToAddress(row1, column1);
                String address2 = CellsUtil.idxToAddress(row2, column2);
                CellData cell1 = map1.get(address1);
                CellData cell2 = map2.get(address2);
                
                return (cell1 == null ? cell2 == null : cell1.dataEquals(cell2))
                        ? null
                        : Pair.<CellData> of(
                                cell1 != null ? cell1 : CellData.empty(row1, column1, saveMemory),
                                cell2 != null ? cell2 : CellData.empty(row2, column2, saveMemory));
            });
        }).filter(Objects::nonNull).toList();
    }
}
