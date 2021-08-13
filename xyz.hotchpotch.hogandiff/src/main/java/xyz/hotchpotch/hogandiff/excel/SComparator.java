package xyz.hotchpotch.hogandiff.excel;

import java.util.Set;

/**
 * 2つのシートから抽出したセルセット同士を比較するコンパレータを表します。<br>
 * これは、{@link #compare(Set, Set)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
* @author nmby
 */
@FunctionalInterface
public interface SComparator {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 2つのシートから抽出したセルセット同士を比較して結果を返します。<br>
     * 
     * @param cells1 セルセット1
     * @param cells2 セルセット2
     * @return 比較結果
     */
    SResult compare(
            Set<CellData> cells1,
            Set<CellData> cells2);
}
