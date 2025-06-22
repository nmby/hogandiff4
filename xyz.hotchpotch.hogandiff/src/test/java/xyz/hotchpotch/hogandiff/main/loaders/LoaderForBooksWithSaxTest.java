package xyz.hotchpotch.hogandiff.main.loaders;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.main.misc.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.main.models.SheetType;
import xyz.hotchpotch.hogandiff.main.models.BookInfo;

class LoaderForBooksWithSaxTest {

        // [static members] ********************************************************

        private static Path test1_xls;
        private static Path test1_xlsb;
        private static Path test1_xlsm;
        private static Path test1_xlsx;
        private static Path test2_xls;
        private static Path test2_xlsx;

        @BeforeAll
        static void beforeAll() throws URISyntaxException {
                test1_xls = Path.of(LoaderForBooksWithSaxTest.class.getResource("Test1.xls").toURI());
                test1_xlsb = Path.of(LoaderForBooksWithSaxTest.class.getResource("Test1.xlsb").toURI());
                test1_xlsm = Path.of(LoaderForBooksWithSaxTest.class.getResource("Test1.xlsm").toURI());
                test1_xlsx = Path.of(LoaderForBooksWithSaxTest.class.getResource("Test1.xlsx").toURI());
                test2_xls = Path.of(LoaderForBooksWithSaxTest.class.getResource("Test2_passwordAAA.xls").toURI());
                test2_xlsx = Path.of(LoaderForBooksWithSaxTest.class.getResource("Test2_passwordAAA.xlsx").toURI());
        }

        // [instance members] ******************************************************

        @Test
        void testOf() {
                // 異常系
                assertThrows(
                                NullPointerException.class,
                                () -> LoaderForBooksWithSax.of(null));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> LoaderForBooksWithSax.of(Set.of()));

                // 正常系
                assertTrue(
                                LoaderForBooksWithSax.of(
                                                EnumSet.allOf(SheetType.class)) instanceof LoaderForBooksWithSax);
        }

        @Test
        void testLoadSheetNames_例外系_非チェック例外() {
                LoaderForBooks testee = LoaderForBooksWithSax.of(Set.of(SheetType.WORKSHEET));

                // null パラメータ
                assertThrows(
                                NullPointerException.class,
                                () -> testee.loadBookInfo(null, null));

                // サポート対象外のブック形式
                assertThrows(
                                IllegalArgumentException.class,
                                () -> testee.loadBookInfo(test1_xls, null));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> testee.loadBookInfo(test1_xlsb, null));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> testee.loadBookInfo(test2_xls, null));
        }

        @Test
        void testLoadSheetNames_例外系_チェック例外() {
                LoaderForBooks testee = LoaderForBooksWithSax.of(Set.of(SheetType.WORKSHEET));

                // 存在しないファイル
                assertEquals(
                                BookInfo.ofLoadFailed(Path.of("X:\\dummy\\dummy.xlsx")),
                                testee.loadBookInfo(Path.of("X:\\dummy\\dummy.xlsx"), null));

                // 暗号化ファイル
                assertEquals(
                                BookInfo.ofNeedsPassword(test2_xlsx),
                                testee.loadBookInfo(test2_xlsx, null));
        }

        @Test
        void testLoadSheetNames_全てのシート種別が対象の場合() throws ExcelHandlingException {
                LoaderForBooks testee = LoaderForBooksWithSax.of(EnumSet.allOf(SheetType.class));

                assertEquals(
                                BookInfo.ofLoadCompleted(
                                                test1_xlsx,
                                                List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                                testee.loadBookInfo(test1_xlsx, null));
                assertEquals(
                                BookInfo.ofLoadCompleted(
                                                test1_xlsm,
                                                List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                                testee.loadBookInfo(test1_xlsm, null));
        }

        @Test
        void testLoadSheetNames_ワークシートのみが対象の場合() throws ExcelHandlingException {
                LoaderForBooks testee = LoaderForBooksWithSax.of(EnumSet.of(SheetType.WORKSHEET));

                // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
                // 「x4_マクロ」も取得されている。
                assertEquals(
                                BookInfo.ofLoadCompleted(
                                                test1_xlsx,
                                                List.of("A1_ワークシート", "A4_マクロ",
                                                                "B1_ワークシート", "B4_マクロ")),
                                testee.loadBookInfo(test1_xlsx, null));
                assertEquals(
                                BookInfo.ofLoadCompleted(
                                                test1_xlsm,
                                                List.of("A1_ワークシート",
                                                                "B1_ワークシート")),
                                testee.loadBookInfo(test1_xlsm, null));
        }

        @Test
        void testLoadSheetNames_グラフシートのみが対象の場合() throws ExcelHandlingException {
                LoaderForBooks testee = LoaderForBooksWithSax.of(EnumSet.of(SheetType.CHART_SHEET));

                assertEquals(
                                BookInfo.ofLoadCompleted(
                                                test1_xlsx,
                                                List.of("A2_グラフ",
                                                                "B2_グラフ")),
                                testee.loadBookInfo(test1_xlsx, null));
                assertEquals(
                                BookInfo.ofLoadCompleted(
                                                test1_xlsm,
                                                List.of("A2_グラフ",
                                                                "B2_グラフ")),
                                testee.loadBookInfo(test1_xlsm, null));
        }

        @Test
        void testLoadSheetNames_ダイアログシートのみが対象の場合() throws ExcelHandlingException {
                LoaderForBooks testee = LoaderForBooksWithSax.of(EnumSet.of(SheetType.DIALOG_SHEET));

                assertEquals(
                                BookInfo.ofLoadCompleted(
                                                test1_xlsx,
                                                List.of("A3_ダイアログ",
                                                                "B3_ダイアログ")),
                                testee.loadBookInfo(test1_xlsx, null));
                assertEquals(
                                BookInfo.ofLoadCompleted(
                                                test1_xlsm,
                                                List.of("A3_ダイアログ",
                                                                "B3_ダイアログ")),
                                testee.loadBookInfo(test1_xlsm, null));
        }

        @Test
        void testLoadSheetNames_マクロシートのみが対象の場合() throws ExcelHandlingException {
                LoaderForBooks testee = LoaderForBooksWithSax.of(EnumSet.of(SheetType.MACRO_SHEET));

                // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
                // 「x4_マクロ」が取得されない。
                assertEquals(
                                BookInfo.ofLoadCompleted(
                                                test1_xlsx,
                                                List.of()),
                                testee.loadBookInfo(test1_xlsx, null));
                assertEquals(
                                BookInfo.ofLoadCompleted(
                                                test1_xlsm,
                                                List.of("A4_マクロ",
                                                                "B4_マクロ")),
                                testee.loadBookInfo(test1_xlsm, null));
        }
}
