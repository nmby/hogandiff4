package xyz.hotchpotch.hogandiff.logic.plain.matchers;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.logic.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

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
     * @param vertical        縦方向のインデックス抽出関数
     * @param horizontal      横方向のインデックス抽出関数
     * @param considerVGaps   縦方向の挿入／削除を考慮する場合は {@code true}
     * @param considerHGaps   横方向の挿入／削除を考慮する場合は {@code true}
     * @param prioritizeSpeed 比較処理の速度を優先する場合は {@code true}
     * @return 縦方向の対応付けを行うマッチャー
     */
    private static ItemMatcher matcherOf(
            ToIntFunction<CellData> vertical,
            ToIntFunction<CellData> horizontal,
            boolean considerVGaps,
            boolean considerHGaps,
            boolean prioritizeSpeed) {
        
        if (!considerVGaps) {
            return new ItemMatcherImpl0(vertical);
        }
        
        Comparator<CellData> horizontalComparator = considerHGaps
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
     * @param considerRowGaps    行の挿入／削除を考慮する場合は {@code true}
     * @param considerColumnGaps 列の挿入／削除を考慮する場合は {@code true}
     * @param prioritizeSpeed    比較処理の速度を優先する場合は {@code true}
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
     * @param considerRowGaps    行の挿入／削除を考慮する場合は {@code true}
     * @param considerColumnGaps 列の挿入／削除を考慮する場合は {@code true}
     * @param prioritizeSpeed    比較処理の速度を優先する場合は {@code true}
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
    
    public static Pair<Set<Integer>> extractRedundants(List<IntPair> horizontalPairs) {
        return Side.map(side -> horizontalPairs == null
                ? Set.of()
                : horizontalPairs.stream()
                        .filter(pair -> pair.isOnly(side))
                        .map(pair -> pair.get(side))
                        .collect(Collectors.toSet()));
    }
    
    /**
     * セルセットを縦方向リストに変換します。<br>
     * 
     * @param <T> リストの要素の型
     * @param cellsSetPair セルセット
     * @param horizontalRedundants 横方向の余剰インデックス
     * @param vertical 縦インデックス抽出関数
     * @param horizontal 横インデックス抽出関数
     * @param horizontalDefaultValue 横方向にセルが存在しない場合のデフォルト値
     * @param horizontalConverter 横方向リスト変換関数
     * @return 縦方向リスト
     */
    public static <T> Pair<List<T>> convertCellsToList(
            Pair<Set<CellData>> cellsSetPair,
            Pair<Set<Integer>> horizontalRedundants,
            ToIntFunction<CellData> vertical,
            ToIntFunction<CellData> horizontal,
            T horizontalDefaultValue,
            Function<List<CellData>, T> horizontalConverter) {
        
        Objects.requireNonNull(cellsSetPair);
        Objects.requireNonNull(horizontal);
        
        return Side.map(side -> {
            Map<Integer, List<CellData>> map = cellsSetPair.get(side).parallelStream()
                    .filter(cell -> !horizontalRedundants.get(side).contains(horizontal.applyAsInt(cell)))
                    .collect(Collectors.groupingBy(vertical::applyAsInt));
            
            int max = map.keySet().stream().mapToInt(n -> n).max().orElse(0);
            
            return IntStream.rangeClosed(0, max).parallel()
                    .mapToObj(i -> {
                        if (map.containsKey(i)) {
                            List<CellData> list = map.get(i);
                            return horizontalConverter.apply(list);
                        } else {
                            return horizontalDefaultValue;
                        }
                    })
                    .toList();
        });
    }
    
    // [instance members] ******************************************************
    
    /**
     * 行方向または列方向の対応付けを行い結果を返します。<br>
     * 
     * @param cellsSetPair    比較対象シートのセルセット
     * @param horizontalPairs 既に決定済みの横方向の対応付け
     * @return 縦方向の対応付け
     */
    List<IntPair> makePairs(
            Pair<Set<CellData>> cellsSetPair,
            List<IntPair> horizontalPairs);
}
