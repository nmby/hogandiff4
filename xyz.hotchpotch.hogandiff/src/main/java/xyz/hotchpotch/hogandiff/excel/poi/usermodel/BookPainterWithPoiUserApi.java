package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import java.awt.Color;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellAddress;

import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SResult.Piece;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.common.SheetHandler;

/**
 * Apache POI のユーザーモデル API を利用して
 * .xlsx/.xlsm/.xls 形式のExcelブックのワークシートに着色を行う
 * {@link BookPainter} の実装です。<br>
 * 
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM, BookType.XLS })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class BookPainterWithPoiUserApi implements BookPainter {
    
    // [static members] ********************************************************
    
    /**
     * 新しいペインターを構成します。<br>
     * 
     * @param redundantColor 余剰行・余剰列に着ける色のインデックス値
     * @param diffColor 差分セルに着ける色のインデックス値
     * @param redundantCommentColor 余剰セルコメントに着ける色
     * @param diffCommentColor 差分セルコメントに着ける色
     * @param redundantSheetColor 余剰シートの見出しに着ける色
     * @param diffSheetColor 差分シートの見出しに着ける色
     * @param sameSheetColor 差分が無いシートの見出しに着ける色
     * @return 新たなペインター
     * @throws NullPointerException
     *          {@code redundantCommentColor}, {@code diffCommentColor},
     *          {@code redundantSheetColor}, {@code diffSheetColor}, {@code sameSheetColor}
     *          のいずれかが {@code null} の場合
     */
    public static BookPainter of(
            short redundantColor,
            short diffColor,
            Color redundantCommentColor,
            Color diffCommentColor,
            Color redundantSheetColor,
            Color diffSheetColor,
            Color sameSheetColor) {
        
        Objects.requireNonNull(redundantCommentColor, "redundantCommentColor");
        Objects.requireNonNull(diffCommentColor, "diffCommentColor");
        Objects.requireNonNull(redundantSheetColor, "redundantSheetColor");
        Objects.requireNonNull(diffSheetColor, "diffSheetColor");
        Objects.requireNonNull(sameSheetColor, "sameSheetColor");
        
        return new BookPainterWithPoiUserApi(
                redundantColor,
                diffColor,
                redundantCommentColor,
                diffCommentColor,
                redundantSheetColor,
                diffSheetColor,
                sameSheetColor);
    }
    
    // [instance members] ******************************************************
    
    private final short redundantColor;
    private final short diffColor;
    private final Color redundantCommentColor;
    private final Color diffCommentColor;
    private final Color redundantSheetColor;
    private final Color diffSheetColor;
    private final Color sameSheetColor;
    
    private BookPainterWithPoiUserApi(
            short redundantColor,
            short diffColor,
            Color redundantCommentColor,
            Color diffCommentColor,
            Color redundantSheetColor,
            Color diffSheetColor,
            Color sameSheetColor) {
        
        assert redundantCommentColor != null;
        assert diffCommentColor != null;
        assert redundantSheetColor != null;
        assert diffSheetColor != null;
        assert sameSheetColor != null;
        
        this.redundantColor = redundantColor;
        this.diffColor = diffColor;
        this.redundantCommentColor = redundantCommentColor;
        this.diffCommentColor = diffCommentColor;
        this.redundantSheetColor = redundantSheetColor;
        this.diffSheetColor = diffSheetColor;
        this.sameSheetColor = sameSheetColor;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code srcBookPath}, {@code dstBookPath}, {@code diffs}
     *              のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code srcBookPath} がサポート対象外の形式もしくは不明な形式の場合
     * @throws IllegalArgumentException
     *              {@code srcBookPath} と {@code dstBookPath} が同じパスの場合
     * @throws IllegalArgumentException
     *              {@code srcBookPath} と {@code dstBookPath} の形式が異なる場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックが見つからないとか、ファイル内容がおかしく予期せぬ実行時例外が発生したとか。
    @Override
    public void paintAndSave(
            Path srcBookPath,
            Path dstBookPath,
            Map<String, Optional<Piece>> diffs)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(srcBookPath, "srcBookPath");
        Objects.requireNonNull(dstBookPath, "dstBookPath");
        Objects.requireNonNull(diffs, "diffs");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(srcBookPath));
        if (srcBookPath.equals(dstBookPath)) {
            throw new IllegalArgumentException(
                    "異なるパスを指定する必要があります：%s -> %s".formatted(srcBookPath, dstBookPath));
        }
        if (BookType.of(srcBookPath) != BookType.of(dstBookPath)) {
            throw new IllegalArgumentException(
                    "拡張子が異なります：%s -> %s".formatted(srcBookPath, dstBookPath));
        }
        
        // 1. 目的のブックをコピーする。
        try {
            Files.copy(srcBookPath, dstBookPath);
            dstBookPath.toFile().setReadable(true, false);
            dstBookPath.toFile().setWritable(true, false);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "Excelファイルのコピーに失敗しました：%s -> %s".formatted(srcBookPath, dstBookPath),
                    e);
        }
        
        // 2. コピーしたファイルをExcelブックとしてロードする。
        try (InputStream is = Files.newInputStream(dstBookPath);
                Workbook book = WorkbookFactory.create(is)) {
            
            // 例外が発生した場合もその部分だけをスキップして処理継続した方が
            // ユーザーにとっては有益であると考え、小刻みに try-catch で囲うことにする。
            
            try {
                // 3. まず、全ての色をクリアする。
                PoiUtil.clearAllColors(book);
            } catch (RuntimeException e) {
                e.printStackTrace();
                // nop
            }
            
            // 4. 差分個所に色を付ける。
            diffs.forEach((sheetName, piece) -> {
                try {
                    Sheet sheet = book.getSheet(sheetName);
                    
                    if (piece.isPresent()) {
                        Piece p = piece.get();
                        
                        PoiUtil.paintRows(sheet, p.redundantRows(), redundantColor);
                        PoiUtil.paintColumns(sheet, p.redundantColumns(), redundantColor);
                        
                        Set<CellAddress> diffContents = p.diffCellContents().stream()
                                .map(c -> new CellAddress(c.row(), c.column()))
                                .collect(Collectors.toSet());
                        PoiUtil.paintCells(sheet, diffContents, diffColor);
                        
                        Set<CellAddress> diffComments = p.diffCellComments().stream()
                                .map(c -> new CellAddress(c.row(), c.column()))
                                .collect(Collectors.toSet());
                        PoiUtil.paintComments(sheet, diffComments, diffCommentColor);
                        
                        Set<CellAddress> redundantComments = p.redundantCellComments().stream()
                                .map(c -> new CellAddress(c.row(), c.column()))
                                .collect(Collectors.toSet());
                        PoiUtil.paintComments(sheet, redundantComments, redundantCommentColor);
                        
                        PoiUtil.paintSheetTab(sheet, p.hasDiff() ? diffSheetColor : sameSheetColor);
                        
                    } else {
                        PoiUtil.paintSheetTab(sheet, redundantSheetColor);
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    // nop
                }
            });
            
            // 5. Excelブックを上書き保存する。
            try (OutputStream os = Files.newOutputStream(dstBookPath)) {
                book.write(os);
            }
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "Excelブックの着色と保存に失敗しました：" + dstBookPath, e);
        }
    }
}
