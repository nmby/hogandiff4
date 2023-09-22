package xyz.hotchpotch.hogandiff.excel.common;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetResult.Piece;
import xyz.hotchpotch.hogandiff.util.function.UnsafeSupplier;

/**
 * 処理が成功するまで複数のペインターで順に処理を行う {@link BookPainter} の実装です。<br>
 * 
 * @author nmby
 */
@BookHandler
@SheetHandler
public class CombinedBookPainter implements BookPainter {
    
    // [static members] ********************************************************
    
    /**
     * 新しいペインターを構成します。<br>
     * 
     * @param suppliers このペインターを構成するペインターたちのサプライヤ
     * @return 新しいペインター
     * @throws NullPointerException {@code suppliers} が {@code null} の場合
     * @throws IllegalArgumentException {@code suppliers} が空の場合
     */
    public static BookPainter of(List<UnsafeSupplier<BookPainter>> suppliers) {
        Objects.requireNonNull(suppliers);
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("param \"suppliers\" is empty.");
        }
        
        return new CombinedBookPainter(suppliers);
    }
    
    // [instance members] ******************************************************
    
    private final List<UnsafeSupplier<BookPainter>> suppliers;
    
    private CombinedBookPainter(List<UnsafeSupplier<BookPainter>> suppliers) {
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
     *              {@code srcBookOpenInfo}, {@code dstBookOpenInfo}, {@code diffs}
     *              のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code srcBookOpenInfo} がサポート対象外の形式の場合
     * @throws IllegalArgumentException
     *              {@code srcBookOpenInfo} と {@code dstBookOpenInfo} が同じパスの場合
     * @throws IllegalArgumentException
     *              {@code srcBookOpenInfo} と {@code dstBookOpenInfo} の形式が異なる場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックが見つからないとか、ファイル内容がおかしく予期せぬ実行時例外が発生したとか。
    @Override
    public void paintAndSave(
            BookOpenInfo srcBookOpenInfo,
            BookOpenInfo dstBookOpenInfo,
            Map<String, Optional<Piece>> diffs)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(srcBookOpenInfo, "srcBookOpenInfo");
        Objects.requireNonNull(dstBookOpenInfo, "dstBookOpenInfo");
        Objects.requireNonNull(diffs, "diffs");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), srcBookOpenInfo.bookType());
        if (Objects.equals(srcBookOpenInfo.bookPath(), dstBookOpenInfo.bookPath())) {
            throw new IllegalArgumentException(
                    "different book paths are required : %s -> %s".formatted(srcBookOpenInfo, dstBookOpenInfo));
        }
        if (srcBookOpenInfo.bookType() != dstBookOpenInfo.bookType()) {
            throw new IllegalArgumentException(
                    "extentions must be the same : %s -> %s".formatted(srcBookOpenInfo, dstBookOpenInfo));
        }
        
        ExcelHandlingException failed = new ExcelHandlingException(
                "processiong failed : %s -> %s".formatted(srcBookOpenInfo, dstBookOpenInfo));
        
        Iterator<UnsafeSupplier<BookPainter>> itr = suppliers.iterator();
        while (itr.hasNext()) {
            try {
                BookPainter painter = itr.next().get();
                painter.paintAndSave(srcBookOpenInfo, dstBookOpenInfo, diffs);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                failed.addSuppressed(e);
            }
            
            // painterの処理に失敗し、かつ後続painterがある場合は、
            // 保存先ファイルを削除しておく。
            if (itr.hasNext()) {
                try {
                    Files.deleteIfExists(dstBookOpenInfo.bookPath());
                } catch (IOException e) {
                    failed.addSuppressed(e);
                }
            }
        }
        throw failed;
    }
}
