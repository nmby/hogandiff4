package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.util.Pair;

public sealed interface CompareInfo<P, C, I>
        permits SheetCompareInfo, BookCompareInfo, DirCompareInfo {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 比較対象のペアを返します。<br>
     * 
     * @return 比較対象のペア
     */
    Pair<P> parentPair();
    
    /**
     * 子要素の組み合わせを返します。<br>
     * 
     * @return 子要素の組み合わせ
     */
    List<Pair<C>> childPairs();
    
    /**
     * 子要素同士の比較情報を返します。<br>
     * 
     * @return 子要素同士の比較情報
     */
    Map<Pair<C>, Optional<I>> childCompareInfos();
}
