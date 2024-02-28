package xyz.hotchpotch.hogandiff.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

class CombinedMatcherTest {
    
    // [static members] ********************************************************
    
    // "A" から始まる要素同士だけを単純マッチングさせるマッチャー
    private static Matcher<String> matcherA = (list1, list2) -> Matcher
            .identityMatcherOf().makeIdxPairs(list1, list2).stream()
            .flatMap(p -> !p.isPaired()
                    ? Stream.of(p)
                    : list1.get(p.a()).startsWith("A") && list2.get(p.b()).startsWith("A")
                            ? Stream.of(p)
                            : Stream.of(IntPair.onlyA(p.a()), IntPair.onlyB(p.b())))
            .toList();
    
    // "B" から始まる要素同士だけを単純マッチングさせるマッチャー
    private static Matcher<String> matcherB = (list1, list2) -> Matcher
            .identityMatcherOf().makeIdxPairs(list1, list2).stream()
            .flatMap(p -> !p.isPaired()
                    ? Stream.of(p)
                    : list1.get(p.a()).startsWith("B") && list2.get(p.b()).startsWith("B")
                            ? Stream.of(p)
                            : Stream.of(IntPair.onlyA(p.a()), IntPair.onlyB(p.b())))
            .toList();
    
    // [instance members] ******************************************************
    
    @Test
    void test() {
        Matcher<String> testee = new CombinedMatcher<>(List.of(matcherA, matcherB));
        
        assertEquals(
                List.of(
                        new Pair<>("A-001", "A-001"),
                        new Pair<>("A-002", "A-002"),
                        new Pair<>("B-003", "B-003"),
                        new Pair<>("A-003", null),
                        new Pair<>("B-001", null),
                        new Pair<>("B-002", null),
                        new Pair<>(null, "B-999")),
                testee.makeItemPairs(
                        List.of("A-001", "A-002", "A-003", "B-001", "B-002", "B-003"),
                        List.of("B-999", "A-002", "B-003", "A-001")));
        
        assertEquals(
                List.of(
                        new Pair<>("A-001", "A-001"),
                        new Pair<>("A-002", "A-002"),
                        new Pair<>("A-003", null),
                        new Pair<>("B-001", null),
                        new Pair<>("B-002", null),
                        new Pair<>("B-003", null)),
                testee.makeItemPairs(
                        List.of("A-001", "A-002", "A-003", "B-001", "B-002", "B-003"),
                        List.of("A-002", "A-001")));
        
        assertEquals(
                List.of(
                        new Pair<>("A-001", "A-001"),
                        new Pair<>("A-002", "A-002")),
                testee.makeItemPairs(
                        List.of("A-001", "A-002"),
                        List.of("A-002", "A-001")));
    }
    
}
