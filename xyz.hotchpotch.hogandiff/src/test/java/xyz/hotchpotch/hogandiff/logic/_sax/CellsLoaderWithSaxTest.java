package xyz.hotchpotch.hogandiff.logic._sax;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.logic.BookInfo;
import xyz.hotchpotch.hogandiff.logic.CellData;
import xyz.hotchpotch.hogandiff.logic.CellsLoader;
import xyz.hotchpotch.hogandiff.logic.ExcelHandlingException;

class CellsLoaderWithSaxTest {
    
    // [static members] ********************************************************
    
    private static BookInfo test1_xlsm;
    private static BookInfo test1_xlsx;
    private static BookInfo test3_xlsx;
    private static BookInfo test4_xlsx;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xlsm = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithSaxTest.class.getResource("Test1.xlsm").toURI()), List.of());
        test1_xlsx = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithSaxTest.class.getResource("Test1.xlsx").toURI()), List.of());
        test3_xlsx = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithSaxTest.class.getResource("Test3.xlsx").toURI()), List.of());
        test4_xlsx = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithSaxTest.class.getResource("Test4a.xlsx").toURI()), List.of());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() throws ExcelHandlingException {
        // ■非チェック例外
        
        // ■正常系
        assertTrue(
                new CellsLoaderWithSax(true) instanceof CellsLoaderWithSax);
        assertTrue(
                new CellsLoaderWithSax(false) instanceof CellsLoaderWithSax);
    }
    
    @Test
    void testLoadCells_例外系_非チェック例外() throws ExcelHandlingException {
        CellsLoader testee = new CellsLoaderWithSax(true);
        
        // 対照
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xlsm, null, "A1_ワークシート"));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, null, "A1_ワークシート"));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(test1_xlsm, null, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, null, null));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外() throws ExcelHandlingException {
        CellsLoader testee = new CellsLoaderWithSax(true);
        
        // 存在しないシート
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, null, "X9_ダミー"));
        
        // サポート対象外のシート形式
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, null, "A2_グラフ"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, null, "A3_ダイアログ"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, null, "A4_マクロ"));
    }
    
    @Test
    void testLoadCells_正常系1() throws ExcelHandlingException {
        CellsLoader testee = new CellsLoaderWithSax(true);
        
        assertEquals(
                Set.of(
                        new CellData(0, 0, "これはワークシートです。", null),
                        new CellData(2, 1, "X", null),
                        new CellData(3, 1, "Y", null),
                        new CellData(4, 1, "Z", null),
                        new CellData(2, 2, "90", null),
                        new CellData(3, 2, "20", null),
                        new CellData(4, 2, "60", null)),
                testee.loadCells(test1_xlsx, null, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_正常系2_バリエーション_値抽出() throws ExcelHandlingException {
        CellsLoader testee = new CellsLoaderWithSax(true);
        
        List<CellData> actual = new ArrayList<>(
                testee.loadCells(test3_xlsx, null, "A_バリエーション"));
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
                        // FIXME: [No.06 小数の扱い改善] 小数精度は仕方ないのかな？
                        // CellReplica.of(2, 3, "3.141592", null),
                        new CellData(2, 3, "3.1415920000000002", null),
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
                        // FIXME: [No.06 小数の扱い改善] 小数精度は仕方ないのかな？
                        // CellReplica.of(14, 3, "13:47", null),
                        new CellData(14, 3, "0.57430555555555551", null)),
                actual.subList(24, 28));
        
        assertEquals(
                List.of(
                        new CellData(16, 2, "数式（数値：整数）", null),
                        new CellData(16, 3, "31400", null),
                        new CellData(17, 2, "数式（数値：小数）", null),
                        // FIXME: [No.06 小数の扱い改善] 小数精度は仕方ないのかな？
                        new CellData(17, 3, "3.3333333333333335", null),
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
                        // FIXME: [No.06 小数の扱い改善] 小数精度は仕方ないのかな？
                        // CellReplica.of(29, 3, "12:47", null)),
                        new CellData(29, 3, "0.53263888888888888", null)),
                actual.subList(52, 56));
    }
    
    @Test
    void testLoadCells_正常系3_数式抽出() throws ExcelHandlingException {
        CellsLoader testee = new CellsLoaderWithSax(false);
        
        List<CellData> actual = new ArrayList<>(
                testee.loadCells(test3_xlsx, null, "A_バリエーション"));
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
                        // FIXME: [No.06 小数の扱い改善] 小数精度は仕方ないのかな？
                        // CellReplica.of(2, 3, "3.141592", null),
                        new CellData(2, 3, "3.1415920000000002", null),
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
                        // FIXME: [No.06 小数の扱い改善] 小数精度は仕方ないのかな？
                        // CellReplica.of(14, 3, "13:47", null),
                        new CellData(14, 3, "0.57430555555555551", null)),
                actual.subList(24, 28));
        
        assertEquals(
                List.of(
                        new CellData(16, 2, "数式（数値：整数）", null),
                        new CellData(16, 3, " ROUND(D3 * 100, 0) * 100", null),
                        new CellData(17, 2, "数式（数値：小数）", null),
                        new CellData(17, 3, " 10 / 3", null),
                        new CellData(18, 2, "数式（文字列）", null),
                        new CellData(18, 3, " D5 & \"だよ\"", null),
                        new CellData(19, 2, "数式（真偽値：真）", null),
                        new CellData(19, 3, "(1=1)", null),
                        new CellData(20, 2, "数式（真偽値：偽）", null),
                        new CellData(20, 3, " (\"あ\" = \"い\")", null)),
                actual.subList(28, 38));
        
        assertEquals(
                List.of(
                        new CellData(21, 2, "数式（エラー：ゼロ除算）", null),
                        new CellData(21, 3, " D3 / (D2 - 1234567890)", null),
                        new CellData(22, 2, "数式（エラー：該当なし）", null),
                        new CellData(22, 3, " VLOOKUP(\"dummy\", C17:D22, 2)", null),
                        new CellData(23, 2, "数式（エラー：名前不正）", null),
                        new CellData(23, 3, " dummy()", null),
                        new CellData(24, 2, "数式（エラー：ヌル）", null),
                        new CellData(24, 3, " MAX(D2:D3 D17:D18)", null),
                        new CellData(25, 2, "数式（エラー：数値不正）", null),
                        new CellData(25, 3, " DATE(-1, -1, -1)", null),
                        new CellData(26, 2, "数式（エラー：参照不正）", null),
                        new CellData(26, 3, " INDIRECT(\"dummy\") + 100", null),
                        new CellData(27, 2, "数式（エラー：値不正）", null),
                        new CellData(27, 3, " \"abc\" + 123", null)),
                actual.subList(38, 52));
        
        assertEquals(
                List.of(
                        new CellData(28, 2, "数式（日付）", null),
                        new CellData(28, 3, " DATE(2019, 7, 28)", null),
                        new CellData(29, 2, "数式（時刻）", null),
                        new CellData(29, 3, " D15 - \"1:00\"", null)),
                actual.subList(52, 56));
    }
    
    @Test
    void testLoadCells_正常系4_コメント関連a() throws ExcelHandlingException {
        CellsLoader testee = new CellsLoaderWithSax(true);
        
        assertEquals(
                Set.of(
                        new CellData(1, 1, "", "Author:\nComment\nComment"),
                        new CellData(4, 1, "", "Authorなし"),
                        new CellData(7, 1, "", "非表示"),
                        new CellData(10, 1, "", "書式設定"),
                        new CellData(13, 1, "セル値あり", "コメント"),
                        new CellData(16, 1, "空コメント", ""),
                        new CellData(19, 1, "セル値のみ", null)),
                testee.loadCells(test4_xlsx, null, "コメント"));
    }
    
    @Test
    void testLoadCells_正常系4_コメント関連b() throws ExcelHandlingException {
        CellsLoader testee = new CellsLoaderWithSax(false);
        
        assertEquals(
                Set.of(
                        new CellData(1, 1, "", "Author:\nComment\nComment"),
                        new CellData(4, 1, "", "Authorなし"),
                        new CellData(7, 1, "", "非表示"),
                        new CellData(10, 1, "", "書式設定"),
                        new CellData(13, 1, "セル値あり", "コメント"),
                        new CellData(16, 1, "空コメント", ""),
                        new CellData(19, 1, " \"セル値\" & \"のみ\"", null)),
                testee.loadCells(test4_xlsx, null, "コメント"));
    }
}
