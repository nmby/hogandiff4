package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excelブック比較情報を表す不変クラスです。<br>
 * 
 * @param parentBookInfoPair 親Excelブック情報
 * @param childSheetNamePairs 子シート名の組み合わせ
 * @author nmby
 */
public final record BookInfoComparison(
        Pair<BookInfo> parentBookInfoPair,
        List<Pair<String>> childSheetNamePairs)
        implements CompareInfo {
    
    // [static members] ********************************************************
    
    /**
     * 与えられたマッチャーを使用して新たな {@link BookInfoComparison} インスタンスを生成します。<br>
     * 
     * @param parentBookInfoPair 比較対象Excelブックの情報
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @throws NullPointerException パラメータが {@code null} の場合
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static BookInfoComparison calculate(
            Pair<BookInfo> parentBookInfoPair,
            Matcher<String> sheetNamesMatcher) {
        
        Objects.requireNonNull(parentBookInfoPair);
        Objects.requireNonNull(sheetNamesMatcher);
        
        if (parentBookInfoPair.isPaired()) {
            List<Pair<String>> sheetNamePairs = sheetNamesMatcher.makeItemPairs(
                    parentBookInfoPair.a().sheetNames(),
                    parentBookInfoPair.b().sheetNames());
            return new BookInfoComparison(parentBookInfoPair, sheetNamePairs);
            
        } else if (parentBookInfoPair.hasA()) {
            List<Pair<String>> sheetNamePairs = parentBookInfoPair.a().sheetNames().stream()
                    .map(sheetName -> Pair.ofOnly(Side.A, sheetName))
                    .toList();
            return new BookInfoComparison(parentBookInfoPair, sheetNamePairs);
            
        } else if (parentBookInfoPair.hasB()) {
            List<Pair<String>> sheetNamePairs = parentBookInfoPair.b().sheetNames().stream()
                    .map(sheetName -> Pair.ofOnly(Side.B, sheetName))
                    .toList();
            return new BookInfoComparison(parentBookInfoPair, sheetNamePairs);
            
        } else {
            return new BookInfoComparison(parentBookInfoPair, List.of());
        }
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param parentBookInfoPair Excelブック情報
     * @param childSheetNamePairs シート名の組み合わせ
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public BookInfoComparison {
        Objects.requireNonNull(parentBookInfoPair);
        Objects.requireNonNull(childSheetNamePairs);
        
        childSheetNamePairs = List.copyOf(childSheetNamePairs);
    }
}
