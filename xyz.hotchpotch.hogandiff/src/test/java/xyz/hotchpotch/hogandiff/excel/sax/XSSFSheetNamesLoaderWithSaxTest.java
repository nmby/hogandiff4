package xyz.hotchpotch.hogandiff.excel.sax;

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
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;

class XSSFSheetNamesLoaderWithSaxTest {
    
    // [static members] ********************************************************
    
    private static Path test1_xls;
    private static Path test1_xlsb;
    private static Path test1_xlsm;
    private static Path test1_xlsx;
    private static Path test2_xls;
    private static Path test2_xlsx;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = Path.of(XSSFSheetNamesLoaderWithSaxTest.class.getResource("Test1.xls").toURI());
        test1_xlsb = Path.of(XSSFSheetNamesLoaderWithSaxTest.class.getResource("Test1.xlsb").toURI());
        test1_xlsm = Path.of(XSSFSheetNamesLoaderWithSaxTest.class.getResource("Test1.xlsm").toURI());
        test1_xlsx = Path.of(XSSFSheetNamesLoaderWithSaxTest.class.getResource("Test1.xlsx").toURI());
        test2_xls = Path.of(XSSFSheetNamesLoaderWithSaxTest.class.getResource("Test2_passwordAAA.xls").toURI());
        test2_xlsx = Path.of(XSSFSheetNamesLoaderWithSaxTest.class.getResource("Test2_passwordAAA.xlsx").toURI());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> XSSFSheetNamesLoaderWithSax.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> XSSFSheetNamesLoaderWithSax.of(Set.of()));
        
        // 正常系
        assertTrue(
                XSSFSheetNamesLoaderWithSax.of(
                        EnumSet.allOf(SheetType.class)) instanceof XSSFSheetNamesLoaderWithSax);
    }
    
    @Test
    void testLoadSheetNames_例外系_非チェック例外() {
        SheetNamesLoader testee = XSSFSheetNamesLoaderWithSax.of(Set.of(SheetType.WORKSHEET));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadSheetNames(null, null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xls, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xlsb, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test2_xls, null));
    }
    
    @Test
    void testLoadSheetNames_例外系_チェック例外() {
        SheetNamesLoader testee = XSSFSheetNamesLoaderWithSax.of(Set.of(SheetType.WORKSHEET));
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(Path.of("X:\\dummy\\dummy.xlsx"), null));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(test2_xlsx, null));
    }
    
    @Test
    void testLoadSheetNames_全てのシート種別が対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = XSSFSheetNamesLoaderWithSax.of(EnumSet.allOf(SheetType.class));
        
        assertEquals(
                new BookInfo(
                        new BookOpenInfo(test1_xlsx, null),
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadSheetNames(test1_xlsx, null));
        assertEquals(
                new BookInfo(
                        new BookOpenInfo(test1_xlsm, null),
                        List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                                "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ")),
                testee.loadSheetNames(test1_xlsm, null));
    }
    
    @Test
    void testLoadSheetNames_ワークシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = XSSFSheetNamesLoaderWithSax.of(EnumSet.of(SheetType.WORKSHEET));
        
        // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
        // 「x4_マクロ」も取得されている。
        assertEquals(
                new BookInfo(
                        new BookOpenInfo(test1_xlsx, null),
                        List.of("A1_ワークシート", "A4_マクロ",
                                "B1_ワークシート", "B4_マクロ")),
                testee.loadSheetNames(test1_xlsx, null));
        assertEquals(
                new BookInfo(
                        new BookOpenInfo(test1_xlsm, null),
                        List.of("A1_ワークシート",
                                "B1_ワークシート")),
                testee.loadSheetNames(test1_xlsm, null));
    }
    
    @Test
    void testLoadSheetNames_グラフシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = XSSFSheetNamesLoaderWithSax.of(EnumSet.of(SheetType.CHART_SHEET));
        
        assertEquals(
                new BookInfo(
                        new BookOpenInfo(test1_xlsx, null),
                        List.of("A2_グラフ",
                                "B2_グラフ")),
                testee.loadSheetNames(test1_xlsx, null));
        assertEquals(
                new BookInfo(
                        new BookOpenInfo(test1_xlsm, null),
                        List.of("A2_グラフ",
                                "B2_グラフ")),
                testee.loadSheetNames(test1_xlsm, null));
    }
    
    @Test
    void testLoadSheetNames_ダイアログシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = XSSFSheetNamesLoaderWithSax.of(EnumSet.of(SheetType.DIALOG_SHEET));
        
        assertEquals(
                new BookInfo(
                        new BookOpenInfo(test1_xlsx, null),
                        List.of("A3_ダイアログ",
                                "B3_ダイアログ")),
                testee.loadSheetNames(test1_xlsx, null));
        assertEquals(
                new BookInfo(
                        new BookOpenInfo(test1_xlsm, null),
                        List.of("A3_ダイアログ",
                                "B3_ダイアログ")),
                testee.loadSheetNames(test1_xlsm, null));
    }
    
    @Test
    void testLoadSheetNames_マクロシートのみが対象の場合() throws ExcelHandlingException {
        SheetNamesLoader testee = XSSFSheetNamesLoaderWithSax.of(EnumSet.of(SheetType.MACRO_SHEET));
        
        // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
        // 「x4_マクロ」が取得されない。
        assertEquals(
                new BookInfo(
                        new BookOpenInfo(test1_xlsx, null),
                        List.of()),
                testee.loadSheetNames(test1_xlsx, null));
        assertEquals(
                new BookInfo(
                        new BookOpenInfo(test1_xlsm, null),
                        List.of("A4_マクロ",
                                "B4_マクロ")),
                testee.loadSheetNames(test1_xlsm, null));
    }
}
