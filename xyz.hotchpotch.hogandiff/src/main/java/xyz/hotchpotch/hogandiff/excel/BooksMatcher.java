package xyz.hotchpotch.hogandiff.excel;

import java.util.List;

import xyz.hotchpotch.hogandiff.excel.common.StandardBooksMatcher;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのフォルダに含まれるExcelブック同士の対応関係を決めるマッチャーを表します。<br>
 * これは、{@link #pairingBooks(DirInfo, DirInfo)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
 * @author nmby
 */
@FunctionalInterface
public interface BooksMatcher {
    
    // [static members] ********************************************************
    
    /**
     * 2つのフォルダに含まれるExcelブック同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly Excelブック名のゆらぎを許容する場合は {@code true}
     * @return Excelブック名同士の対応関係を決めるマッチャー
     */
    public static BooksMatcher of(boolean matchNamesStrictly) {
        return StandardBooksMatcher.of(matchNamesStrictly);
    }
    
    // [instance members] ******************************************************
    
    /**
     * 2つのフォルダに含まれるExcelブック同士の対応関係を、Excelブック名のペアのリストとして返します。<br>
     * 
     * @param dirInfo1 フォルダ情報1
     * @param dirInfo2 フォルダ情報2
     * @return Excelブック名のペアのリスト
     */
    public List<Pair<String>> pairingBooks(
            DirInfo dirInfo1,
            DirInfo dirInfo2);
}
