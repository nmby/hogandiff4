package xyz.hotchpotch.hogandiff.core;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのリスト間の編集距離が最小となるように要素同士を対応付ける {@link Matcher} の実装です。<br>
 * 文字列（文字を要素とするリスト）同士のマッチングだけでなく、
 * 任意の型の要素のリスト同士のマッチングに利用することができます。<br>
 * 
 * @param <T> リストの要素の型
 * @author nmby
 */
/*package*/ class MinimumEditDistanceMatcher<T> implements Matcher<T> {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /**
     * 内部処理用の列挙型です。<br>
     * エディットグラフ上の各点における最適遷移方向を表します。<br>
     * 
     * @author nmby
     */
    private static enum ComeFrom {
        
        // [static members] ----------------------------------------------------
        
        /**
         * エディットグラフを左上から右下に遷移すること、すなわち、
         * リストAとリストBの要素が対応することを表します。<br>
         */
        UPPER_LEFT,
        
        /**
         * エディットグラフを上から下に遷移すること、すなわち、
         * リストAの要素が余剰であり対応する要素がリストBにないことを表します。
         */
        UPPER,
        
        /**
         * エディットグラフを左から右に遷移すること、すなわち、
         * リストBの要素が余剰であり対応する要素がリストAにないことを表します。
         */
        LEFT;
        
        // [instance members] --------------------------------------------------
    }
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private final ToIntFunction<? super T> gapEvaluator;
    private final ToIntBiFunction<? super T, ? super T> diffEvaluator;
    
    /*package*/ MinimumEditDistanceMatcher(
            ToIntFunction<? super T> gapEvaluator,
            ToIntBiFunction<? super T, ? super T> diffEvaluator) {
        
        assert gapEvaluator != null;
        assert diffEvaluator != null;
        
        this.gapEvaluator = gapEvaluator;
        this.diffEvaluator = diffEvaluator;
    }
    
    /**
     * {@inheritDoc}
     * <br>
     * この実装は、2つのリスト間の編集距離が最小となるような要素同士の組み合わせを返します。<br>
     * 
     * @throws NullPointerException {@code listA}, {@code listB} のいずれかが {@code null} の場合
     */
    @Override
    public List<Pair<Integer>> makePairs(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        Objects.requireNonNull(listA, "listA");
        Objects.requireNonNull(listB, "listB");
        
        if (listA == listB) {
            return IntStream.range(0, listA.size())
                    .mapToObj(n -> Pair.of(n, n))
                    .toList();
        }
        
        ComeFrom[][] bestDirections = calcBestDirections(listA, listB);
        
        List<Pair<Integer>> pairs = traceBestRoute(bestDirections);
        
        return pairs;
    }
    
    private ComeFrom[][] calcBestDirections(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        assert listA != null;
        assert listB != null;
        assert listA != listB;
        
        // エディットグラフ上の各点における最小到達コストを保持する二次元配列。
        // 座標系が他とは +1 ずれているので注意。
        long[][] accumulatedCosts = new long[listA.size() + 1][listB.size() + 1];
        
        // エディットグラフ上の各点における最適遷移方向を保持する二次元配列。
        // 座標系が他とは +1 ずれているので注意。
        ComeFrom[][] bestDirections = new ComeFrom[listA.size() + 1][listB.size() + 1];
        
        // 1. リストA, リストBの要素の余剰コストを計算する。
        int[] gapCostsA = listA.stream().mapToInt(gapEvaluator::applyAsInt).toArray();
        int[] gapCostsB = listB.stream().mapToInt(gapEvaluator::applyAsInt).toArray();
        
        // 2. エディットグラフを縦だけに進んだ場合（エディットグラフの左端）
        //    および横だけに進んだ場合（エディットグラフの上端）の
        //    最小到達コストと最適遷移方向を計算する。
        for (int i = 0; i < listA.size(); i++) {
            accumulatedCosts[i + 1][0] = accumulatedCosts[i][0] + gapCostsA[i];
            bestDirections[i + 1][0] = ComeFrom.UPPER;
        }
        for (int j = 0; j < listB.size(); j++) {
            accumulatedCosts[0][j + 1] = accumulatedCosts[0][j] + gapCostsB[j];
            bestDirections[0][j + 1] = ComeFrom.LEFT;
        }
        
        // 3. エディットグラフの残りの部分の最小到達コストと最適遷移方向を計算する。
        //    比較対象リストが長くなるほど、すなわちエディットグラフ（探索平面）が広くなるほど
        //    処理の並列化が効果を発揮すると信じて、処理を並列化する。
        //    縦方向、横方向には並列化できないため、探索平面を斜めにスライスして並列化する。
        // FIXME: [No.91 内部実装改善] ループの外側で並列化する方策を考える。何らかできるはず。
        for (int n = 0; n <= listA.size() + listB.size(); n++) {
            final int nn = n;
            IntStream.rangeClosed(Math.max(0, nn - listB.size() + 1), Math.min(nn, listA.size() - 1))
                    .parallel().forEach(i -> {
                        int j = nn - i;
                        
                        // 左上からの遷移（つまりリストA, リストBの要素が対応する場合）が最適であると仮置きする。
                        long minCost = accumulatedCosts[i][j] + diffEvaluator.applyAsInt(listA.get(i), listB.get(j));
                        ComeFrom minDirection = ComeFrom.UPPER_LEFT;
                        
                        // 左から遷移した場合（つまりリストBの要素が余剰である場合）のコストを求めて比較する。
                        long tmpCostB = accumulatedCosts[i + 1][j] + gapCostsB[j];
                        if (tmpCostB < minCost) {
                            minCost = tmpCostB;
                            minDirection = ComeFrom.LEFT;
                        }
                        
                        // 上から遷移した場合（つまりリストAの要素が余剰である場合）のコストを求めて比較する。
                        long tmpCostA = accumulatedCosts[i][j + 1] + gapCostsA[i];
                        if (tmpCostA < minCost) {
                            minCost = tmpCostA;
                            minDirection = ComeFrom.UPPER;
                        }
                        
                        accumulatedCosts[i + 1][j + 1] = minCost;
                        bestDirections[i + 1][j + 1] = minDirection;
                    });
        }
        return bestDirections;
    }
    
    private List<Pair<Integer>> traceBestRoute(ComeFrom[][] bestDirections) {
        assert bestDirections != null;
        
        LinkedList<Pair<Integer>> bestRoute = new LinkedList<>();
        int i = bestDirections.length - 1;
        int j = bestDirections[0].length - 1;
        
        while (0 < i || 0 < j) {
            switch (bestDirections[i][j]) {
            case UPPER_LEFT:
                i--;
                j--;
                bestRoute.addFirst(Pair.of(i, j));
                break;
            case UPPER:
                i--;
                bestRoute.addFirst(Pair.onlyA(i));
                break;
            case LEFT:
                j--;
                bestRoute.addFirst(Pair.onlyB(j));
                break;
            default:
                throw new AssertionError(bestDirections[i][j]);
            }
        }
        return bestRoute;
    }
}
