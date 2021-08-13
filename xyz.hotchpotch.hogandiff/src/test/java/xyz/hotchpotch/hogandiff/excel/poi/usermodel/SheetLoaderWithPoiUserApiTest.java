package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;

class SheetLoaderWithPoiUserApiTest {
    
    // [static members] ********************************************************
    
    private static final Function<Cell, CellData> converter = cell -> CellData.of(
            cell.getRowIndex(),
            cell.getColumnIndex(),
            PoiUtil.getCellContentAsString(cell, false));
    
    private static Path test1_xls;
    private static Path test1_xlsb;
    private static Path test1_xlsm;
    private static Path test1_xlsx;
    private static Path test2_xls;
    private static Path test2_xlsx;
    private static Path test4_xls;
    private static Path test4_xlsx;
    
    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        test1_xls = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test1.xls").toURI());
        test1_xlsb = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test1.xlsb").toURI());
        test1_xlsm = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test1.xlsm").toURI());
        test1_xlsx = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test1.xlsx").toURI());
        test2_xls = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test2_passwordAAA.xls").toURI());
        test2_xlsx = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test2_passwordAAA.xlsx").toURI());
        test4_xls = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test4.xls").toURI());
        test4_xlsx = Path.of(SheetLoaderWithPoiUserApiTest.class.getResource("Test4.xlsx").toURI());
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testOf() {
        assertThrows(
                NullPointerException.class,
                () -> SheetLoaderWithPoiUserApi.of(true, true, null));
        assertThrows(
                NullPointerException.class,
                () -> SheetLoaderWithPoiUserApi.of(true, false, null));
        
        assertThrows(
                IllegalArgumentException.class,
                () -> SheetLoaderWithPoiUserApi.of(false, true, converter));
        assertThrows(
                IllegalArgumentException.class,
                () -> SheetLoaderWithPoiUserApi.of(false, false, converter));
        
        assertTrue(
                SheetLoaderWithPoiUserApi.of(true, true, converter) instanceof SheetLoaderWithPoiUserApi);
        assertTrue(
                SheetLoaderWithPoiUserApi.of(true, false, converter) instanceof SheetLoaderWithPoiUserApi);
        assertTrue(
                SheetLoaderWithPoiUserApi.of(false, true, null) instanceof SheetLoaderWithPoiUserApi);
        assertTrue(
                SheetLoaderWithPoiUserApi.of(false, false, null) instanceof SheetLoaderWithPoiUserApi);
    }
    
