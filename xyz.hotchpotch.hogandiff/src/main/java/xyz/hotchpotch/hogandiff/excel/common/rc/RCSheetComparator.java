package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.CellsUtil;
import xyz.hotchpotch.hogandiff.excel.SheetComparator;
import xyz.hotchpotch.hogandiff.excel.SheetResult;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 行同士の対応関係と列同士の対応関係をそれぞれ求めることによりセル同士の対応関係を決定する
 * {@link SheetComparator} の実装です。<br>
 *
 * @author nmby
 */
public class RCSheetComparator implements SheetComparator {
    
    // [static members] ********************************************************
    
    private static final int[] EMPTY_INT_ARRAY = new int[] {};
    private static final Pair<int[]> EMPTY_INT_ARRAY_PAIR = new Pair<>(EMPTY_INT_ARRAY, EMPTY_INT_ARRAY);
    
    /**
     * 新たなコンパレータを返します。<br>
     * 
     * @param considerRowGaps 行の挿入／削除を考慮する場合は {@code true}
     * @param considerColumnGaps 列の挿入／削除を考慮する場合は {@code true}
     * @param prioritizeSpeed 比較処理の速度を優先する場合は {@code true}
     * @return 新たなコンパレータ
     */
    public static RCSheetComparator of(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean prioritizeSpeed) {
        
        return new RCSheetComparator(
                RCMatcher.of(
                        considerRowGaps,
                        considerColumnGaps,
                        prioritizeSpeed));
    }
    
    // [instance members] ******************************************************
    
    private final RCMatcher rcMatcher;
    
    private RCSheetComparator(RCMatcher rcMatcher) {
        assert rcMatcher != null;
        
        this.rcMatcher = rcMatcher;
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
    public SheetResult compare(Pair<Set<CellData>> cellsSets) {
        Objects.requireNonNull(cellsSets, "cellsSets");
        
        if (cellsSets.a() == cellsSets.b()) {
            if (cellsSets.a().isEmpty()) {
                return new SheetResult(
                        cellsSets,
                        EMPTY_INT_ARRAY_PAIR,
                        EMPTY_INT_ARRAY_PAIR,
                        List.of());
            } else {
                throw new IllegalArgumentException("cells1 == cells2");
            }
        }
        
        Pair<List<IntPair>> pairs = rcMatcher.make2Pairs(cellsSets);
        List<IntPair> rowPairs = pairs.a();
        List<IntPair> columnPairs = pairs.b();
        
        // 余剰行の収集
        Pair<int[]> redundantRows = Side.map(side -> rowPairs.stream()
                .filter(pair -> pair.isOnly(side))
                .mapToInt(pair -> pair.get(side))
                .toArray());
        
        // 余剰列の収集
        Pair<int[]> redundantColumns = Side.map(side -> columnPairs.stream()
                .filter(pair -> pair.isOnly(side))
                .mapToInt(pair -> pair.get(side))
                .toArray());
        
        // 差分セルの収集
        List<Pair<CellData>> diffCells = extractDiffs(cellsSets, rowPairs, columnPairs);
        
        return new SheetResult(
                cellsSets,
                redundantRows,
                redundantColumns,
                diffCells);
    }
    
    private List<Pair<CellData>> extractDiffs(
            Pair<Set<CellData>> cellsSets,
            List<IntPair> rowPairs,
            List<IntPair> columnPairs) {
        
        assert cellsSets != null;
        assert cellsSets.a() != cellsSets.b();
        assert rowPairs != null;
        assert columnPairs != null;
        
        Pair<Map<String, CellData>> maps = cellsSets.map(cells -> cells.stream()
                .collect(Collectors.toMap(CellData::address, Function.identity())));
        
        List<IntPair> columnPairsFiltered = columnPairs.stream().filter(IntPair::isPaired).toList();
        
        return rowPairs.parallelStream().filter(IntPair::isPaired).flatMap(
                rows -> columnPairsFiltered.stream().map(columns -> {
                    Pair<String> addresses = Side.map(
                            side -> CellsUtil.idxToAddress(rows.get(side), columns.get(side)));
                    Pair<CellData> cells = Side.map(side -> maps.get(side).get(addresses.get(side)));
                    
                    return (cells.a() != null && cells.b() != null && cells.a().dataEquals(cells.b())
                            || cells.a() == null && cells.b() == null)
                                    ? null
                                    : Side.map(side -> cells.get(side) != null
                                            ? cells.get(side)
                                            : CellData.empty(rows.get(side), columns.get(side)));
                }).filter(Objects::nonNull)).toList();
    }
}
