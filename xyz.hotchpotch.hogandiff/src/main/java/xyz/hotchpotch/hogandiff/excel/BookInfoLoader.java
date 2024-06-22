package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.common.CombinedBookInfoLoader;
import xyz.hotchpotch.hogandiff.excel.poi.eventmodel.HSSFBookInfoLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.BookInfoLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.sax.XSSFBookInfoLoaderWithSax;

/**
 * Excelブック情報を抽出するローダーを表します。<br>
 * これは、{@link #loadBookInfo(Path, String)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface BookInfoLoader {
    
    // [static members] ********************************************************
    
    /**
     * Excelブック情報を抽出するローダーを返します。<br>
     * 
     * @param bookPath Excepブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @return Excelブックからシート名の一覧を抽出するローダー
     * @throws NullPointerException {@code bookPath} が {@code null} の場合
     * @throws UnsupportedOperationException {@code bookPath} がサポート対象外の形式の場合
     */
    public static BookInfoLoader of(
            Path bookPath,
            String readPassword) {
        
        Objects.requireNonNull(bookPath, "bookPath");
        // readPassword may be null.
        
        Set<SheetType> targetSheetTypes = EnumSet.of(SheetType.WORKSHEET);
        
        return switch (BookType.of(bookPath)) {
            case XLS -> CombinedBookInfoLoader.of(List.of(
                    () -> HSSFBookInfoLoaderWithPoiEventApi.of(targetSheetTypes),
                    () -> BookInfoLoaderWithPoiUserApi.of(targetSheetTypes)));
        
            case XLSX, XLSM -> CombinedBookInfoLoader.of(List.of(
                    () -> XSSFBookInfoLoaderWithSax.of(targetSheetTypes),
                    () -> BookInfoLoaderWithPoiUserApi.of(targetSheetTypes)));
        
            // FIXME: [No.2 .xlsbのサポート]
            case XLSB -> throw new UnsupportedOperationException("unsupported book type: " + BookType.XLSB);
            default -> throw new AssertionError("unknown book type: " + BookType.of(bookPath));
        };
    }
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたパスのExcelブック情報を抽出して返します。<br>
     * 
     * @param bookPath Excepブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @return Excelブック情報
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    BookInfo loadBookInfo(
            Path bookPath,
            String readPassword)
            throws ExcelHandlingException;
}
