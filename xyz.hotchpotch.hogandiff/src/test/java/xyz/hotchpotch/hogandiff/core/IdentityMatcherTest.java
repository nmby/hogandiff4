package xyz.hotchpotch.hogandiff.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

class IdentityMatcherTest {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private static final List<String> list0_1 = List.of();
    private static final List<String> list0_2 = new ArrayList<>();
    private static final List<String> listABC_1 = List.of("A", "B", "C");
    private static final List<String> listABC_2 = List.of("A", "B", "C");
    private static final List<String> listXXX_1 = List.of("X", "X", "X");
    private static final List<String> listBCA_1 = List.of("B", "C", "A");
    private static final List<String> listXYBAZ_1 = List.of("X", "Y", "B", "A", "Z");
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    @Test
    void testMakePairs1_パラメータチェック() {
        IdentityMatcher<String> testee = new IdentityMatcher<>();
        
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
    void testMakePairs1_パラメータチェック_重複要素あり() {
        IdentityMatcher<String> testee = new IdentityMatcher<>();
        
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.makeIdxPairs(listABC_1, listXXX_1));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.makeIdxPairs(listXXX_1, listABC_1));
    }
    
    @Test
    void testMakePairs2_マッチングロジック_同じ内容() {
        IdentityMatcher<String> testee = new IdentityMatcher<>();
        
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
        IdentityMatcher<String> testee = new IdentityMatcher<>();
        
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
                        IntPair.of(0, 2),
                        IntPair.of(1, 0),
                        IntPair.of(2, 1)),
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
                        IntPair.of(0, 3),
                        IntPair.of(1, 2),
                        IntPair.onlyA(2),
                        IntPair.onlyB(0),
                        IntPair.onlyB(1),
                        IntPair.onlyB(4)),
                testee.makeIdxPairs(listABC_1, listXYBAZ_1));
        assertEquals(
                List.of(
                        IntPair.of(2, 1),
                        IntPair.of(3, 0),
                        IntPair.onlyA(0),
                        IntPair.onlyA(1),
                        IntPair.onlyA(4),
                        IntPair.onlyB(2)),
                testee.makeIdxPairs(listXYBAZ_1, listABC_1));
    }
    
    @Test
    void monkeyTest() {
        assertEquals(
                List.of(
                        new Pair<>("SUN", null),
                        new Pair<>("MON", null),
                        new Pair<>("TUE", null),
                        new Pair<>("WED", null),
                        new Pair<>("THU", null),
                        new Pair<>("FRI", null),
                        new Pair<>("SAT", null),
                        new Pair<>(null, "sun"),
                        new Pair<>(null, "mon"),
                        new Pair<>(null, "tue"),
                        new Pair<>(null, "wed"),
                        new Pair<>(null, "thu"),
                        new Pair<>(null, "fri"),
                        new Pair<>(null, "sat")),
                new IdentityMatcher<>().makeItemPairs(
                        List.of("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"),
                        List.of("sun", "mon", "tue", "wed", "thu", "fri", "sat")));
        
        assertEquals(
                List.of(
                        new Pair<>("SUN", "sun"),
                        new Pair<>("MON", "mon"),
                        new Pair<>("TUE", "tue"),
                        new Pair<>("WED", "wed"),
                        new Pair<>("THU", "thu"),
                        new Pair<>("FRI", "fri"),
                        new Pair<>("SAT", "sat")),
                new IdentityMatcher<String>(String::toUpperCase).makeItemPairs(
                        List.of("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"),
                        List.of("sun", "mon", "tue", "wed", "thu", "fri", "sat")));
    }
}
