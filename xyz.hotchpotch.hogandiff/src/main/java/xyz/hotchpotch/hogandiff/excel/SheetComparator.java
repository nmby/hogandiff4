package xyz.hotchpotch.hogandiff.excel;

import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.common.rc.RCSheetComparator;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのシートから抽出したセルセット同士を比較するコンパレータを表します。<br>
 * これは、{@link #compare(Pair)} を関数メソッドに持つ関数型インタフェースです。<br>
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
    public static SheetComparator of(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean prioritizeSpeed) {
        
        return RCSheetComparator.of(
                considerRowGaps,
                considerColumnGaps,
                prioritizeSpeed);
    }
    
    // [instance members] ******************************************************
    
    /**
     * 2つのシートから抽出したセルセット同士を比較して結果を返します。<br>
     * 
     * @param cellsSets セルセット
     * @return 比較結果
     */
    SheetResult compare(Pair<Set<CellData>> cellsSets);
}
