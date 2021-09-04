package xyz.hotchpotch.hogandiff.core;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 2つのリストの要素同士を、リストの先頭から順に対応付ける {@link Matcher} の実装です。<br>
 *
 * @param <T> リストの要素の型
 * @author nmby
 */
/*package*/ class SimpleMatcher<T> implements Matcher<T> {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /*package*/ SimpleMatcher() {
    }
    
    /**
     * {@inheritDoc}
     * <br>
     * この実装は、2つのリストの要素同士をリストの先頭から組み合わせていきます。<br>
     * 
     * @throws NullPointerException {@code listA}, {@code listB} のいずれかが {@code null} の場合
     */
    @Override
    public List<IntPair> makePairs(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        Objects.requireNonNull(listA, "listA");
        Objects.requireNonNull(listB, "listB");
        
        return IntStream.range(0, Math.max(listA.size(), listB.size()))
                .mapToObj(n -> listA.size() <= n
                        ? IntPair.onlyB(n)
                        : listB.size() <= n
                                ? IntPair.onlyA(n)
                                : IntPair.of(n, n))
                .toList();
    }
}
