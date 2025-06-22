package xyz.hotchpotch.hogandiff.main;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.EnumSet;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.main.models.SheetType;

class PoiUtilTest1_possibleTypes {

        // [static members] ********************************************************

        private static Workbook test1_xls;
        private static Workbook test1_xlsm;

        private static Sheet test1_xls_A1_Worksheet;
        private static Sheet test1_xls_A2_ChartSheet;
        private static Sheet test1_xls_A3_DialogSheet;
        private static Sheet test1_xls_A4_MacroSheet;

        private static Sheet test1_xlsm_A1_Worksheet;
        private static Sheet test1_xlsm_A2_ChartSheet;
        private static Sheet test1_xlsm_A3_DialogSheet;
        private static Sheet test1_xlsm_A4_MacroSheet;

        @BeforeAll
        static void beforeAll() throws IOException {
                test1_xls = WorkbookFactory
                                .create(PoiUtilTest1_possibleTypes.class.getResource("Test1.xls").openStream());
                test1_xls_A1_Worksheet = test1_xls.getSheet("A1_ワークシート");
                test1_xls_A2_ChartSheet = test1_xls.getSheet("A2_グラフ");
                test1_xls_A3_DialogSheet = test1_xls.getSheet("A3_ダイアログ");
                test1_xls_A4_MacroSheet = test1_xls.getSheet("A4_マクロ");

                test1_xlsm = WorkbookFactory
                                .create(PoiUtilTest1_possibleTypes.class.getResource("Test1.xlsm").openStream());
                test1_xlsm_A1_Worksheet = test1_xlsm.getSheet("A1_ワークシート");
                test1_xlsm_A2_ChartSheet = test1_xlsm.getSheet("A2_グラフ");
                test1_xlsm_A3_DialogSheet = test1_xlsm.getSheet("A3_ダイアログ");
                test1_xlsm_A4_MacroSheet = test1_xlsm.getSheet("A4_マクロ");
        }

        @AfterAll
        static void afterAll() throws IOException {
                test1_xls.close();
                test1_xlsm.close();
        }

        // [instance members] ******************************************************

        @Test
        void testPossibleTypes_エラー系() {
                assertThrows(
                                NullPointerException.class,
                                () -> PoiUtil.possibleTypes(null));
        }

        @Test
        void testPossibleTypes_xls形式() {
                // FIXME: [No.01 シート識別不正 - usermodel] 「1_ワークシート」「2_グラフ」「4_マクロ」を見分ける術が分からない。
                assertEquals(
                                EnumSet.allOf(SheetType.class),
                                PoiUtil.possibleTypes(test1_xls_A1_Worksheet));

                // FIXME: [No.01 シート識別不正 - usermodel] 「1_ワークシート」「2_グラフ」「4_マクロ」を見分ける術が分からない。
                assertEquals(
                                EnumSet.allOf(SheetType.class),
                                PoiUtil.possibleTypes(test1_xls_A2_ChartSheet));

                // FIXME: [No.01 シート識別不正 - usermodel] 「3_ダイアログ」が正しく識別されない。
                assertEquals(
                                EnumSet.allOf(SheetType.class),
                                PoiUtil.possibleTypes(test1_xls_A3_DialogSheet));

                // FIXME: [No.01 シート識別不正 - usermodel] 「1_ワークシート」「2_グラフ」「4_マクロ」を見分ける術が分からない。
                assertEquals(
                                EnumSet.allOf(SheetType.class),
                                PoiUtil.possibleTypes(test1_xls_A4_MacroSheet));
        }

        @Test
        void testPossibleTypes_xlsm形式() {
                // FIXME: [No.01 シート識別不正 - usermodel] 「1_ワークシート」と「4_マクロ」を見分ける術が分からない。
                assertEquals(
                                EnumSet.of(SheetType.WORKSHEET, SheetType.MACRO_SHEET),
                                PoiUtil.possibleTypes(test1_xlsm_A1_Worksheet));

                assertEquals(
                                EnumSet.of(SheetType.CHART_SHEET),
                                PoiUtil.possibleTypes(test1_xlsm_A2_ChartSheet));

                // FIXME: [No.01 シート識別不正 - usermodel] どういう訳か .xlsm
                // 形式のExcelブックから「3_ダイアログ」を読み込めない。
                // つまり test1_xlsm_A3_DialogSheet == null。なのでテストできない。
                // どうしようもないのかしら？？
                // assertEquals(
                // EnumSet.of(SheetType.WORKSHEET, SheetType.CHART_SHEET,
                // SheetType.MACRO_SHEET),
                // PoiUtil.possibleTypes(test1_xlsm_A3_DialogSheet));
                assertNull(test1_xlsm_A3_DialogSheet);

                // FIXME: [No.01 シート識別不正 - usermodel] どういう訳か .xlsm 形式のExcelブックから「4_マクロ」を読み込めない。
                // つまり test1_xlsm_A4_MacroSheet == null。なのでテストできない。
                // どうしようもないのかしら？？
                // assertEquals(
                // EnumSet.of(SheetType.WORKSHEET, SheetType.CHART_SHEET,
                // SheetType.MACRO_SHEET),
                // PoiUtil.possibleTypes(test1_xlsm_A4_MacroSheet));
                assertNull(test1_xlsm_A4_MacroSheet);
        }
}
