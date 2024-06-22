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
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.PasswordHandlingException;
import xyz.hotchpotch.hogandiff.excel.BookInfoLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;

class BookInfoLoaderWithPoiUserApiTest {
    
    // [static members] ********************************************************
    
    private static Path test1_xls;
    private static Path test1_xlsb;
    private static Path test1_xlsm;
    private static Path test1_xlsx;
    private static Path test2_xls;
    private static Path test2_xlsx;
    
    private static Path bookPwTest1_xls;
    private static Path bookPwTest1_xlsx;
    private static Path bookPwTest2_xls;
    private static Path bookPwTest2_xlsx;
    private static Path bookPwTest3_xls;
    private static Path bookPwTest3_xlsx;
    private static Path bookPwTest4_xls;
    private static Path bookPwTest4_xlsx;
    
    private static Class<?> me = BookInfoLoaderWithPoiUserApiTest.class;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = Path.of(me.getResource("Test1.xls").toURI());
        test1_xlsb = Path.of(me.getResource("Test1.xlsb").toURI());
        test1_xlsm = Path.of(me.getResource("Test1.xlsm").toURI());
        test1_xlsx = Path.of(me.getResource("Test1.xlsx").toURI());
        test2_xls = Path.of(me.getResource("Test2_passwordAAA.xls").toURI());
        test2_xlsx = Path.of(me.getResource("Test2_passwordAAA.xlsx").toURI());
        
