package xyz.hotchpotch.hogandiff.excel.common;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.util.function.UnsafeSupplier;

class CombinedSheetLoaderTest {
    
    // [static members] ********************************************************
    
    private static final CellData cell1 = CellData.of(1, 2, "success");
    
    private static final SheetLoader successLoader = (bookPath, sheetName) -> Set.of(cell1);
    
    private static final SheetLoader failLoader = (bookPath, sheetName) -> {
        throw new RuntimeException("fail");
    };
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> CombinedSheetLoader.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> CombinedSheetLoader.of(List.of()));
        
        // 正常系
        assertTrue(
                CombinedSheetLoader.of(List.of(
                        UnsafeSupplier.from(() -> successLoader))) instanceof CombinedSheetLoader);
        assertTrue(
                CombinedSheetLoader.of(List.of(
                        UnsafeSupplier.from(() -> successLoader),
                        UnsafeSupplier.from(() -> failLoader))) instanceof CombinedSheetLoader);
    }
    
    @Test
    void testLoadCells_パラメータチェック() {
        SheetLoader testee = CombinedSheetLoader.of(List.of(
                UnsafeSupplier.from(() -> successLoader)));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, "dummy"));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(Path.of("dummy.xlsx"), null));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, null));
        
        assertDoesNotThrow(
                () -> testee.loadCells(Path.of("dummy.xlsx"), "dummy"));
    }
    
    @Test
    void testLoadCells_失敗系() {
        SheetLoader testeeF = CombinedSheetLoader.of(List.of(
                UnsafeSupplier.from(() -> failLoader)));
        SheetLoader testeeFFF = CombinedSheetLoader.of(List.of(
                UnsafeSupplier.from(() -> failLoader),
                UnsafeSupplier.from(() -> failLoader),
                UnsafeSupplier.from(() -> failLoader)));
        
        // 失敗１つ
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeF.loadCells(Path.of("dummy.xlsx"), "dummy"));
        
        // 全て失敗
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeFFF.loadCells(Path.of("dummy.xlsx"), "dummy"));
    }
    
    @Test
    void testLoadSheetNames_成功系() throws ExcelHandlingException {
        SheetLoader testeeS = CombinedSheetLoader.of(List.of(
                UnsafeSupplier.from(() -> successLoader)));
        SheetLoader testeeFFSF = CombinedSheetLoader.of(List.of(
                UnsafeSupplier.from(() -> failLoader),
                UnsafeSupplier.from(() -> failLoader),
                UnsafeSupplier.from(() -> successLoader),
                UnsafeSupplier.from(() -> failLoader)));
        
        // 成功１つ
        assertEquals(
                Set.of(cell1),
                testeeS.loadCells(Path.of("dummy.xlsx"), "dummy"));
        
        // いくつかの失敗ののちに成功
        assertEquals(
                Set.of(cell1),
                testeeFFSF.loadCells(Path.of("dummy.xlsx"), "dummy"));
    }
}
