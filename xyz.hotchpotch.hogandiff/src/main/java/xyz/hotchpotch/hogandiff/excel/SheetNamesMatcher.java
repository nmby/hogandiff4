package xyz.hotchpotch.hogandiff.excel;

import java.util.List;

import xyz.hotchpotch.hogandiff.excel.common.SheetNamesMatcherImpl;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのExcelブックに含まれるシート名同士の対応関係を決めるマッチャーを表します。<br>
 * これは、{@link #pairingSheetNames(BookInfo, BookInfo)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
 * @author nmby
 */
@FunctionalInterface
public interface SheetNamesMatcher {
    
    // [static members] ********************************************************
    
    /**
     * 2つのExcelブックに含まれるシート名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly シート名の揺らぎを許容しない場合は {@code true}
     * @return シート名同士の対応関係を決めるマッチャー
     */
    public static SheetNamesMatcher of(boolean matchNamesStrictly) {
        return SheetNamesMatcherImpl.of(matchNamesStrictly);
    }
    
    // [instance members] ******************************************************
    
    public List<Pair<String>> pairingSheetNames(
            BookInfo bookInfo1,
            BookInfo bookInfo2);
}
