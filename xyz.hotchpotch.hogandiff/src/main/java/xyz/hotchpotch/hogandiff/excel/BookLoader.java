package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.common.CombinedBookLoader;
import xyz.hotchpotch.hogandiff.excel.poi.eventmodel.HSSFBookLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.BookLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.sax.XSSFBookLoaderWithSax;

/**
 * Excelブック情報を抽出するローダーを表します。<br>
 * これは、{@link #loadBookInfo(Path, String)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface BookLoader {
    
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
    public static BookLoader of(
            Path bookPath,
            String readPassword) {
        
        Objects.requireNonNull(bookPath);
        // readPassword may be null.
        
        Set<SheetType> targetSheetTypes = EnumSet.of(SheetType.WORKSHEET);
        
        return switch (BookType.of(bookPath)) {
            case XLS -> CombinedBookLoader.of(List.of(
                    () -> HSSFBookLoaderWithPoiEventApi.of(targetSheetTypes),
                    () -> BookLoaderWithPoiUserApi.of(targetSheetTypes)));
        
            case XLSX, XLSM -> CombinedBookLoader.of(List.of(
                    () -> XSSFBookLoaderWithSax.of(targetSheetTypes),
                    () -> BookLoaderWithPoiUserApi.of(targetSheetTypes)));
        
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
     */
    BookInfo loadBookInfo(
            Path bookPath,
            String readPassword);
}
