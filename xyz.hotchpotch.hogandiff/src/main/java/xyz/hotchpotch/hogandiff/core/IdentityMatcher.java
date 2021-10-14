package xyz.hotchpotch.hogandiff.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * リスト内における要素の順番に関わりなく、
 * 2つのリストの等しい要素同士を対応付ける {@link Matcher} の実装です。<br>
 * 
 * @param <T> リストの要素の型
 * @author nmby
 */
/*package*/ class IdentityMatcher<T> implements Matcher<T> {
    
    // [static members] ********************************************************
    
    private static final Comparator<IntPair> PairComparator = (p1, p2) -> {
        if (p1.isPaired() && p2.isPaired()) {
            return Integer.compare(p1.a(), p2.a());
        }
        if (p1.isPaired() != p2.isPaired()) {
            return p1.isPaired() ? -1 : 1;
        }
        if (p1.isOnlyA() != p2.isOnlyA()) {
            return p1.isOnlyA() ? -1 : 1;
        }
        return p1.isOnlyA()
                ? Integer.compare(p1.a(), p2.a())
                : Integer.compare(p1.b(), p2.b());
    };
    
    // [instance members] ******************************************************
    
    /*package*/ IdentityMatcher() {
    }
    
    /**
     * {@inheritDoc}
     * <br>
     * この実装は、リスト内における要素の順番に関わりなく、
     * 2つのリストの等しい要素同士を対応付けます。<br>
     * 等しいか否かは {@link Objects#equals} により判断されます。<br>
     * <br>
     * <strong>注意：</strong>
     * この実装は、重複要素を持つリストを受け付けません。<br>
     * 
     * @throws NullPointerException {@code listA}, {@code listB} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException {@code listA}, {@code listB} のいずれかに重複要素が含まれる場合
     */
    @Override
    public List<IntPair> makePairs(
            List<? extends T> listA,
            List<? extends T> listB) {
        
        Objects.requireNonNull(listA, "listA");
        Objects.requireNonNull(listB, "listB");
        
        Map<? extends T, Integer> mapA = IntStream.range(0, listA.size())
                .collect(
                        HashMap::new,
                        (map, i) -> map.put(listA.get(i), i),
                        Map::putAll);
        Map<? extends T, Integer> mapB = IntStream.range(0, listB.size())
                .collect(
                        HashMap::new,
                        (map, i) -> map.put(listB.get(i), i),
                        Map::putAll);
        
        if (listA.size() != mapA.size() || listB.size() != mapB.size()) {
            throw new IllegalArgumentException("list has duplicate values.");
        }
        
        if (listA == listB) {
            return IntStream.range(0, listA.size())
                    .mapToObj(n -> IntPair.of(n, n))
                    .toList();
        }
        
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
        
        result.sort(PairComparator);
        return result;
    }
}
