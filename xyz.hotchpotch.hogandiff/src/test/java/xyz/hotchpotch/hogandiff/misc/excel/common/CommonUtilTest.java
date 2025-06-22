package xyz.hotchpotch.hogandiff.misc.excel.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.models.BookType;
import xyz.hotchpotch.hogandiff.models.SheetType;

@BookHandler(targetTypes = { BookType.XLSB, BookType.XLS, BookType.XLSB })
@SheetHandler(targetTypes = { SheetType.WORKSHEET, SheetType.CHART_SHEET })
class CommonUtilTest {

        // [static members] ********************************************************

        private static class BTestNone {
        }

        @BookHandler(targetTypes = { BookType.XLS, BookType.XLSX })
        private static class BTest2 {
        }

        @BookHandler
        private static class BTestAll {
        }

        private static class STestNone {
        }

        @SheetHandler(targetTypes = { SheetType.WORKSHEET, SheetType.MACRO_SHEET })
        private static class STest2 {
        }

        @SheetHandler
        private static class STestAll {
        }

        // [instance members] ******************************************************

        @Test
        void testIsSupportedBookType_異常系() {
                // nullパラメータ
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.isSupportedBookType(null, BookType.XLSX));
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.isSupportedBookType(BTest2.class, null));
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.isSupportedBookType(null, null));
                assertDoesNotThrow(
                                () -> CommonUtil.isSupportedBookType(BTest2.class, BookType.XLSX));

                // アノテーション無し
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CommonUtil.isSupportedBookType(BTestNone.class, BookType.XLSX));
        }

        @Test
        void testIsSupportedBookType_正常系() {
                assertTrue(CommonUtil.isSupportedBookType(BTest2.class, BookType.XLS));
                assertTrue(CommonUtil.isSupportedBookType(BTest2.class, BookType.XLSX));
                assertFalse(CommonUtil.isSupportedBookType(BTest2.class, BookType.XLSM));
                assertFalse(CommonUtil.isSupportedBookType(BTest2.class, BookType.XLSB));

                assertTrue(CommonUtil.isSupportedBookType(BTestAll.class, BookType.XLS));
                assertTrue(CommonUtil.isSupportedBookType(BTestAll.class, BookType.XLSX));
                assertTrue(CommonUtil.isSupportedBookType(BTestAll.class, BookType.XLSM));
                assertTrue(CommonUtil.isSupportedBookType(BTestAll.class, BookType.XLSB));
        }

        @Test
        void testIfNotSupportedBookTypeThenThrow_異常系() {
                // nullパラメータ
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(null, BookType.XLSX));
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTest2.class, null));
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(null, null));
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTest2.class, BookType.XLSX));

                // アノテーション無し
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTestNone.class, BookType.XLSX));
        }

        @Test
        void testIfNotSupportedBookTypeThenThrow_正常系() {
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTest2.class, BookType.XLS));
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTest2.class, BookType.XLSX));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTest2.class, BookType.XLSM));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTest2.class, BookType.XLSB));

                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTestAll.class, BookType.XLS));
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTestAll.class, BookType.XLSX));
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTestAll.class, BookType.XLSM));
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedBookTypeThenThrow(BTestAll.class, BookType.XLSB));
        }

        @Test
        void testIsSupportedSheetType_異常系() {
                // nullパラメータ
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.isSupportedSheetType(null, Set.of(SheetType.WORKSHEET)));
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.isSupportedSheetType(STest2.class, null));
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.isSupportedSheetType(null, null));
                assertDoesNotThrow(
                                () -> CommonUtil.isSupportedSheetType(STest2.class, Set.of(SheetType.WORKSHEET)));

                // アノテーション無し
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CommonUtil.isSupportedSheetType(STestNone.class, Set.of(SheetType.WORKSHEET)));
        }

        @Test
        void testIsSupportedSheetType_正常系1() {
                assertTrue(CommonUtil.isSupportedSheetType(STest2.class, Set.of(SheetType.WORKSHEET)));
                assertTrue(CommonUtil.isSupportedSheetType(STest2.class, Set.of(SheetType.MACRO_SHEET)));
                assertFalse(CommonUtil.isSupportedSheetType(STest2.class, Set.of(SheetType.CHART_SHEET)));
                assertFalse(CommonUtil.isSupportedSheetType(STest2.class, Set.of(SheetType.DIALOG_SHEET)));

                assertTrue(CommonUtil.isSupportedSheetType(STestAll.class, Set.of(SheetType.WORKSHEET)));
                assertTrue(CommonUtil.isSupportedSheetType(STestAll.class, Set.of(SheetType.MACRO_SHEET)));
                assertTrue(CommonUtil.isSupportedSheetType(STestAll.class, Set.of(SheetType.CHART_SHEET)));
                assertTrue(CommonUtil.isSupportedSheetType(STestAll.class, Set.of(SheetType.DIALOG_SHEET)));
        }

        @Test
        void testIsSupportedSheetType_正常系2() {
                assertFalse(CommonUtil.isSupportedSheetType(
                                STest2.class,
                                Set.of()));
                assertFalse(CommonUtil.isSupportedSheetType(
                                STest2.class,
                                Set.of(SheetType.CHART_SHEET, SheetType.DIALOG_SHEET)));

                assertTrue(CommonUtil.isSupportedSheetType(
                                STest2.class,
                                Set.of(SheetType.CHART_SHEET, SheetType.DIALOG_SHEET, SheetType.WORKSHEET)));
        }

        @Test
        void testIfNotSupportedSheetTypeThenThrow_異常系() {
                // nullパラメータ
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                null, Set.of(SheetType.WORKSHEET)));
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STest2.class, null));
                assertThrows(
                                NullPointerException.class,
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                null, null));
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STest2.class, Set.of(SheetType.WORKSHEET)));

                // アノテーション無し
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STestNone.class, Set.of(SheetType.WORKSHEET)));
        }

        @Test
        void testIfNotSupportedSheetTypeThenThrow_正常系1() {
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STest2.class, Set.of(SheetType.WORKSHEET)));
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STest2.class, Set.of(SheetType.MACRO_SHEET)));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STest2.class, Set.of(SheetType.CHART_SHEET)));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STest2.class, Set.of(SheetType.DIALOG_SHEET)));

                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STestAll.class, Set.of(SheetType.WORKSHEET)));
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STestAll.class, Set.of(SheetType.MACRO_SHEET)));
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STestAll.class, Set.of(SheetType.CHART_SHEET)));
                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STestAll.class, Set.of(SheetType.DIALOG_SHEET)));
        }

        @Test
        void testIfNotSupportedSheetTypeThenThrow_正常系2() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STest2.class,
                                                Set.of()));
                assertThrows(
                                IllegalArgumentException.class,
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STest2.class,
                                                Set.of(SheetType.CHART_SHEET, SheetType.DIALOG_SHEET)));

                assertDoesNotThrow(
                                () -> CommonUtil.ifNotSupportedSheetTypeThenThrow(
                                                STest2.class,
                                                Set.of(SheetType.CHART_SHEET, SheetType.DIALOG_SHEET,
                                                                SheetType.WORKSHEET)));
        }
}
