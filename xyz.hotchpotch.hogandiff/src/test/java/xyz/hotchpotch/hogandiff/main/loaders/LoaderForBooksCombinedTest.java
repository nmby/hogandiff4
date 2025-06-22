package xyz.hotchpotch.hogandiff.main.loaders;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.main.misc.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.main.models.BookInfo;

class LoaderForBooksCombinedTest {

        // [static members] ********************************************************

        private static final LoaderForBooks successLoader = (bookPath, readPassword) -> BookInfo.ofLoadCompleted(
                        bookPath, List.of("success"));

        private static final LoaderForBooks failLoader = (bookPath, readPassword) -> {
                throw new RuntimeException("fail");
        };

        // [instance members] ******************************************************

        @Test
        void testOf() {
                // 異常系
                assertThrows(
                                NullPointerException.class,
                                () -> LoaderForBooksCombined.of(null));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> LoaderForBooksCombined.of(List.of()));

                // 正常系
                assertTrue(
                                LoaderForBooksCombined.of(List.of(
                                                () -> successLoader)) instanceof LoaderForBooksCombined);
                assertTrue(
                                LoaderForBooksCombined.of(List.of(
                                                () -> successLoader,
                                                () -> failLoader)) instanceof LoaderForBooksCombined);
        }

        @Test
        void testLoadSheetNames_パラメータチェック() {
                LoaderForBooks testee = LoaderForBooksCombined.of(List.of(() -> successLoader));

                // null パラメータ
                assertThrows(
                                NullPointerException.class,
                                () -> testee.loadBookInfo(null, null));
        }

        @Test
        void testLoadSheetNames_失敗系() {
                LoaderForBooks testeeF = LoaderForBooksCombined.of(List.of(() -> failLoader));
                LoaderForBooks testeeFFF = LoaderForBooksCombined.of(List.of(
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
                LoaderForBooks testeeS = LoaderForBooksCombined.of(List.of(() -> successLoader));
                LoaderForBooks testeeFFSF = LoaderForBooksCombined.of(List.of(
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
