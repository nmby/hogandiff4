package xyz.hotchpotch.hogandiff.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.util.IntPair;

class SimpleMatcherTest {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private static final List<String> list0 = List.of();
    private static final List<String> listABC = List.of("A", "B", "C");
    private static final List<String> listXYZ = List.of("X", "Y", "Z");
    private static final List<String> listVWXYZ = List.of("V", "W", "X", "Y", "Z");
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    @Test
    void testConstructor() {
        assertDoesNotThrow(
                () -> new SimpleMatcher<>());
    }
    
    @Test
    void testMakePairs1_パラメータチェック() {
        SimpleMatcher<String> testee = new SimpleMatcher<>();
        
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(null, list0));
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(list0, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.makePairs(null, null));
        
        assertDoesNotThrow(
                () -> testee.makePairs(list0, list0));
    }
    
    @Test
    void testMakePairs2_マッチングロジック() {
        SimpleMatcher<String> testee = new SimpleMatcher<>();
        
        // 長さが同じ場合
        assertEquals(
                List.of(),
                testee.makePairs(list0, list0));
        assertEquals(
                List.of(
                        IntPair.of(0, 0),
                        IntPair.of(1, 1),
                        IntPair.of(2, 2)),
                testee.makePairs(listABC, listABC));
        assertEquals(
                List.of(
                        IntPair.of(0, 0),
                        IntPair.of(1, 1),
                        IntPair.of(2, 2)),
                testee.makePairs(listABC, listXYZ));
        
        // 長さが異なる場合
        assertEquals(
                List.of(
                        IntPair.onlyB(0),
                        IntPair.onlyB(1),
                        IntPair.onlyB(2)),
                testee.makePairs(list0, listABC));
        assertEquals(
                List.of(
                        IntPair.onlyA(0),
                        IntPair.onlyA(1),
                        IntPair.onlyA(2)),
                testee.makePairs(listABC, list0));
        assertEquals(
                List.of(
                        IntPair.of(0, 0),
                        IntPair.of(1, 1),
                        IntPair.of(2, 2),
                        IntPair.onlyB(3),
                        IntPair.onlyB(4)),
                testee.makePairs(listXYZ, listVWXYZ));
        assertEquals(
                List.of(
                        IntPair.of(0, 0),
                        IntPair.of(1, 1),
                        IntPair.of(2, 2),
                        IntPair.onlyA(3),
                        IntPair.onlyA(4)),
                testee.makePairs(listVWXYZ, listXYZ));
    }
}
