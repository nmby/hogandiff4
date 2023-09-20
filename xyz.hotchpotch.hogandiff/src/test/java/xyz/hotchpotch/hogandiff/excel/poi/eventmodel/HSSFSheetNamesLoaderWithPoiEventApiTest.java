package xyz.hotchpotch.hogandiff.excel.poi.eventmodel;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetType;

class HSSFSheetNamesLoaderWithPoiEventApiTest {
    
    // [static members] ********************************************************
    
    private static BookInfo test1_xls;
    private static BookInfo test1_xlsb;
    private static BookInfo test1_xlsm;
    private static BookInfo test1_xlsx;
    private static BookInfo test2_xls;
    private static BookInfo test2_xlsx;
    private static BookInfo test4_xls;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = BookInfo.of(
                Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test1.xls").toURI()),
                null);
        test1_xlsb = BookInfo.of(
                Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test1.xlsb").toURI()),
                null);
        test1_xlsm = BookInfo.of(
                Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test1.xlsm").toURI()),
                null);
        test1_xlsx = BookInfo.of(
                Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test1.xlsx").toURI()),
                null);
        test2_xls = BookInfo.of(
                Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xls").toURI()),
                null);
        test2_xlsx = BookInfo.of(
                Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI()),
                null);
        test4_xls = BookInfo.of(
                Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test4_containsVBModule.xls").toURI()),
                null);
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> HSSFSheetNamesLoaderWithPoiEventApi.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> HSSFSheetNamesLoaderWithPoiEventApi.of(Set.of()));
        
        // 正常系
        assertTrue(
                HSSFSheetNamesLoaderWithPoiEventApi.of(
                        EnumSet.allOf(SheetType.class)) instanceof HSSFSheetNamesLoaderWithPoiEventApi);
    }
    
    @Test
    void testLoadSheetNames_例外系_非チェック例外() {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(Set.of(SheetType.WORKSHEET));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadSheetNames(null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xlsx));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xlsm));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xlsb));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test2_xlsx));
    }
    
    @Test
    void testLoadSheetNames_例外系_チェック例外() {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(Set.of(SheetType.WORKSHEET));
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(BookInfo.of(Path.of("X:\\dummy\\dummy.xls"), null)));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(test2_xls));
    }
    
    @Test
    void testLoadSheetNames_全てのシート種別が対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.allOf(SheetType.class));
        
        assertEquals(
                List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                        "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ"),
                testee.loadSheetNames(test1_xls));
    }
    
    @Test
    void testLoadSheetNames_ワークシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.of(SheetType.WORKSHEET));
        
        // FIXME: [No.1 シート識別不正 - HSSF] ダイアログシートもワークシートと判別されてしまう。
        // どうしようもないのかしら？？
        assertEquals(
                List.of("A1_ワークシート", "A3_ダイアログ",
                        "B1_ワークシート", "B3_ダイアログ"),
                testee.loadSheetNames(test1_xls));
    }
    
    @Test
    void testLoadSheetNames_グラフシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.of(SheetType.CHART_SHEET));
        
        assertEquals(
                List.of("A2_グラフ",
                        "B2_グラフ"),
                testee.loadSheetNames(test1_xls));
    }
    
    @Test
    void testLoadSheetNames_ダイアログシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.of(SheetType.DIALOG_SHEET));
        
        // FIXME: [No.1 シート識別不正 - HSSF] ダイアログシートもワークシートと判別されてしまう。
        // どうしようもないのかしら？？
        assertEquals(
                List.of(),
                testee.loadSheetNames(test1_xls));
    }
    
    @Test
    void testLoadSheetNames_マクロシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.of(SheetType.MACRO_SHEET));
        
        assertEquals(
                List.of("A4_マクロ",
                        "B4_マクロ"),
                testee.loadSheetNames(test1_xls));
    }
    
    @Test
    void testLoadSheetNames_VBモジュールが含まれる場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.allOf(SheetType.class));
        
        assertEquals(
                List.of("A1_ワークシート",
                        "A2_ワークシート"),
                testee.loadSheetNames(test4_xls));
    }
}
