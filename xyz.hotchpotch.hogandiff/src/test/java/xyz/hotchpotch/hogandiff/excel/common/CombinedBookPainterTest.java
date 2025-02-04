package xyz.hotchpotch.hogandiff.excel.common;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetResult.Piece;

class CombinedBookPainterTest {
    
    // [static members] ********************************************************
    
    private static final BookPainter successPainter = new BookPainter() {
        @Override
        public void paintAndSave(
                Path srcBookPath,
                Path dstBookPath,
                String readPassword,
                Map<String, Optional<Piece>> diffs)
                throws ExcelHandlingException {
            
            // nop
        }
    };
    
    private static final BookPainter failPainter = new BookPainter() {
        @Override
        public void paintAndSave(
                Path srcBookPath,
                Path dstBookPath,
                String readPassword,
                Map<String, Optional<Piece>> diffs)
                throws ExcelHandlingException {
            
            throw new ExcelHandlingException();
        }
    };
    
    private static final Path dummy1_xlsx = Path.of("dummy1.xlsx");
    private static final Path dummy1_xls = Path.of("dummy1.xls");
    private static final Path dummy2_xlsx = Path.of("dummy2.xlsx");
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> CombinedBookPainter.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> CombinedBookPainter.of(List.of()));
        
        // 正常系
        assertTrue(
                CombinedBookPainter.of(List.of(
                        () -> successPainter)) instanceof CombinedBookPainter);
        assertTrue(
                CombinedBookPainter.of(List.of(
                        () -> successPainter,
                        () -> failPainter)) instanceof CombinedBookPainter);
    }
    
    @Test
    void testPaintAndSave_パラメータチェック() {
        BookPainter testee = CombinedBookPainter.of(List.of(() -> successPainter));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.paintAndSave(
                        null, Path.of("dummy2.xlsx"),
                        null,
                        Map.of()));
        assertThrows(
                NullPointerException.class,
                () -> testee.paintAndSave(
                        dummy1_xlsx, null,
                        null,
                        Map.of()));
        assertThrows(
                NullPointerException.class,
                () -> testee.paintAndSave(
                        dummy1_xlsx, Path.of("dummy2.xlsx"),
                        null,
                        null));
        assertThrows(
                NullPointerException.class,
                () -> testee.paintAndSave(
                        null, null,
                        null,
                        null));
        
        assertDoesNotThrow(
                () -> testee.paintAndSave(
                        dummy1_xlsx, dummy2_xlsx,
                        null,
                        Map.of()));
        
        // 同一パス
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.paintAndSave(
                        dummy1_xlsx, dummy1_xlsx,
                        null,
                        Map.of()));
        
        // 異なる拡張子
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.paintAndSave(
                        dummy1_xlsx, dummy1_xls,
                        null,
                        Map.of()));
    }
    
    @Test
    void testPaintAndSave_失敗系() {
        BookPainter testeeF = CombinedBookPainter.of(List.of(() -> failPainter));
        BookPainter testeeFFF = CombinedBookPainter.of(List.of(
                () -> failPainter, () -> failPainter, () -> failPainter));
        
        // 失敗１つ
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeF.paintAndSave(
                        dummy1_xlsx, dummy2_xlsx,
                        null,
                        Map.of()));
        
        // 全て失敗
        assertThrows(
                ExcelHandlingException.class,
                () -> testeeFFF.paintAndSave(
                        dummy1_xlsx, dummy2_xlsx,
                        null,
                        Map.of()));
    }
    
    @Test
    void testPaintAndSave_成功系() {
        BookPainter testeeS = CombinedBookPainter.of(List.of(() -> successPainter));
        BookPainter testeeFFSF = CombinedBookPainter.of(List.of(
                () -> failPainter, () -> failPainter, () -> successPainter, () -> failPainter));
        
        // 成功１つ
        assertDoesNotThrow(
                () -> testeeS.paintAndSave(
                        dummy1_xlsx, dummy2_xlsx,
                        null,
                        Map.of()));
        
        // いくつかの失敗ののちに成功
        assertDoesNotThrow(
                () -> testeeFFSF.paintAndSave(
                        dummy1_xlsx, dummy2_xlsx,
                        null,
                        Map.of()));
    }
}
