package xyz.hotchpotch.hogandiff.core;

import java.util.LinkedList;
import java.util.List;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 2つのリスト間の編集距離が最小となるように要素同士を対応付ける {@link Matcher} の実装です。<br>
 * 文字列（文字を要素とするリスト）同士のマッチングだけでなく、
 * 任意の型の要素のリスト同士のマッチングに利用することができます。<br>
 * 
 * @param <T> リストの要素の型
 * @author nmby
 */
/*package*/ class MinimumEditDistanceMatcher<T> extends MatcherBase<T> {
    
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
     * エディットグラフ上の各点における遷移経路を表します。<br>
     * 
     * @author nmby
     */
    private static sealed interface ComeFrom
            permits ComeFrom.Upper, ComeFrom.Left, ComeFrom.UpperLeft {
        
        // [static members] ----------------------------------------------------
        
        /**
         * エディットグラフの上から遷移してきたことを表します。
         * 
         * @param prev 遷移元ノード
         */
        static record Upper(ComeFrom prev) implements ComeFrom {
        }
        
        /**
         * エディットグラフの左から遷移してきたことを表します。
         * 
         * @param prev 遷移元ノード
         */
        static record Left(ComeFrom prev) implements ComeFrom {
        }
        
        /**
         * エディットグラフの左上から遷移してきたことを表します。
         * 
         * @param prev 遷移元ノード
         */
        static record UpperLeft(ComeFrom prev) implements ComeFrom {
        }
        
        // [instance members] --------------------------------------------------
        
        default Direction direction() {
            return switch (this) {
                case Upper from -> Direction.FROM_UPPER;
                case Left from -> Direction.FROM_LEFT;
                case UpperLeft from -> Direction.FROM_UPPER_LEFT;
            };
        }
        
        ComeFrom prev();
    }
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /**
     * コンストラクタ
     * 
     * @param gapEvaluator 余剰評価関数
     * @param diffEvaluator 差分評価関数
     */
    /*package*/ MinimumEditDistanceMatcher(
            ToIntFunction<? super T> gapEvaluator,
            ToIntBiFunction<? super T, ? super T> diffEvaluator) {
        
        super(gapEvaluator, diffEvaluator);
        
        assert gapEvaluator != null;
        assert diffEvaluator != null;
    }
    
    /**
     * コンストラクタ
     * 
     * @param gapEvaluatorA 比較対象Aに適用する余剰評価関数
     * @param gapEvaluatorB 比較対象Bに適用する余剰評価関数
     * @param diffEvaluator 差分評価関数
     */
    /*package*/ MinimumEditDistanceMatcher(
            ToIntFunction<? super T> gapEvaluatorA,
            ToIntFunction<? super T> gapEvaluatorB,
            ToIntBiFunction<? super T, ? super T> diffEvaluator) {
        
        super(gapEvaluatorA, gapEvaluatorB, diffEvaluator);
        
        assert gapEvaluatorA != null;
        assert gapEvaluatorB != null;
        assert diffEvaluator != null;
    }
    
    protected List<IntPair> makeIdxPairsMain(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        // 親クラスでバリデーションチェック実施済み
        
        ComeFrom bestRoute = calcBestRoute(listA, listB);
        
        List<IntPair> pairs = traceBestRoute(listA, listB, bestRoute);
        
        return pairs;
    }
    
    private ComeFrom calcBestRoute(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        assert listA != null;
        assert listB != null;
        assert listA != listB;
        
        // 1. リストA, リストBの要素の余剰コストを計算する。
        int[] gapCostsA = listA.parallelStream().mapToInt(gapEvaluatorA::applyAsInt).toArray();
        int[] gapCostsB = listB.parallelStream().mapToInt(gapEvaluatorB::applyAsInt).toArray();
        
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
        int sliceLen1 = 1;
        
        for (int n = 0; n < sumSize; n++) {
            int sliceLen0 = n < minSize
                    ? n + 2
                    : n < maxSize
                            ? minSize + 2
                            : sumSize - n + 2;
            
            // FIXME: [No.12 性能改善] ループごとにメモリ領域を確保するのではなく使い回す方式に変更する
            accCosts0 = new long[sliceLen0];
            comeFrom0 = new ComeFrom[sliceLen0];
            
            if (n < listA.size()) {
                accCosts0[0] = accCosts1[0] + gapCostsA[n];
                comeFrom0[0] = new ComeFrom.Upper(comeFrom1[0]);
            }
            if (n < listB.size()) {
                accCosts0[sliceLen0 - 1] = accCosts1[sliceLen1 - 1] + gapCostsB[n];
                comeFrom0[sliceLen0 - 1] = new ComeFrom.Left(comeFrom1[sliceLen1 - 1]);
            }
            
            final int nf = n;
            final long[] accCosts2f = accCosts2;
            final long[] accCosts1f = accCosts1;
            final long[] accCosts0f = accCosts0;
            final ComeFrom[] comeFrom2f = comeFrom2;
            final ComeFrom[] comeFrom1f = comeFrom1;
            final ComeFrom[] comeFrom0f = comeFrom0;
            
            IntStream.range(1, sliceLen0 - 1).parallel().forEach(k -> {
                int a = nf < listA.size() ? nf - k : listA.size() - k;
                int b = nf - a - 1;
                
                //// それぞれの方向から遷移した場合のコストを計算する。
                int dk1 = (nf <= listA.size()) ? -1 : 0;
                int dk2 = (nf <= listA.size()) ? -1 : (nf == listA.size() + 1) ? 0 : 1;
                
                // 左上からの遷移（つまりリストA, リストBの要素が対応する場合）が最適であると仮置きする。
                long tmpCostAB = accCosts2f[k + dk2] + diffEvaluator.applyAsInt(listA.get(a), listB.get(b));
                
                // 左から遷移した場合（つまりリストBの要素が余剰である場合）のコストを求めて比較する。
                long tmpCostB = accCosts1f[k + dk1] + gapCostsB[b];
                
                // 上から遷移した場合（つまりリストAの要素が余剰である場合）のコストを求めて比較する。
                long tmpCostA = accCosts1f[k + dk1 + 1] + gapCostsA[a];
                
                //// 最も小さいコストの遷移元を採用する。
                if (tmpCostA < tmpCostB && tmpCostA < tmpCostAB) {
                    comeFrom0f[k] = new ComeFrom.Upper(comeFrom1f[k + dk1 + 1]);
                    accCosts0f[k] = tmpCostA;
                    
                } else if (tmpCostB <= tmpCostA && tmpCostB < tmpCostAB) {
                    comeFrom0f[k] = new ComeFrom.Left(comeFrom1f[k + dk1]);
                    accCosts0f[k] = tmpCostB;
                    
                } else {
                    comeFrom0f[k] = new ComeFrom.UpperLeft(comeFrom2f[k + dk2]);
                    accCosts0f[k] = tmpCostAB;
                }
            });
            
            accCosts2 = accCosts1;
            accCosts1 = accCosts0;
            comeFrom2 = comeFrom1;
            comeFrom1 = comeFrom0;
            sliceLen1 = sliceLen0;
        }
        
        return comeFrom0[1];
    }
    
    private List<IntPair> traceBestRoute(
            List<? extends T> listA,
            List<? extends T> listB,
            ComeFrom comeFrom) {
        
        assert listA != null;
        assert listB != null;
        assert comeFrom != null;
        
        LinkedList<IntPair> bestRoute = new LinkedList<>();
        int a = listA.size();
        int b = listB.size();
        
        while (comeFrom != null) {
            switch (comeFrom.direction()) {
                case FROM_UPPER_LEFT:
                    a--;
                    b--;
                    bestRoute.addFirst(IntPair.of(a, b));
                    break;
                case FROM_UPPER:
                    a--;
                    bestRoute.addFirst(IntPair.onlyA(a));
                    break;
                case FROM_LEFT:
                    b--;
                    bestRoute.addFirst(IntPair.onlyB(b));
                    break;
                default:
                    throw new AssertionError(comeFrom.direction());
            }
            comeFrom = comeFrom.prev();
        }
        
        return bestRoute;
    }
}
