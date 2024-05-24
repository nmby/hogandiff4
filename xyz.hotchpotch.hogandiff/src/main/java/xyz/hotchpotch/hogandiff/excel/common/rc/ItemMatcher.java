package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 行方向または列方向の対応付けを行うマッチャーを表します。<br>
 * 既に決定済みの横方向の対応付けを加味することにより、縦方向の対応付けを精度高く行うことを目指したマッチャーです。<br>
 * 
 * @author nmby
 */
@FunctionalInterface
/* package */ interface ItemMatcher {
    
    // [static members] ********************************************************
    
    private static ItemMatcher matcherOf(
            Function<CellData, Integer> vertical,
            Function<CellData, Integer> horizontal,
            boolean considerVGaps,
            boolean considerHGaps,
            boolean prioritizeSpeed) {
        
        if (!considerVGaps) {
            return new ItemMatcherImpl0(vertical);
        }
        
        Comparator<CellData> horizontalComparator = considerHGaps
                // TODO: セルコメント加味の要否について再検討する。
                ? Comparator.comparing(CellData::content)
                : Comparator.comparing(horizontal);
        
        return prioritizeSpeed
                ? new ItemMatcherImpl1(
                        vertical,
                        horizontal,
                        horizontalComparator)
                : new ItemMatcherImpl2(
                        vertical,
                        horizontal,
                        horizontalComparator);
    }
    
    public static ItemMatcher rowsMatcherOf(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean prioritizeSpeed) {
        
        return matcherOf(
                CellData::row,
                CellData::column,
                considerRowGaps,
                considerColumnGaps,
                prioritizeSpeed);
    }
    
    public static ItemMatcher columnsMatcherOf(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean prioritizeSpeed) {
        
        return matcherOf(
                CellData::column,
                CellData::row,
                considerColumnGaps,
                considerRowGaps,
                prioritizeSpeed);
    }
    
    // [instance members] ******************************************************
    
    /**
     * 行方向または列方向の対応付けを行い結果を返します。<br>
     * 
     * @param cellsSets 比較対象シートのセルセット
     * @param horizontalPairs 既に決定済みの横方向の対応付け
     * @return 縦方向の対応付け
     */
    List<IntPair> makePairs(
            Pair<Set<CellData>> cellsSets,
            List<IntPair> horizontalPairs);
}
