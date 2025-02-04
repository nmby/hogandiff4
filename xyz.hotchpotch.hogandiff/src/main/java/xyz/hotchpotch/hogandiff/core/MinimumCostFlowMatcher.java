package xyz.hotchpotch.hogandiff.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 2つのリストの要素同士の一致度の総和が最大になるような（換言すると差異度の総和が最小になるような）
 * 対応付けを行う {@link Matcher} の実装です。<br>
 * 
 * @param <T> リストの要素の型
 * @author nmby
 */
public class MinimumCostFlowMatcher<T> extends MatcherBase<T> {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param gapEvaluator 余剰評価関数
     * @param diffEvaluator 差分評価関数
     */
    /*package*/ MinimumCostFlowMatcher(
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
    /*package*/ MinimumCostFlowMatcher(
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
        
        return new Graph(listA, listB).execute();
    }
    
    private class Graph {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final int sizeA;
        private final int sizeB;
        
        private final int[][] costs;
        
        private final int[] maxFlowsSA;
        private final int[][] maxFlowsAB;
        private final int[] maxFlowsBT;
        
        private final int[] currFlowsSA;
        private final int[][] currFlowsAB;
        private final int[] currFlowsBT;
        
        private Graph(
                List<? extends T> listA,
                List<? extends T> listB) {
            
            assert listA != null;
            assert listB != null;
            assert listA != listB;
            
            sizeA = listA.size();
            sizeB = listB.size();
            
            costs = IntStream.range(0, sizeA + 1).parallel()
                    .mapToObj(i -> IntStream.range(0, sizeB + 1)
                            .map(j -> {
                                if (i < sizeA && j < sizeB) {
                                    return diffEvaluator.applyAsInt(listA.get(i), listB.get(j));
                                } else if (i < sizeA) {
                                    return gapEvaluatorA.applyAsInt(listA.get(i));
                                } else if (j < sizeB) {
                                    return gapEvaluatorB.applyAsInt(listB.get(j));
                                } else {
                                    return 0;
                                }
                            })
                            .toArray())
                    .toArray(int[][]::new);
            
            maxFlowsSA = IntStream.range(0, sizeA + 1)
                    .map(i -> i < sizeA ? 1 : sizeB)
                    .toArray();
            maxFlowsAB = IntStream.range(0, sizeA + 1)
                    .mapToObj(i -> IntStream.range(0, sizeB + 1)
                            .map(j -> i < sizeA || j < sizeB ? 1 : Math.min(sizeA, sizeB))
                            .toArray())
                    .toArray(int[][]::new);
            maxFlowsBT = IntStream.range(0, sizeB + 1)
                    .map(j -> j < sizeB ? 1 : sizeA)
                    .toArray();
            
            currFlowsSA = new int[sizeA + 1];
            currFlowsAB = new int[sizeA + 1][sizeB + 1];
            currFlowsBT = new int[sizeB + 1];
        }
        
        private List<IntPair> execute() {
            while (update(calcBestPath())) {
            }
            return traceBestRoute();
        }
        
        private List<Integer> calcBestPath() {
            int[] bestCostsA = new int[sizeA + 1];
            int[] bestCostsB = new int[sizeB + 1];
            Arrays.fill(bestCostsA, Integer.MAX_VALUE);
            Arrays.fill(bestCostsB, Integer.MAX_VALUE);
            int bestCostT = Integer.MAX_VALUE;
            
            @SuppressWarnings("unchecked")
            List<Integer>[] bestPathsA = new List[sizeA + 1];
            @SuppressWarnings("unchecked")
            List<Integer>[] bestPathsB = new List[sizeB + 1];
            List<Integer> bestPathT = List.of();
            
            Set<Integer> nextNodesA = new HashSet<>(sizeA + 1);
            Set<Integer> nextNodesB = new HashSet<>(sizeB + 1);
            
            // [S]から移動可能な[A]を抽出し、その到達可能コストとパスを設定する。
            for (int i = 0; i < sizeA + 1; i++) {
                if (0 < maxFlowsSA[i] - currFlowsSA[i]) {
                    nextNodesA.add(i);
                    bestCostsA[i] = 0;
                    bestPathsA[i] = List.of();
                }
            }
            
            while (!nextNodesA.isEmpty()) {
                nextNodesB.clear();
                for (int i : nextNodesA) {
                    // [A]から移動可能な[B]を抽出し、その到達可能コストとパスを更新する。
                    for (int j = 0; j < sizeB + 1; j++) {
                        if (0 < maxFlowsAB[i][j] - currFlowsAB[i][j]) {
                            int newCost = bestCostsA[i] + costs[i][j];
                            if (newCost < bestCostsB[j]) {
                                nextNodesB.add(j);
                                bestCostsB[j] = newCost;
                                bestPathsB[j] = new ArrayList<>(bestPathsA[i]);
                                bestPathsB[j].add(i);
                            }
                        }
                    }
                }
                
                nextNodesA.clear();
                for (int j : nextNodesB) {
                    // [B]から[T]に移動可能な場合は、その到達可能コストとパスを更新する。
                    if (0 < maxFlowsBT[j] - currFlowsBT[j]) {
                        if (bestCostsB[j] < bestCostT) {
                            bestCostT = bestCostsB[j];
                            bestPathT = new ArrayList<>(bestPathsB[j]);
                            bestPathT.add(j);
                        }
                    }
                    
                    // [B]から移動可能な[A]を抽出し、その到達可能コストとパスを更新する。
                    for (int i = 0; i < sizeA + 1; i++) {
                        if (0 < currFlowsAB[i][j]) {
                            int newCost = bestCostsB[j] - costs[i][j];
                            if (newCost < bestCostsA[i]) {
                                nextNodesA.add(i);
                                bestCostsA[i] = newCost;
                                bestPathsA[i] = new ArrayList<>(bestPathsB[j]);
                                bestPathsA[i].add(j);
                            }
                        }
                    }
                }
            }
            return bestPathT;
        }
        
        private boolean update(List<Integer> bestPath) {
            assert bestPath != null;
            assert bestPath.size() % 2 == 0;
            
            if (bestPath.isEmpty()) {
                return false;
            }
            
            Queue<Integer> path = new ArrayDeque<>(bestPath);
            
            int i = path.remove();
            int j = path.remove();
            currFlowsSA[i]++;
            currFlowsAB[i][j]++;
            
            while (!path.isEmpty()) {
                i = path.remove();
                currFlowsAB[i][j]--;
                
                j = path.remove();
                currFlowsAB[i][j]++;
            }
            
            currFlowsBT[j]++;
            return true;
        }
        
        private List<IntPair> traceBestRoute() {
            assert IntStream.range(0, sizeA + 1).allMatch(i -> currFlowsSA[i] == maxFlowsSA[i]);
            assert IntStream.range(0, sizeB + 1).allMatch(j -> currFlowsBT[j] == maxFlowsBT[j]);
            assert Arrays.stream(currFlowsAB).flatMapToInt(Arrays::stream).sum() == sizeA + sizeB;
            
            return IntStream.range(0, sizeA + 1)
                    .mapToObj(i -> IntStream.range(0, sizeB + 1)
                            .filter(j -> 0 < currFlowsAB[i][j])
                            .mapToObj(j -> i < sizeA && j < sizeB ? IntPair.of(i, j)
                                    : i < sizeA ? IntPair.onlyA(i)
                                            : j < sizeB ? IntPair.onlyB(j)
                                                    : IntPair.empty())
                            .filter(Predicate.not(IntPair::isEmpty)))
                    .flatMap(Function.identity())
                    .sorted()
                    .toList();
        }
    }
}
