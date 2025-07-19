package xyz.hotchpotch.hogandiff.logic.models;

import java.util.List;

import xyz.hotchpotch.hogandiff.logic.models.ResultOfSheets.SheetStats;

/**
 * 各種比較操作の比較結果を表します。<br>
 * 
 * @author nmby
 */
// sealed を使ってみる
public sealed interface Result
        permits ResultOfSheets, ResultOfBooks, ResultOtDirs, ResultOfTrees {

    // [static members] ********************************************************

    // [instance members] ******************************************************

    /**
     * 比較結果のシートごとの統計情報を返します。<br>
     * 
     * @return シートごとの統計情報
     */
    List<SheetStats> sheetStats();
}
