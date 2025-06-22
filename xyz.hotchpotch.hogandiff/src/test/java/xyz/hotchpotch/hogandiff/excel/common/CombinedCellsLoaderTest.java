package xyz.hotchpotch.hogandiff.excel.common;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.task.CellsLoader;
import xyz.hotchpotch.hogandiff.util.function.UnsafeSupplier;

class CombinedCellsLoaderTest {

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
                                () -> CombinedCellsLoader.of(null));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CombinedCellsLoader.of(List.of()));

                // 正常系
                assertTrue(
                                CombinedCellsLoader.of(List.of(
                                                UnsafeSupplier.from(
                                                                () -> successLoader))) instanceof CombinedCellsLoader);
                assertTrue(
                                CombinedCellsLoader.of(List.of(
                                                UnsafeSupplier.from(() -> successLoader),
                                                UnsafeSupplier.from(() -> failLoader))) instanceof CombinedCellsLoader);
        }

        @Test
        void testLoadCells_パラメータチェック() {
                CellsLoader testee = CombinedCellsLoader.of(List.of(
                                UnsafeSupplier.from(() -> successLoader)));

                // null パラメータ
                assertThrows(
                                NullPointerException.class,
                                () -> testee.loadCells(null, "readPassword", "sheetName"));
                assertDoesNotThrow(
                                () -> testee.loadCells(Path.of("dummy.xlsx"), null, "dummy"));
                assertThrows(
                                NullPointerException.class,
                                () -> testee.loadCells(Path.of("dummy.xlsx"), "readPassword", null));
        }

        @Test
        void testLoadCells_失敗系() {
                CellsLoader testeeF = CombinedCellsLoader.of(List.of(
                                UnsafeSupplier.from(() -> failLoader)));
                CellsLoader testeeFFF = CombinedCellsLoader.of(List.of(
                                UnsafeSupplier.from(() -> failLoader),
                                UnsafeSupplier.from(() -> failLoader),
                                UnsafeSupplier.from(() -> failLoader)));

                // 失敗１つ
                assertThrows(
                                ExcelHandlingException.class,
                                () -> testeeF.loadCells(Path.of("dummy.xlsx"), null, "dummy"));

                // 全て失敗
                assertThrows(
                                ExcelHandlingException.class,
                                () -> testeeFFF.loadCells(Path.of("dummy.xlsx"), null, "dummy"));
        }

        @Test
        void testLoadSheetNames_成功系() throws ExcelHandlingException {
                CellsLoader testeeS = CombinedCellsLoader.of(List.of(
                                UnsafeSupplier.from(() -> successLoader)));
                CellsLoader testeeFFSF = CombinedCellsLoader.of(List.of(
                                UnsafeSupplier.from(() -> failLoader),
                                UnsafeSupplier.from(() -> failLoader),
                                UnsafeSupplier.from(() -> successLoader),
                                UnsafeSupplier.from(() -> failLoader)));

                // 成功１つ
                assertEquals(
                                Set.of(cell1),
                                testeeS.loadCells(Path.of("dummy.xlsx"), null, "dummy"));

                // いくつかの失敗ののちに成功
                assertEquals(
                                Set.of(cell1),
                                testeeFFSF.loadCells(Path.of("dummy.xlsx"), null, "dummy"));
        }
}
