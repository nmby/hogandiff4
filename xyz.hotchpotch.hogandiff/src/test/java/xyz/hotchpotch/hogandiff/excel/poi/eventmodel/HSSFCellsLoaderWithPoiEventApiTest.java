package xyz.hotchpotch.hogandiff.excel.poi.eventmodel;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.CellsLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;

class HSSFCellsLoaderWithPoiEventApiTest {
    
    // [static members] ********************************************************
    
    private static final boolean saveMemory = false;
    
    private static BookOpenInfo test1_xls;
    private static BookOpenInfo test1_xlsb;
    private static BookOpenInfo test1_xlsm;
    private static BookOpenInfo test1_xlsx;
    private static BookOpenInfo test2_xls;
    private static BookOpenInfo test2_xlsx;
    private static BookOpenInfo test3_xls;
    private static BookOpenInfo test5_xls;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = new BookOpenInfo(
                Path.of(HSSFCellsLoaderWithPoiEventApiTest.class.getResource("Test1.xls").toURI()),
                null);
        test1_xlsb = new BookOpenInfo(
                Path.of(HSSFCellsLoaderWithPoiEventApiTest.class.getResource("Test1.xlsb").toURI()),
                null);
        test1_xlsm = new BookOpenInfo(
                Path.of(HSSFCellsLoaderWithPoiEventApiTest.class.getResource("Test1.xlsm").toURI()),
                null);
        test1_xlsx = new BookOpenInfo(
                Path.of(HSSFCellsLoaderWithPoiEventApiTest.class.getResource("Test1.xlsx").toURI()),
                null);
        test2_xls = new BookOpenInfo(
                Path.of(HSSFCellsLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xls").toURI()),
                null);
        test2_xlsx = new BookOpenInfo(
                Path.of(HSSFCellsLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI()),
                null);
        test3_xls = new BookOpenInfo(
                Path.of(HSSFCellsLoaderWithPoiEventApiTest.class.getResource("Test3.xls").toURI()),
                null);
        test5_xls = new BookOpenInfo(
                Path.of(HSSFCellsLoaderWithPoiEventApiTest.class.getResource("Test5.xls").toURI()),
                null);
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        assertTrue(
                HSSFCellsLoaderWithPoiEventApi.of(true, saveMemory) instanceof HSSFCellsLoaderWithPoiEventApi);
        assertTrue(
                HSSFCellsLoaderWithPoiEventApi.of(false, saveMemory) instanceof HSSFCellsLoaderWithPoiEventApi);
    }
    
    @Test
    void testLoadCells_例外系_非チェック例外() {
        CellsLoader testee = HSSFCellsLoaderWithPoiEventApi.of(true, saveMemory);
        
        // 対照群
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xls, "A1_ワークシート"));
        assertDoesNotThrow(
                () -> testee.loadCells(test3_xls, "A_バリエーション"));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, "A1_ワークシート"));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(test1_xls, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test1_xlsx, "A1_ワークシート"));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test1_xlsm, "A1_ワークシート"));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test1_xlsb, "A1_ワークシート"));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test2_xlsx, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外1() {
        CellsLoader testee = HSSFCellsLoaderWithPoiEventApi.of(true, saveMemory);
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(new BookOpenInfo(Path.of("X:\\dummy\\dummy.xls"), null), "A1_ワークシート"));
        
        // 存在しないシート
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xls, "X9_ダミー"));
        
        // サポート対象外のシート種類
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xls, "A2_グラフ"));
        // FIXME: [No.1 シート識別不正 - usermodel] どういう訳かダイアログシートとワークシートを見分けられない。
        //assertThrows(
        //        ExcelHandlingException.class,
        //        () -> testee.loadCells(test1_xls, "A3_ダイアログ"));
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xls, "A3_ダイアログ"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xls, "A4_マクロ"));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test2_xls, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外2() {
        CellsLoader testee = HSSFCellsLoaderWithPoiEventApi.of(false, saveMemory);
        
        // FIXME: [No.4 数式サポート改善] 現時点では、.xls 形式からの数式文字列抽出はサポート対象外。
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test3_xls, "A_バリエーション"));
        
        // of(false) で生成されても、目的のシートに数式が含まれない場合は例外をスローしない。
        assertDoesNotThrow(
                () -> testee.loadCells(test3_xls, "B_数式なし"));
    }
    
    @Test
    void testLoadCells_正常系1() throws ExcelHandlingException {
        CellsLoader testee = HSSFCellsLoaderWithPoiEventApi.of(true, saveMemory);
        
        assertEquals(
                Set.of(
                        new CellData(0, 0, "これはワークシートです。", null),
                        new CellData(2, 1, "X", null),
                        new CellData(3, 1, "Y", null),
                        new CellData(4, 1, "Z", null),
                        new CellData(2, 2, "90", null),
                        new CellData(3, 2, "20", null),
                        new CellData(4, 2, "60", null)),
                testee.loadCells(test1_xls, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_正常系2_バリエーション_値抽出() throws ExcelHandlingException {
        CellsLoader testee = HSSFCellsLoaderWithPoiEventApi.of(true, saveMemory);
        
        List<CellData> actual = new ArrayList<>(
                testee.loadCells(test3_xls, "A_バリエーション"));
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
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        new CellData(13, 2, "日付", null),
                        //CellReplica.of(13, 3, "2019/7/28", null),
                        new CellData(13, 3, "43674", null),
                        new CellData(14, 2, "時刻", null),
                        //CellReplica.of(14, 3, "13:47", null),
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
                        // FIXME: [No.5 日付と時刻の扱い改善] 日付と時刻が数値フォーマットで取得されてしまう。
                        new CellData(28, 2, "数式（日付）", null),
                        //CellReplica.of(28, 3, "2019/7/28", null),
                        new CellData(28, 3, "43674", null),
                        new CellData(29, 2, "数式（時刻）", null),
                        //CellReplica.of(29, 3, "12:47", null)),
                        new CellData(29, 3, "0.532638888888889", null)),
                actual.subList(52, 56));
    }
    
    @Test
    void testLoadCells_正常系3_バリエーション_数式抽出() throws ExcelHandlingException {
        CellsLoader testee = HSSFCellsLoaderWithPoiEventApi.of(false, saveMemory);
        
        // FIXME: [No.4 数式サポート改善] 現時点では、.xls 形式からの数式文字列抽出はサポート対象外。
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test3_xls, "A_バリエーション"));
    }
    
    @Test
    void testLoadCells_正常系4_コメント関連a() throws ExcelHandlingException {
        CellsLoader testee = HSSFCellsLoaderWithPoiEventApi.of(true, saveMemory);
        
        assertEquals(
                Set.of(
                        new CellData(1, 1, "", "Author:\nComment\nComment"),
                        new CellData(4, 1, "", "Authorなし"),
                        new CellData(7, 1, "", "非表示"),
                        new CellData(10, 1, "", "書式設定"),
                        new CellData(13, 1, "セル値あり", "コメント"),
                        new CellData(16, 1, "空コメント", ""),
                        new CellData(19, 1, "セル値のみ", null)),
                testee.loadCells(test5_xls, "コメント"));
    }
}
