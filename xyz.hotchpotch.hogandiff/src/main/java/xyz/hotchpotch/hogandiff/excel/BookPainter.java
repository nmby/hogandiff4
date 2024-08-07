package xyz.hotchpotch.hogandiff.excel;

import java.awt.Color;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.excel.SheetResult.Piece;
import xyz.hotchpotch.hogandiff.excel.common.CombinedBookPainter;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.BookPainterWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.stax.XSSFBookPainterWithStax;

/**
 * Excelブックの差分個所に色を付けて新しいファイルとして保存するペインターを表します。<br>
 * これは、{@link #paintAndSave(Path, Path, String, Map)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface BookPainter {
    
    // [static members] ********************************************************
    
    /**
     * Excelブックの差分個所に色を付けて新しいファイルとして保存する
     * ペインターを返します。<br>
     * 
     * @param bookPath Excepブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @param redundantColor 余剰行、余剰列に着ける色の色番号
     * @param diffColor 差分セルに着ける色の色番号
     * @param redundantCommentColor 余剰コメントに着ける色
     * @param diffCommentColor 差分コメントに着ける色
     * @param redundantCommentHex 余剰コメントに着ける色の16進表現
     * @param diffCommentHex 差分コメントに着ける色の16進表現
     * @param redundantSheetColor 余剰シートのタブに着ける色
     * @param diffSheetColor 差分シートのタブに着ける色
     * @param sameSheetColor 差分なしシートのタブに着ける色
     * @return Excelブックの差分個所に色を付けて保存するペインター
     * @throws NullPointerException {@code bookPath} が {@code null} の場合
     * @throws UnsupportedOperationException {@code bookPath} がサポート対象外の形式の場合
     */
    public static BookPainter of(
            Path bookPath,
            String readPassword,
            short redundantColor,
            short diffColor,
            Color redundantCommentColor,
            Color diffCommentColor,
            String redundantCommentHex,
            String diffCommentHex,
            Color redundantSheetColor,
            Color diffSheetColor,
            Color sameSheetColor) {
        
        Objects.requireNonNull(bookPath);
        
        return switch (BookType.of(bookPath)) {
            case XLS -> CombinedBookPainter.of(List.of(
                    // FIXME: [No.03 着色関連] 形式特化型ペインターも実装して追加する
                    () -> new BookPainterWithPoiUserApi(
                            redundantColor,
                            diffColor,
                            redundantCommentColor,
                            diffCommentColor,
                            redundantSheetColor,
                            diffSheetColor,
                            sameSheetColor)));
        
            case XLSX, XLSM -> CombinedBookPainter.of(List.of(
                    () -> new XSSFBookPainterWithStax(
                            redundantColor,
                            diffColor,
                            redundantCommentHex,
                            diffCommentHex,
                            redundantSheetColor,
                            diffSheetColor,
                            sameSheetColor),
                    () -> new BookPainterWithPoiUserApi(
                            redundantColor,
                            diffColor,
                            redundantCommentColor,
                            diffCommentColor,
                            redundantSheetColor,
                            diffSheetColor,
                            sameSheetColor)));
        
            // FIXME: [No.02 .xlsbのサポート]
            case XLSB -> throw new UnsupportedOperationException("unsupported book type: " + BookType.XLSB);
            default -> throw new AssertionError("unknown book type: " + BookType.of(bookPath));
        };
    }
    
    // [instance members] ******************************************************
    
    /**
     * 元のExcelブックの差分個所に色を付けたものを
     * 指定されたパスに保存します。<br>
     * 
     * @param srcBookPath 元のExcelブックのパス
     * @param dstBookPath 保存先Excelブックのパス
     * @param readPassword 双方のExcelブックの読み取りパスワード
     * @param diffs シート名とその差分個所のマップ
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    void paintAndSave(
            Path srcBookPath,
            Path dstBookPath,
            String readPassword,
            Map<String, Optional<Piece>> diffs)
            throws ExcelHandlingException;
}
