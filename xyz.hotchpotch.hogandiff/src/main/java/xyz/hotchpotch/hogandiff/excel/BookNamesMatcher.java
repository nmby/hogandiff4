package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのフォルダに含まれるExcelブック名同士の対応関係を決めるマッチャーです。<br>
 * 
 * @author nmby
 */
public class BookNamesMatcher {
    
    // [static members] ********************************************************
    
    public static BookNamesMatcher of(boolean matchNamesStrictly) {
        //TODO: Excelブック名だけでなく内包するシートも加味したマッチャーに改善可能
        return new BookNamesMatcher(matchNamesStrictly
                ? Matcher.identityMatcher()
                : Matcher.nerutonMatcherOf(
                        String::length,
                        StringDiffUtil::levenshteinDistance));
    }
    
    // [instance members] ******************************************************
    
    private final Matcher<String> coreMatcher;
    
    private BookNamesMatcher(Matcher<String> coreMatcher) {
        assert coreMatcher != null;
        
        this.coreMatcher = coreMatcher;
    }
    
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
