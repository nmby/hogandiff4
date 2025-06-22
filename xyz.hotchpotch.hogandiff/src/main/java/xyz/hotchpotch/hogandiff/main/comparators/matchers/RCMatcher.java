package xyz.hotchpotch.hogandiff.main.comparators.matchers;

import java.util.List;
import java.util.Set;

import xyz.hotchpotch.hogandiff.main.models.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * Excelシート同士の比較を、行同士の比較と列同士の比較によって求めるマッチャーを表します。<br>
 * これは、{@link #make2Pairs(Pair)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
 * @author nmby
 */
@FunctionalInterface
public interface RCMatcher {

    // [static members] ********************************************************

    /**
     * 新たなマッチャーを返します。<br>
     * 
     * @param considerRowGaps    行の挿入／削除を考慮する場合は {@code true}
     * @param considerColumnGaps 列の挿入／削除を考慮する場合は {@code true}
     * @param prioritizeSpeed    比較処理の速度を優先する場合は {@code true}
     * @return 新たなマッチャー
     */
    public static RCMatcher of(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean prioritizeSpeed) {

        ItemMatcher rowsMatcher = ItemMatcher
                .rowsMatcherOf(considerRowGaps, considerColumnGaps, prioritizeSpeed);
        ItemMatcher columnsMatcher = ItemMatcher
                .columnsMatcherOf(considerRowGaps, considerColumnGaps, prioritizeSpeed);

        if (considerRowGaps && considerColumnGaps) {
            return cellsSetPair -> {
                List<IntPair> columnPairs = columnsMatcher.makePairs(cellsSetPair, null);
                List<IntPair> rowPairs = rowsMatcher.makePairs(cellsSetPair, columnPairs);

                return new Pair<>(rowPairs, columnPairs);
            };

        } else {
            return cellsSetPair -> {
                List<IntPair> columnPairs = columnsMatcher.makePairs(cellsSetPair, null);
                List<IntPair> rowPairs = rowsMatcher.makePairs(cellsSetPair, null);

                return new Pair<>(rowPairs, columnPairs);
            };
        }
    }

    // [instance members] ******************************************************

    /**
     * 2つのシートに含まれるセルセット同士を比較し、行同士、列同士の対応関係を返します。<br>
     * 
     * @param cellsSetPair 比較対象シートに含まれるセルセット
     * @return 行同士、列同士の対応関係
     */
    Pair<List<IntPair>> make2Pairs(Pair<Set<CellData>> cellsSetPair);
}
