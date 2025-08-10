package xyz.hotchpotch.hogandiff.logic.models;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.logic.SheetType;

class SheetTypeTest {

    // [static members] ********************************************************

    // [instance members] ******************************************************

    @Test
    void testValues() {
        // WORKSHEET, CHART_SHEET, DIALOG_SHEET, MACRO_SHEET の 4 つであることを確認する。
        // 定義順は任意であるべき（当てにすべきでない）ため、テストしない。
        assertEquals(4, SheetType.values().length);
    }

    @Test
    void testValueOf() {
        // 正常系
        assertSame(SheetType.WORKSHEET, SheetType.valueOf("WORKSHEET"));
        assertSame(SheetType.CHART_SHEET, SheetType.valueOf("CHART_SHEET"));
        assertSame(SheetType.DIALOG_SHEET, SheetType.valueOf("DIALOG_SHEET"));
        assertSame(SheetType.MACRO_SHEET, SheetType.valueOf("MACRO_SHEET"));

        // 異常系
        assertThrows(
                NullPointerException.class,
                () -> SheetType.valueOf(null));
        assertThrows(
                IllegalArgumentException.class,
                () -> SheetType.valueOf("DUMMY"));
    }

    @Test
    void testDescription() {
        assertEquals("ワークシート", SheetType.WORKSHEET.description());
        assertEquals("グラフシート", SheetType.CHART_SHEET.description());
        assertEquals("MS Excel 5.0 ダイアログシート", SheetType.DIALOG_SHEET.description());
        assertEquals("Excel 4.0 マクロシート", SheetType.MACRO_SHEET.description());
    }
}
