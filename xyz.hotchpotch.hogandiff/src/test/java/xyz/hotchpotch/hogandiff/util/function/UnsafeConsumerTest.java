package xyz.hotchpotch.hogandiff.util.function;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class UnsafeConsumerTest {
    
    // [static members] ********************************************************
    
    private static final UnsafeConsumer<String, Exception> unsafe1 = x -> {
    };
    private static final UnsafeConsumer<String, IOException> unsafe2 = x -> {
        throw new IOException("unsafe2");
    };
    private static final UnsafeConsumer<String, Exception> unsafe3 = x -> {
        throw new IllegalArgumentException("unsafe3");
    };
    
    // [instance members] ******************************************************
    
    @Test
    void testFrom() {
        assertThrows(
                NullPointerException.class,
                () -> UnsafeConsumer.from(null));
        
        assertTrue(
                UnsafeConsumer.from(x -> {
                }) instanceof UnsafeConsumer);
    }
    
    @Test
    void testAccept() {
        assertDoesNotThrow(
                () -> unsafe1.accept("abcde"));
        
        assertThrows(
                IOException.class,
                () -> unsafe2.accept("abcde"),
                "unsafe2");
        assertThrows(
                IllegalArgumentException.class,
                () -> unsafe3.accept("abcde"),
                "unsafe3");
    }
    
    @Test
    void testToConsumer1_1() {
        Consumer<String> transformed1 = unsafe1.toConsumer();
        
        assertDoesNotThrow(
                () -> transformed1.accept("abcde"));
    }
    
    @Test
    void testToConsumer1_2() {
        Consumer<String> transformed2 = unsafe2.toConsumer();
        
        assertThrows(
                RuntimeException.class,
                () -> transformed2.accept("abcde"),
                (String) null);
        
        Exception thrown = null;
        try {
            transformed2.accept("abcde");
            fail();
        } catch (Exception e) {
            thrown = e;
        }
        
        assertSame(
                IOException.class,
                thrown.getCause().getClass());
        assertEquals(
                "unsafe2",
                thrown.getCause().getMessage());
    }
    
    @Test
    void testToConsumer1_3() {
        Consumer<String> transformed3 = unsafe3.toConsumer();
        
        assertThrows(
                IllegalArgumentException.class,
                () -> transformed3.accept("abcde"),
                "unsafe3");
        
        Exception thrown = null;
        try {
            transformed3.accept("abcde");
            fail();
        } catch (Exception e) {
            thrown = e;
        }
        
        assertNull(
                thrown.getCause());
    }
    
    @Test
    void testToConsumer2_2() {
        Consumer<String> transformed2 = unsafe2.toConsumer(UnsupportedOperationException::new);
        
        assertThrows(
                UnsupportedOperationException.class,
                () -> transformed2.accept("abcde"),
                (String) null);
        
        Exception thrown = null;
        try {
            transformed2.accept("abcde");
            fail();
        } catch (Exception e) {
            thrown = e;
        }
        
        assertSame(
                IOException.class,
                thrown.getCause().getClass());
        assertEquals(
                "unsafe2",
                thrown.getCause().getMessage());
    }
    
    @Test
    void testToConsumer2_3() {
        Consumer<String> transformed3 = unsafe3.toConsumer(UnsupportedOperationException::new);
        
        assertThrows(
                IllegalArgumentException.class,
                () -> transformed3.accept("abcde"),
                "unsafe3");
        
        Exception thrown = null;
        try {
            transformed3.accept("abcde");
            fail();
        } catch (Exception e) {
            thrown = e;
        }
        
        assertNull(
                thrown.getCause());
    }
}
