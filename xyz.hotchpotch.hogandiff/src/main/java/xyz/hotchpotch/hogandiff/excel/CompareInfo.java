package xyz.hotchpotch.hogandiff.excel;

import java.util.Map;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.util.Pair;

public sealed interface CompareInfo<P, C, I>
        permits BookCompareInfo, DirCompareInfo {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 子要素同士の比較情報を返します。<br>
     * 
     * @return 子要素同士の比較情報
     */
    Map<Pair<C>, Optional<I>> childCompareInfos();
}
