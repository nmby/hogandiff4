package xyz.hotchpotch.hogandiff.logic._poi;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.logic.BookInfo;
import xyz.hotchpotch.hogandiff.logic.CellData;
import xyz.hotchpotch.hogandiff.logic.CellsLoader;
import xyz.hotchpotch.hogandiff.logic.ExcelHandlingException;

class CellsLoaderWithPoiUserApiTest {
    
    // [static members] ********************************************************
    
    private static final Function<Cell, CellData> converter = cell -> new CellData(
            cell.getRowIndex(),
            cell.getColumnIndex(),
            PoiUtil.getCellContentAsString(cell, false),
            null);
    
    private static BookInfo test1_xls;
    private static BookInfo test1_xlsb;
    private static BookInfo test1_xlsm;
    private static BookInfo test1_xlsx;
    private static BookInfo test2_xls;
    private static BookInfo test2_xlsx;
    private static BookInfo test4_xls;
    private static BookInfo test4_xlsx;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test1.xls").toURI()), List.of());
        test1_xlsb = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test1.xlsb").toURI()), List.of());
        test1_xlsm = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test1.xlsm").toURI()), List.of());
        test1_xlsx = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test1.xlsx").toURI()), List.of());
        test2_xls = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test2_passwordAAA.xls").toURI()), List.of());
        test2_xlsx = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI()), List.of());
        test4_xls = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test4.xls").toURI()), List.of());
        test4_xlsx = BookInfo.ofLoadCompleted(
                Path.of(CellsLoaderWithPoiUserApiTest.class.getResource("Test4.xlsx").toURI()), List.of());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        assertThrows(
                NullPointerException.class,
                () -> new CellsLoaderWithPoiUserApi(null));
        
        assertTrue(
                new CellsLoaderWithPoiUserApi(converter) instanceof CellsLoaderWithPoiUserApi);
    }
    
    @Test
    void testLoadCells_例外系_非チェック例外() {
        CellsLoader testee = new CellsLoaderWithPoiUserApi(converter);
        
        // 対照群
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xlsx, null, "A1_ワークシート"));
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xlsm, null, "A1_ワークシート"));
        assertDoesNotThrow(
                () -> testee.loadCells(test1_xls, null, "A1_ワークシート"));
        
        // null パラメータ
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, null, "A1_ワークシート"));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(test1_xlsx, null, null));
        assertThrows(
                NullPointerException.class,
                () -> testee.loadCells(null, null, null));
        
        // サポート対象外のブック形式
        assertThrows(
                IllegalArgumentException.class,
                () -> testee.loadCells(test1_xlsb, null, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_例外系_チェック例外() {
        CellsLoader testee = new CellsLoaderWithPoiUserApi(converter);
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(
                        BookInfo.ofLoadCompleted(Path.of("X:\\dummy\\dummy.xlsx"), List.of()), null, "A1_ワークシート"));
        
        // 存在しないシート
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsx, null, "X9_ダミー"));
        
        // サポート対象外のシート種類
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, null, "A2_グラフ"));
        assertThrows(
                // FIXME: [No.01 シート識別不正 - usermodel] どういう訳か、Apache POI ユーザーモデルAPIでは
                // .xlsm 形式のExcelブックからダイアログシートを読み込めない。
                // そのため「当該シート無し」と判定され、
                // 結果的には目的通りの ExcelHandlingException がスローされる。
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, null, "A3_ダイアログ"));
        assertThrows(
                // FIXME: [No.01 シート識別不正 - usermodel] どういう訳か、Apache POI ユーザーモデルAPIでは
                // .xlsm 形式のExcelブックからマクロシートを読み込めない。
                // そのため「当該シート無し」と判定され、
                // 結果的には目的通りの ExcelHandlingException がスローされる。
                ExcelHandlingException.class,
                () -> testee.loadCells(test1_xlsm, null, "A4_マクロ"));
        
        // 暗号化ファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test2_xlsx, null, "A1_ワークシート"));
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(test2_xls, null, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_セル内容抽出1() throws ExcelHandlingException {
        CellsLoader testee1 = new CellsLoaderWithPoiUserApi(converter);
        
        assertEquals(
                Set.of(
                        new CellData(0, 0, "これはワークシートです。", null),
                        new CellData(2, 1, "X", null),
                        new CellData(3, 1, "Y", null),
                        new CellData(4, 1, "Z", null),
                        new CellData(2, 2, "90", null),
                        new CellData(3, 2, "20", null),
                        new CellData(4, 2, "60", null)),
                testee1.loadCells(test1_xls, null, "A1_ワークシート"));
        assertEquals(
                Set.of(
                        new CellData(0, 0, "これはワークシートです。", null),
                        new CellData(2, 1, "X", null),
                        new CellData(3, 1, "Y", null),
                        new CellData(4, 1, "Z", null),
                        new CellData(2, 2, "90", null),
                        new CellData(3, 2, "20", null),
                        new CellData(4, 2, "60", null)),
                testee1.loadCells(test1_xlsx, null, "A1_ワークシート"));
        assertEquals(
                Set.of(
                        new CellData(0, 0, "これはワークシートです。", null),
                        new CellData(2, 1, "X", null),
                        new CellData(3, 1, "Y", null),
                        new CellData(4, 1, "Z", null),
                        new CellData(2, 2, "90", null),
                        new CellData(3, 2, "20", null),
                        new CellData(4, 2, "60", null)),
                testee1.loadCells(test1_xlsm, null, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_コメント抽出1() throws ExcelHandlingException {
        CellsLoader testee1 = new CellsLoaderWithPoiUserApi(converter);
        
        assertEquals(
                Set.of(
                        new CellData(2, 1, "", "Author:\nComment\nComment"),
                        new CellData(6, 1, "", "Authorなし"),
                        new CellData(10, 1, "", "非表示"),
                        new CellData(14, 1, "", "書式設定"),
                        new CellData(18, 1, "セル値あり", "コメント"),
                        new CellData(22, 1, "空コメント", "")),
                testee1.loadCells(test4_xls, null, "コメント"));
        assertEquals(
                Set.of(
                        new CellData(2, 1, "", "Author:\nComment\nComment"),
                        new CellData(6, 1, "", "Authorなし"),
                        new CellData(10, 1, "", "非表示"),
                        new CellData(14, 1, "", "書式設定"),
                        new CellData(18, 1, "セル値あり", "コメント"),
                        new CellData(22, 1, "空コメント", "")),
                testee1.loadCells(test4_xlsx, null, "コメント"));
    }
}
