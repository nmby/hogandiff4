package xyz.hotchpotch.hogandiff.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.util.IntPair;

class GreedyMatcherTest {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private static final ToIntFunction<String> gapEvaluator = String::length;
    private static final ToIntBiFunction<String, String> diffEvaluator = (s1,
            s2) -> StringDiffUtil.levenshteinDistance(s1, s2);
    
    private static final List<String> list0_1 = List.of();
    private static final List<String> list0_2 = new ArrayList<>();
    private static final List<String> listABC_1 = List.of("A", "B", "C");
    private static final List<String> listABC_2 = List.of("A", "B", "C");
    private static final List<String> listXXX_1 = List.of("X", "X", "X");
    private static final List<String> listXXX_2 = List.of("X", "X", "X");
    private static final List<String> listBCA_1 = List.of("B", "C", "A");
    private static final List<String> listXXBAX_1 = List.of("X", "X", "B", "A", "X");
    private static final List<String> listBB_1 = List.of("BB");
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private final Matcher<String> testee = new GreedyMatcher<>(gapEvaluator, diffEvaluator);
    
    @Test
    void testConstructor() {
        assertThrows(
                AssertionError.class,
                () -> new GreedyMatcher<>(null, diffEvaluator));
        assertThrows(
                AssertionError.class,
                () -> new GreedyMatcher<>(gapEvaluator, null));
        assertThrows(
                AssertionError.class,
                () -> new GreedyMatcher<>(null, null));
        
        assertDoesNotThrow(
                () -> new GreedyMatcher<>(gapEvaluator, diffEvaluator));
    }
    
    @Test
    void testMakePairs1_パラメータチェック() {
        assertThrows(
                NullPointerException.class,
                () -> testee.makeIdxPairs(null, list0_1));
        assertThrows(
                NullPointerException.class,
                () -> testee.makeIdxPairs(list0_1, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.makeIdxPairs(null, null));
        
        assertDoesNotThrow(
                () -> testee.makeIdxPairs(list0_1, list0_1));
    }
    
    @Test
    void testMakePairs2_マッチングロジック_同じ内容() {
        // 同一インスタンス
        assertEquals(
                List.of(),
                testee.makeIdxPairs(list0_1, list0_1));
        assertEquals(
                List.of(
                        IntPair.of(0, 0),
                        IntPair.of(1, 1),
                        IntPair.of(2, 2)),
                testee.makeIdxPairs(listABC_1, listABC_1));
        
        // 別インスタンス同一内容
        assertEquals(
                List.of(),
                testee.makeIdxPairs(list0_1, list0_2));
        assertEquals(
                List.of(
                        IntPair.of(0, 0),
                        IntPair.of(1, 1),
                        IntPair.of(2, 2)),
                testee.makeIdxPairs(listABC_1, listABC_2));
        assertEquals(
                List.of(
                        IntPair.of(0, 0),
                        IntPair.of(1, 1),
                        IntPair.of(2, 2)),
                testee.makeIdxPairs(listXXX_1, listXXX_2));
    }
    
    @Test
    void testMakePairs3_マッチングロジック_異なる内容() {
        // 一方が長さゼロ
        assertEquals(
                List.of(
                        IntPair.onlyB(0),
                        IntPair.onlyB(1),
                        IntPair.onlyB(2)),
                testee.makeIdxPairs(list0_1, listABC_1));
        assertEquals(
                List.of(
                        IntPair.onlyA(0),
                        IntPair.onlyA(1),
                        IntPair.onlyA(2)),
                testee.makeIdxPairs(listABC_1, list0_1));
        
        // 同じ長さでギャップ無し
        assertEquals(
                List.of(
                        IntPair.of(1, 0),
                        IntPair.of(2, 1),
                        IntPair.of(0, 2)),
                testee.makeIdxPairs(listABC_1, listBCA_1));
        assertEquals(
                List.of(
                        IntPair.of(0, 1),
                        IntPair.of(1, 2),
                        IntPair.of(2, 0)),
                testee.makeIdxPairs(listBCA_1, listABC_1));
        
        // ギャップあり
        assertEquals(
                List.of(
                        IntPair.of(1, 2),
                        IntPair.of(0, 3),
                        IntPair.onlyA(2),
                        IntPair.onlyB(0),
                        IntPair.onlyB(1),
                        IntPair.onlyB(4)),
                testee.makeIdxPairs(listABC_1, listXXBAX_1));
        assertEquals(
                List.of(
                        IntPair.of(2, 1),
                        IntPair.of(3, 0),
                        IntPair.onlyA(0),
                        IntPair.onlyA(1),
                        IntPair.onlyA(4),
                        IntPair.onlyB(2)),
                testee.makeIdxPairs(listXXBAX_1, listABC_1));
        assertEquals(
                List.of(
                        IntPair.of(1, 0),
                        IntPair.onlyA(0),
                        IntPair.onlyA(2)),
                testee.makeIdxPairs(listABC_1, listBB_1));
        assertEquals(
                List.of(
                        IntPair.of(0, 1),
                        IntPair.onlyB(0),
                        IntPair.onlyB(2)),
                testee.makeIdxPairs(listBB_1, listABC_1));
    }
}
