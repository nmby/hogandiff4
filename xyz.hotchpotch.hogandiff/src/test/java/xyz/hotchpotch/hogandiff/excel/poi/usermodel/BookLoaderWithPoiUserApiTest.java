package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetType;

class BookLoaderWithPoiUserApiTest {
    
    // [static members] ********************************************************
    
    private static BookInfo test1_xls;
    private static BookInfo test1_xlsb;
    private static BookInfo test1_xlsm;
    private static BookInfo test1_xlsx;
    private static BookInfo test2_xls;
    private static BookInfo test2_xlsx;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = BookInfo.of(Path.of(BookLoaderWithPoiUserApiTest.class.getResource("Test1.xls").toURI()));
        test1_xlsb = BookInfo.of(Path.of(BookLoaderWithPoiUserApiTest.class.getResource("Test1.xlsb").toURI()));
        test1_xlsm = BookInfo.of(Path.of(BookLoaderWithPoiUserApiTest.class.getResource("Test1.xlsm").toURI()));
        test1_xlsx = BookInfo.of(Path.of(BookLoaderWithPoiUserApiTest.class.getResource("Test1.xlsx").toURI()));
        test2_xls = BookInfo.of(Path.of(BookLoaderWithPoiUserApiTest.class.getResource("Test2_passwordAAA.xls").toURI()));
        test2_xlsx = BookInfo.of(Path.of(BookLoaderWithPoiUserApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI()));
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> BookLoaderWithPoiUserApi.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> BookLoaderWithPoiUserApi.of(Set.of()));
        
        // 正常系
        assertTrue(
                BookLoaderWithPoiUserApi.of(
                        EnumSet.allOf(SheetType.class)) instanceof BookLoaderWithPoiUserApi);
    }
    
    @Test
    void testLoadSheetNames_例外系_非チェック例外() {
        BookLoader testee = BookLoaderWithPoiUserApi.of(Set.of(SheetType.WORKSHEET));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadSheetNames(null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xlsb));
    }
    
    @Test
    void testLoadSheetNames_例外系_チェック例外() {
        BookLoader testee = BookLoaderWithPoiUserApi.of(Set.of(SheetType.WORKSHEET));
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(BookInfo.of(Path.of("X:\\dummy\\dummy.xlsx"))));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(test2_xls));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(test2_xlsx));
    }
    
    @Test
    void testLoadSheetNames_全てのシート種別が対象の場合() throws ExcelHandlingException {
        BookLoader testee = BookLoaderWithPoiUserApi.of(EnumSet.allOf(SheetType.class));
        
        assertEquals(
                List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                        "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ"),
                testee.loadSheetNames(test1_xls));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どういう訳か「x3_ダイアログ」と「x4_マクロ」を取得できない。
        // どうしようもないのかしら？？
        assertEquals(
                List.of("A1_ワークシート", "A2_グラフ",
                        "B1_ワークシート", "B2_グラフ"),
                testee.loadSheetNames(test1_xlsm));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どういう訳か「x3_ダイアログ」を取得できない。
        // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
        // 「x4_マクロ」は取得できている。
        // どうしようもないのかしら？？
        assertEquals(
                List.of("A1_ワークシート", "A2_グラフ", "A4_マクロ",
                        "B1_ワークシート", "B2_グラフ", "B4_マクロ"),
                testee.loadSheetNames(test1_xlsx));
    }
    
    @Test
    void testLoadSheetNames_ワークシートのみが対象の場合() throws ExcelHandlingException {
        BookLoader testee = BookLoaderWithPoiUserApi.of(EnumSet.of(SheetType.WORKSHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                        "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ"),
                testee.loadSheetNames(test1_xls));
        
        assertEquals(
                List.of("A1_ワークシート",
                        "B1_ワークシート"),
                testee.loadSheetNames(test1_xlsm));
        
        // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
        // 「x4_マクロ」も取得されている。
        assertEquals(
                List.of("A1_ワークシート", "A4_マクロ",
                        "B1_ワークシート", "B4_マクロ"),
                testee.loadSheetNames(test1_xlsx));
    }
    
    @Test
    void testLoadSheetNames_グラフシートのみが対象の場合() throws ExcelHandlingException {
        BookLoader testee = BookLoaderWithPoiUserApi.of(EnumSet.of(SheetType.CHART_SHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                        "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ"),
                testee.loadSheetNames(test1_xls));
        
        assertEquals(
                List.of("A2_グラフ",
                        "B2_グラフ"),
                testee.loadSheetNames(test1_xlsm));
        
        assertEquals(
                List.of("A2_グラフ",
                        "B2_グラフ"),
                testee.loadSheetNames(test1_xlsx));
    }
    
    @Test
    void testLoadSheetNames_ダイアログシートのみが対象の場合() throws ExcelHandlingException {
        BookLoader testee = BookLoaderWithPoiUserApi.of(EnumSet.of(SheetType.DIALOG_SHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                        "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ"),
                testee.loadSheetNames(test1_xls));
        
        // FIXME: [No.1 シート識別不正 - usermodel] ダイアログシートを正しく識別できない。
        // どうしようもないのかしら？？
        assertEquals(
                List.of(),
                testee.loadSheetNames(test1_xlsm));
        
        // FIXME: [No.1 シート識別不正 - usermodel] ダイアログシートを正しく識別できない。
        // どうしようもないのかしら？？
        assertEquals(
                List.of(),
                testee.loadSheetNames(test1_xlsx));
    }
    
    @Test
    void testLoadSheetNames_マクロシートのみが対象の場合() throws ExcelHandlingException {
        BookLoader testee = BookLoaderWithPoiUserApi.of(EnumSet.of(SheetType.MACRO_SHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                        "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ"),
                testee.loadSheetNames(test1_xls));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どうやら次の２つのバグが重なっているっぽい。
        //   ・.xlsm 形式のExcelブックからは「4_マクロ」を取得できない
        //   ・「1_ワークシート」と「4_マクロ」を判別できない
        // どうしようもないのかしら？？
        assertEquals(
                List.of("A1_ワークシート",
                        "B1_ワークシート"),
                testee.loadSheetNames(test1_xlsm));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どうやら次の２つの事情によりこうなるっぽい。
        //   ・マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存された
        //   ・「1_ワークシート」と「4_マクロ」を判別できない
        // どうしようもないのかしら？？
        assertEquals(
                List.of("A1_ワークシート", "A4_マクロ",
                        "B1_ワークシート", "B4_マクロ"),
                testee.loadSheetNames(test1_xlsx));
    }
}
