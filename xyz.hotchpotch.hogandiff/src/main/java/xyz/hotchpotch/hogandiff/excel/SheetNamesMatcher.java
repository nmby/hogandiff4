package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのExcelブックに含まれるシート名同士の対応関係を決めるマッチャーです。<br>
 * 
 * @author nmby
 */
public class SheetNamesMatcher {
    
    // [static members] ********************************************************
    
    /**
     * 2つのExcelブックに含まれるシート名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly シート名の揺らぎを許容しない場合は {@code true}
     * @return シート名同士の対応関係を決めるマッチャー
     */
    public static SheetNamesMatcher of(boolean matchNamesStrictly) {
        return new SheetNamesMatcher(matchNamesStrictly
                ? Matcher.identityMatcher()
                : Matcher.nerutonMatcherOf(
                        String::length,
                        StringDiffUtil::levenshteinDistance));
    }
    
    // [instance members] ******************************************************
    
    private Matcher<String> coreMatcher;
    
    private SheetNamesMatcher(Matcher<String> coreMatcher) {
        assert coreMatcher != null;
        
        this.coreMatcher = coreMatcher;
    }
    
    public List<Pair<String>> pairingSheetNames(
            BookInfo bookInfo1,
            BookInfo bookInfo2) {
        
        Objects.requireNonNull(bookInfo1, "bookInfo1");
        Objects.requireNonNull(bookInfo2, "bookInfo2");
        
        List<IntPair> pairs = coreMatcher.makePairs(
                bookInfo1.sheetNames(),
                bookInfo2.sheetNames());
        
        return pairs.stream()
                .map(p -> Pair.ofNullable(
                        p.hasA() ? bookInfo1.sheetNames().get(p.a()) : null,
                        p.hasB() ? bookInfo2.sheetNames().get(p.b()) : null))
                .toList();
    }
}