        bookPwTest1_xls = Path.of(me.getResource("BookPwTest1.xls").toURI());
        bookPwTest1_xlsx = Path.of(me.getResource("BookPwTest1.xlsx").toURI());
        bookPwTest2_xls = Path.of(me.getResource("BookPwTest2_r123.xls").toURI());
        bookPwTest2_xlsx = Path.of(me.getResource("BookPwTest2_r123.xlsx").toURI());
        bookPwTest3_xls = Path.of(me.getResource("BookPwTest3_w456.xls").toURI());
        bookPwTest3_xlsx = Path.of(me.getResource("BookPwTest3_w456.xlsx").toURI());
        bookPwTest4_xls = Path.of(me.getResource("BookPwTest4_r123_w456.xls").toURI());
        bookPwTest4_xlsx = Path.of(me.getResource("BookPwTest4_r123_w456.xlsx").toURI());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> BookInfoLoaderWithPoiUserApi.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> BookInfoLoaderWithPoiUserApi.of(Set.of()));
        
        // 正常系
        assertTrue(
                BookInfoLoaderWithPoiUserApi.of(
                        EnumSet.allOf(SheetType.class)) instanceof BookInfoLoaderWithPoiUserApi);
    }
    
    @Test
    void testLoadSheetNames_例外系_非チェック例外() {
        BookInfoLoader testee = BookInfoLoaderWithPoiUserApi.of(Set.of(SheetType.WORKSHEET));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadBookInfo(null, null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadBookInfo(test1_xlsb, null));
    }
    
    @Test
    void testLoadSheetNames_例外系_チェック例外() {
        BookInfoLoader testee = BookInfoLoaderWithPoiUserApi.of(Set.of(SheetType.WORKSHEET));
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadBookInfo(Path.of("X:\\dummy\\dummy.xlsx"), null));
        
        // 暗号化ファイル - 読み取りPW指定なし
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadBookInfo(test2_xls, null));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadBookInfo(test2_xlsx, null));
    }
    
    @Test
    void testLoadSheetNames_全てのシート種別が対象の場合() throws ExcelHandlingException {
        BookInfoLoader testee = BookInfoLoaderWithPoiUserApi.of(EnumSet.allOf(SheetType.class));
        
        assertEquals(
                new BookInfo(
                        test1_xls,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadBookInfo(test1_xls, null));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どういう訳か「x3_ダイアログ」と「x4_マクロ」を取得できない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsm,
                        List.of("A1_ワークシート", "A2_グラフ",
                                "B1_ワークシート", "B2_グラフ")),
                testee.loadBookInfo(test1_xlsm, null));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どういう訳か「x3_ダイアログ」を取得できない。
        // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
        // 「x4_マクロ」は取得できている。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsx,
                        List.of("A1_ワークシート", "A2_グラフ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B4_マクロ")),
                testee.loadBookInfo(test1_xlsx, null));
    }
    
    @Test
    void testLoadSheetNames_ワークシートのみが対象の場合() throws ExcelHandlingException {
        BookInfoLoader testee = BookInfoLoaderWithPoiUserApi.of(EnumSet.of(SheetType.WORKSHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xls,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadBookInfo(test1_xls, null));
        
        assertEquals(
                new BookInfo(
                        test1_xlsm,
                        List.of("A1_ワークシート",
                                "B1_ワークシート")),
                testee.loadBookInfo(test1_xlsm, null));
        
        // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
        // 「x4_マクロ」も取得されている。
        assertEquals(
                new BookInfo(
                        test1_xlsx,
                        List.of("A1_ワークシート", "A4_マクロ",
                                "B1_ワークシート", "B4_マクロ")),
                testee.loadBookInfo(test1_xlsx, null));
    }
    
    @Test
    void testLoadSheetNames_グラフシートのみが対象の場合() throws ExcelHandlingException {
        BookInfoLoader testee = BookInfoLoaderWithPoiUserApi.of(EnumSet.of(SheetType.CHART_SHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xls,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadBookInfo(test1_xls, null));
        
        assertEquals(
                new BookInfo(
                        test1_xlsm,
                        List.of("A2_グラフ",
                                "B2_グラフ")),
                testee.loadBookInfo(test1_xlsm, null));
        
        assertEquals(
                new BookInfo(
                        test1_xlsx,
                        List.of("A2_グラフ",
                                "B2_グラフ")),
                testee.loadBookInfo(test1_xlsx, null));
    }
    
    @Test
    void testLoadSheetNames_ダイアログシートのみが対象の場合() throws ExcelHandlingException {
        BookInfoLoader testee = BookInfoLoaderWithPoiUserApi.of(EnumSet.of(SheetType.DIALOG_SHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xls,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadBookInfo(test1_xls, null));
        
        // FIXME: [No.1 シート識別不正 - usermodel] ダイアログシートを正しく識別できない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsm,
                        List.of()),
                testee.loadBookInfo(test1_xlsm, null));
        
        // FIXME: [No.1 シート識別不正 - usermodel] ダイアログシートを正しく識別できない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsx,
                        List.of()),
                testee.loadBookInfo(test1_xlsx, null));
    }
    
    @Test
    void testLoadSheetNames_マクロシートのみが対象の場合() throws ExcelHandlingException {
        BookInfoLoader testee = BookInfoLoaderWithPoiUserApi.of(EnumSet.of(SheetType.MACRO_SHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xls,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadBookInfo(test1_xls, null));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どうやら次の２つのバグが重なっているっぽい。
        //   ・.xlsm 形式のExcelブックからは「4_マクロ」を取得できない
        //   ・「1_ワークシート」と「4_マクロ」を判別できない
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsm,
                        List.of("A1_ワークシート",
                                "B1_ワークシート")),
                testee.loadBookInfo(test1_xlsm, null));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どうやら次の２つの事情によりこうなるっぽい。
        //   ・マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存された
        //   ・「1_ワークシート」と「4_マクロ」を判別できない
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsx,
                        List.of("A1_ワークシート", "A4_マクロ",
                                "B1_ワークシート", "B4_マクロ")),
                testee.loadBookInfo(test1_xlsx, null));
    }
    
    @Test
    void testLoadSheetNames_読み取りPW指定なしの場合() {
        BookInfoLoader testee = BookInfoLoaderWithPoiUserApi.of(EnumSet.allOf(SheetType.class));
        
        // 開ける
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest1_xls, null));
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest1_xlsx, null));
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest3_xlsx, null));
        
        // FIXME: [No.7 POI関連] 書き込みpw付きのxlsファイルを開けない
        // 書き込みpw有り/読み取りpw無しのxlsファイルは開けるべきだができない。
        // see: BookLoaderWithPoiUserApi#loadSheetNames
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest3_xls, null));
        
        // 開けずにPasswordHandlingExceptionをスロー
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest2_xls, null));
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest2_xlsx, null));
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest4_xls, null));
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest4_xlsx, null));
    }
    
    @Test
    void testLoadSheetNames_読み取りPW指定ありの場合() {
        BookInfoLoader testee = BookInfoLoaderWithPoiUserApi.of(EnumSet.allOf(SheetType.class));
        
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest1_xls, "123"));
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest1_xlsx, "123"));
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest2_xls, "123"));
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest2_xlsx, "123"));
        
        // FIXME: [No.7 POI関連] 書き込みpw付きのxlsファイルを開けない
        // 書き込みpw有り/読み取りpw有りのxlsファイルは
        // 読み取り専用であれば正しい読み取りパスワードで開けるべきだができない。
        // see: BookLoaderWithPoiUserApi#loadSheetNames
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest3_xls, "123"));
        
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest3_xlsx, "123"));
        
        // FIXME: [No.7 POI関連] 書き込みpw付きのxlsファイルを開けない
        // 書き込みpw有り/読み取りpw有りのxlsファイルは
        // 読み取り専用であれば正しい読み取りパスワードで開けるべきだができない。
        // see: BookLoaderWithPoiUserApi#loadSheetNames
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest4_xls, "123"));
        
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest4_xlsx, "123"));
    }
    
    @Test
    void testLoadSheetNames_誤った読み取りPW指定ありの場合() {
        BookInfoLoader testee = BookInfoLoaderWithPoiUserApi.of(EnumSet.allOf(SheetType.class));
        
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest1_xls, "456"));
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest1_xlsx, "456"));
        
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest2_xls, "456"));
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest2_xlsx, "456"));
        
        // FIXME: [No.7 POI関連] 書き込みpw付きのxlsファイルを開けない
        // 書き込みpw有り/読み取りpw有りのxlsファイルは
        // 読み取り専用であれば正しい読み取りパスワードで開けるべきだができない。
        // see: BookLoaderWithPoiUserApi#loadSheetNames
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest3_xls, "456"));
        
        assertDoesNotThrow(
                () -> testee.loadBookInfo(bookPwTest3_xlsx, "456"));
        
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest4_xls, "456"));
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadBookInfo(bookPwTest4_xlsx, "456"));
    }
}