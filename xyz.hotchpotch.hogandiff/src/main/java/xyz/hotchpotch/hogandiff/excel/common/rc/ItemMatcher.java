package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 行方向または列方向の対応付けを行うマッチャーを表します。<br>
 * 既に決定済みの横方向の対応付けを加味することにより、縦方向の対応付けを精度高く行うことを目指したマッチャーです。<br>
 * これは、{@link #makePairs(Pair, List)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
 * @author nmby
 */
@FunctionalInterface
/* package */ interface ItemMatcher {
    
    // [static members] ********************************************************
    
    /**
     * 縦方向の対応付けを行うマッチャーを返します。<br>
     * 
     * @param vertical 縦方向のインデックス抽出関数
     * @param horizontal 横方向のインデックス抽出関数
     * @param considerVGaps 縦方向の挿入／削除を考慮する場合は {@code true}
     * @param considerHGaps 横方向の挿入／削除を考慮する場合は {@code true}
     * @param prioritizeSpeed 比較処理の速度を優先する場合は {@code true}
     * @return 縦方向の対応付けを行うマッチャー
     */
    private static ItemMatcher matcherOf(
            Function<CellData, Integer> vertical,
            ToIntFunction<CellData> horizontal,
            boolean considerVGaps,
            boolean considerHGaps,
            boolean prioritizeSpeed) {
        
        if (!considerVGaps) {
            return new ItemMatcherImpl0(vertical);
        }
        
        Comparator<CellData> horizontalComparator = considerHGaps
                // TODO: セルコメント加味の要否について再検討する。
                ? Comparator.comparing(CellData::content)
                : Comparator.comparingInt(horizontal);
        
        return prioritizeSpeed
                ? new ItemMatcherImpl1(
                        vertical,
                        horizontal,
                        horizontalComparator)
                : new ItemMatcherImpl2(
                        vertical,
                        horizontal,
                        horizontalComparator);
    }
    
    /**
     * 行同士の対応付けを行うマッチャーを返します。<br>
     * 
     * @param considerRowGaps 行の挿入／削除を考慮する場合は {@code true}
     * @param considerColumnGaps 列の挿入／削除を考慮する場合は {@code true}
     * @param prioritizeSpeed 比較処理の速度を優先する場合は {@code true}
     * @return 行同士の対応付けを行うマッチャー
     */
    public static ItemMatcher rowsMatcherOf(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean prioritizeSpeed) {
        
        return matcherOf(
                CellData::row,
                CellData::column,
                considerRowGaps,
                considerColumnGaps,
                prioritizeSpeed);
    }
    
    /**
     * 列同士の対応付けを行うマッチャーを返します。<br>
     * 
     * @param considerRowGaps 行の挿入／削除を考慮する場合は {@code true}
     * @param considerColumnGaps 列の挿入／削除を考慮する場合は {@code true}
     * @param prioritizeSpeed 比較処理の速度を優先する場合は {@code true}
     * @return 行同士の対応付けを行うマッチャー
     */
    public static ItemMatcher columnsMatcherOf(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean prioritizeSpeed) {
        
        return matcherOf(
                CellData::column,
                CellData::row,
                considerColumnGaps,
                considerRowGaps,
                prioritizeSpeed);
    }
    
    // [instance members] ******************************************************
    
    /**
     * 行方向または列方向の対応付けを行い結果を返します。<br>
     * 
     * @param cellsSets 比較対象シートのセルセット
     * @param horizontalPairs 既に決定済みの横方向の対応付け
     * @return 縦方向の対応付け
     */
    List<IntPair> makePairs(
            Pair<Set<CellData>> cellsSets,
            List<IntPair> horizontalPairs);
}
