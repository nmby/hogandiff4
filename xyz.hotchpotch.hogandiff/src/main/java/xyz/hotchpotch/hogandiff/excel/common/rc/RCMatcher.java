package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

@FunctionalInterface
public interface RCMatcher {
    
    // [static members] ********************************************************
    
    public static RCMatcher of(
            boolean considerRowGaps,
            boolean considerColumnGaps) {
        
        BiFunction<Set<CellData>, Set<CellData>, List<IntPair>> rowsMatcher = RCMatcherUtil
                .rowsMatcherOf(considerRowGaps, considerColumnGaps);
        BiFunction<Set<CellData>, Set<CellData>, List<IntPair>> columnsMatcher = RCMatcherUtil
                .columnsMatcherOf(considerRowGaps, considerColumnGaps);
        
        return (cells1, cells2) -> {
            List<IntPair> rowPairs = rowsMatcher.apply(cells1, cells2);
            List<IntPair> columnPairs = columnsMatcher.apply(cells1, cells2);
            return new Pair<>(rowPairs, columnPairs);
        };
    }
    
    // [instance members] ******************************************************
    
    Pair<List<IntPair>> makePairs(Set<CellData> cells1, Set<CellData> cells2);
}
