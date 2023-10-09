package xyz.hotchpotch.hogandiff.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * リスト内における要素の順番に関わりなく、
 * 2つのリスト内の等しい要素同士を対応付ける {@link Matcher} の実装です。<br>
 * <br>
 * <strong>注意：</strong>
 * この実装は、重複要素を持つリストを受け付けません。<br>
 * 
 * @param <T> リストの要素の型
 * @author nmby
 */
/*package*/ class IdentityMatcher<T> extends MatcherBase<T> {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final Function<? super T, ?> idExtractor;
    
    private Map<?, Integer> mapA;
    private Map<?, Integer> mapB;
    
    /*package*/ IdentityMatcher() {
        this(Function.identity());
    }
    
    /*package*/ IdentityMatcher(Function<? super T, ?> extractor) {
        super(null, null);
        assert extractor != null;
        this.idExtractor = extractor;
    }
    
    @Override
    protected void makeIdxPairsPrecheck(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        mapA = IntStream.range(0, listA.size())
                .collect(
                        HashMap::new,
                        (map, i) -> map.put(idExtractor.apply(listA.get(i)), i),
                        Map::putAll);
        mapB = IntStream.range(0, listB.size())
                .collect(
                        HashMap::new,
                        (map, j) -> map.put(idExtractor.apply(listB.get(j)), j),
                        Map::putAll);
        
        if (listA.size() != mapA.size() || listB.size() != mapB.size()) {
            throw new IllegalArgumentException("list has duplicate values.");
        }
        
    }
    
    @Override
    protected List<IntPair> makeIdxPairsMain(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        List<IntPair> result = new ArrayList<>();
        
        mapA.forEach((elemA, i) -> {
            if (mapB.containsKey(elemA)) {
                result.add(IntPair.of(i, mapB.get(elemA)));
                mapB.remove(elemA);
            } else {
                result.add(IntPair.onlyA(i));
            }
        });
        mapB.values().forEach(j -> result.add(IntPair.onlyB(j)));
        
        result.sort(Comparator.naturalOrder());
        return result;
    }
}
