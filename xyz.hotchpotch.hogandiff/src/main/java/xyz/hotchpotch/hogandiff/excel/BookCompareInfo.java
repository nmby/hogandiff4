package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excelブック比較情報を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public final class BookCompareInfo implements CompareInfo<BookInfo, String, Void> {
    
    // [static members] ********************************************************
    
    /**
     * 与えられたマッチャーを使用して新たな {@link BookCompareInfo} インスタンスを生成します。<br>
     * 
     * @param parentBookInfoPair 比較対象Excelブックの情報
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @throws NullPointerException パラメータが {@code null} の場合
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static BookCompareInfo calculate(
            Pair<BookInfo> parentBookInfoPair,
            Matcher<String> sheetNamesMatcher) {
        
        Objects.requireNonNull(parentBookInfoPair);
        Objects.requireNonNull(sheetNamesMatcher);
        
        if (parentBookInfoPair.isPaired()) {
            List<Pair<String>> sheetNamePairs = sheetNamesMatcher.makeItemPairs(
                    parentBookInfoPair.a().sheetNames(),
                    parentBookInfoPair.b().sheetNames());
            return new BookCompareInfo(parentBookInfoPair, sheetNamePairs);
            
        } else if (parentBookInfoPair.hasA()) {
            List<Pair<String>> sheetNamePairs = parentBookInfoPair.a().sheetNames().stream()
                    .map(sheetName -> Pair.ofOnly(Side.A, sheetName))
                    .toList();
            return new BookCompareInfo(parentBookInfoPair, sheetNamePairs);
            
        } else if (parentBookInfoPair.hasB()) {
            List<Pair<String>> sheetNamePairs = parentBookInfoPair.b().sheetNames().stream()
                    .map(sheetName -> Pair.ofOnly(Side.B, sheetName))
                    .toList();
            return new BookCompareInfo(parentBookInfoPair, sheetNamePairs);
            
        } else {
            return new BookCompareInfo(parentBookInfoPair, List.of());
        }
    }
    
    /**
     * 指定した内容で {@link BookCompareInfo} インスタンスを生成します。<br>
     * 
     * @param parentBookInfoPair Excelブック情報
     * @param childSheetNamePairs シート名の組み合わせ
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static BookCompareInfo of(
            Pair<BookInfo> parentBookInfoPair,
            List<Pair<String>> childSheetNamePairs) {
        
        Objects.requireNonNull(parentBookInfoPair);
        Objects.requireNonNull(childSheetNamePairs);
        
        return new BookCompareInfo(parentBookInfoPair, childSheetNamePairs);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<BookInfo> parentBookInfoPair;
    private final List<Pair<String>> childSheetNamePairs;
    
    private BookCompareInfo(
            Pair<BookInfo> parentBookInfoPair,
            List<Pair<String>> childSheetNamePairs) {
        
        assert parentBookInfoPair != null;
        assert childSheetNamePairs != null;
        
        this.parentBookInfoPair = parentBookInfoPair;
        this.childSheetNamePairs = List.copyOf(childSheetNamePairs);
    }
    
    /**
     * 比較対象Excelブック情報を返します。<br>
     * 
     * @return 比較対象Excelブック情報
     */
    public Pair<BookInfo> parentBookInfoPair() {
        return parentBookInfoPair;
    }
    
    /**
     * 子シート名を返します。<br>
     * 
     * @return 子シート名
     */
    public List<Pair<String>> childSheetNamePairs() {
        return childSheetNamePairs;
    }
}
