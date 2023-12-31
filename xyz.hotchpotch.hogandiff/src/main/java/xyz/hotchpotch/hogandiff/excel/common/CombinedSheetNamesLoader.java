package xyz.hotchpotch.hogandiff.excel.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.PasswordHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.util.function.UnsafeSupplier;

/**
 * 処理が成功するまで複数のローダーで順に処理を行う {@link SheetNamesLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler
public class CombinedSheetNamesLoader implements SheetNamesLoader {
    
    // [static members] ********************************************************
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param suppliers このローダーを構成するローダーたちのサプライヤ
     * @return 新しいローダー
     * @throws NullPointerException {@code suppliers} が {@code null} の場合
     * @throws IllegalArgumentException {@code suppliers} が空の場合
     */
    public static SheetNamesLoader of(List<UnsafeSupplier<SheetNamesLoader>> suppliers) {
        Objects.requireNonNull(suppliers);
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("param \"suppliers\" is empty.");
        }
        
        return new CombinedSheetNamesLoader(suppliers);
    }
    
    // [instance members] ******************************************************
    
    private final List<UnsafeSupplier<SheetNamesLoader>> suppliers;
    
    private CombinedSheetNamesLoader(List<UnsafeSupplier<SheetNamesLoader>> suppliers) {
        assert suppliers != null;
        
        this.suppliers = List.copyOf(suppliers);
    }
    
    /**
     * {@inheritDoc}
     * <br>
     * この実装は、構成時に指定されたローダーを使って処理を行います。<br>
     * 一つ目のローダーで処理を行い、正常に終了したらその結果を返します。
     * 失敗したら二つ目のローダーで処理を行い、正常に終了したらその結果を返します。
     * 以下同様に処理を行い、
     * 全てのローダーで処理が失敗したら例外をスローします。<br>
     * 
     * @throws NullPointerException
     *              {@code bookOpenInfo} が {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookOpenInfo} がサポート対象外の形式の場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックが見つからないとか、ファイル内容がおかしく予期せぬ実行時例外が発生したとか。
    @Override
    public BookInfo loadSheetNames(
            BookOpenInfo bookOpenInfo)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), bookOpenInfo.bookType());
        
        List<Exception> suppressed = new ArrayList<>();
        Iterator<UnsafeSupplier<SheetNamesLoader>> itr = suppliers.iterator();
        boolean passwordIssue = false;
        
        while (itr.hasNext()) {
            try {
                SheetNamesLoader loader = itr.next().get();
                return loader.loadSheetNames(bookOpenInfo);
                
            } catch (PasswordHandlingException e) {
                passwordIssue = true;
                e.printStackTrace();
                suppressed.add(e);
                
            } catch (Exception e) {
                e.printStackTrace();
                suppressed.add(e);
            }
        }
        
        ExcelHandlingException failed = passwordIssue
                ? new PasswordHandlingException("processing failed : %s".formatted(bookOpenInfo))
                : new ExcelHandlingException("processing failed : %s".formatted(bookOpenInfo));
        suppressed.forEach(failed::addSuppressed);
        throw failed;
    }
}
