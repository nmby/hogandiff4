package xyz.hotchpotch.hogandiff.excel.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 同一階層の、名称が完全一致するフォルダ同士をペアリングする
 * {@link DirsMatcher} の実装です。<br>
 * 
 * @author nmby
 */
public class StrictDirsMatcher implements DirsMatcher {
    
    // [static members] ********************************************************
    
    private static final Matcher<String> coreMatcher = Matcher.identityMatcher();
    
    private static List<Pair<String>> pairingDirNames(
            List<String> dirNames1,
            List<String> dirNames2) {
        
        assert dirNames1 != null;
        assert dirNames2 != null;
        
        List<IntPair> pairs = coreMatcher.makePairs(dirNames1, dirNames2);
        
        return pairs.stream()
                .map(p -> Pair.ofNullable(
                        p.hasA() ? dirNames1.get(p.a()) : null,
                        p.hasB() ? dirNames2.get(p.b()) : null))
                .toList();
    }
    
    public static DirsMatcher of() {
        return new StrictDirsMatcher();
    }
    
    // [instance members] ******************************************************
    
    private StrictDirsMatcher() {
    }
    
    @Override
    public List<Pair<DirInfo>> pairingDirs(
            DirInfo topDirInfo1,
            DirInfo topDirInfo2) {
        
        Objects.requireNonNull(topDirInfo1, "topDirInfo1");
        Objects.requireNonNull(topDirInfo2, "topDirInfo2");
        
        List<Pair<DirInfo>> pairs = new ArrayList<>();
        
        pairs.add(Pair.of(topDirInfo1, topDirInfo2));
        
        pairingDirs2(pairs, topDirInfo1, topDirInfo2);
        
        return List.copyOf(pairs);
    }
    
    private void pairingDirs2(
            List<Pair<DirInfo>> pairs,
            DirInfo dirInfo1,
            DirInfo dirInfo2) {
        
        assert pairs != null;
        assert dirInfo1 != null;
        assert dirInfo2 != null;
        
        Map<String, DirInfo> children1 = dirInfo1.children().stream()
                .collect(Collectors.toMap(
                        d -> d.path().getFileName().toString(),
                        Function.identity()));
        Map<String, DirInfo> children2 = dirInfo2.children().stream()
                .collect(Collectors.toMap(
                        d -> d.path().getFileName().toString(),
                        Function.identity()));
        
        List<Pair<String>> dirNamePairs = pairingDirNames(
                List.copyOf(children1.keySet()),
                List.copyOf(children2.keySet()));
        
        for (Pair<String> dirNamePair : dirNamePairs) {
            if (!dirNamePair.isPaired()) {
                continue;
            }
            
            DirInfo child1 = children1.get(dirNamePair.a());
            DirInfo child2 = children2.get(dirNamePair.b());
            
            pairs.add(Pair.of(child1, child2));
            pairingDirs2(pairs, child1, child2);
        }
        
        for (Pair<String> dirNamePair : dirNamePairs) {
            if (!dirNamePair.isOnlyA()) {
                continue;
            }
            setAloneDirs(pairs, children1.get(dirNamePair.a()), Side.A);
        }
        
        for (Pair<String> dirNamePair : dirNamePairs) {
            if (!dirNamePair.isOnlyB()) {
                continue;
            }
            setAloneDirs(pairs, children2.get(dirNamePair.b()), Side.B);
        }
    }
    
    private void setAloneDirs(
            List<Pair<DirInfo>> pairs,
            DirInfo dirInfo,
            Side side) {
        
        pairs.add(side == Side.A
                ? Pair.ofNullable(dirInfo, null)
                : Pair.ofNullable(null, dirInfo));
        
        dirInfo.children().forEach(d -> setAloneDirs(pairs, d, side));
    }
}
