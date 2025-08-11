package xyz.hotchpotch.hogandiff.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BookTypeTest {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @Test
    void testValues() {
        // XLS, XLSX, XLSM, XLSB の 4 つであることを確認する。
        // 定義順は任意であるべき（当てにすべきでない）ため、テストしない。
        assertEquals(4, BookType.values().length);
    }
    
    @Test
    void testValueOf() {
        // 正常系
        assertSame(BookType.XLS, BookType.valueOf("XLS"));
        assertSame(BookType.XLSX, BookType.valueOf("XLSX"));
        assertSame(BookType.XLSM, BookType.valueOf("XLSM"));
        assertSame(BookType.XLSB, BookType.valueOf("XLSB"));
        
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> BookType.valueOf(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> BookType.valueOf("PPT"));
    }
    
    @Test
    void testOf() {
        // 正常系
        assertSame(BookType.XLS, BookType.of(Path.of("C:\\aaa\\bbb\\ccc.xls")));
        assertSame(BookType.XLSX, BookType.of(Path.of("C:\\aaa\\bbb\\ccc.xlsx")));
        assertSame(BookType.XLSM, BookType.of(Path.of("C:\\aaa\\bbb\\ccc.xlsm")));
        assertSame(BookType.XLSB, BookType.of(Path.of("C:\\aaa\\bbb\\ccc.xlsb")));
        
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> BookType.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> BookType.of(Path.of("")));
        assertThrows(
                IllegalArgumentException.class,
                () -> BookType.of(Path.of("C:\\aaa\\bbb\\ccc.ppt")));
    }
    
    @Test
    void testExtension() {
        assertEquals(".xls", BookType.XLS.extension());
        assertEquals(".xlsx", BookType.XLSX.extension());
        assertEquals(".xlsm", BookType.XLSM.extension());
        assertEquals(".xlsb", BookType.XLSB.extension());
    }
}
