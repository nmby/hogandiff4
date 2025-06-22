package xyz.hotchpotch.hogandiff.main.misc.excel;

import java.util.List;

import xyz.hotchpotch.hogandiff.main.comparators.matchers.VerticallyStrictDirInfosMatcher;
import xyz.hotchpotch.hogandiff.main.models.DirInfo;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのフォルダツリーに含まれるフォルダ同士の対応関係を決めるマッチャーです。<br>
 * これは、{@link #pairingDirs(Pair)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
 * @author nmby
 */
@FunctionalInterface
public interface DirInfosMatcher {

    // [static members] ********************************************************

    /**
     * 2つのフォルダツリーに含まれるフォルダ同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly フォルダ名と階層のゆらぎを許容する場合は {@code true}
     * @return フォルダ同士の対応関係を決めるマッチャー
     */
    public static DirInfosMatcher of(boolean matchNamesStrictly) {
        return VerticallyStrictDirInfosMatcher.of(matchNamesStrictly);
    }

    // [instance members] ******************************************************

    /**
     * フォルダツリーに含まれるフォルダ同士の組み合わせを決定して返します。<br>
     * 
     * @param topDirInfos トップフォルダ
     * @return フォルダ同士の組み合わせを表すリスト
     */
    public List<Pair<DirInfo>> pairingDirs(Pair<DirInfo> topDirInfos);
}
