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
/*package*/ class MinimumEditDistanceMatcher2<T> implements Matcher<T> {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /**
     * 内部処理用の列挙型です。<br>
     * エディットグラフ上の各点における最適遷移方向を表します。<br>
     * 
     * @author nmby
     */
    private static enum Direction {
        
        // [static members] ----------------------------------------------------
        
        /**
         * エディットグラフを左上から右下に遷移すること、すなわち、
         * リストAとリストBの要素が対応することを表します。<br>
         */
        FROM_UPPER_LEFT,
        
        /**
         * エディットグラフを上から下に遷移すること、すなわち、
         * リストAの要素が余剰であり対応する要素がリストBにないことを表します。
         */
        FROM_UPPER,
        
        /**
         * エディットグラフを左から右に遷移すること、すなわち、
         * リストBの要素が余剰であり対応する要素がリストAにないことを表します。
         */
        FROM_LEFT;
        
        // [instance members] --------------------------------------------------
    }
    
    /**
     * 内部処理用の不変クラス（レコード）です。<br>
     * エディットグラフ上の各点における遷移経路を表します。<br>
     * 
     * @author nmby
     */
    private static record ComeFrom(
            ComeFrom prev,
            Direction direction) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
    }
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private final ToIntFunction<? super T> gapEvaluator;
    private final ToIntBiFunction<? super T, ? super T> diffEvaluator;
    
    /*package*/ MinimumEditDistanceMatcher2(
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
        if (listA.isEmpty() && listB.isEmpty()) {
            return List.of();
        }
        if (listA.isEmpty()) {
            return IntStream.range(0, listB.size()).mapToObj(Pair::onlyB).toList();
        }
        if (listB.isEmpty()) {
            return IntStream.range(0, listA.size()).mapToObj(Pair::onlyA).toList();
        }
        
        ComeFrom bestRoute = calcBestRoute(listA, listB);
        
        List<Pair<Integer>> pairs = traceBestRoute(listA, listB, bestRoute);
        
        return pairs;
    }
    
    private ComeFrom calcBestRoute(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        assert listA != null;
        assert listB != null;
        assert listA != listB;
        
        // 1. リストA, リストBの要素の余剰コストを計算する。
        int[] gapCostsA = listA.stream().mapToInt(gapEvaluator::applyAsInt).toArray();
        int[] gapCostsB = listB.stream().mapToInt(gapEvaluator::applyAsInt).toArray();
        
        // 2. エディットグラフ上の各点の最小到達コストと最適遷移方向を計算する。
        //    比較対象リストが長くなるほど、すなわちエディットグラフ（探索平面）が広くなるほど
        //    処理の並列化が効果を発揮すると信じて、処理を並列化する。
        //    縦方向、横方向には並列化できないため、探索平面を斜めにスライスして並列化する。
        int minSize = Math.min(listA.size(), listB.size());
        int maxSize = Math.max(listA.size(), listB.size());
        int sumSize = listA.size() + listB.size();
        
        long[] accCosts2 = null;
        long[] accCosts1 = new long[] { 0 };
        long[] accCosts0 = null;
        ComeFrom[] comeFrom2 = null;
        ComeFrom[] comeFrom1 = new ComeFrom[] { null };
        ComeFrom[] comeFrom0 = null;
        
        for (int n = 0; n < sumSize; n++) {
            int sliceLen = n < minSize
                    ? n + 2
                    : n < maxSize
                            ? minSize + 2
                            : sumSize - n + 2;
            
            accCosts0 = new long[sliceLen];
            comeFrom0 = new ComeFrom[sliceLen];
            
            if (n < listA.size()) {
                accCosts0[0] = accCosts1[0] + gapCostsA[n];
                comeFrom0[0] = new ComeFrom(comeFrom1[0], Direction.FROM_UPPER);
            }
            if (n < listB.size()) {
                accCosts0[accCosts0.length - 1] = accCosts1[accCosts1.length - 1] + gapCostsB[n];
                comeFrom0[comeFrom0.length - 1] = new ComeFrom(comeFrom1[comeFrom1.length - 1], Direction.FROM_LEFT);
            }
            
            final int nf = n;
            final long[] accCosts2f = accCosts2;
            final long[] accCosts1f = accCosts1;
            final long[] accCosts0f = accCosts0;
            final ComeFrom[] comeFrom2f = comeFrom2;
            final ComeFrom[] comeFrom1f = comeFrom1;
            final ComeFrom[] comeFrom0f = comeFrom0;
            
            IntStream.range(1, sliceLen - 1).parallel().forEach(k -> {
                int a = nf < listA.size() ? nf - k : listA.size() - k;
                int b = nf - a - 1;
                
                // 左上からの遷移（つまりリストA, リストBの要素が対応する場合）が最適であると仮置きする。
                int dk2 = (nf <= listA.size()) ? -1 : (nf == listA.size() + 1) ? 0 : 1;
                long minCost = accCosts2f[k + dk2] + diffEvaluator.applyAsInt(listA.get(a), listB.get(b));
                comeFrom0f[k] = new ComeFrom(comeFrom2f[k + dk2], Direction.FROM_UPPER_LEFT);
                
                // 左から遷移した場合（つまりリストBの要素が余剰である場合）のコストを求めて比較する。
                int dk1 = (nf <= listA.size()) ? -1 : 0;
                long tmpCostB = accCosts1f[k + dk1] + gapCostsB[b];
                if (tmpCostB < minCost) {
                    minCost = tmpCostB;
                    comeFrom0f[k] = new ComeFrom(comeFrom1f[k + dk1], Direction.FROM_LEFT);
                }
                
                // 上から遷移した場合（つまりリストAの要素が余剰である場合）のコストを求めて比較する。
                long tmpCostA = accCosts1f[k + dk1 + 1] + gapCostsA[a];
                if (tmpCostA < minCost) {
                    minCost = tmpCostA;
                    comeFrom0f[k] = new ComeFrom(comeFrom1f[k + dk1 + 1], Direction.FROM_UPPER);
                }
                
                accCosts0f[k] = minCost;
            });
            
            accCosts2 = accCosts1;
            accCosts1 = accCosts0;
            comeFrom2 = comeFrom1;
            comeFrom1 = comeFrom0;
        }
        
        return comeFrom0[1];
    }
    
    private List<Pair<Integer>> traceBestRoute(
            List<? extends T> listA,
            List<? extends T> listB,
            ComeFrom comeFrom) {
        
        assert listA != null;
        assert listB != null;
        assert comeFrom != null;
        
        LinkedList<Pair<Integer>> bestRoute = new LinkedList<>();
        int a = listA.size();
        int b = listB.size();
        
        while (comeFrom != null) {
            switch (comeFrom.direction) {
            case FROM_UPPER_LEFT:
                a--;
                b--;
                bestRoute.addFirst(Pair.of(a, b));
                break;
            case FROM_UPPER:
                a--;
                bestRoute.addFirst(Pair.onlyA(a));
                break;
            case FROM_LEFT:
                b--;
                bestRoute.addFirst(Pair.onlyB(b));
                break;
            default:
                throw new AssertionError(comeFrom.direction);
            }
            comeFrom = comeFrom.prev;
        }
        
        return bestRoute;
    }
}
