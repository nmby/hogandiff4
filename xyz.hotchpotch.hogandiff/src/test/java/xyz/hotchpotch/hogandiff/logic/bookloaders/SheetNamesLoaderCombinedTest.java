package xyz.hotchpotch.hogandiff.logic.bookloaders;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.logic.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.logic.models.BookInfo;

class SheetNamesLoaderCombinedTest {

        // [static members] ********************************************************

        private static final SheetNamesLoader successLoader = (bookPath, readPassword) -> BookInfo.ofLoadCompleted(
                        bookPath, List.of("success"));

        private static final SheetNamesLoader failLoader = (bookPath, readPassword) -> {
                throw new RuntimeException("fail");
        };

        // [instance members] ******************************************************

        @Test
        void testOf() {
                // 異常系
                assertThrows(
                                NullPointerException.class,
                                () -> SheetNamesLoaderCombined.of(null));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> SheetNamesLoaderCombined.of(List.of()));

                // 正常系
                assertTrue(
                                SheetNamesLoaderCombined.of(List.of(
                                                () -> successLoader)) instanceof SheetNamesLoaderCombined);
                assertTrue(
                                SheetNamesLoaderCombined.of(List.of(
                                                () -> successLoader,
                                                () -> failLoader)) instanceof SheetNamesLoaderCombined);
        }

        @Test
        void testLoadSheetNames_パラメータチェック() {
                SheetNamesLoader testee = SheetNamesLoaderCombined.of(List.of(() -> successLoader));

                // null パラメータ
                assertThrows(
                                NullPointerException.class,
                                () -> testee.loadBookInfo(null, null));
        }

        @Test
        void testLoadSheetNames_失敗系() {
                SheetNamesLoader testeeF = SheetNamesLoaderCombined.of(List.of(() -> failLoader));
                SheetNamesLoader testeeFFF = SheetNamesLoaderCombined.of(List.of(
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
                SheetNamesLoader testeeS = SheetNamesLoaderCombined.of(List.of(() -> successLoader));
                SheetNamesLoader testeeFFSF = SheetNamesLoaderCombined.of(List.of(
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
