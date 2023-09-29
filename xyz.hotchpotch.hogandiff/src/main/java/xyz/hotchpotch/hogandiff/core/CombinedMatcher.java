package xyz.hotchpotch.hogandiff.core;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 非ペア要素がなくなるまで複数のマッチャーで順に対応付けを行う {@link Matcher} の実装です。<br>
 * <br>
 * この実装における {@link #makePairs(List, List)} メソッドでは、
 * インスタンス生成時に指定された複数のマッチャーのうち、まず一つめのマッチャーで対応付けを行います。<br>
 * ペアを構成しなかった要素（残要素）を対象に、二つめのマッチャーで対応付けを行います。<br>
 * さらに残要素を三つ目以降のマッチャーで対応付けを行います。<br>
 * 
 * @author nmby
 */
/*package*/ class CombinedMatcher<T> extends MatcherBase<T> {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final List<Matcher<T>> matchers;
    
    /*package*/ CombinedMatcher(
            List<Matcher<T>> matchers) {
        
        super(null, null);
        
        assert matchers != null;
        
        this.matchers = List.copyOf(matchers);
    }
    
    @Override
    protected List<IntPair> makePairs3(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        List<IntPair> result = new ArrayList<>();
        
        List<Integer> mappingA = IntStream.range(0, listA.size())
                .mapToObj(Integer::valueOf)
                .toList();
        List<Integer> mappingB = IntStream.range(0, listB.size())
                .mapToObj(Integer::valueOf)
                .toList();
        
        List<? extends T> tmpA = listA;
        List<? extends T> tmpB = listB;
        
        for (Matcher<T> matcher : matchers) {
            List<IntPair> res = matcher.makePairs(tmpA, tmpB);
            BitSet pairedA = new BitSet(tmpA.size());
            BitSet pairedB = new BitSet(tmpB.size());
            
            for (IntPair pair : res) {
                if (pair.isPaired()) {
                    result.add(IntPair.of(
                            mappingA.get(pair.a()),
                            mappingB.get(pair.b())));
                    
                    pairedA.set(pair.a());
                    pairedB.set(pair.b());
                }
            }
            
            mappingA = IntStream.range(0, tmpA.size())
                    .filter(i -> !pairedA.get(i))
                    .mapToObj(mappingA::get)
                    .toList();
            mappingB = IntStream.range(0, tmpB.size())
                    .filter(j -> !pairedB.get(j))
                    .mapToObj(mappingB::get)
                    .toList();
            tmpA = IntStream.range(0, tmpA.size())
                    .filter(i -> !pairedA.get(i))
                    .mapToObj(tmpA::get)
                    .toList();
            tmpB = IntStream.range(0, tmpB.size())
                    .filter(j -> !pairedB.get(j))
                    .mapToObj(tmpB::get)
                    .toList();
            
            if (tmpA.isEmpty() && tmpB.isEmpty()) {
                return result;
                
            } else if (tmpA.isEmpty() || tmpB.isEmpty()) {
                break;
            }
        }
        
        result.addAll(mappingA.stream().map(IntPair::onlyA).toList());
        result.addAll(mappingB.stream().map(IntPair::onlyB).toList());
        
        return result;
    }
    
}
