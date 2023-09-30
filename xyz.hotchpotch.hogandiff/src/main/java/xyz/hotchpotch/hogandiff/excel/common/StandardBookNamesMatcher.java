package xyz.hotchpotch.hogandiff.excel.common;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.excel.BookNamesMatcher;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * {@link BookNamesMatcher} の標準的な実装です。<br>
 * 
 * @author nmby
 */
public class StandardBookNamesMatcher implements BookNamesMatcher {
    
    // [static members] ********************************************************
    
    /**
     * 2つのフォルダに含まれるExcelブック名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly Excelブック名のゆらぎを許容する場合は {@code true}
     * @return Excelブック名同士の対応関係を決めるマッチャー
     */
    public static StandardBookNamesMatcher of(boolean matchNamesStrictly) {
        return new StandardBookNamesMatcher(matchNamesStrictly
                ? Matcher.identityMatcher()
                : Matcher.combinedMatcher(List.of(
                        Matcher.identityMatcher(),
                        Matcher.greedyMatcherOf(
                                String::length,
                                (s1, s2) -> StringDiffUtil.levenshteinDistance(s1, s2) + 1))));
    }
    
    // [instance members] ******************************************************
    
    private final Matcher<String> coreMatcher;
    
    private StandardBookNamesMatcher(Matcher<String> coreMatcher) {
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
