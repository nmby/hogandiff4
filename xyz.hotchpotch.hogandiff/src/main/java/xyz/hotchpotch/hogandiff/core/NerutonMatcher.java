package xyz.hotchpotch.hogandiff.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのリストの要素同士の組み合わせの中で、リスト内における要素の順番に関わりなく
 * 最も一致度の高いペアから対応付けを確定していく {@link Matcher} の実装です。<br>
 * 
 * @param <T> リストの要素の型
 * @author nmby
 */
/*package*/ class NerutonMatcher<T> implements Matcher<T> {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /**
     * 要素同士の差分コストもしくは要素単独の余剰コストを保持する内部計算用の不変クラスです。<br>
     * 
     * @author nmby
     */
    private static class Cost implements Comparable<Cost> {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final Integer idxA;
        private final Integer idxB;
        private final int cost;
        
        private Cost(Integer idxA, Integer idxB, int cost) {
            assert idxA != null || idxB != null;
            assert idxA == null || 0 <= idxA;
            assert idxB == null || 0 <= idxB;
            
            this.idxA = idxA;
            this.idxB = idxB;
            this.cost = cost;
        }
        
        private boolean isPaired() {
            return idxA != null && idxB != null;
        }
        
        @Override
        public int compareTo(Cost other) {
            Objects.requireNonNull(other, "other");
            
            if (cost != other.cost) {
                // コストそのものが異なる場合は、それに基づいて比較する。
                return cost < other.cost ? -1 : 1;
            }
            if (isPaired() && other.isPaired()) {
                // コストが同じでともにペアリング済みの場合
                
                int iA = idxA;
                int iB = idxB;
                int oA = other.idxA;
                int oB = other.idxB;
                
                if (Math.abs(iA - iB) != Math.abs(oA - oB)) {
                    // ペア間の距離が異なる場合は、近い方を「小さい」と判断する。
                    return Math.abs(iA - iB) < Math.abs(oA - oB) ? -1 : 1;
                }
                if (iA + iB != oA + oB) {
                    // 原点からの距離の和が異なる場合は、原点に近い方を「小さい」と判断する。
                    return iA + iB < oA + oB ? -1 : 1;
                }
                if (iA != oA) {
                    // ここまでで差異が無い場合は、A座標が原点に近い方を「小さい」と判断する。
                    return iA < oA ? -1 : 1;
                }
                // ここまで到達しないはず
                throw new AssertionError(String.format("this:%s, other:%s", this, other));
                
            } else if (isPaired() != other.isPaired()) {
                // コストが同じで片方のみペアリング済みの場合は、ペアリング済みの方を「小さい」と判断する。
                return isPaired() ? -1 : 1;
                
            } else {
                // コストが同じでともに単独の場合
                
                if ((idxA == null) != (other.idxA == null)) {
                    // idxAが存在する方を「小さい」と判断する。
                    return idxA != null ? -1 : 1;
                }
                
                int i = idxA != null ? idxA : idxB;
                int o = other.idxA != null ? other.idxA : other.idxB;
                if (i != o) {
                    // 存在する方の原点からの距離が異なる場合は、原点に近い方を「小さい」と判断する。
                    return i < o ? -1 : 1;
                }
                // ここまで到達しないはず
                throw new AssertionError(String.format("this:%s, other:%s", this, other));
            }
        }
    }
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private final ToIntFunction<? super T> gapEvaluator;
    private final ToIntBiFunction<? super T, ? super T> diffEvaluator;
    
    /*package*/ NerutonMatcher(
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
     * この実装は、2つのリストの要素同士の組み合わせの中で
     * 最も一致度の高いペアから対応付けを確定していきます。<br>
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
        
        // まず、全ての組み合わせのコストを計算する。
        Stream<Cost> gapCostsA = IntStream.range(0, listA.size()).parallel()
                .mapToObj(i -> new Cost(i, null, gapEvaluator.applyAsInt(listA.get(i))));
        Stream<Cost> gapCostsB = IntStream.range(0, listB.size()).parallel()
                .mapToObj(j -> new Cost(null, j, gapEvaluator.applyAsInt(listB.get(j))));
        Stream<Cost> diffCosts = IntStream.range(0, listA.size()).parallel()
                .boxed()
                .flatMap(i -> IntStream.range(0, listB.size()).parallel()
                        .mapToObj(j -> new Cost(i, j, diffEvaluator.applyAsInt(listA.get(i), listB.get(j)))));
        
        // これらを結合し、小さい順にソートする。
        LinkedList<Cost> costs = Stream.concat(Stream.concat(gapCostsA, gapCostsB), diffCosts)
                .sorted()
                .collect(Collectors.toCollection(LinkedList::new));
        
        List<Pair<Integer>> pairs = new ArrayList<>();
        while (0 < costs.size()) {
            
            // 小さいものから結果として採用する。
            Cost cost = costs.removeFirst();
            pairs.add(Pair.ofNullable(cost.idxA, cost.idxB));
            
            // 結果として採用された要素を含む候補を除去する。
            if (cost.idxA != null) {
                costs.removeIf(c -> cost.idxA.equals(c.idxA));
            }
            if (cost.idxB != null) {
                costs.removeIf(c -> cost.idxB.equals(c.idxB));
            }
        }
        return pairs;
    }
}
