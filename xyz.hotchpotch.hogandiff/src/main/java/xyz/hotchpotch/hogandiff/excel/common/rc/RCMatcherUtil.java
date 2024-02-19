package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import xyz.hotchpotch.hogandiff.excel.CellData;

/* package */ class RCMatcherUtil {
    
    // [static members] ********************************************************
    
    private static ItemMatcher matcherOf(
            Function<CellData, Integer> vertical,
            ToIntFunction<CellData> horizontal,
            boolean considerVGaps,
            boolean considerHGaps) {
        
        if (!considerVGaps) {
            return new ItemMatcherImpl0(vertical);
        }
        
        Comparator<CellData> horizontalComparator = considerHGaps
                // TODO: セルコメント加味の要否について再検討する。
                ? Comparator.comparing(CellData::content)
                : Comparator.comparingInt(horizontal);
        
        return new ItemMatcherImpl1(
                vertical,
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
    
    private RCMatcherUtil() {
    }
}
