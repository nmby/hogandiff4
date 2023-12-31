package xyz.hotchpotch.hogandiff.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.util.IntPair;

class MinimumEditDistanceMatcherTest {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private static final ToIntFunction<Character> gapEvaluator = c -> 1;
    private static final ToIntBiFunction<Character, Character> diffEvaluator = (c1, c2) -> c1.equals(c2) ? 0 : 3;
    
    private static final List<Character> list0_1 = List.of();
    private static final List<Character> list0_2 = new ArrayList<>();
    private static final List<Character> listABC_1 = List.of('A', 'B', 'C');
    private static final List<Character> listABC_2 = List.of('A', 'B', 'C');
    private static final List<Character> listKITTEN = List.of('K', 'I', 'T', 'T', 'E', 'N');
    private static final List<Character> listSITTING = List.of('S', 'I', 'T', 'T', 'I', 'N', 'G');
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    @Test
    void testConstructor() {
        assertThrows(
                AssertionError.class,
                () -> new MinimumEditDistanceMatcher<>(null, diffEvaluator));
        assertThrows(
                AssertionError.class,
                () -> new MinimumEditDistanceMatcher<>(gapEvaluator, null));
        assertThrows(
                AssertionError.class,
                () -> new MinimumEditDistanceMatcher<>(null, null));
        
        assertDoesNotThrow(
                () -> new MinimumEditDistanceMatcher<>(gapEvaluator, diffEvaluator));
    }
    
    @Test
    void testMakePairs1_パラメータチェック() {
        MinimumEditDistanceMatcher<Character> testee = new MinimumEditDistanceMatcher<>(gapEvaluator, diffEvaluator);
        
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
        MinimumEditDistanceMatcher<Character> testee = new MinimumEditDistanceMatcher<>(gapEvaluator, diffEvaluator);
        
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
    }
    
    @Test
    void testMakePairs3_マッチングロジック_異なる内容() {
        MinimumEditDistanceMatcher<Character> testee = new MinimumEditDistanceMatcher<>(gapEvaluator, diffEvaluator);
        
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
        
        // 一般
        // K ITTE N
        //   |||  |
        //  SITT ING
        assertEquals(
                List.of(
                        IntPair.onlyA(0),
                        IntPair.onlyB(0),
                        IntPair.of(1, 1),
                        IntPair.of(2, 2),
                        IntPair.of(3, 3),
                        IntPair.onlyA(4),
                        IntPair.onlyB(4),
                        IntPair.of(5, 5),
                        IntPair.onlyB(6)),
                testee.makeIdxPairs(listKITTEN, listSITTING));
        // S ITTI NG
        //   |||  |
        //  KITT EN
        assertEquals(
                List.of(
                        IntPair.onlyA(0),
                        IntPair.onlyB(0),
                        IntPair.of(1, 1),
                        IntPair.of(2, 2),
                        IntPair.of(3, 3),
                        IntPair.onlyA(4),
                        IntPair.onlyB(4),
                        IntPair.of(5, 5),
                        IntPair.onlyA(6)),
                testee.makeIdxPairs(listSITTING, listKITTEN));
    }
}
