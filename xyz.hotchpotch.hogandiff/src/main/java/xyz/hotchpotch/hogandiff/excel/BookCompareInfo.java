package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * Excelブック同士を比較するときのシートの組み合わせ情報を保持する不変クラスです。<br>
 * 
 * @author nmby
 */
public class BookCompareInfo {
    
    // [static members] ********************************************************
    
    /**
     * 
     * @param bookInfoPair 比較対象Excelブックの情報
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @return 新たなインスタンス
     */
    public static BookCompareInfo of(
            Pair<BookInfo> bookInfoPair,
            Matcher<String> sheetNamesMatcher) {
        
        Objects.requireNonNull(bookInfoPair);
        Objects.requireNonNull(sheetNamesMatcher);
        
        return new BookCompareInfo(
                bookInfoPair,
                sheetNamesMatcher.makeItemPairs(
                        bookInfoPair.a().sheetNames(),
                        bookInfoPair.b().sheetNames()));
    }
    
    // [instance members] ******************************************************
    
    private Pair<BookInfo> bookInfoPair;
    private List<Pair<String>> sheetNamePairs;
    
    private BookCompareInfo(
            Pair<BookInfo> bookInfoPair,
            List<Pair<String>> sheetNamePairs) {
        
        assert bookInfoPair != null;
        assert sheetNamePairs != null;
        
        this.bookInfoPair = bookInfoPair;
        this.sheetNamePairs = List.copyOf(sheetNamePairs);
    }
    
    /**
     * 比較対象Excelブックの情報のペアを返します。<br>
     * 
     * @return 比較対象Excelブックの情報のペア
     */
    public Pair<BookInfo> bookInfoPair() {
        return bookInfoPair;
    }
    
    /**
     * シート名の組み合わせ情報を返します。<br>
     * 
     * @return シート名の組み合わせ情報
     */
    public List<Pair<String>> sheetNamePairs() {
        return sheetNamePairs;
    }
}
