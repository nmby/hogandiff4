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
            boolean considerColumnGaps,
            boolean prioritizeSpeed) {
        
        ItemMatcher rowsMatcher = ItemMatcher
                .rowsMatcherOf(considerRowGaps, considerColumnGaps, prioritizeSpeed);
        ItemMatcher columnsMatcher = ItemMatcher
                .columnsMatcherOf(considerRowGaps, considerColumnGaps, prioritizeSpeed);
        
        if (considerRowGaps && considerColumnGaps) {
            return cellsSets -> {
                List<IntPair> columnPairs = columnsMatcher.makePairs(cellsSets, null);
                List<IntPair> rowPairs = rowsMatcher.makePairs(cellsSets, columnPairs);
                
                return new Pair<>(rowPairs, columnPairs);
            };
            
        } else {
            return cellsSets -> {
                List<IntPair> columnPairs = columnsMatcher.makePairs(cellsSets, null);
                List<IntPair> rowPairs = rowsMatcher.makePairs(cellsSets, null);
                
                return new Pair<>(rowPairs, columnPairs);
            };
        }
    }
    
    // [instance members] ******************************************************
    
    Pair<List<IntPair>> make2Pairs(Pair<Set<CellData>> cellsSets);
}
