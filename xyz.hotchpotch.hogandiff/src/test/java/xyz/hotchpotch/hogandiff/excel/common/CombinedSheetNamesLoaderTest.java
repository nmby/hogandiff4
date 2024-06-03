package xyz.hotchpotch.hogandiff.excel.common;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;

class CombinedSheetNamesLoaderTest {
    
    // [static members] ********************************************************
    
    private static final SheetNamesLoader successLoader = (bookPath, readPassword) -> new BookInfo(
            bookPath, readPassword, List.of("success"));
    
    private static final SheetNamesLoader failLoader = (bookPath, readPassword) -> {
        throw new RuntimeException("fail");
    };
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> CombinedSheetNamesLoader.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> CombinedSheetNamesLoader.of(List.of()));
        
        // 正常系
        assertTrue(
                CombinedSheetNamesLoader.of(List.of(
                        () -> successLoader)) instanceof CombinedSheetNamesLoader);
        assertTrue(
                CombinedSheetNamesLoader.of(List.of(
                        () -> successLoader,
                        () -> failLoader)) instanceof CombinedSheetNamesLoader);
    }
    
    @Test
    void testLoadSheetNames_パラメータチェック() {
        SheetNamesLoader testee = CombinedSheetNamesLoader.of(List.of(() -> successLoader));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadSheetNames(null, null));
    }
    
    @Test
    void testLoadSheetNames_失敗系() {
        SheetNamesLoader testeeF = CombinedSheetNamesLoader.of(List.of(() -> failLoader));
        SheetNamesLoader testeeFFF = CombinedSheetNamesLoader.of(List.of(
                () -> failLoader, () -> failLoader, () -> failLoader));
        
        // 失敗１つ
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeF.loadSheetNames(Path.of("dummy.xlsx"), null));
        
        // 全て失敗
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeFFF.loadSheetNames(Path.of("dummy.xlsx"), null));
    }
    
    @Test
    void testLoadSheetNames_成功系() throws ExcelHandlingException {
        SheetNamesLoader testeeS = CombinedSheetNamesLoader.of(List.of(() -> successLoader));
        SheetNamesLoader testeeFFSF = CombinedSheetNamesLoader.of(List.of(
                () -> failLoader,
                () -> failLoader,
                () -> successLoader,
                () -> failLoader));
        
        // 成功１つ
        Path path1 = Path.of("dummy.xlsx");
        assertEquals(
                new BookInfo(
                        path1,
                        null,
                        List.of("success")),
                testeeS.loadSheetNames(path1, null));
        
        // いくつかの失敗ののちに成功
        Path path2 = Path.of("dummy.xlsx");
        assertEquals(
                new BookInfo(
                        path2,
                        null,
                        List.of("success")),
                testeeFFSF.loadSheetNames(path2, null));
    }
}