    @Test
    void testLoadCells_例外系_非チェック例外() {
        SheetLoader testee = SheetLoaderWithPoiUserApi.of(true, true, converter);
        
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
        SheetLoader testee = SheetLoaderWithPoiUserApi.of(true, true, converter);
        
        // 存在しないファイル
        assertThrows(
                ExcelHandlingException.class,
                () -> testee.loadCells(Path.of("X:\\dummy\\dummy.xlsx"), "A1_ワークシート"));
        
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
        SheetLoader testee1 = SheetLoaderWithPoiUserApi.of(true, true, converter);
        SheetLoader testee2 = SheetLoaderWithPoiUserApi.of(true, false, converter);
        
        assertEquals(
                Set.of(
                        CellData.of(0, 0, "これはワークシートです。"),
                        CellData.of(2, 1, "X"),
                        CellData.of(3, 1, "Y"),
                        CellData.of(4, 1, "Z"),
                        CellData.of(2, 2, "90"),
                        CellData.of(3, 2, "20"),
                        CellData.of(4, 2, "60")),
                testee1.loadCells(test1_xls, "A1_ワークシート"));
        assertEquals(
                Set.of(
                        CellData.of(0, 0, "これはワークシートです。"),
                        CellData.of(2, 1, "X"),
                        CellData.of(3, 1, "Y"),
                        CellData.of(4, 1, "Z"),
                        CellData.of(2, 2, "90"),
                        CellData.of(3, 2, "20"),
                        CellData.of(4, 2, "60")),
                testee1.loadCells(test1_xlsx, "A1_ワークシート"));
        assertEquals(
                Set.of(
                        CellData.of(0, 0, "これはワークシートです。"),
                        CellData.of(2, 1, "X"),
                        CellData.of(3, 1, "Y"),
                        CellData.of(4, 1, "Z"),
                        CellData.of(2, 2, "90"),
                        CellData.of(3, 2, "20"),
                        CellData.of(4, 2, "60")),
                testee1.loadCells(test1_xlsm, "A1_ワークシート"));
        
        assertEquals(
                testee1.loadCells(test1_xls, "A1_ワークシート"),
                testee2.loadCells(test1_xls, "A1_ワークシート"));
        assertEquals(
                testee1.loadCells(test1_xlsx, "A1_ワークシート"),
                testee2.loadCells(test1_xlsx, "A1_ワークシート"));
        assertEquals(
                testee1.loadCells(test1_xlsm, "A1_ワークシート"),
                testee2.loadCells(test1_xlsm, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_セル内容抽出2() throws ExcelHandlingException {
        SheetLoader testee1 = SheetLoaderWithPoiUserApi.of(false, true, null);
        SheetLoader testee2 = SheetLoaderWithPoiUserApi.of(false, false, null);
        
        assertEquals(
                Set.of(),
                testee1.loadCells(test1_xls, "A1_ワークシート"));
        assertEquals(
                Set.of(),
                testee1.loadCells(test1_xlsx, "A1_ワークシート"));
        assertEquals(
                Set.of(),
                testee1.loadCells(test1_xlsm, "A1_ワークシート"));
        
        assertEquals(
                testee1.loadCells(test1_xls, "A1_ワークシート"),
                testee2.loadCells(test1_xls, "A1_ワークシート"));
        assertEquals(
                testee1.loadCells(test1_xlsx, "A1_ワークシート"),
                testee2.loadCells(test1_xlsx, "A1_ワークシート"));
        assertEquals(
                testee1.loadCells(test1_xlsm, "A1_ワークシート"),
                testee2.loadCells(test1_xlsm, "A1_ワークシート"));
    }
    
    @Test
    void testLoadCells_コメント抽出1() throws ExcelHandlingException {
        SheetLoader testee1 = SheetLoaderWithPoiUserApi.of(true, true, converter);
        SheetLoader testee2 = SheetLoaderWithPoiUserApi.of(false, true, null);
        
        assertEquals(
                Set.of(
                        CellData.of(2, 1, "").addComment("Author:\nComment\nComment"),
                        CellData.of(6, 1, "").addComment("Authorなし"),
                        CellData.of(10, 1, "").addComment("非表示"),
                        CellData.of(14, 1, "").addComment("書式設定"),
                        CellData.of(18, 1, "セル値あり").addComment("コメント"),
                        CellData.of(22, 1, "空コメント").addComment("")),
                testee1.loadCells(test4_xls, "コメント"));
        assertEquals(
                Set.of(
                        CellData.of(2, 1, "").addComment("Author:\nComment\nComment"),
                        CellData.of(6, 1, "").addComment("Authorなし"),
                        CellData.of(10, 1, "").addComment("非表示"),
                        CellData.of(14, 1, "").addComment("書式設定"),
                        CellData.of(18, 1, "セル値あり").addComment("コメント"),
                        CellData.of(22, 1, "空コメント").addComment("")),
                testee1.loadCells(test4_xlsx, "コメント"));
        
        assertEquals(
                Set.of(
                        CellData.of(2, 1, "").addComment("Author:\nComment\nComment"),
                        CellData.of(6, 1, "").addComment("Authorなし"),
                        CellData.of(10, 1, "").addComment("非表示"),
                        CellData.of(14, 1, "").addComment("書式設定"),
                        CellData.of(18, 1, "").addComment("コメント"),
                        CellData.of(22, 1, "").addComment("")),
                testee2.loadCells(test4_xls, "コメント"));
        assertEquals(
                Set.of(
                        CellData.of(2, 1, "").addComment("Author:\nComment\nComment"),
                        CellData.of(6, 1, "").addComment("Authorなし"),
                        CellData.of(10, 1, "").addComment("非表示"),
                        CellData.of(14, 1, "").addComment("書式設定"),
                        CellData.of(18, 1, "").addComment("コメント"),
                        CellData.of(22, 1, "").addComment("")),
                testee2.loadCells(test4_xlsx, "コメント"));
    }
    
    @Test
    void testLoadCells_コメント抽出2() throws ExcelHandlingException {
        SheetLoader testee1 = SheetLoaderWithPoiUserApi.of(true, false, converter);
        SheetLoader testee2 = SheetLoaderWithPoiUserApi.of(false, false, null);
        
        assertEquals(
                Set.of(
                        CellData.of(18, 1, "セル値あり"),
                        CellData.of(22, 1, "空コメント")),
                testee1.loadCells(test4_xls, "コメント"));
        assertEquals(
                Set.of(
                        CellData.of(18, 1, "セル値あり"),
                        CellData.of(22, 1, "空コメント")),
                testee1.loadCells(test4_xlsx, "コメント"));
        
        assertEquals(
                Set.of(),
                testee2.loadCells(test4_xls, "コメント"));
        assertEquals(
                Set.of(),
                testee2.loadCells(test4_xlsx, "コメント"));
    }
}
