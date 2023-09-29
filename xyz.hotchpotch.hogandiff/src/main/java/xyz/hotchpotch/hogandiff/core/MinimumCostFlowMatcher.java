package xyz.hotchpotch.hogandiff.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.util.IntPair;

public class MinimumCostFlowMatcher<T> implements Matcher<T> {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
    // [instance members] ******************************************************
    
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
            
            costs = IntStream.range(0, sizeA + 1)
                    .mapToObj(i -> IntStream.range(0, sizeB + 1)
                            .map(j -> {
                                if (i < sizeA && j < sizeB) {
                                    return diffEvaluator.applyAsInt(listA.get(i), listB.get(j));
                                } else if (i < sizeA) {
                                    return gapEvaluator.applyAsInt(listA.get(i));
                                } else if (j < sizeB) {
                                    return gapEvaluator.applyAsInt(listB.get(j));
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
            System.out.println("[PRE]------------------------------------------------------");
            System.out.println("sizeA: " + sizeA);
            System.out.println("sizeB: " + sizeB);
            System.out.println();
            
            System.out.println("[START]----------------------------------------------------");
            while (true) {
                System.out.println("--------------------------------");
                System.out.print(this);
                List<Integer> bestPath = calcBestPath();
                System.out.println("bestPath: " + bestPath);
                System.out.println();
                boolean isUpdated = update(bestPath);
                
                if (!isUpdated) {
                    break;
                }
            }
            
            System.out.println("[END]------------------------------------------------------");
            System.out.println(this);
            System.out.println();
            
            List<IntPair> pairs = traceBestRoute();
            System.out.println("pairs: " + pairs);
            
            return pairs;
        }
        
        private List<Integer> calcBestPath() {
            int[] bestCostsA = IntStream.range(0, sizeA + 1).map(i -> Integer.MAX_VALUE).toArray();
            int[] bestCostsB = IntStream.range(0, sizeB + 1).map(j -> Integer.MAX_VALUE).toArray();
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
                System.out.println("*** layer-A ***");
                nextNodesB.clear();
                for (int i : nextNodesA) {
                    // [A]から移動可能な[B]を抽出し、その到達可能コストとパスを更新する。
                    for (int j = 0; j < sizeB + 1; j++) {
                        if (0 < maxFlowsAB[i][j] - currFlowsAB[i][j]) {
                            int newCost = bestCostsA[i] + costs[i][j];
                            System.out.print("(%d -> %d) cost:%d ".formatted(i, j, newCost));
                            if (newCost < bestCostsB[j]) {
                                System.out.println("Best!!");
                                nextNodesB.add(j);
                                bestCostsB[j] = newCost;
                                bestPathsB[j] = new ArrayList<>(bestPathsA[i]);
                                bestPathsB[j].add(i);
                            } else {
                                System.out.println("ng(%d)".formatted(bestCostsB[j]));
                            }
                        }
                    }
                }
                
                System.out.println("*** layer-B ***");
                nextNodesA.clear();
                for (int j : nextNodesB) {
                    // [B]から[T]に移動可能な場合は、その到達可能コストとパスを更新する。
                    if (0 < maxFlowsBT[j] - currFlowsBT[j]) {
                        System.out.print("(%d -> T) cost:%d ".formatted(j, bestCostsB[j]));
                        if (bestCostsB[j] < bestCostT) {
                            System.out.println("Best!!");
                            bestCostT = bestCostsB[j];
                            bestPathT = new ArrayList<>(bestPathsB[j]);
                            bestPathT.add(j);
                        } else {
                            System.out.println("ng(%d)".formatted(bestCostT));
                        }
                    }
                    
                    // [B]から移動可能な[A]を抽出し、その到達可能コストとパスを更新する。
                    for (int i = 0; i < sizeA + 1; i++) {
                        if (0 < currFlowsAB[i][j]) {
                            int newCost = bestCostsB[j] - costs[i][j];
                            System.out.print("(%d <- %d) cost:%d ".formatted(i, j, newCost));
                            if (newCost < bestCostsA[i]) {
                                System.out.println("Best!!");
                                nextNodesA.add(i);
                                bestCostsA[i] = newCost;
                                bestPathsA[i] = new ArrayList<>(bestPathsB[j]);
                                bestPathsA[i].add(j);
                            } else {
                                System.out.println("ng(%d)".formatted(bestCostsA[i]));
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
                int i2 = path.remove();
                int j2 = path.remove();
                currFlowsAB[i2][j]--;
                currFlowsAB[i2][j2]++;
                j = j2;
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
        
        @Override
        public String toString() {
            StringBuilder str = new StringBuilder();
            
            str.append("S->A :").append(BR);
            str.append(Arrays.toString(
                    IntStream.range(0, sizeA + 1)
                            .mapToObj(i -> "%d/%d".formatted(currFlowsSA[i], maxFlowsSA[i]))
                            .toArray()))
                    .append(BR).append(BR);
            
            for (int i = 0; i < sizeA + 1; i++) {
                int ii = i;
                str.append(Arrays.toString(
                        IntStream.range(0, sizeB + 1)
                                .mapToObj(j -> "%d/%d".formatted(currFlowsAB[ii][j], maxFlowsAB[ii][j]))
                                .toArray(String[]::new)))
                        .append(BR);
            }
            str.append(BR);
            
            str.append("B->T :").append(BR);
            str.append(Arrays.toString(
                    IntStream.range(0, sizeB + 1)
                            .mapToObj(j -> "%d/%d".formatted(currFlowsBT[j], maxFlowsBT[j]))
                            .toArray()))
                    .append(BR).append(BR);
            
            return str.toString();
        }
    }
    
    private final ToIntFunction<? super T> gapEvaluator;
    private final ToIntBiFunction<? super T, ? super T> diffEvaluator;
    
    /*package*/ MinimumCostFlowMatcher(
            ToIntFunction<? super T> gapEvaluator,
            ToIntBiFunction<? super T, ? super T> diffEvaluator) {
        
        assert gapEvaluator != null;
        assert diffEvaluator != null;
        
        this.gapEvaluator = gapEvaluator;
        this.diffEvaluator = diffEvaluator;
    }
    
    @Override
    public List<IntPair> makePairs(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        Objects.requireNonNull(listA, "listA");
        Objects.requireNonNull(listB, "listB");
        
        if (listA.isEmpty() && listB.isEmpty()) {
            return List.of();
        }
        if (listA == listB) {
            return IntStream.range(0, listA.size())
                    .mapToObj(n -> IntPair.of(n, n))
                    .toList();
        }
        if (listA.isEmpty()) {
            return IntStream.range(0, listB.size()).mapToObj(IntPair::onlyB).toList();
        }
        if (listB.isEmpty()) {
            return IntStream.range(0, listA.size()).mapToObj(IntPair::onlyA).toList();
        }
        
        Graph graph = new Graph(listA, listB);
        return graph.execute();
    }
}
