package xyz.hotchpotch.hogandiff.excel;

import java.util.List;

import xyz.hotchpotch.hogandiff.excel.common.StrictDirsMatcher;
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
    
    /**
     * 2つのフォルダツリーに含まれるフォルダ同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly フォルダ名と階層のゆらぎを許容する場合は {@code true}
     * @return フォルダ同士の対応関係を決めるマッチャー
     */
    public static DirsMatcher of(boolean matchNamesStrictly) {
        return matchNamesStrictly
                ? StrictDirsMatcher.of()
                // TODO: 曖昧マッチャーを実装する
                : StrictDirsMatcher.of();
    }
    
    // [instance members] ******************************************************
    
    public List<Pair<DirInfo>> pairingDirs(
            DirInfo topDirInfo1,
            DirInfo topDirInfo2);
}
