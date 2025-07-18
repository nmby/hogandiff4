package xyz.hotchpotch.hogandiff.logic.loaders;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.logic.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.logic.models.CellData;

class LoaderForCellsWithPoiEventApiTest {

        // [static members] ********************************************************

        private static Path test1_xls;
        private static Path test1_xlsb;
        private static Path test1_xlsm;
        private static Path test1_xlsx;
        private static Path test2_xls;
        private static Path test2_xlsx;
        private static Path test3_xls;
        private static Path test5_xls;

        @BeforeAll
        static void beforeAll() throws URISyntaxException {
                test1_xls = Path.of(LoaderForCellsWithPoiEventApiTest.class.getResource("Test1.xls").toURI());
                test1_xlsb = Path.of(LoaderForCellsWithPoiEventApiTest.class.getResource("Test1.xlsb").toURI());
                test1_xlsm = Path.of(LoaderForCellsWithPoiEventApiTest.class.getResource("Test1.xlsm").toURI());
                test1_xlsx = Path.of(LoaderForCellsWithPoiEventApiTest.class.getResource("Test1.xlsx").toURI());
                test2_xls = Path.of(
                                LoaderForCellsWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xls").toURI());
                test2_xlsx = Path.of(
                                LoaderForCellsWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI());
                test3_xls = Path.of(LoaderForCellsWithPoiEventApiTest.class.getResource("Test3.xls").toURI());
                test5_xls = Path.of(LoaderForCellsWithPoiEventApiTest.class.getResource("Test5.xls").toURI());
        }

        // [instance members] ******************************************************

        @Test
        void testOf() {
                assertTrue(
                                new LoaderForCellsWithPoiEventApi(true) instanceof LoaderForCellsWithPoiEventApi);
                assertTrue(
                                new LoaderForCellsWithPoiEventApi(false) instanceof LoaderForCellsWithPoiEventApi);
        }

        @Test
        void testLoadCells_例外系_非チェック例外() {
                LoaderForCells testee = new LoaderForCellsWithPoiEventApi(true);

                // 対照群
                assertDoesNotThrow(
                                () -> testee.loadCells(test1_xls, null, "A1_ワークシート"));
                assertDoesNotThrow(
                                () -> testee.loadCells(test3_xls, null, "A_バリエーション"));

                // null パラメータ
                assertThrows(
                                NullPointerException.class,
                                () -> testee.loadCells(null, null, "A1_ワークシート"));
                assertThrows(
                                NullPointerException.class,
                                () -> testee.loadCells(test1_xls, null, null));
                assertThrows(
                                NullPointerException.class,
                                () -> testee.loadCells(null, null, null));

                // サポート対象外のブック形式
                assertThrows(
                                IllegalArgumentException.class,
                                () -> testee.loadCells(test1_xlsx, null, "A1_ワークシート"));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> testee.loadCells(test1_xlsm, null, "A1_ワークシート"));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> testee.loadCells(test1_xlsb, null, "A1_ワークシート"));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> testee.loadCells(test2_xlsx, null, "A1_ワークシート"));
        }

        @Test
        void testLoadCells_例外系_チェック例外1() {
                LoaderForCells testee = new LoaderForCellsWithPoiEventApi(true);

                // 存在しないファイル
                assertThrows(
                                ExcelHandlingException.class,
                                () -> testee.loadCells(Path.of("X:\\dummy\\dummy.xls"), null, "A1_ワークシート"));

                // 存在しないシート
                assertThrows(
                                ExcelHandlingException.class,
                                () -> testee.loadCells(test1_xls, null, "X9_ダミー"));

                // サポート対象外のシート種類
                assertThrows(
                                ExcelHandlingException.class,
                                () -> testee.loadCells(test1_xls, null, "A2_グラフ"));
                // FIXME: [No.01 シート識別不正 - usermodel] どういう訳かダイアログシートとワークシートを見分けられない。
                // assertThrows(
                // ExcelHandlingException.class,
                // () -> testee.loadCells(test1_xls, "A3_ダイアログ"));
                assertDoesNotThrow(
                                () -> testee.loadCells(test1_xls, null, "A3_ダイアログ"));
                assertThrows(
                                ExcelHandlingException.class,
                                () -> testee.loadCells(test1_xls, null, "A4_マクロ"));

                // 暗号化ファイル
                assertThrows(
                                ExcelHandlingException.class,
                                () -> testee.loadCells(test2_xls, null, "A1_ワークシート"));
        }

        @Test
        void testLoadCells_例外系_チェック例外2() {
                LoaderForCells testee = new LoaderForCellsWithPoiEventApi(false);

                // FIXME: [No.04 数式サポート改善] 現時点では、.xls 形式からの数式文字列抽出はサポート対象外。
                assertThrows(
                                ExcelHandlingException.class,
                                () -> testee.loadCells(test3_xls, null, "A_バリエーション"));

                // of(false) で生成されても、目的のシートに数式が含まれない場合は例外をスローしない。
                assertDoesNotThrow(
                                () -> testee.loadCells(test3_xls, null, "B_数式なし"));
        }

        @Test
        void testLoadCells_正常系1() throws ExcelHandlingException {
                LoaderForCells testee = new LoaderForCellsWithPoiEventApi(true);

                assertEquals(
                                Set.of(
                                                new CellData(0, 0, "これはワークシートです。", null),
                                                new CellData(2, 1, "X", null),
                                                new CellData(3, 1, "Y", null),
                                                new CellData(4, 1, "Z", null),
                                                new CellData(2, 2, "90", null),
                                                new CellData(3, 2, "20", null),
                                                new CellData(4, 2, "60", null)),
                                testee.loadCells(test1_xls, null, "A1_ワークシート"));
        }

        @Test
        void testLoadCells_正常系2_バリエーション_値抽出() throws ExcelHandlingException {
                LoaderForCells testee = new LoaderForCellsWithPoiEventApi(true);

                List<CellData> actual = new ArrayList<>(
                                testee.loadCells(test3_xls, null, "A_バリエーション"));
                actual.sort((c1, c2) -> {
                        if (c1.row() != c2.row()) {
                                return c1.row() < c2.row() ? -1 : 1;
                        } else if (c1.column() != c2.column()) {
                                return c1.column() < c2.column() ? -1 : 1;
                        } else {
                                throw new AssertionError();
                        }
                });

                assertEquals(56, actual.size());

                assertEquals(
                                List.of(
                                                new CellData(1, 2, "数値：整数", null),
                                                new CellData(1, 3, "1234567890", null),
                                                new CellData(2, 2, "数値：小数", null),
                                                new CellData(2, 3, "3.141592", null),
                                                new CellData(3, 2, "文字列", null),
                                                new CellData(3, 3, "abcあいう123", null),
                                                new CellData(4, 2, "真偽値：真", null),
                                                new CellData(4, 3, "true", null),
                                                new CellData(5, 2, "真偽値：偽", null),
                                                new CellData(5, 3, "false", null)),
                                actual.subList(0, 10));

                assertEquals(
                                List.of(
                                                new CellData(6, 2, "エラー：ゼロ除算", null),
                                                new CellData(6, 3, "#DIV/0!", null),
                                                new CellData(7, 2, "エラー：該当なし", null),
                                                new CellData(7, 3, "#N/A", null),
                                                new CellData(8, 2, "エラー：名前不正", null),
                                                new CellData(8, 3, "#NAME?", null),
                                                new CellData(9, 2, "エラー：ヌル", null),
                                                new CellData(9, 3, "#NULL!", null),
                                                new CellData(10, 2, "エラー：数値不正", null),
                                                new CellData(10, 3, "#NUM!", null),
                                                new CellData(11, 2, "エラー：参照不正", null),
                                                new CellData(11, 3, "#REF!", null),
                                                new CellData(12, 2, "エラー：値不正", null),
                                                new CellData(12, 3, "#VALUE!", null)),
                                actual.subList(10, 24));

                assertEquals(
                                List.of(
                                                // FIXME: [No.05 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                                                new CellData(13, 2, "日付", null),
                                                // CellReplica.of(13, 3, "2019/7/28", null),
                                                new CellData(13, 3, "43674", null),
                                                new CellData(14, 2, "時刻", null),
                                                // CellReplica.of(14, 3, "13:47", null),
                                                new CellData(14, 3, "0.574305555555556", null)),
                                actual.subList(24, 28));

                assertEquals(
                                List.of(
                                                new CellData(16, 2, "数式（数値：整数）", null),
                                                new CellData(16, 3, "31400", null),
                                                new CellData(17, 2, "数式（数値：小数）", null),
                                                new CellData(17, 3, "3.33333333333333", null),
                                                new CellData(18, 2, "数式（文字列）", null),
                                                new CellData(18, 3, "TRUEだよ", null),
                                                new CellData(19, 2, "数式（真偽値：真）", null),
                                                new CellData(19, 3, "true", null),
                                                new CellData(20, 2, "数式（真偽値：偽）", null),
                                                new CellData(20, 3, "false", null)),
                                actual.subList(28, 38));

                assertEquals(
                                List.of(
                                                new CellData(21, 2, "数式（エラー：ゼロ除算）", null),
                                                new CellData(21, 3, "#DIV/0!", null),
                                                new CellData(22, 2, "数式（エラー：該当なし）", null),
                                                new CellData(22, 3, "#N/A", null),
                                                new CellData(23, 2, "数式（エラー：名前不正）", null),
                                                new CellData(23, 3, "#NAME?", null),
                                                new CellData(24, 2, "数式（エラー：ヌル）", null),
                                                new CellData(24, 3, "#NULL!", null),
                                                new CellData(25, 2, "数式（エラー：数値不正）", null),
                                                new CellData(25, 3, "#NUM!", null),
                                                new CellData(26, 2, "数式（エラー：参照不正）", null),
                                                new CellData(26, 3, "#REF!", null),
                                                new CellData(27, 2, "数式（エラー：値不正）", null),
                                                new CellData(27, 3, "#VALUE!", null)),
                                actual.subList(38, 52));

                assertEquals(
                                List.of(
                                                // FIXME: [No.05 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                                                new CellData(28, 2, "数式（日付）", null),
                                                // CellReplica.of(28, 3, "2019/7/28", null),
                                                new CellData(28, 3, "43674", null),
                                                new CellData(29, 2, "数式（時刻）", null),
                                                // CellReplica.of(29, 3, "12:47", null)),
                                                new CellData(29, 3, "0.532638888888889", null)),
                                actual.subList(52, 56));
        }

        @Test
        void testLoadCells_正常系3_バリエーション_数式抽出() throws ExcelHandlingException {
                LoaderForCells testee = new LoaderForCellsWithPoiEventApi(false);

                // FIXME: [No.04 数式サポート改善] 現時点では、.xls 形式からの数式文字列抽出はサポート対象外。
                assertThrows(
                                ExcelHandlingException.class,
                                () -> testee.loadCells(test3_xls, null, "A_バリエーション"));
        }

        @Test
        void testLoadCells_正常系4_コメント関連a() throws ExcelHandlingException {
                LoaderForCells testee = new LoaderForCellsWithPoiEventApi(true);

                assertEquals(
                                Set.of(
                                                new CellData(1, 1, "", "Author:\nComment\nComment"),
                                                new CellData(4, 1, "", "Authorなし"),
                                                new CellData(7, 1, "", "非表示"),
                                                new CellData(10, 1, "", "書式設定"),
                                                new CellData(13, 1, "セル値あり", "コメント"),
                                                new CellData(16, 1, "空コメント", ""),
                                                new CellData(19, 1, "セル値のみ", null)),
                                testee.loadCells(test5_xls, null, "コメント"));
        }
}
