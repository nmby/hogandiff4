package xyz.hotchpotch.hogandiff.excel.common;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.excel.BookNamesMatcher;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのフォルダに含まれるExcelブック名同士の対応関係を決めるマッチャーです。<br>
 * 
 * @author nmby
 */
public class BookNamesMatcherImpl implements BookNamesMatcher {
    
    // [static members] ********************************************************
    
    /**
     * 2つのフォルダに含まれるExcelブック名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly Excelブック名のゆらぎを許容する場合は {@code true}
     * @return Excelブック名同士の対応関係を決めるマッチャー
     */
    public static BookNamesMatcherImpl of(boolean matchNamesStrictly) {
        return new BookNamesMatcherImpl(matchNamesStrictly
                ? Matcher.identityMatcher()
                : Matcher.nerutonMatcherOf(
                        String::length,
                        StringDiffUtil::levenshteinDistance));
    }
    
    // [instance members] ******************************************************
    
    private final Matcher<String> coreMatcher;
    
    private BookNamesMatcherImpl(Matcher<String> coreMatcher) {
        assert coreMatcher != null;
        
        this.coreMatcher = coreMatcher;
    }
    
    @Override
    public List<Pair<String>> pairingBooks(
            DirInfo dirInfo1,
            DirInfo dirInfo2) {
        
        Objects.requireNonNull(dirInfo1, "dirInfo1");
        Objects.requireNonNull(dirInfo2, "dirInfo2");
        
        List<IntPair> pairs = coreMatcher.makePairs(
                dirInfo1.getBookNames(),
                dirInfo2.getBookNames());
        
        return pairs.stream()
                .map(p -> Pair.ofNullable(
                        p.hasA() ? dirInfo1.getBookNames().get(p.a()) : null,
                        p.hasB() ? dirInfo2.getBookNames().get(p.b()) : null))
                .toList();
    }
}
