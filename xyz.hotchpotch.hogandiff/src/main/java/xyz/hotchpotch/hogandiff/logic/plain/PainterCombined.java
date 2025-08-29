package xyz.hotchpotch.hogandiff.logic.plain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.logic.BookHandler;
import xyz.hotchpotch.hogandiff.logic.BookType;
import xyz.hotchpotch.hogandiff.logic.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.logic.Painter;
import xyz.hotchpotch.hogandiff.logic.ResultOfSheets.Piece;
import xyz.hotchpotch.hogandiff.logic.SheetHandler;
import xyz.hotchpotch.hogandiff.util.function.UnsafeSupplier;

/**
 * 処理が成功するまで複数のペインターで順に処理を行う {@link Painter} の実装です。<br>
 * 
 * @author nmby
 */
@BookHandler
@SheetHandler
public class PainterCombined implements Painter {
    
    // [static members] ********************************************************
    
    /**
     * 新しいペインターを構成します。<br>
     * 
     * @param suppliers このペインターを構成するペインターたちのサプライヤ
     * @return 新しいペインター
     * @throws NullPointerException     {@code suppliers} が {@code null} の場合
     * @throws IllegalArgumentException {@code suppliers} が空の場合
     */
    public static Painter of(List<UnsafeSupplier<Painter, ExcelHandlingException>> suppliers) {
        Objects.requireNonNull(suppliers);
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("param \"suppliers\" is empty.");
        }
        
        return new PainterCombined(suppliers);
    }
    
    // [instance members] ******************************************************
    
    private final List<UnsafeSupplier<Painter, ExcelHandlingException>> suppliers;
    
    private PainterCombined(List<UnsafeSupplier<Painter, ExcelHandlingException>> suppliers) {
        assert suppliers != null;
        
        this.suppliers = List.copyOf(suppliers);
    }
    
    /**
     * {@inheritDoc}
     * <br>
     * この実装は、構成時に指定されたペインターを使って処理を行います。<br>
     * 一つ目のペインターで処理を行い、正常に終了したらそのまま終了します。
     * 失敗したら二つ目のペインターで処理を行い、正常に終了したらそのまま終了します。
     * 以下同様に処理を行い、
     * 全てのペインターで処理が失敗したら例外をスローします。<br>
     * 
     * @throws NullPointerException
     *                                  {@code srcBookPath}, {@code dstBookPath},
     *                                  {@code diffs}
     *                                  のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *                                  {@code srcBookPath} がサポート対象外の形式の場合
     * @throws IllegalArgumentException
     *                                  {@code srcBookPath} と {@code dstBookPath}
     *                                  が同じパスの場合
     * @throws IllegalArgumentException
     *                                  {@code srcBookPath} と {@code dstBookPath}
     *                                  の形式が異なる場合
     * @throws ExcelHandlingException
     *                                  処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    // 例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    // 例えば、ブックが見つからないとか、ファイル内容がおかしく予期せぬ実行時例外が発生したとか。
    @Override
    public void paintAndSave(
            Path srcBookPath,
            Path dstBookPath,
            String readPassword,
            Map<String, Optional<Piece>> diffs)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(srcBookPath);
        Objects.requireNonNull(dstBookPath);
        // readPassword may be null.
        Objects.requireNonNull(diffs);
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(srcBookPath));
        if (Objects.equals(srcBookPath, dstBookPath)) {
            throw new IllegalArgumentException(
                    "different book paths are required : %s -> %s".formatted(srcBookPath, dstBookPath));
        }
        if (BookType.of(srcBookPath) != BookType.of(dstBookPath)) {
            throw new IllegalArgumentException(
                    "extentions must be the same : %s -> %s".formatted(srcBookPath, dstBookPath));
        }
        
        ExcelHandlingException failed = new ExcelHandlingException(
                "processiong failed : %s -> %s".formatted(srcBookPath, dstBookPath));
        
        Iterator<UnsafeSupplier<Painter, ExcelHandlingException>> itr = suppliers.iterator();
        while (itr.hasNext()) {
            try {
                Painter painter = itr.next().get();
                painter.paintAndSave(srcBookPath, dstBookPath, readPassword, diffs);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                failed.addSuppressed(e);
            }
            
            // painterの処理に失敗し、かつ後続painterがある場合は、
            // 保存先ファイルを削除しておく。
            if (itr.hasNext()) {
                try {
                    Files.deleteIfExists(dstBookPath);
                } catch (IOException e) {
                    failed.addSuppressed(e);
                }
            }
        }
        throw failed;
    }
}
