package xyz.hotchpotch.hogandiff.core;

import java.util.List;
import java.util.Objects;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.util.IntPair;

/*package*/ abstract class MatcherBase<T> implements Matcher<T> {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    protected final ToIntFunction<? super T> gapEvaluator;
    protected final ToIntBiFunction<? super T, ? super T> diffEvaluator;
    
    protected MatcherBase(
            ToIntFunction<? super T> gapEvaluator,
            ToIntBiFunction<? super T, ? super T> diffEvaluator) {
        
        this.gapEvaluator = gapEvaluator;
        this.diffEvaluator = diffEvaluator;
    }
    
    protected void makeIdxPairsPrecheck(
            List<? extends T> listA,
            List<? extends T> listB) {
    }
    
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
