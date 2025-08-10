package xyz.hotchpotch.hogandiff.logic._plain;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.logic.BookInfo;
import xyz.hotchpotch.hogandiff.logic.CellData;
import xyz.hotchpotch.hogandiff.logic.CellsLoader;
import xyz.hotchpotch.hogandiff.logic.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.util.function.UnsafeSupplier;

class CellsLoaderCombinedTest {
    
    // [static members] ********************************************************
    
    private static final CellData cell1 = new CellData(1, 2, "success", null);
    
    private static final CellsLoader successLoader = (bookPath, readPassword, sheetName) -> Set.of(cell1);
    
    private static final CellsLoader failLoader = (bookPath, readPassword, sheetName) -> {
        throw new RuntimeException("fail");
    };
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> CellsLoaderCombined.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> CellsLoaderCombined.of(List.of()));
        
        // 正常系
        assertTrue(
                CellsLoaderCombined.of(List.of(
                        UnsafeSupplier.from(
                                () -> successLoader))) instanceof CellsLoaderCombined);
        assertTrue(
                CellsLoaderCombined.of(List.of(
                        UnsafeSupplier.from(() -> successLoader),
                        UnsafeSupplier.from(
                                () -> failLoader))) instanceof CellsLoaderCombined);
    }
    
    @Test
    void testLoadCells_パラメータチェック() {
        CellsLoader testee = CellsLoaderCombined.of(List.of(
                UnsafeSupplier.from(() -> successLoader)));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, "readPassword", "sheetName"));
        assertDoesNotThrow(
                () -> testee.loadCells(BookInfo.ofLoadCompleted(Path.of("dummy.xlsx"), List.of()), null, "dummy"));
        assertThrows(NullPointerException.class,
                () -> testee.loadCells(BookInfo.ofLoadCompleted(Path.of("dummy.xlsx"), List.of()), "readPassword",
                        null));
    }
    
    @Test
    void testLoadCells_失敗系() {
        CellsLoader testeeF = CellsLoaderCombined.of(List.of(
                UnsafeSupplier.from(() -> failLoader)));
        CellsLoader testeeFFF = CellsLoaderCombined.of(List.of(
                UnsafeSupplier.from(() -> failLoader),
                UnsafeSupplier.from(() -> failLoader),
                UnsafeSupplier.from(() -> failLoader)));
        
        // 失敗１つ
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeF.loadCells(BookInfo.ofLoadCompleted(Path.of("dummy.xlsx"), List.of()), null, "dummy"));
        
        // 全て失敗
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeFFF.loadCells(BookInfo.ofLoadCompleted(Path.of("dummy.xlsx"), List.of()), null, "dummy"));
    }
    
    @Test
    void testLoadSheetNames_成功系() throws ExcelHandlingException {
        CellsLoader testeeS = CellsLoaderCombined.of(List.of(
                UnsafeSupplier.from(() -> successLoader)));
        CellsLoader testeeFFSF = CellsLoaderCombined.of(List.of(
                UnsafeSupplier.from(() -> failLoader),
                UnsafeSupplier.from(() -> failLoader),
                UnsafeSupplier.from(() -> successLoader),
                UnsafeSupplier.from(() -> failLoader)));
        
        // 成功１つ
        assertEquals(
                Set.of(cell1),
                testeeS.loadCells(BookInfo.ofLoadCompleted(Path.of("dummy.xlsx"), List.of()), null, "dummy"));
        
        // いくつかの失敗ののちに成功
        assertEquals(
                Set.of(cell1),
                testeeFFSF.loadCells(BookInfo.ofLoadCompleted(Path.of("dummy.xlsx"), List.of()), null, "dummy"));
    }
}
