package xyz.hotchpotch.hogandiff.excel.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 同一階層のフォルダ同士をペアリングする {@link DirsMatcher} の実装です。<br>
 * 
 * @author nmby
 */
public class VerticallyStrictDirsMatcher implements DirsMatcher {
    
    // [static members] ********************************************************
    
    private static final Function<DirInfo, String> dirNameExtractor = d -> d.path().getFileName().toString();
    
    private static final Matcher<DirInfo> strictDirNamesMatcher = Matcher.identityMatcher(dirNameExtractor);
    
    private static final Matcher<DirInfo> fuzzyButSimpleDirsMatcher = Matcher.minimumCostFlowMatcherOf(
            d -> d.children().size() + d.bookNames().size(),
            (d1, d2) -> {
                List<String> childrenNames1 = d1.children().stream().map(dirNameExtractor).toList();
                List<String> childrenNames2 = d2.children().stream().map(dirNameExtractor).toList();
                
                int gapChildren = (int) Matcher.identityMatcher().makeIdxPairs(childrenNames1, childrenNames2)
                        .stream().filter(Predicate.not(IntPair::isPaired)).count();
                int gapBookNames = (int) Matcher.identityMatcher().makeIdxPairs(d1.bookNames(), d2.bookNames())
                        .stream().filter(Predicate.not(IntPair::isPaired)).count();
                
                return gapChildren + gapBookNames;
            });
    
    /**
     * {@link DirsMatcher} のインスタンスを返します。<br>
     * 
     * @param matchNamesStrictly フォルダ名の曖昧一致を許さない場合は {@code true}
     * @return マッチャー
     */
    public static DirsMatcher of(boolean matchNamesStrictly) {
        return new VerticallyStrictDirsMatcher(matchNamesStrictly
                ? strictDirNamesMatcher
                : Matcher.combinedMatcher(List.of(
                        strictDirNamesMatcher,
                        fuzzyButSimpleDirsMatcher)));
    }
    
    // [instance members] ******************************************************
    
    private final Matcher<DirInfo> coreMatcher;
    
    private VerticallyStrictDirsMatcher(Matcher<DirInfo> coreMatcher) {
        assert coreMatcher != null;
        this.coreMatcher = coreMatcher;
    }
    
    @Override
    public List<Pair<DirInfo>> pairingDirs(
            DirInfo topDirInfo1,
            DirInfo topDirInfo2) {
        
        Objects.requireNonNull(topDirInfo1, "topDirInfo1");
        Objects.requireNonNull(topDirInfo2, "topDirInfo2");
        
        List<Pair<DirInfo>> resultPairs = new ArrayList<>();
        
        resultPairs.add(new Pair<>(topDirInfo1, topDirInfo2));
        
        pairingDirs2(resultPairs, topDirInfo1, topDirInfo2);
        
        return List.copyOf(resultPairs);
    }
    
    private void pairingDirs2(
            List<Pair<DirInfo>> resultPairs,
            DirInfo dirInfo1,
            DirInfo dirInfo2) {
        
        assert resultPairs != null;
        assert dirInfo1 != null;
        assert dirInfo2 != null;
        
        List<Pair<DirInfo>> dirPairs = coreMatcher.makeItemPairs(
                dirInfo1.children(),
                dirInfo2.children());
        
        for (Pair<DirInfo> dirPair : dirPairs) {
            if (dirPair.isPaired()) {
                resultPairs.add(dirPair);
                pairingDirs2(resultPairs, dirPair.a(), dirPair.b());
                
            } else if (dirPair.isOnlyA()) {
                setAloneDirs(resultPairs, dirPair.a(), Side.A);
                
            } else if (dirPair.isOnlyB()) {
                setAloneDirs(resultPairs, dirPair.b(), Side.B);
            }
        }
    }
    
    private void setAloneDirs(
            List<Pair<DirInfo>> pairs,
            DirInfo dirInfo,
            Side side) {
        
        pairs.add(side == Side.A
                ? new Pair<>(dirInfo, null)
                : new Pair<>(null, dirInfo));
        
        dirInfo.children().forEach(d -> setAloneDirs(pairs, d, side));
    }
}
