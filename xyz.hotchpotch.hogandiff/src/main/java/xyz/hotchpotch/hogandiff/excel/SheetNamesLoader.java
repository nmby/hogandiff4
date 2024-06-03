package xyz.hotchpotch.hogandiff.excel;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.common.CombinedSheetNamesLoader;
import xyz.hotchpotch.hogandiff.excel.poi.eventmodel.HSSFSheetNamesLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.SheetNamesLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.sax.XSSFSheetNamesLoaderWithSax;

/**
 * Excelブックからシート名の一覧を抽出するローダーを表します。<br>
 * これは、{@link #loadSheetNames(BookOpenInfo)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface SheetNamesLoader {
    
    // [static members] ********************************************************
    
    /**
     * Excelブックからシート名の一覧を抽出するローダーを返します。<br>
     * 
     * @param bookOpenInfo Excelブックの情報
     * @return Excelブックからシート名の一覧を抽出するローダー
     * @throws NullPointerException
     *              {@code bookOpenInfo} が {@code null} の場合
     * @throws UnsupportedOperationException
     *              {@code bookOpenInfo} がサポート対象外の形式の場合
     */
    public static SheetNamesLoader of(
            BookOpenInfo bookOpenInfo) {
        
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        
        Set<SheetType> targetSheetTypes = EnumSet.of(SheetType.WORKSHEET);
        
        return switch (bookOpenInfo.bookType()) {
            case XLS -> CombinedSheetNamesLoader.of(List.of(
                    () -> HSSFSheetNamesLoaderWithPoiEventApi.of(targetSheetTypes),
                    () -> SheetNamesLoaderWithPoiUserApi.of(targetSheetTypes)));
        
            case XLSX, XLSM -> CombinedSheetNamesLoader.of(List.of(
                    () -> XSSFSheetNamesLoaderWithSax.of(targetSheetTypes),
                    () -> SheetNamesLoaderWithPoiUserApi.of(targetSheetTypes)));
        
            // FIXME: [No.2 .xlsbのサポート]
            case XLSB -> throw new UnsupportedOperationException("unsupported book type: " + bookOpenInfo.bookType());
            default -> throw new AssertionError("unknown book type: " + bookOpenInfo.bookType());
        };
    }
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたExcelブックに含まれるシート名の一覧を返します。<br>
     * 
     * @param bookOpenInfo Excelブックオープン情報
     * @return Excelブック情報
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    BookInfo loadSheetNames(BookOpenInfo bookOpenInfo) throws ExcelHandlingException;
}
