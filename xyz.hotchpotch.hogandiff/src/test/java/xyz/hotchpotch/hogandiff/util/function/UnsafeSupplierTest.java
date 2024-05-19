package xyz.hotchpotch.hogandiff.util.function;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

class UnsafeSupplierTest {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @Test
    void testFrom() {
        assertThrows(
                NullPointerException.class,
                () -> UnsafeSupplier.from(null));
        
        assertTrue(
                UnsafeSupplier.from(() -> "Hello!!") instanceof UnsafeSupplier);
    }
    
    @Test
    void testGet1() throws Exception {
        assertEquals(
                "Hello!!",
                UnsafeSupplier.from(() -> "Hello!!").get());
        assertSame(
                Boolean.TRUE,
                UnsafeSupplier.from(() -> Boolean.TRUE).get());
        assertNull(
                UnsafeSupplier.from(() -> null).get());
    }
    
    @Test
    void testGet2() throws Exception {
        UnsafeSupplier<Void, Exception> testee = () -> {
            throw new IOException("Hello!!");
        };
        
        assertThrows(
                IOException.class,
                () -> testee.get(),
                "Hello!!");
    }
    
    @Test
    void testToSupplier1_1() {
        UnsafeSupplier<String, Exception> testee = () -> "Hello!!";
        Supplier<String> transformed = testee.toSupplier();
        
        assertEquals(
                "Hello!!",
                transformed.get());
    }
    
    @Test
    void testToSupplier1_2() {
        UnsafeSupplier<String, Exception> testee = () -> {
            throw new SAXException("Hello!!");
        };
        Supplier<String> transformed = testee.toSupplier();
        
        assertThrows(
                RuntimeException.class,
                () -> transformed.get(),
                (String) null);
        
        Exception thrown = null;
        try {
            transformed.get();
            fail();
        } catch (Exception e) {
            thrown = e;
        }
        
        assertSame(
                SAXException.class,
                thrown.getCause().getClass());
        assertEquals(
                "Hello!!",
                thrown.getCause().getMessage());
    }
    
    @Test
    void testToSupplier1_3() {
        UnsafeSupplier<String, Exception> testee = () -> {
            throw new IllegalArgumentException("Hello!!");
        };
        Supplier<String> transformed = testee.toSupplier();
        
        assertThrows(
                IllegalArgumentException.class,
                () -> transformed.get(),
                "Hello!!");
        
        Exception thrown = null;
        try {
            transformed.get();
            fail();
        } catch (Exception e) {
            thrown = e;
        }
        
        assertNull(
                thrown.getCause());
    }
    
    @Test
    void testToSupplier2_2() {
        UnsafeSupplier<String, Exception> testee = () -> {
            throw new IOException("Hello!!");
        };
        Supplier<String> transformed = testee.toSupplier(UnsupportedOperationException::new);
        
        assertThrows(
                UnsupportedOperationException.class,
                () -> transformed.get(),
                (String) null);
        
        Exception thrown = null;
        try {
            transformed.get();
            fail();
        } catch (Exception e) {
            thrown = e;
        }
        
        assertSame(
                IOException.class,
                thrown.getCause().getClass());
        assertEquals(
                "Hello!!",
                thrown.getCause().getMessage());
    }
    
    @Test
    void testToSupplier2_3() {
        UnsafeSupplier<String, Exception> testee = () -> {
            throw new IllegalArgumentException("Hello!!");
        };
        Supplier<String> transformed = testee.toSupplier(UnsupportedOperationException::new);
        
        assertThrows(
                IllegalArgumentException.class,
                () -> transformed.get(),
                "Hello!!");
        
        Exception thrown = null;
        try {
            transformed.get();
            fail();
        } catch (Exception e) {
            thrown = e;
        }
        
        assertNull(
                thrown.getCause());
    }
}
