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
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;

class HSSFSheetNamesLoaderWithPoiEventApiTest {
    
    // [static members] ********************************************************
    
    private static Path test1_xls;
    private static Path test1_xlsb;
    private static Path test1_xlsm;
    private static Path test1_xlsx;
    private static Path test2_xls;
    private static Path test2_xlsx;
    private static Path test4_xls;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test1.xls").toURI());
        test1_xlsb = Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test1.xlsb").toURI());
        test1_xlsm = Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test1.xlsm").toURI());
        test1_xlsx = Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test1.xlsx").toURI());
        test2_xls = Path.of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xls").toURI());
        test2_xlsx = Path
                .of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI());
        test4_xls = Path
                .of(HSSFSheetNamesLoaderWithPoiEventApiTest.class.getResource("Test4_containsVBModule.xls").toURI());
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
                () -> testee.loadSheetNames(null, null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xlsx, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xlsm, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xlsb, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test2_xlsx, null));
    }
    
    @Test
    void testLoadSheetNames_例外系_チェック例外() {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(Set.of(SheetType.WORKSHEET));
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(Path.of("X:\\dummy\\dummy.xls"), null));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(test2_xls, null));
    }
    
    @Test
    void testLoadSheetNames_全てのシート種別が対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.allOf(SheetType.class));
        
        assertEquals(
                new BookInfo(
                        test1_xls,
                        null,
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadSheetNames(test1_xls, null));
    }
    
    @Test
    void testLoadSheetNames_ワークシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.of(SheetType.WORKSHEET));
        
        // FIXME: [No.1 シート識別不正 - HSSF] ダイアログシートもワークシートと判別されてしまう。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xls,
                        null,
                        List.of("A1_ワークシート", "A3_ダイアログ",
                                "B1_ワークシート", "B3_ダイアログ")),
                testee.loadSheetNames(test1_xls, null));
    }
    
    @Test
    void testLoadSheetNames_グラフシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.of(SheetType.CHART_SHEET));
        
        assertEquals(
                new BookInfo(
                        test1_xls,
                        null,
                        List.of("A2_グラフ",
                                "B2_グラフ")),
                testee.loadSheetNames(test1_xls, null));
    }
    
    @Test
    void testLoadSheetNames_ダイアログシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.of(SheetType.DIALOG_SHEET));
        
        // FIXME: [No.1 シート識別不正 - HSSF] ダイアログシートもワークシートと判別されてしまう。
        // どうしようもないのかしら？？
        assertEquals(
                new BookInfo(
                        test1_xls,
                        null,
                        List.of()),
                testee.loadSheetNames(test1_xls, null));
    }
    
    @Test
    void testLoadSheetNames_マクロシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.of(SheetType.MACRO_SHEET));
        
        assertEquals(
                new BookInfo(
                        test1_xls,
                        null,
                        List.of("A4_マクロ",
                                "B4_マクロ")),
                testee.loadSheetNames(test1_xls, null));
    }
    
    @Test
    void testLoadSheetNames_VBモジュールが含まれる場合() throws ExcelHandlingException {
        SheetNamesLoader testee = HSSFSheetNamesLoaderWithPoiEventApi.of(EnumSet.allOf(SheetType.class));
        
        assertEquals(
                new BookInfo(
                        test4_xls,
                        null,
                        List.of("A1_ワークシート",
                                "A2_ワークシート")),
                testee.loadSheetNames(test4_xls, null));
    }
}
