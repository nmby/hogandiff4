package xyz.hotchpotch.hogandiff.excel.common;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.excel.BooksMatcher;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * {@link BooksMatcher} の標準的な実装です。<br>
 * 
 * @author nmby
 */
public class StandardBooksMatcher implements BooksMatcher {
    
    // [static members] ********************************************************
    
    /**
     * 2つのフォルダに含まれるExcelブック名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly Excelブック名のゆらぎを許容する場合は {@code true}
     * @return Excelブック名同士の対応関係を決めるマッチャー
     */
    public static StandardBooksMatcher of(boolean matchNamesStrictly) {
        return new StandardBooksMatcher(matchNamesStrictly
                ? Matcher.identityMatcher()
                : Matcher.greedyMatcherOf(
                        String::length,
                        StringDiffUtil::levenshteinDistance));
    }
    
    // [instance members] ******************************************************
    
    private final Matcher<String> coreMatcher;
    
    private StandardBooksMatcher(Matcher<String> coreMatcher) {
        assert coreMatcher != null;
        
        this.coreMatcher = coreMatcher;
    }
    
    @Override
    public List<Pair<String>> pairingBooks(
            DirInfo dirInfo1,
            DirInfo dirInfo2) {
        
        Objects.requireNonNull(dirInfo1, "dirInfo1");
        Objects.requireNonNull(dirInfo2, "dirInfo2");
        
        return coreMatcher.makePairs2(
                dirInfo1.bookNames(),
                dirInfo2.bookNames());
    }
}
