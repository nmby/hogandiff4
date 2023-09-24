package xyz.hotchpotch.hogandiff.excel.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 同一階層のフォルダ同士をペアリングする {@link DirsMatcher} の実装です。<br>
 * 
 * @author nmby
 */
public abstract class VerticallyStrictDirsMatcher implements DirsMatcher {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    protected VerticallyStrictDirsMatcher() {
    }
    
    @Override
    public List<Pair<DirInfo>> pairingDirs(
            DirInfo topDirInfo1,
            DirInfo topDirInfo2) {
        
        Objects.requireNonNull(topDirInfo1, "topDirInfo1");
        Objects.requireNonNull(topDirInfo2, "topDirInfo2");
        
        start();
        List<Pair<DirInfo>> resultPairs = new ArrayList<>();
        
        resultPairs.add(Pair.of(topDirInfo1, topDirInfo2));
        
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
        
        List<Pair<DirInfo>> dirPairs = pairingDirs(
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
    
    protected void start() {
    }
    
    protected abstract List<Pair<DirInfo>> pairingDirs(
            List<DirInfo> dirs1,
            List<DirInfo> dirs2);
    
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
