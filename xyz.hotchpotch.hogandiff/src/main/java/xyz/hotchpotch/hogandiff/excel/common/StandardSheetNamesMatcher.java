package xyz.hotchpotch.hogandiff.excel.common;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
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
                ? Matcher.identityMatcher()
                : Matcher.greedyMatcherOf(
                        String::length,
                        StringDiffUtil::levenshteinDistance));
    }
    
    // [instance members] ******************************************************
    
    private Matcher<String> coreMatcher;
    
    private StandardSheetNamesMatcher(Matcher<String> coreMatcher) {
        assert coreMatcher != null;
        
        this.coreMatcher = coreMatcher;
    }
    
    @Override
    public List<Pair<String>> pairingSheetNames(
            BookInfo bookInfo1,
            BookInfo bookInfo2) {
        
        Objects.requireNonNull(bookInfo1, "bookInfo1");
        Objects.requireNonNull(bookInfo2, "bookInfo2");
        
        return coreMatcher.makeItemPairs(
                bookInfo1.sheetNames(),
                bookInfo2.sheetNames());
    }
}
