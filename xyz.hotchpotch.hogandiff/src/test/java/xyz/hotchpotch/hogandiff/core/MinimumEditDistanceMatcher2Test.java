package xyz.hotchpotch.hogandiff.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.util.Pair;

class MinimumEditDistanceMatcher2Test {
    
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
                () -> new MinimumEditDistanceMatcher2<>(null, diffEvaluator));
        assertThrows(
                AssertionError.class,
                () -> new MinimumEditDistanceMatcher2<>(gapEvaluator, null));
        assertThrows(
                AssertionError.class,
                () -> new MinimumEditDistanceMatcher2<>(null, null));
        
        assertDoesNotThrow(
                () -> new MinimumEditDistanceMatcher2<>(gapEvaluator, diffEvaluator));
    }
    
    @Test
    void testMakePairs1_パラメータチェック() {
        MinimumEditDistanceMatcher2<Character> testee = new MinimumEditDistanceMatcher2<>(gapEvaluator, diffEvaluator);
        
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(null, list0_1));
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(list0_1, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(null, null));
        
        assertDoesNotThrow(
                () -> testee.makePairs(list0_1, list0_1));
    }
    
    @Test
    void testMakePairs2_マッチングロジック_同じ内容() {
        MinimumEditDistanceMatcher2<Character> testee = new MinimumEditDistanceMatcher2<>(gapEvaluator, diffEvaluator);
        
        // 同一インスタンス
        assertEquals(
                List.of(),
                testee.makePairs(list0_1, list0_1));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2)),
                testee.makePairs(listABC_1, listABC_1));
        
        // 別インスタンス同一内容
        assertEquals(
                List.of(),
                testee.makePairs(list0_1, list0_2));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2)),
                testee.makePairs(listABC_1, listABC_2));
    }
    
    @Test
    void testMakePairs3_マッチングロジック_異なる内容() {
        MinimumEditDistanceMatcher2<Character> testee = new MinimumEditDistanceMatcher2<>(gapEvaluator, diffEvaluator);
        
        // 一方が長さゼロ
        assertEquals(
                List.of(
                        Pair.ofNullable(null, 0),
                        Pair.ofNullable(null, 1),
                        Pair.ofNullable(null, 2)),
                testee.makePairs(list0_1, listABC_1));
        assertEquals(
                List.of(
                        Pair.ofNullable(0, null),
                        Pair.ofNullable(1, null),
                        Pair.ofNullable(2, null)),
                testee.makePairs(listABC_1, list0_1));
        
        // 一般
        // K ITTE N
        //   |||  |
        //  SITT ING
        assertEquals(
                List.of(
                        Pair.ofNullable(0, null),
                        Pair.ofNullable(null, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2),
                        Pair.ofNullable(3, 3),
                        Pair.ofNullable(4, null),
                        Pair.ofNullable(null, 4),
                        Pair.ofNullable(5, 5),
                        Pair.ofNullable(null, 6)),
                testee.makePairs(listKITTEN, listSITTING));
        // S ITTI NG
        //   |||  |
        //  KITT EN
        assertEquals(
                List.of(
                        Pair.ofNullable(0, null),
                        Pair.ofNullable(null, 0),
                        Pair.ofNullable(1, 1),
                        Pair.ofNullable(2, 2),
                        Pair.ofNullable(3, 3),
                        Pair.ofNullable(4, null),
                        Pair.ofNullable(null, 4),
                        Pair.ofNullable(5, 5),
                        Pair.ofNullable(6, null)),
                testee.makePairs(listSITTING, listKITTEN));
    }
}
