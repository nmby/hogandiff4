package xyz.hotchpotch.hogandiff.logic.plain;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import xyz.hotchpotch.hogandiff.logic.BookHandler;
import xyz.hotchpotch.hogandiff.logic.BookInfo;
import xyz.hotchpotch.hogandiff.logic.BookType;
import xyz.hotchpotch.hogandiff.logic.BookInfoLoader;

/**
 * 処理が成功するまで複数のローダーで順に処理を行う {@link BookInfoLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler
public class BookInfoLoaderCombined implements BookInfoLoader {
    
    // [static members] ********************************************************
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param suppliers このローダーを構成するローダーたちのサプライヤ
     * @return 新しいローダー
     * @throws NullPointerException     パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code suppliers} が空の場合
     */
    public static BookInfoLoader of(List<Supplier<BookInfoLoader>> suppliers) {
        Objects.requireNonNull(suppliers);
        if (suppliers.isEmpty()) {
            throw new IllegalArgumentException("param \"suppliers\" is empty.");
        }
        
        return new BookInfoLoaderCombined(suppliers);
    }
    
    // [instance members] ******************************************************
    
    private final List<Supplier<BookInfoLoader>> suppliers;
    
    private BookInfoLoaderCombined(List<Supplier<BookInfoLoader>> suppliers) {
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
     *                                  {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException
     *                                  {@code bookPath} がサポート対象外の形式の場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    // 例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・抽出処理中に発生したあらゆる例外は catch し、呼出元には必ず {@link BookInfo} オブジェクトを返却する。
    @Override
    public BookInfo loadBookInfo(
            Path bookPath,
            String readPassword) {
        
        Objects.requireNonNull(bookPath);
        // readPassword may be null.
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(bookPath));
        
        try {
            Iterator<Supplier<BookInfoLoader>> itr = suppliers.iterator();
            
            while (itr.hasNext()) {
                BookInfoLoader loader = itr.next().get();
                BookInfo bookInfo = loader.loadBookInfo(bookPath, readPassword);
                
                switch (bookInfo.status()) {
                case LOAD_COMPLETED:
                case NEEDS_PASSWORD:
                    return bookInfo;
                
                case LOAD_FAILED:
                    // continue
                }
            }
            return BookInfo.ofLoadFailed(bookPath);
            
        } catch (Exception e) {
            return BookInfo.ofLoadFailed(bookPath);
        }
    }
}
