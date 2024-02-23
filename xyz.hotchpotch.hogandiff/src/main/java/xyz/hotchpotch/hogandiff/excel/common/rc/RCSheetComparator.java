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
    public SheetResult compare(
            Set<CellData> cells1,
            Set<CellData> cells2) {
        
        Objects.requireNonNull(cells1, "cells1");
        Objects.requireNonNull(cells2, "cells2");
        
        if (cells1 == cells2) {
            if (cells1.isEmpty()) {
                return new SheetResult(
                        EMPTY_INT_ARRAY_PAIR,
                        EMPTY_INT_ARRAY_PAIR,
                        List.of());
            } else {
                throw new IllegalArgumentException("cells1 == cells2");
            }
        }
        
        Pair<List<IntPair>> pairs = rcMatcher.make2Pairs(cells1, cells2);
        List<IntPair> rowPairs = pairs.a();
        List<IntPair> columnPairs = pairs.b();
        
        // 余剰行の収集
        int[] redundantRows1 = rowPairs.stream()
                .filter(IntPair::isOnlyA).mapToInt(IntPair::a).toArray();
        int[] redundantRows2 = rowPairs.stream()
                .filter(IntPair::isOnlyB).mapToInt(IntPair::b).toArray();
        
        // 余剰列の収集
        int[] redundantColumns1 = columnPairs.stream()
                .filter(IntPair::isOnlyA).mapToInt(IntPair::a).toArray();
        int[] redundantColumns2 = columnPairs.stream()
                .filter(IntPair::isOnlyB).mapToInt(IntPair::b).toArray();
        
        // 差分セルの収集
        List<Pair<CellData>> diffCells = extractDiffs(
                cells1, cells2, rowPairs, columnPairs);
        
        return new SheetResult(
                new Pair<>(redundantRows1, redundantRows2),
                new Pair<>(redundantColumns1, redundantColumns2),
                diffCells);
    }
    
    private List<Pair<CellData>> extractDiffs(
            Set<CellData> cells1,
            Set<CellData> cells2,
            List<IntPair> rowPairs,
            List<IntPair> columnPairs) {
        
        assert cells1 != null;
        assert cells2 != null;
        assert cells1 != cells2;
        assert rowPairs != null;
        assert columnPairs != null;
        
        Map<String, CellData> map1 = cells1.stream()
                .collect(Collectors.toMap(CellData::address, Function.identity()));
        Map<String, CellData> map2 = cells2.stream()
                .collect(Collectors.toMap(CellData::address, Function.identity()));
        
        List<IntPair> columnPairsFiltered = columnPairs.stream().filter(IntPair::isPaired).toList();
        
        return rowPairs.parallelStream().filter(IntPair::isPaired).flatMap(rp -> {
            int row1 = rp.a();
            int row2 = rp.b();
            
            return columnPairsFiltered.stream().map(cp -> {
                int column1 = cp.a();
                int column2 = cp.b();
                String address1 = CellsUtil.idxToAddress(row1, column1);
                String address2 = CellsUtil.idxToAddress(row2, column2);
                CellData cell1 = map1.get(address1);
                CellData cell2 = map2.get(address2);
                
                return (cell1 != null && cell2 != null && cell1.dataEquals(cell2) || cell1 == null && cell2 == null)
                        ? null
                        : new Pair<>(
                                cell1 != null ? cell1 : CellData.empty(row1, column1),
                                cell2 != null ? cell2 : CellData.empty(row2, column2));
            }).filter(Objects::nonNull);
        }).toList();
    }
}