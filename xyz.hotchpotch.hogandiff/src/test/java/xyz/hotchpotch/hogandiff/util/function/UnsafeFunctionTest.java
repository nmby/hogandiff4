package xyz.hotchpotch.hogandiff.util.function;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class UnsafeFunctionTest {
    
    // [static members] ********************************************************
    
    private static final UnsafeFunction<String, Integer> unsafe1_1 = String::length;
    private static final UnsafeFunction<Integer, String> unsafe1_2 = "x"::repeat;
    private static final UnsafeFunction<String, Integer> unsafe2_1 = s -> {
        throw new IOException("unsafe2_1");
    };
    private static final UnsafeFunction<Integer, String> unsafe2_2 = i -> {
        throw new SAXException("unsafe2_2");
    };
    private static final UnsafeFunction<String, Integer> unsafe2_3 = i -> {
        throw new IllegalArgumentException("unsafe2_3");
    };
    
    private static final Function<String, Integer> safe1_1 = String::length;
    private static final Function<Integer, String> safe1_2 = "x"::repeat;
    private static final Function<String, Integer> safe2_1 = s -> {
        throw new IllegalArgumentException("safe2_1");
    };
    private static final Function<Integer, String> safe2_2 = i -> {
        throw new UnsupportedOperationException("unsafe2_2");
    };
    
    // [instance members] ******************************************************
    
    @Test
    void testIdentity() throws Exception {
        UnsafeFunction<Object, Object> testee = UnsafeFunction.identity();
        
        assertTrue(
                testee instanceof UnsafeFunction);
        
        Object o = new Object();
        
        assertSame(
                o,
                testee.apply(o));
    }
    
    @Test
    void testFrom() {
        assertThrows(
                NullPointerException.class,
                () -> UnsafeFunction.from(null));
        
        assertTrue(
                UnsafeFunction.from(x -> "Hello!!") instanceof UnsafeFunction);
    }
    
    @Test
    void testApply() throws Exception {
        assertEquals(
                5,
                unsafe1_1.apply("abcde"));
        assertEquals(
                "xxxxx",
                unsafe1_2.apply(5));
        
        assertThrows(
                IOException.class,
                () -> unsafe2_1.apply("abcde"),
                "unsafe2_1");
        assertThrows(
                SAXException.class,
                () -> unsafe2_2.apply(5),
                "unsafe2_2");
    }
    
    @Test
    void testCompose1() throws Exception {
        assertTrue(
                unsafe1_2.compose(unsafe1_1) instanceof UnsafeFunction);
        assertTrue(
                unsafe2_2.compose(unsafe2_1) instanceof UnsafeFunction);
        
        // 成功 -> 成功
        assertEquals(
                "xxxxx",
                unsafe1_2.compose(unsafe1_1).apply("abcde"));
        
        // 失敗 -> 成功
        assertThrows(
                IOException.class,
                () -> unsafe1_2.compose(unsafe2_1).apply("abcde"));
        
        // 成功 -> 失敗
        assertThrows(
                SAXException.class,
                () -> unsafe2_2.compose(unsafe1_1).apply("abcde"),
                "unsafe2_2");
        
        // 失敗 -> 失敗
        assertThrows(
                IOException.class,
                () -> unsafe2_2.compose(unsafe2_1).apply("abcde"),
                "unsafe2_2");
    }
    
    @Test
    void testCompose2() throws Exception {
        assertTrue(
                unsafe1_2.compose(safe1_1) instanceof UnsafeFunction);
        assertTrue(
                unsafe2_2.compose(safe2_1) instanceof UnsafeFunction);
        
        // 成功 -> 成功
        assertEquals(
                "xxxxx",
                unsafe1_2.compose(safe1_1).apply("abcde"));
        
        // 失敗 -> 成功
        assertThrows(
                IllegalArgumentException.class,
                () -> unsafe1_2.compose(safe2_1).apply("abcde"),
                "safe2_1");
        
        // 成功 -> 失敗
        assertThrows(
                SAXException.class,
                () -> unsafe2_2.compose(safe1_1).apply("abcde"),
                "unsafe2_2");
        
        // 失敗 -> 失敗
        assertThrows(
                IllegalArgumentException.class,
                () -> unsafe2_2.compose(safe2_1).apply("abcde"),
                "safe2_1");
    }
    
    @Test
    void testAndThen1() throws Exception {
        assertTrue(
                unsafe1_1.andThen(unsafe1_2) instanceof UnsafeFunction);
        assertTrue(
                unsafe2_1.andThen(unsafe2_2) instanceof UnsafeFunction);
        
        // 成功 -> 成功
        assertEquals(
                "xxxxx",
                unsafe1_1.andThen(unsafe1_2).apply("abcde"));
        
        // 失敗 -> 成功
        assertThrows(
                IOException.class,
                () -> unsafe2_1.andThen(unsafe1_2).apply("abcde"),
                "unsafe2_1");
        
        // 成功 -> 失敗
        assertThrows(
                SAXException.class,
                () -> unsafe1_1.andThen(unsafe2_2).apply("abcde"),
                "unsafe2_2");
        
        // 失敗 -> 失敗
        assertThrows(
                IOException.class,
                () -> unsafe2_1.andThen(unsafe2_2).apply("abcde"),
                "unsafe2_1");
    }
    
    @Test
    void testAndThen2() throws Exception {
        assertTrue(
                unsafe1_1.andThen(safe1_2) instanceof UnsafeFunction);
        assertTrue(
                unsafe2_1.andThen(safe2_2) instanceof UnsafeFunction);
        
        // 成功 -> 成功
        assertEquals(
                "xxxxx",
                unsafe1_1.andThen(safe1_2).apply("abcde"));
        
        // 失敗 -> 成功
        assertThrows(
                IOException.class,
                () -> unsafe2_1.andThen(safe1_2).apply("abcde"),
                "unsafe2_1");
        
        // 成功 -> 失敗
        assertThrows(
                UnsupportedOperationException.class,
                () -> unsafe1_1.andThen(safe2_2).apply("abcde"),
                "safe2_2");
        
        // 失敗 -> 失敗
        assertThrows(
                IOException.class,
                () -> unsafe2_1.andThen(safe2_2).apply("abcde"),
                "unsafe2_1");
    }
    
    @Test
    void testToFunction1_1() {
        Function<String, Integer> transformed = unsafe1_1.toFunction();
        
        assertEquals(
                5,
                transformed.apply("abcde"));
    }
    
    @Test
    void testToFunction1_2() {
        Function<String, Integer> transformed = unsafe2_1.toFunction();
        
        assertThrows(
                RuntimeException.class,
                () -> transformed.apply("abcde"),
                (String) null);
        
        RuntimeException thrown = null;
        try {
            transformed.apply("abcde");
            fail();
        } catch (RuntimeException e) {
            thrown = e;
        }
        
        assertSame(
                IOException.class,
                thrown.getCause().getClass());
        assertEquals(
                "unsafe2_1",
                thrown.getCause().getMessage());
    }
    
    @Test
    void testToFunction2_1() {
        Function<String, Integer> transformed = unsafe2_1.toFunction(IllegalArgumentException::new);
        
        assertThrows(
                IllegalArgumentException.class,
                () -> transformed.apply("abcde"),
                (String) null);
        
        RuntimeException thrown = null;
        try {
            transformed.apply("abcde");
            fail();
        } catch (RuntimeException e) {
            thrown = e;
        }
        
        assertSame(
                IOException.class,
                thrown.getCause().getClass());
        assertEquals(
                "unsafe2_1",
                thrown.getCause().getMessage());
    }
    
    @Test
    void testToFunction2_2() {
        Function<String, Integer> transformed = unsafe2_3.toFunction(UnsupportedOperationException::new);
        
        assertThrows(
                IllegalArgumentException.class,
                () -> transformed.apply("abcde"),
                "unsafe2_3");
        
        RuntimeException thrown = null;
        try {
            transformed.apply("abcde");
            fail();
        } catch (RuntimeException e) {
            thrown = e;
        }
        
        assertNull(
                thrown.getCause());
    }
}
