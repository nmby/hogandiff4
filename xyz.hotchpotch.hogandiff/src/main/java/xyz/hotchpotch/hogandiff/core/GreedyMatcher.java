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

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 2つのリストの要素同士の組み合わせの中で、リスト内における要素の順番に関わりなく
 * 最も一致度の高いペアから対応付けを確定していく {@link Matcher} の実装です。<br>
 * 
 * @param <T> リストの要素の型
 * @author nmby
 */
/*package*/ class GreedyMatcher<T> extends MatcherBase<T> {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /**
     * 要素同士の差分コストもしくは要素単独の余剰コストを保持する内部計算用の不変クラスです。<br>
     * 
     * @author nmby
     */
    // java16で正式導入されたRecordを使ってみる。
    private static record Cost(IntPair idxs, int cost)
            implements Comparable<Cost> {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private Cost {
            assert idxs != null;
        }
        
        @Override
        public int compareTo(Cost other) {
            Objects.requireNonNull(other, "other");
            
            if (cost != other.cost) {
                // コストそのものが異なる場合は、それに基づいて比較する。
                return cost < other.cost ? -1 : 1;
            }
            if (idxs.isPaired() && other.idxs.isPaired()) {
                // コストが同じでともにペアリング済みの場合
                
                int iA = idxs.a();
                int iB = idxs.b();
                int oA = other.idxs.a();
                int oB = other.idxs.b();
                
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
                throw new AssertionError("this:%s, other:%s".formatted(this, other));
                
            } else if (idxs.isPaired() != other.idxs.isPaired()) {
                // コストが同じで片方のみペアリング済みの場合は、ペアリング済みの方を「小さい」と判断する。
                return idxs.isPaired() ? -1 : 1;
                
            } else {
                // コストが同じでともに単独の場合
                
                if (idxs.hasA() != other.idxs.hasA()) {
                    // idxAが存在する方を「小さい」と判断する。
                    return idxs.hasA() ? -1 : 1;
                }
                
                int i = idxs.hasA() ? idxs.a() : idxs.b();
                int o = other.idxs.hasA() ? other.idxs.a() : other.idxs.b();
                if (i != o) {
                    // 存在する方の原点からの距離が異なる場合は、原点に近い方を「小さい」と判断する。
                    return i < o ? -1 : 1;
                }
                // ここまで到達しないはず
                throw new AssertionError("this:%s, other:%s".formatted(this, other));
            }
        }
    }
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /*package*/ GreedyMatcher(
            ToIntFunction<? super T> gapEvaluator,
            ToIntBiFunction<? super T, ? super T> diffEvaluator) {
        
        super(gapEvaluator, diffEvaluator);
        
        assert gapEvaluator != null;
        assert diffEvaluator != null;
    }
    
    protected List<IntPair> makeIdxPairsMain(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        // まず、全ての組み合わせのコストを計算する。
        Stream<Cost> gapCostsA = IntStream.range(0, listA.size()).parallel()
                .mapToObj(i -> new Cost(IntPair.onlyA(i), gapEvaluator.applyAsInt(listA.get(i))));
        Stream<Cost> gapCostsB = IntStream.range(0, listB.size()).parallel()
                .mapToObj(j -> new Cost(IntPair.onlyB(j), gapEvaluator.applyAsInt(listB.get(j))));
        Stream<Cost> diffCosts = IntStream.range(0, listA.size()).parallel()
                .boxed()
                .flatMap(i -> IntStream.range(0, listB.size()).parallel()
                        .mapToObj(j -> new Cost(
                                IntPair.of(i, j),
                                diffEvaluator.applyAsInt(listA.get(i), listB.get(j)))));
        
        // これらを結合し、小さい順にソートする。
        LinkedList<Cost> costs = Stream.concat(Stream.concat(gapCostsA, gapCostsB), diffCosts)
                .sorted()
                .collect(Collectors.toCollection(LinkedList::new));
        
        List<IntPair> pairs = new ArrayList<>();
        while (0 < costs.size()) {
            
            // 小さいものから結果として採用する。
            Cost cost = costs.removeFirst();
            pairs.add(cost.idxs);
            
            // 結果として採用された要素を含む候補を除去する。
            if (cost.idxs.hasA()) {
                costs.removeIf(c -> c.idxs.hasA() && c.idxs.a() == cost.idxs.a());
            }
            if (cost.idxs.hasB()) {
                costs.removeIf(c -> c.idxs.hasB() && c.idxs.b() == cost.idxs.b());
            }
        }
        return pairs;
    }
}
