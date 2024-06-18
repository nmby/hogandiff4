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
// TODO: フォルダ階層の変更にも対応できる柔軟な {@link DirsMatcher} も実装する
public class VerticallyStrictDirsMatcher implements DirsMatcher {
    
    // [static members] ********************************************************
    
    private static final Function<DirInfo, String> dirNameExtractor = d -> d.dirPath().getFileName().toString();
    
    private static final Matcher<DirInfo> strictDirNamesMatcher = Matcher.identityMatcherOf(dirNameExtractor);
    
    private static final Matcher<DirInfo> fuzzyButSimpleDirsMatcher = Matcher.minimumCostFlowMatcherOf(
            d -> d.childDirInfos().size() + d.childBookNames().size(),
            (d1, d2) -> {
                List<String> childrenNames1 = d1.childDirInfos().stream().map(dirNameExtractor).toList();
                List<String> childrenNames2 = d2.childDirInfos().stream().map(dirNameExtractor).toList();
                
                int gapChildren = (int) Matcher.identityMatcherOf().makeIdxPairs(childrenNames1, childrenNames2)
                        .stream().filter(Predicate.not(IntPair::isPaired)).count();
                int gapBookNames = (int) Matcher.identityMatcherOf().makeIdxPairs(d1.childBookNames(), d2.childBookNames())
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
                : Matcher.combinedMatcherOf(List.of(
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
    public List<Pair<DirInfo>> pairingDirs(Pair<DirInfo> topDirInfos) {
        Objects.requireNonNull(topDirInfos, "topDirInfos");
        
        List<Pair<DirInfo>> resultPairs = new ArrayList<>();
        
        resultPairs.add(topDirInfos);
        
        pairingDirs2(resultPairs, topDirInfos);
        
        return List.copyOf(resultPairs);
    }
    
    private void pairingDirs2(
            List<Pair<DirInfo>> resultPairs,
            Pair<DirInfo> dirInfos) {
        
        assert resultPairs != null;
        assert dirInfos != null;
        
        List<Pair<DirInfo>> dirPairs = coreMatcher.makeItemPairs(
                dirInfos.a().childDirInfos(),
                dirInfos.b().childDirInfos());
        
        for (Pair<DirInfo> dirPair : dirPairs) {
            if (dirPair.isPaired()) {
                resultPairs.add(dirPair);
                pairingDirs2(resultPairs, dirPair);
                
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
        
        pairs.add(Pair.ofOnly(side, dirInfo));
        
        dirInfo.childDirInfos().forEach(d -> setAloneDirs(pairs, d, side));
    }
}
