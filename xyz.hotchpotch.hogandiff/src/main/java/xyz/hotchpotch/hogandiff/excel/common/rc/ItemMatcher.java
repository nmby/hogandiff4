package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;

@FunctionalInterface
/* package */ interface ItemMatcher {
    
    // [static members] ********************************************************
    
    private static ItemMatcher matcherOf(
            Function<CellData, Integer> vertical,
            Function<CellData, Integer> horizontal,
            boolean considerVGaps,
            boolean considerHGaps) {
        
        if (!considerVGaps) {
            return new ItemMatcherImpl0(vertical);
        }
        
        Comparator<CellData> horizontalComparator = considerHGaps
                // TODO: セルコメント加味の要否について再検討する。
                ? Comparator.comparing(CellData::content)
                : Comparator.comparing(horizontal);
        
        return new ItemMatcherImpl2(
                vertical,
                horizontal,
                horizontalComparator);
    }
    
    public static ItemMatcher rowsMatcherOf(
            boolean considerRowGaps,
            boolean considerColumnGaps) {
        
        return matcherOf(
                CellData::row,
                CellData::column,
                considerRowGaps,
                considerColumnGaps);
    }
    
    public static ItemMatcher columnsMatcherOf(
            boolean considerRowGaps,
            boolean considerColumnGaps) {
        
        return matcherOf(
                CellData::column,
                CellData::row,
                considerColumnGaps,
                considerRowGaps);
    }
    
    // [instance members] ******************************************************
    
    List<IntPair> makePairs(
            Set<CellData> cells1,
            Set<CellData> cells2,
            List<IntPair> horizontalPairs);
}
