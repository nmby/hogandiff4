package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excelブック同士を比較するときのシートの組み合わせ情報を保持する不変クラスです。<br>
 * 
 * @author nmby
 */
public class BookCompareInfo {
    
    // [static members] ********************************************************
    
    /**
     * 与えられたマッチャーを使用して新たな {@link BookCompareInfo} インスタンスを生成します。<br>
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
        
        if (bookInfoPair.isPaired()) {
            List<Pair<String>> sheetNamePairs = sheetNamesMatcher.makeItemPairs(
                    bookInfoPair.a().sheetNames(),
                    bookInfoPair.b().sheetNames());
            return new BookCompareInfo(bookInfoPair, sheetNamePairs);
            
        } else if (bookInfoPair.hasA()) {
            List<Pair<String>> sheetNamePairs = bookInfoPair.a().sheetNames().stream()
                    .map(sheetName -> Pair.ofOnly(Side.A, sheetName))
                    .toList();
            return new BookCompareInfo(bookInfoPair, sheetNamePairs);
            
        } else if (bookInfoPair.hasB()) {
            List<Pair<String>> sheetNamePairs = bookInfoPair.b().sheetNames().stream()
                    .map(sheetName -> Pair.ofOnly(Side.B, sheetName))
                    .toList();
            return new BookCompareInfo(bookInfoPair, sheetNamePairs);
            
        } else {
            return new BookCompareInfo(bookInfoPair, List.of());
        }
    }
    
    // [instance members] ******************************************************
    
    private final Pair<BookInfo> bookInfoPair;
    private final List<Pair<String>> sheetNamePairs;
    
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
