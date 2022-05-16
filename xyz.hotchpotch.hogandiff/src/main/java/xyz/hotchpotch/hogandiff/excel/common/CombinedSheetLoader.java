package xyz.hotchpotch.hogandiff.excel.common;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.util.function.UnsafeSupplier;

/**
 * 処理が成功するまで複数のローダーで順に処理を行う {@link SheetLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler
@SheetHandler
public class CombinedSheetLoader implements SheetLoader {
    
    // [static members] ********************************************************
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param suppliers このローダーを構成するローダーたちのサプライヤ
     * @return 新しいローダー
     * @throws NullPointerException {@code suppliers} が {@code null} の場合
     * @throws IllegalArgumentException {@code suppliers} が空の場合
     */
    public static SheetLoader of(List<UnsafeSupplier<SheetLoader>> suppliers) {
        Objects.requireNonNull(suppliers, "suppliers");
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("param \"suppliers\" is empty.");
        }
        
        return new CombinedSheetLoader(suppliers);
    }
    
    // [instance members] ******************************************************
    
    private final List<UnsafeSupplier<SheetLoader>> suppliers;
    
    private CombinedSheetLoader(List<UnsafeSupplier<SheetLoader>> suppliers) {
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
     *              {@code bookInfo}, {@code sheetName} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookInfo} がサポート対象外の形式もしくは不明な形式の場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックやシートが見つからないとか、シート種類がサポート対象外とか。
    @Override
    public Set<CellData> loadCells(BookInfo bookInfo, String sheetName)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookInfo, "bookInfo");
        Objects.requireNonNull(sheetName, "sheetName");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), bookInfo.bookType());
        
        ExcelHandlingException failed = new ExcelHandlingException(
                "処理に失敗しました：%s - %s".formatted(bookInfo.bookPath(), sheetName));
        
        Iterator<UnsafeSupplier<SheetLoader>> itr = suppliers.iterator();
        while (itr.hasNext()) {
            try {
                SheetLoader loader = itr.next().get();
                return loader.loadCells(bookInfo, sheetName);
            } catch (Exception e) {
                e.printStackTrace();
                failed.addSuppressed(e);
            }
        }
        throw failed;
    }
}
