package xyz.hotchpotch.hogandiff.excel;

import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.common.rc.RCSheetComparator;

/**
 * 2つのシートから抽出したセルセット同士を比較するコンパレータを表します。<br>
 * これは、{@link #compare(Set, Set)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
* @author nmby
 */
@FunctionalInterface
public interface SheetComparator {
    
    // [static members] ********************************************************
    
    /**
     * {@link SheetComparator} のインスタンスを生成して返します。<br>
     * 
     * @param considerRowGaps 行の挿入／削除を考慮する場合は {@code true}
     * @param considerColumnGaps 列の挿入／削除を考慮する場合は {@code true}
     * @return コンパレータ
     */
    public static SheetComparator of(boolean considerRowGaps, boolean considerColumnGaps) {
        return RCSheetComparator.of(considerRowGaps, considerColumnGaps, true);
    }
    
    // [instance members] ******************************************************
    
    /**
     * 2つのシートから抽出したセルセット同士を比較して結果を返します。<br>
     * 
     * @param cells1 セルセット1
     * @param cells2 セルセット2
     * @return 比較結果
     */
    SheetResult compare(
            Set<CellData> cells1,
            Set<CellData> cells2);
}
