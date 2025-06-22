package xyz.hotchpotch.hogandiff.main.loaders;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.main.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.main.models.CellData;
import xyz.hotchpotch.hogandiff.util.function.UnsafeSupplier;

class LoaderForCellsCombinedTest {

        // [static members] ********************************************************

        private static final CellData cell1 = new CellData(1, 2, "success", null);

        private static final LoaderForCells successLoader = (bookPath, readPassword, sheetName) -> Set.of(cell1);

        private static final LoaderForCells failLoader = (bookPath, readPassword, sheetName) -> {
                throw new RuntimeException("fail");
        };

        // [instance members] ******************************************************

        @Test
        void testOf() {
                // 異常系
                assertThrows(
                                NullPointerException.class,
                                () -> LoaderForCellsCombined.of(null));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> LoaderForCellsCombined.of(List.of()));

                // 正常系
                assertTrue(
                                LoaderForCellsCombined.of(List.of(
                                                UnsafeSupplier.from(
                                                                () -> successLoader))) instanceof LoaderForCellsCombined);
                assertTrue(
                                LoaderForCellsCombined.of(List.of(
                                                UnsafeSupplier.from(() -> successLoader),
                                                UnsafeSupplier.from(
                                                                () -> failLoader))) instanceof LoaderForCellsCombined);
        }

        @Test
        void testLoadCells_パラメータチェック() {
                LoaderForCells testee = LoaderForCellsCombined.of(List.of(
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
                LoaderForCells testeeF = LoaderForCellsCombined.of(List.of(
                                UnsafeSupplier.from(() -> failLoader)));
                LoaderForCells testeeFFF = LoaderForCellsCombined.of(List.of(
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
                LoaderForCells testeeS = LoaderForCellsCombined.of(List.of(
                                UnsafeSupplier.from(() -> successLoader)));
                LoaderForCells testeeFFSF = LoaderForCellsCombined.of(List.of(
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
