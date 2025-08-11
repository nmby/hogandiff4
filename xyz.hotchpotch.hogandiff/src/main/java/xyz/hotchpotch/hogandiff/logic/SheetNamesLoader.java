package xyz.hotchpotch.hogandiff.logic;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.logic.plain.SheetNamesLoaderCombined;
import xyz.hotchpotch.hogandiff.logic.poi.SheetNamesLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.logic.poi.SheetNamesLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.logic.sax.SheetNamesLoaderWithSax;

/**
 * Excelブック情報を抽出するローダーを表します。<br>
 * これは、{@link #loadBookInfo(Path, String)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface SheetNamesLoader {
    
    // [static members] ********************************************************
    
    /**
     * Excelブック情報を抽出するローダーを返します。<br>
     * 
     * @param bookPath Excepブックのパス
     * @return Excelブックからシート名の一覧を抽出するローダー
     * @throws NullPointerException          {@code bookPath} が {@code null} の場合
     * @throws UnsupportedOperationException {@code bookPath} がサポート対象外の形式の場合
     */
    public static SheetNamesLoader of(Path bookPath) {
        Objects.requireNonNull(bookPath);
        
        Set<SheetType> targetSheetTypes = EnumSet.of(SheetType.WORKSHEET);
        
        return switch (BookType.of(bookPath)) {
        case XLS -> SheetNamesLoaderCombined.of(List.of(
                () -> SheetNamesLoaderWithPoiEventApi.of(targetSheetTypes),
                () -> SheetNamesLoaderWithPoiUserApi.of(targetSheetTypes)));
    
        case XLSX, XLSM -> SheetNamesLoaderCombined.of(List.of(
                () -> SheetNamesLoaderWithSax.of(targetSheetTypes),
                () -> SheetNamesLoaderWithPoiUserApi.of(targetSheetTypes)));
    
        // FIXME: [No.02 .xlsbのサポート]
        case XLSB -> throw new UnsupportedOperationException("unsupported book type: " + BookType.XLSB);
        default -> throw new AssertionError("unknown book type: " + BookType.of(bookPath));
        };
    }
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたパスのExcelブック情報を抽出して返します。<br>
     * 
     * @param bookPath     Excepブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @return Excelブック情報
     */
    BookInfo loadBookInfo(
            Path bookPath,
            String readPassword);
}
