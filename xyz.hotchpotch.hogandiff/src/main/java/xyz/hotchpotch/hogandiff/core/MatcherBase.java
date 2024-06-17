package xyz.hotchpotch.hogandiff.core;

import java.util.List;
import java.util.Objects;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * {@link Matcher} の基底実装です。<br>
 * 
 * @param <T> リストの要素の型
 * @author nmby
 */
/*package*/ abstract class MatcherBase<T> implements Matcher<T> {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /** 比較対象Aに適用する余剰評価関数 */
    protected final ToIntFunction<? super T> gapEvaluatorA;
    
    /** 比較対象Bに適用する余剰評価関数 */
    protected final ToIntFunction<? super T> gapEvaluatorB;
    
    /** 差分評価関数 */
    protected final ToIntBiFunction<? super T, ? super T> diffEvaluator;
    
    /**
     * コンストラクタ
     * 
     * @param gapEvaluator 余剰評価関数
     * @param diffEvaluator 差分評価関数
     */
    protected MatcherBase(
            ToIntFunction<? super T> gapEvaluator,
            ToIntBiFunction<? super T, ? super T> diffEvaluator) {
        
        this(gapEvaluator, gapEvaluator, diffEvaluator);
    }
    
    /**
     * コンストラクタ
     * 
     * @param gapEvaluatorA 比較対象Aに適用する余剰評価関数
     * @param gapEvaluatorB 比較対象Bに適用する余剰評価関数
     * @param diffEvaluator 差分評価関数
     */
    protected MatcherBase(
            ToIntFunction<? super T> gapEvaluatorA,
            ToIntFunction<? super T> gapEvaluatorB,
            ToIntBiFunction<? super T, ? super T> diffEvaluator) {
        
        this.gapEvaluatorA = gapEvaluatorA;
        this.gapEvaluatorB = gapEvaluatorB;
        this.diffEvaluator = diffEvaluator;
    }
    
    /**
     * 比較処理の冒頭で行うチェック処理。<br>
     * サブクラスで必要な場合はオーバーライドし、チェックNGの場合に例外をスローする。<br>
     * 
     * @param listA 比較対象リストA
     * @param listB 比較対象リストB
     */
    protected void makeIdxPairsPrecheck(
            List<? extends T> listA,
            List<? extends T> listB) {
    }
    
    /**
     * 比較処理の本体。汎用的な事前チェック処理は実施済み。<br>
     * 
     * @param listA 比較対象リストA
     * @param listB 比較対象リストB
     * @return リストAとリストBの対応関係
     */
    protected abstract List<IntPair> makeIdxPairsMain(
            List<? extends T> listA,
            List<? extends T> listB);
    
    /**
     * {@inheritDoc}
     * <br>
     * @throws NullPointerException {@code listA}, {@code listB} のいずれかが {@code null} の場合
     */
    @Override
    public List<IntPair> makeIdxPairs(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        Objects.requireNonNull(listA, "listA");
        Objects.requireNonNull(listB, "listB");
        
        makeIdxPairsPrecheck(listA, listB);
        
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
        
        return makeIdxPairsMain(listA, listB);
    }
}
