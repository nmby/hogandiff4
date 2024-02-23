package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.List;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

@FunctionalInterface
public interface RCMatcher {
    
    // [static members] ********************************************************
    
    public static RCMatcher of(
            boolean considerRowGaps,
            boolean considerColumnGaps) {
        
        return (cells1, cells2) -> {
            ItemMatcher rowsMatcher = ItemMatcher.rowsMatcherOf(considerRowGaps, considerColumnGaps);
            List<IntPair> rowPairs = rowsMatcher.makePairs(cells1, cells2);
            
            ItemMatcher columnsMatcher = ItemMatcher.columnsMatcherOf(considerRowGaps, considerColumnGaps);
            List<IntPair> columnPairs = columnsMatcher.makePairs(cells1, cells2);
            
            return new Pair<>(rowPairs, columnPairs);
        };
    }
    
    // [instance members] ******************************************************
    
    Pair<List<IntPair>> make2Pairs(Set<CellData> cells1, Set<CellData> cells2);
}
