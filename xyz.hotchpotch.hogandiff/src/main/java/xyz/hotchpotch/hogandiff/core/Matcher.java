package xyz.hotchpotch.hogandiff.core;

import java.util.List;
import java.util.Objects;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 2つのリストの要素同士の最適な組み合わせを返すマッチャーを表します。<br>
 * これは、{@link #makePairs(List, List)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
 * @param <T> リストの要素の型
 * @author nmby
 */
@FunctionalInterface
public interface Matcher<T> {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /**
     * 2つのリストの要素同士をリストの先頭から順に対応付けるマッチャーを返します。<br>
     *
     * @param <T> リストの要素の型
     * @return 新しいマッチャー
     */
    public static <T> Matcher<T> simpleMatcherOf() {
        return new SimpleMatcher<>();
    }
    
    /**
     * 2つのリスト間の編集距離が最小となるように要素同士を対応付けるマッチャーを返します。<br>
     * 
     * @param <T> リストの要素の型
     * @param gapEvaluator 余剰コスト評価関数
     * @param diffEvaluator 差分コスト評価関数
     * @return 新しいマッチャー
     * @throws NullPointerException
     *              {@code gapEvaluator}, {@code diffEvaluator} のいずれかが {@code null} の場合
     */
    public static <T> Matcher<T> minimumEditDistanceMatcherOf(
            ToIntFunction<? super T> gapEvaluator,
            ToIntBiFunction<? super T, ? super T> diffEvaluator) {
        
        Objects.requireNonNull(gapEvaluator, "gapEvaluator");
        Objects.requireNonNull(diffEvaluator, "diffEvaluator");
        
        return new MinimumEditDistanceMatcher2<>(gapEvaluator, diffEvaluator);
    }
    
    /**
     * 2つのリストの要素同士の組み合わせの中で、リスト内における要素の順番に関わりなく
     * 最も一致度の高いペアから対応付けを確定していくマッチャーを返します。<br>
     * 
     * @param <T> リストの要素の型
     * @param gapEvaluator 余剰コスト評価関数
     * @param diffEvaluator 差分コスト評価関数
     * @return 新しいマッチャー
     * @throws NullPointerException
     *              {@code gapEvaluator}, {@code diffEvaluator} のいずれかが {@code null} の場合
     */
    public static <T> Matcher<T> greedyMatcherOf(
            ToIntFunction<? super T> gapEvaluator,
            ToIntBiFunction<? super T, ? super T> diffEvaluator) {
        
        Objects.requireNonNull(gapEvaluator, "gapEvaluator");
        Objects.requireNonNull(diffEvaluator, "diffEvaluator");
        
        return new GreedyMatcher<>(gapEvaluator, diffEvaluator);
    }
    
    /**
     * リスト内における要素の順番に関わりなく
     * 2つのリストの等しい要素同士を対応付けるマッチャーを返します。<br>
     * 
     * @param <T> リストの要素の型
     * @return 新しいマッチャー
     */
    public static <T> Matcher<T> identityMatcher() {
        return new IdentityMatcher<>();
    }
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /**
     * 2つのリストの要素同士の最適な組み合わせを返します。<br>
     * 「最適な」の定義は {@link Matcher} の実装ごとに異なります。<br>
     * 
     * @param listA リストA
     * @param listB リストB
     * @return リストA, Bの要素同士の最適な組み合わせを表す、要素のインデックスのペアのリスト
     */
    List<IntPair> makePairs(
            List<? extends T> listA,
            List<? extends T> listB);
}
