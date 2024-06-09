package xyz.hotchpotch.hogandiff.excel.common;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.excel.BooksMatcher;
import xyz.hotchpotch.hogandiff.excel.DirCompareInfo;
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
                ? Matcher.identityMatcherOf()
                : Matcher.combinedMatcherOf(List.of(
                        Matcher.identityMatcherOf(),
                        Matcher.minimumCostFlowMatcherOf(
                                String::length,
                                (s1, s2) -> StringDiffUtil.levenshteinDistance(s1, s2) + 1))));
    }
    
    // [instance members] ******************************************************
    
    private final Matcher<String> coreMatcher;
    
    private StandardBooksMatcher(Matcher<String> coreMatcher) {
        assert coreMatcher != null;
        
        this.coreMatcher = coreMatcher;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException {@code dirInfoPair} が {@code null} の場合
     */
    @Override
    public DirCompareInfo pairingBooks(Pair<DirInfo> dirInfoPair) {
        Objects.requireNonNull(dirInfoPair, "dirInfoPair");
        
        return DirCompareInfo.of(
                dirInfoPair,
                coreMatcher,
                // TODO: シート名のペアリングもここで実施するようにする
                Map.of());
    }
}
