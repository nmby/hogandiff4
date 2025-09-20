package xyz.hotchpotch.hogandiff.logic.plain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.logic.BookInfo;
import xyz.hotchpotch.hogandiff.logic.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.logic.BookInfoLoader;

class BookInfoLoaderCombinedTest {
    
    // [static members] ********************************************************
    
    private static final BookInfoLoader successLoader = (bookPath, readPassword) -> BookInfo.ofLoadCompleted(
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
                () -> BookInfoLoaderCombined.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> BookInfoLoaderCombined.of(List.of()));
        
        // 正常系
        assertTrue(
                BookInfoLoaderCombined.of(List.of(
                        () -> successLoader)) instanceof BookInfoLoaderCombined);
        assertTrue(
                BookInfoLoaderCombined.of(List.of(
                        () -> successLoader,
                        () -> failLoader)) instanceof BookInfoLoaderCombined);
    }
    
    @Test
    void testLoadSheetNames_パラメータチェック() {
        BookInfoLoader testee = BookInfoLoaderCombined.of(List.of(() -> successLoader));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadBookInfo(null, null));
    }
    
    @Test
    void testLoadSheetNames_失敗系() {
        BookInfoLoader testeeF = BookInfoLoaderCombined.of(List.of(() -> failLoader));
        BookInfoLoader testeeFFF = BookInfoLoaderCombined.of(List.of(
                () -> failLoader, () -> failLoader, () -> failLoader));
        
        // 失敗１つ
        assertEquals(
                BookInfo.ofLoadFailed(Path.of("dummy.xlsx")),
                testeeF.loadBookInfo(Path.of("dummy.xlsx"), null));
        
        // 全て失敗
        assertEquals(
                BookInfo.ofLoadFailed(Path.of("dummy.xlsx")),
                testeeFFF.loadBookInfo(Path.of("dummy.xlsx"), null));
    }
    
    @Test
    void testLoadSheetNames_成功系() throws ExcelHandlingException {
        BookInfoLoader testeeS = BookInfoLoaderCombined.of(List.of(() -> successLoader));
        BookInfoLoader testeeFFSF = BookInfoLoaderCombined.of(List.of(
                () -> failLoader,
                () -> failLoader,
                () -> successLoader,
                () -> failLoader));
        
        // 成功１つ
        Path path1 = Path.of("dummy.xlsx");
        assertEquals(
                BookInfo.ofLoadCompleted(
                        path1,
                        List.of("success")),
                testeeS.loadBookInfo(path1, null));
        
        // いくつかの失敗ののちに成功
        Path path2 = Path.of("dummy.xlsx");
        assertEquals(
                BookInfo.ofLoadCompleted(
                        path2,
                        List.of("success")),
                testeeFFSF.loadBookInfo(path2, null));
    }
}
