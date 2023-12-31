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
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.PasswordHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;

class SheetNamesLoaderWithPoiUserApiTest {
    
    // [static members] ********************************************************
    
    private static BookOpenInfo test1_xls;
    private static BookOpenInfo test1_xlsb;
    private static BookOpenInfo test1_xlsm;
    private static BookOpenInfo test1_xlsx;
    private static BookOpenInfo test2_xls;
    private static BookOpenInfo test2_xlsx;
    
    private static Path bookPwTest1_xls;
    private static Path bookPwTest1_xlsx;
    private static Path bookPwTest2_xls;
    private static Path bookPwTest2_xlsx;
    private static Path bookPwTest3_xls;
    private static Path bookPwTest3_xlsx;
    private static Path bookPwTest4_xls;
    private static Path bookPwTest4_xlsx;
    
    private static Class<?> me = SheetNamesLoaderWithPoiUserApiTest.class;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = new BookOpenInfo(Path.of(me.getResource("Test1.xls").toURI()), null);
        test1_xlsb = new BookOpenInfo(Path.of(me.getResource("Test1.xlsb").toURI()), null);
        test1_xlsm = new BookOpenInfo(Path.of(me.getResource("Test1.xlsm").toURI()), null);
        test1_xlsx = new BookOpenInfo(Path.of(me.getResource("Test1.xlsx").toURI()), null);
        test2_xls = new BookOpenInfo(Path.of(me.getResource("Test2_passwordAAA.xls").toURI()), null);
        test2_xlsx = new BookOpenInfo(Path.of(me.getResource("Test2_passwordAAA.xlsx").toURI()), null);
        
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
                () -> SheetNamesLoaderWithPoiUserApi.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> SheetNamesLoaderWithPoiUserApi.of(Set.of()));
        
        // 正常系
        assertTrue(
                SheetNamesLoaderWithPoiUserApi.of(
                        EnumSet.allOf(SheetType.class)) instanceof SheetNamesLoaderWithPoiUserApi);
    }
    
    @Test
    void testLoadSheetNames_例外系_非チェック例外() {
        SheetNamesLoader testee = SheetNamesLoaderWithPoiUserApi.of(Set.of(SheetType.WORKSHEET));
        
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
        SheetNamesLoader testee = SheetNamesLoaderWithPoiUserApi.of(Set.of(SheetType.WORKSHEET));
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(Path.of("X:\\dummy\\dummy.xlsx"), null)));
        
        // 暗号化ファイル - 読み取りPW指定なし
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(test2_xls));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(test2_xlsx));
    }
    
    @Test
    void testLoadSheetNames_全てのシート種別が対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = SheetNamesLoaderWithPoiUserApi.of(EnumSet.allOf(SheetType.class));
        
        assertEquals(
                new BookInfo(
                        test1_xls,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadSheetNames(test1_xls));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どういう訳か「x3_ダイアログ」と「x4_マクロ」を取得できない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsm,
                        List.of("A1_ワークシート", "A2_グラフ",
                                "B1_ワークシート", "B2_グラフ")),
                testee.loadSheetNames(test1_xlsm));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どういう訳か「x3_ダイアログ」を取得できない。
        // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
        // 「x4_マクロ」は取得できている。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsx,
                        List.of("A1_ワークシート", "A2_グラフ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B4_マクロ")),
                testee.loadSheetNames(test1_xlsx));
    }
    
    @Test
    void testLoadSheetNames_ワークシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = SheetNamesLoaderWithPoiUserApi.of(EnumSet.of(SheetType.WORKSHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xls,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadSheetNames(test1_xls));
        
        assertEquals(
                new BookInfo(
                        test1_xlsm,
                        List.of("A1_ワークシート",
                                "B1_ワークシート")),
                testee.loadSheetNames(test1_xlsm));
        
        // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
        // 「x4_マクロ」も取得されている。
        assertEquals(
                new BookInfo(
                        test1_xlsx,
                        List.of("A1_ワークシート", "A4_マクロ",
                                "B1_ワークシート", "B4_マクロ")),
                testee.loadSheetNames(test1_xlsx));
    }
    
    @Test
    void testLoadSheetNames_グラフシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = SheetNamesLoaderWithPoiUserApi.of(EnumSet.of(SheetType.CHART_SHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xls,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadSheetNames(test1_xls));
        
        assertEquals(
                new BookInfo(
                        test1_xlsm,
                        List.of("A2_グラフ",
                                "B2_グラフ")),
                testee.loadSheetNames(test1_xlsm));
        
        assertEquals(
                new BookInfo(
                        test1_xlsx,
                        List.of("A2_グラフ",
                                "B2_グラフ")),
                testee.loadSheetNames(test1_xlsx));
    }
    
    @Test
    void testLoadSheetNames_ダイアログシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = SheetNamesLoaderWithPoiUserApi.of(EnumSet.of(SheetType.DIALOG_SHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xls,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadSheetNames(test1_xls));
        
        // FIXME: [No.1 シート識別不正 - usermodel] ダイアログシートを正しく識別できない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsm,
                        List.of()),
                testee.loadSheetNames(test1_xlsm));
        
        // FIXME: [No.1 シート識別不正 - usermodel] ダイアログシートを正しく識別できない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsx,
                        List.of()),
                testee.loadSheetNames(test1_xlsx));
    }
    
    @Test
    void testLoadSheetNames_マクロシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = SheetNamesLoaderWithPoiUserApi.of(EnumSet.of(SheetType.MACRO_SHEET));
        
        // FIXME: [No.1 シート識別不正 - usermodel] .xls 形式の場合はシート種別を見分けられない。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xls,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadSheetNames(test1_xls));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どうやら次の２つのバグが重なっているっぽい。
        //   ・.xlsm 形式のExcelブックからは「4_マクロ」を取得できない
        //   ・「1_ワークシート」と「4_マクロ」を判別できない
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsm,
                        List.of("A1_ワークシート",
                                "B1_ワークシート")),
                testee.loadSheetNames(test1_xlsm));
        
        // FIXME: [No.1 シート識別不正 - usermodel] どうやら次の２つの事情によりこうなるっぽい。
        //   ・マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存された
        //   ・「1_ワークシート」と「4_マクロ」を判別できない
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xlsx,
                        List.of("A1_ワークシート", "A4_マクロ",
                                "B1_ワークシート", "B4_マクロ")),
                testee.loadSheetNames(test1_xlsx));
    }
    
    @Test
    void testLoadSheetNames_読み取りPW指定なしの場合() {
        SheetNamesLoader testee = SheetNamesLoaderWithPoiUserApi.of(EnumSet.allOf(SheetType.class));
        
        // 開ける
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest1_xls, null)));
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest1_xlsx, null)));
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest3_xlsx, null)));
        
        // FIXME: [No.7 POI関連] 書き込みpw付きのxlsファイルを開けない
        // 書き込みpw有り/読み取りpw無しのxlsファイルは開けるべきだができない。
        // see: BookLoaderWithPoiUserApi#loadSheetNames
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest3_xls, null)));
        
        // 開けずにPasswordHandlingExceptionをスロー
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest2_xls, null)));
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest2_xlsx, null)));
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest4_xls, null)));
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest4_xlsx, null)));
    }
    
    @Test
    void testLoadSheetNames_読み取りPW指定ありの場合() {
        SheetNamesLoader testee = SheetNamesLoaderWithPoiUserApi.of(EnumSet.allOf(SheetType.class));
        
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest1_xls, "123")));
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest1_xlsx, "123")));
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest2_xls, "123")));
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest2_xlsx, "123")));
        
        // FIXME: [No.7 POI関連] 書き込みpw付きのxlsファイルを開けない
        // 書き込みpw有り/読み取りpw有りのxlsファイルは
        // 読み取り専用であれば正しい読み取りパスワードで開けるべきだができない。
        // see: BookLoaderWithPoiUserApi#loadSheetNames
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest3_xls, "123")));
        
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest3_xlsx, "123")));
        
        // FIXME: [No.7 POI関連] 書き込みpw付きのxlsファイルを開けない
        // 書き込みpw有り/読み取りpw有りのxlsファイルは
        // 読み取り専用であれば正しい読み取りパスワードで開けるべきだができない。
        // see: BookLoaderWithPoiUserApi#loadSheetNames
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest4_xls, "123")));
        
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest4_xlsx, "123")));
    }
    
    @Test
    void testLoadSheetNames_誤った読み取りPW指定ありの場合() {
        SheetNamesLoader testee = SheetNamesLoaderWithPoiUserApi.of(EnumSet.allOf(SheetType.class));
        
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest1_xls, "456")));
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest1_xlsx, "456")));
        
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest2_xls, "456")));
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest2_xlsx, "456")));
        
        // FIXME: [No.7 POI関連] 書き込みpw付きのxlsファイルを開けない
        // 書き込みpw有り/読み取りpw有りのxlsファイルは
        // 読み取り専用であれば正しい読み取りパスワードで開けるべきだができない。
        // see: BookLoaderWithPoiUserApi#loadSheetNames
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest3_xls, "456")));
        
        assertDoesNotThrow(
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest3_xlsx, "456")));
        
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest4_xls, "456")));
        assertThrows(
                PasswordHandlingException.class,
                () -> testee.loadSheetNames(new BookOpenInfo(bookPwTest4_xlsx, "456")));
    }
}
