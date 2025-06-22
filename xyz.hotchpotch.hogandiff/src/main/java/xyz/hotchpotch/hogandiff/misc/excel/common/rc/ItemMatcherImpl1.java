package xyz.hotchpotch.hogandiff.misc.excel.common.rc;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.models.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 縦方向の挿入／削除を考慮して縦方向の対応付けを行う {@link ItemMatcher} の実装です。<br>
 * 横方向の挿入／削除を考慮しない場合と考慮する場合の双方に対応できます。<br>
 * 横方向の要素の重みづけは行いません。<br>
 * 
 * @author nmby
 */
public class ItemMatcherImpl1 implements ItemMatcher {

    // [static members] ********************************************************

    /** 余剰評価関数 */
    private static final ToIntFunction<List<CellData>> gapEvaluator = List::size;

    /**
     * 差分評価関数を返します。<br>
     * 
     * @param horizontalComparator 横方向の比較関数
     * @return 差分評価関数
     */
    private static ToIntBiFunction<List<CellData>, List<CellData>> diffEvaluator(
            Comparator<CellData> horizontalComparator) {

        assert horizontalComparator != null;

        return (list1, list2) -> {
            int idx1 = 0;
            int idx2 = 0;
            int comp = 0;
            int cost = 0;
            CellData cell1 = null;
            CellData cell2 = null;

            while (idx1 < list1.size() && idx2 < list2.size()) {
                if (comp <= 0) {
                    cell1 = list1.get(idx1);
                    idx1++;
                }
                if (0 <= comp) {
                    cell2 = list2.get(idx2);
                    idx2++;
                }

                comp = horizontalComparator.compare(cell1, cell2);

                if (comp < 0) {
                    cost++;
                } else if (0 < comp) {
                    cost++;
                } else if (!cell1.contentEquals(cell2)) {
                    cost += 2;
                }
            }

            if (idx1 < list1.size()) {
                cost += list1.size() - idx1;
            }
            if (idx2 < list2.size()) {
                cost += list2.size() - idx2;
            }
            return cost;
        };
    }

    // [instance members] ******************************************************

    private final ToIntFunction<CellData> vertical;
    private final ToIntFunction<CellData> horizontal;
    private final Comparator<CellData> horizontalComparator;
    private final Matcher<List<CellData>> matcher;

    /**
     * コンストラクタ
     * 
     * @param vertical             縦インデックス抽出関数
     * @param horizontal           横インデックス抽出関数
     * @param horizontalComparator 横方向比較関数
     */
    /* package */ ItemMatcherImpl1(
            ToIntFunction<CellData> vertical,
            ToIntFunction<CellData> horizontal,
            Comparator<CellData> horizontalComparator) {

        assert vertical != null;
        assert horizontal != null;
        assert horizontalComparator != null;

        this.vertical = vertical;
        this.horizontal = horizontal;
        this.horizontalComparator = horizontalComparator;
        this.matcher = Matcher.minimumEditDistanceMatcherOf(
                gapEvaluator,
                diffEvaluator(horizontalComparator));
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException {@code cellsSetPair} が {@code null} の場合
     */
    @Override
    public List<IntPair> makePairs(
            Pair<Set<CellData>> cellsSetPair,
            List<IntPair> horizontalPairs) {

        Objects.requireNonNull(cellsSetPair);

        Pair<Set<Integer>> horizontalRedundants = horizontalPairs == null
                ? new Pair<>(Set.of(), Set.of())
                : Side.map(side -> horizontalPairs.stream()
                        .filter(pair -> pair.isOnly(side))
                        .map(pair -> pair.get(side))
                        .collect(Collectors.toSet()));

        List<List<CellData>> listA = convert(cellsSetPair.a(), horizontalRedundants.a());
        List<List<CellData>> listB = convert(cellsSetPair.b(), horizontalRedundants.b());

        return matcher.makeIdxPairs(listA, listB);
    }

    /**
     * セルセットを横方向リストを要素に持つ縦方向リストに変換します。<br>
     * 
     * @param cells                セルセット
     * @param horizontalRedundants 横方向の余剰インデックス
     * @return 横方向リストを要素に持つ縦方向リスト
     */
    private List<List<CellData>> convert(
            Set<CellData> cells,
            Set<Integer> horizontalRedundants) {

        assert cells != null;
        assert horizontalRedundants != null;

        Map<Integer, List<CellData>> map = cells.parallelStream()
                .filter(cell -> !horizontalRedundants.contains(horizontal.applyAsInt(cell)))
                .collect(Collectors.groupingBy(vertical::applyAsInt));

        int max = map.keySet().stream().mapToInt(n -> n).max().orElse(0);

        return IntStream.rangeClosed(0, max).parallel()
                .mapToObj(i -> {
                    if (map.containsKey(i)) {
                        List<CellData> list = map.get(i);
                        list.sort(horizontalComparator);
                        return list;
                    } else {
                        return List.<CellData>of();
                    }
                })
                .toList();
    }
}
