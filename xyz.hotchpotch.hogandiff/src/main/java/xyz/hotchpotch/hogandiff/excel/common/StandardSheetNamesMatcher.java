package xyz.hotchpotch.hogandiff.excel.common;

import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.excel.BookCompareInfo;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.SheetNamesMatcher;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * {@link SheetNamesMatcher} の標準的な実装です。<br>
 * 
 * @author nmby
 */
public class StandardSheetNamesMatcher implements SheetNamesMatcher {
    
    // [static members] ********************************************************
    
    /**
     * 2つのExcelブックに含まれるシート名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly シート名の揺らぎを許容しない場合は {@code true}
     * @return シート名同士の対応関係を決めるマッチャー
     */
    public static StandardSheetNamesMatcher of(boolean matchNamesStrictly) {
        return new StandardSheetNamesMatcher(matchNamesStrictly
                ? Matcher.identityMatcherOf()
                : Matcher.minimumCostFlowMatcherOf(
                        String::length,
                        (str1, str2) -> StringDiffUtil.levenshteinDistance(str1, str2) + 1));
    }
    
    // [instance members] ******************************************************
    
    private Matcher<String> coreMatcher;
    
    private StandardSheetNamesMatcher(Matcher<String> coreMatcher) {
        assert coreMatcher != null;
        
        this.coreMatcher = coreMatcher;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException {@code bookInfoPair} が {@code null} の場合
     */
    @Override
    public BookCompareInfo pairingSheetNames(
            Pair<BookInfo> bookInfoPair) {
        
        Objects.requireNonNull(bookInfoPair, "bookInfoPair");
        
        return new BookCompareInfo(
                bookInfoPair,
                coreMatcher.makeItemPairs(
                        bookInfoPair.a().sheetNames(),
                        bookInfoPair.b().sheetNames()));
    }
}
