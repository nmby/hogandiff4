package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.excel.common.VerticallyStrictDirsMatcher;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのフォルダツリーに含まれるフォルダ同士の対応関係を決めるマッチャーです。<br>
 * これは、{@link #pairingDirs(Pair)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
 * @author nmby
 */
@FunctionalInterface
public interface DirsMatcher {
    
    // [static members] ********************************************************
    
    /**
     * フォルダの組み合わせを表すレコードです。<br>
     * 
     * @param dirPair 比較対象のフォルダ情報のペア
     * @param bookNamePairs 比較対象のExcelブック名同士のペアのリスト
     */
    public static record DirPairData(
            Pair<DirInfo> dirPair,
            List<Pair<String>> bookNamePairs) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        /**
         * コンストラクタ
         * 
         * @param dirPair 比較対象のフォルダ情報のペア
         * @param bookNamePairs 比較対象のExcelブック名同士のペアのリスト
         * @throws NullPointerException {@code dirPair}, {@code bookNamePairs} のいずれかが {@code null} の場合
         */
        public DirPairData {
            Objects.requireNonNull(dirPair, "dirPair");
            Objects.requireNonNull(bookNamePairs, "bookNamePairs");
        }
    }
    
    /**
     * 2つのフォルダツリーに含まれるフォルダ同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly フォルダ名と階層のゆらぎを許容する場合は {@code true}
     * @return フォルダ同士の対応関係を決めるマッチャー
     */
    public static DirsMatcher of(boolean matchNamesStrictly) {
        return VerticallyStrictDirsMatcher.of(matchNamesStrictly);
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
