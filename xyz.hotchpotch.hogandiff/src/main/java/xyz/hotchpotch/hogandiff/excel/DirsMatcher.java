package xyz.hotchpotch.hogandiff.excel;

import java.util.List;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのフォルダツリーに含まれるフォルダ同士の対応関係を決めるマッチャーです。<br>
 * これは、{@link #pairingDirs(DirInfo, DirInfo)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
 * @author nmby
 */
@FunctionalInterface
public interface DirsMatcher {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    public List<Pair<DirInfo>> pairingDirs(
            DirInfo topDirInfo1,
            DirInfo topDirInfo2);
}
