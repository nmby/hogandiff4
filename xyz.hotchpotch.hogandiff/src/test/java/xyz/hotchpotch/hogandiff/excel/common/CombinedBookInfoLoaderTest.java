package xyz.hotchpotch.hogandiff.excel.common;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.BookInfoLoader;

class CombinedBookInfoLoaderTest {
    
    // [static members] ********************************************************
    
    private static final BookInfoLoader successLoader = (bookPath, readPassword) -> new BookInfo(
            bookPath, List.of("success"));
    
    private static final BookInfoLoader failLoader = (bookPath, readPassword) -> {
        throw new RuntimeException("fail");
    };
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> CombinedBookInfoLoader.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> CombinedBookInfoLoader.of(List.of()));
        
        // 正常系
        assertTrue(
                CombinedBookInfoLoader.of(List.of(
                        () -> successLoader)) instanceof CombinedBookInfoLoader);
        assertTrue(
                CombinedBookInfoLoader.of(List.of(
                        () -> successLoader,
                        () -> failLoader)) instanceof CombinedBookInfoLoader);
    }
    
    @Test
    void testLoadSheetNames_パラメータチェック() {
        BookInfoLoader testee = CombinedBookInfoLoader.of(List.of(() -> successLoader));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadBookInfo(null, null));
    }
    
    @Test
    void testLoadSheetNames_失敗系() {
        BookInfoLoader testeeF = CombinedBookInfoLoader.of(List.of(() -> failLoader));
        BookInfoLoader testeeFFF = CombinedBookInfoLoader.of(List.of(
                () -> failLoader, () -> failLoader, () -> failLoader));
        
        // 失敗１つ
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeF.loadBookInfo(Path.of("dummy.xlsx"), null));
        
        // 全て失敗
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeFFF.loadBookInfo(Path.of("dummy.xlsx"), null));
    }
    
    @Test
    void testLoadSheetNames_成功系() throws ExcelHandlingException {
        BookInfoLoader testeeS = CombinedBookInfoLoader.of(List.of(() -> successLoader));
        BookInfoLoader testeeFFSF = CombinedBookInfoLoader.of(List.of(
                () -> failLoader,
                () -> failLoader,
                () -> successLoader,
                () -> failLoader));
        
        // 成功１つ
        Path path1 = Path.of("dummy.xlsx");
        assertEquals(
                new BookInfo(
                        path1,
                        List.of("success")),
                testeeS.loadBookInfo(path1, null));
        
        // いくつかの失敗ののちに成功
        Path path2 = Path.of("dummy.xlsx");
        assertEquals(
                new BookInfo(
                        path2,
                        List.of("success")),
                testeeFFSF.loadBookInfo(path2, null));
    }
}
