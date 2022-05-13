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
import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetType;

class XSSFBookLoaderWithSaxTest {
    
    // [static members] ********************************************************
    
    private static BookInfo test1_xls;
    private static BookInfo test1_xlsb;
    private static BookInfo test1_xlsm;
    private static BookInfo test1_xlsx;
    private static BookInfo test2_xls;
    private static BookInfo test2_xlsx;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = BookInfo.of(Path.of(XSSFBookLoaderWithSaxTest.class.getResource("Test1.xls").toURI()));
        test1_xlsb = BookInfo.of(Path.of(XSSFBookLoaderWithSaxTest.class.getResource("Test1.xlsb").toURI()));
        test1_xlsm = BookInfo.of(Path.of(XSSFBookLoaderWithSaxTest.class.getResource("Test1.xlsm").toURI()));
        test1_xlsx = BookInfo.of(Path.of(XSSFBookLoaderWithSaxTest.class.getResource("Test1.xlsx").toURI()));
        test2_xls = BookInfo.of(Path.of(XSSFBookLoaderWithSaxTest.class.getResource("Test2_passwordAAA.xls").toURI()));
        test2_xlsx = BookInfo.of(Path.of(XSSFBookLoaderWithSaxTest.class.getResource("Test2_passwordAAA.xlsx").toURI()));
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> XSSFBookLoaderWithSax.of(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> XSSFBookLoaderWithSax.of(Set.of()));
        
        // 正常系
        assertTrue(
                XSSFBookLoaderWithSax.of(
                        EnumSet.allOf(SheetType.class)) instanceof XSSFBookLoaderWithSax);
    }
    
    @Test
    void testLoadSheetNames_例外系_非チェック例外() {
        BookLoader testee = XSSFBookLoaderWithSax.of(Set.of(SheetType.WORKSHEET));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadSheetNames(null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xls));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test1_xlsb));
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadSheetNames(test2_xls));
    }
    
    @Test
    void testLoadSheetNames_例外系_チェック例外() {
        BookLoader testee = XSSFBookLoaderWithSax.of(Set.of(SheetType.WORKSHEET));
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(BookInfo.of(Path.of("X:\\dummy\\dummy.xlsx"))));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadSheetNames(test2_xlsx));
    }
    
    @Test
    void testLoadSheetNames_全てのシート種別が対象の場合() throws ExcelHandlingException {
        BookLoader testee = XSSFBookLoaderWithSax.of(EnumSet.allOf(SheetType.class));
        
        assertEquals(
                List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                        "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ"),
                testee.loadSheetNames(test1_xlsx));
        assertEquals(
                List.of("A1_ワークシート", "A2_グラフ", "A3_ダイアログ", "A4_マクロ",
                        "B1_ワークシート", "B2_グラフ", "B3_ダイアログ", "B4_マクロ"),
                testee.loadSheetNames(test1_xlsm));
    }
    
    @Test
    void testLoadSheetNames_ワークシートのみが対象の場合() throws ExcelHandlingException {
        BookLoader testee = XSSFBookLoaderWithSax.of(EnumSet.of(SheetType.WORKSHEET));
        
        // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
        // 「x4_マクロ」も取得されている。
        assertEquals(
                List.of("A1_ワークシート", "A4_マクロ",
                        "B1_ワークシート", "B4_マクロ"),
                testee.loadSheetNames(test1_xlsx));
        assertEquals(
                List.of("A1_ワークシート",
                        "B1_ワークシート"),
                testee.loadSheetNames(test1_xlsm));
    }
    
    @Test
    void testLoadSheetNames_グラフシートのみが対象の場合() throws ExcelHandlingException {
        BookLoader testee = XSSFBookLoaderWithSax.of(EnumSet.of(SheetType.CHART_SHEET));
        
        assertEquals(
                List.of("A2_グラフ",
                        "B2_グラフ"),
                testee.loadSheetNames(test1_xlsx));
        assertEquals(
                List.of("A2_グラフ",
                        "B2_グラフ"),
                testee.loadSheetNames(test1_xlsm));
    }
    
    @Test
    void testLoadSheetNames_ダイアログシートのみが対象の場合() throws ExcelHandlingException {
        BookLoader testee = XSSFBookLoaderWithSax.of(EnumSet.of(SheetType.DIALOG_SHEET));
        
        assertEquals(
                List.of("A3_ダイアログ",
                        "B3_ダイアログ"),
                testee.loadSheetNames(test1_xlsx));
        assertEquals(
                List.of("A3_ダイアログ",
                        "B3_ダイアログ"),
                testee.loadSheetNames(test1_xlsm));
    }
    
    @Test
    void testLoadSheetNames_マクロシートのみが対象の場合() throws ExcelHandlingException {
        BookLoader testee = XSSFBookLoaderWithSax.of(EnumSet.of(SheetType.MACRO_SHEET));
        
        // マクロ無しのブックのため「x4_マクロ」が通常のワークシートとして保存されたためか、
        // 「x4_マクロ」が取得されない。
        assertEquals(
                List.of(),
                testee.loadSheetNames(test1_xlsx));
        assertEquals(
                List.of("A4_マクロ",
                        "B4_マクロ"),
                testee.loadSheetNames(test1_xlsm));
    }
}
