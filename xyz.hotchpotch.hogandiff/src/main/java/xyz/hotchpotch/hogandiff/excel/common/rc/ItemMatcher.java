package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.List;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;

@FunctionalInterface
/* package */ interface ItemMatcher {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    List<IntPair> makePairs(Set<CellData> cells1, Set<CellData> cells2);
}
