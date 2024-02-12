package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.CellsLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;

class CellsLoaderWithPoiUserApiTest {
    
    // [static members] ********************************************************
    
    private static final boolean saveMemory = false;
    
    private static final Function<Cell, CellData> converter = cell -> new CellData(
            cell.getRowIndex(),
            cell.getColumnIndex(),
            PoiUtil.getCellContentAsString(cell, false),
            null);
    
    private static BookOpenInfo test1_xls;
    private static BookOpenInfo test1_xlsb;
    private static BookOpenInfo test1_xlsm;
    private static BookOpenInfo test1_xlsx;
    private static BookOpenInfo test2_xls;
    private static BookOpenInfo test2_xlsx;
    private static BookOpenInfo test4_xls;
    private static BookOpenInfo test4_xlsx;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = new BookOpenInfo(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test1.xls").toURI()),
                null);
        test1_xlsb = new BookOpenInfo(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test1.xlsb").toURI()),
                null);
        test1_xlsm = new BookOpenInfo(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test1.xlsm").toURI()),
                null);
        test1_xlsx = new BookOpenInfo(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test1.xlsx").toURI()),
                null);
        test2_xls = new BookOpenInfo(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test2_passwordAAA.xls").toURI()),
                null);
        test2_xlsx = new BookOpenInfo(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI()),
                null);
        test4_xls = new BookOpenInfo(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test4.xls").toURI()),
                null);
        test4_xlsx = new BookOpenInfo(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test4.xlsx").toURI()),
                null);
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        assertThrows(
                NullPointerException.class,
                () -> CellsLoaderWithPoiUserApi.of(saveMemory, null));
        
        assertTrue(
                CellsLoaderWithPoiUserApi.of(saveMemory, converter) instanceof CellsLoaderWithPoiUserApi);
    }
    
    @Test
    void testLoadCells_例外系_非チェック例外() {
        CellsLoader testee = CellsLoaderWithPoiUserApi.of(saveMemory, converter);
        
        // 対照群
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xlsx, "A1_ワークシート"));
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xlsm, "A1_ワークシート"));
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xls, "A1_ワークシート"));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, "A1_ワークシート"));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(test1_xlsx, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test1_xlsb, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外() {
        CellsLoader testee = CellsLoaderWithPoiUserApi.of(saveMemory, converter);
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(new BookOpenInfo(Path.of("X:\\dummy\\dummy.xlsx"), null), "A1_ワークシート"));
        
        // 存在しないシート
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsx, "X9_ダミー"));
        
        // サポート対象外のシート種類
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, "A2_グラフ"));
        assertThrows(
                // FIXME: [No.1 シート識別不正 - usermodel] どういう訳か、Apache POI ユーザーモデルAPIでは
                // .xlsm 形式のExcelブックからダイアログシートを読み込めない。
                // そのため「当該シート無し」と判定され、
                // 結果的には目的通りの ExcelHandlingException がスローされる。
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, "A3_ダイアログ"));
        assertThrows(
                // FIXME: [No.1 シート識別不正 - usermodel] どういう訳か、Apache POI ユーザーモデルAPIでは
                // .xlsm 形式のExcelブックからマクロシートを読み込めない。
                // そのため「当該シート無し」と判定され、
                // 結果的には目的通りの ExcelHandlingException がスローされる。
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, "A4_マクロ"));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test2_xlsx, "A1_ワークシート"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test2_xls, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_セル内容抽出1() throws ExcelHandlingException {
        CellsLoader testee1 = CellsLoaderWithPoiUserApi.of(saveMemory, converter);
        
        assertEquals(
                Set.of(
                        new CellData(0, 0, "これはワークシートです。", null),
                        new CellData(2, 1, "X", null),
                        new CellData(3, 1, "Y", null),
                        new CellData(4, 1, "Z", null),
                        new CellData(2, 2, "90", null),
                        new CellData(3, 2, "20", null),
                        new CellData(4, 2, "60", null)),
                testee1.loadCells(test1_xls, "A1_ワークシート"));
        assertEquals(
                Set.of(
                        new CellData(0, 0, "これはワークシートです。", null),
                        new CellData(2, 1, "X", null),
                        new CellData(3, 1, "Y", null),
                        new CellData(4, 1, "Z", null),
                        new CellData(2, 2, "90", null),
                        new CellData(3, 2, "20", null),
                        new CellData(4, 2, "60", null)),
                testee1.loadCells(test1_xlsx, "A1_ワークシート"));
        assertEquals(
                Set.of(
                        new CellData(0, 0, "これはワークシートです。", null),
                        new CellData(2, 1, "X", null),
                        new CellData(3, 1, "Y", null),
                        new CellData(4, 1, "Z", null),
                        new CellData(2, 2, "90", null),
                        new CellData(3, 2, "20", null),
                        new CellData(4, 2, "60", null)),
                testee1.loadCells(test1_xlsm, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_コメント抽出1() throws ExcelHandlingException {
        CellsLoader testee1 = CellsLoaderWithPoiUserApi.of(saveMemory, converter);
        
        assertEquals(
                Set.of(
                        new CellData(2, 1, "", "Author:\nComment\nComment"),
                        new CellData(6, 1, "", "Authorなし"),
                        new CellData(10, 1, "", "非表示"),
                        new CellData(14, 1, "", "書式設定"),
                        new CellData(18, 1, "セル値あり", "コメント"),
                        new CellData(22, 1, "空コメント", "")),
                testee1.loadCells(test4_xls, "コメント"));
        assertEquals(
                Set.of(
                        new CellData(2, 1, "", "Author:\nComment\nComment"),
                        new CellData(6, 1, "", "Authorなし"),
                        new CellData(10, 1, "", "非表示"),
                        new CellData(14, 1, "", "書式設定"),
                        new CellData(18, 1, "セル値あり", "コメント"),
                        new CellData(22, 1, "空コメント", "")),
                testee1.loadCells(test4_xlsx, "コメント"));
    }
}
