package xyz.hotchpotch.hogandiff.excel;

import xyz.hotchpotch.hogandiff.excel.common.StandardBooksMatcher;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのフォルダに含まれるExcelブック同士の対応関係を決めるマッチャーを表します。<br>
 * これは、{@link #pairingBooks(Pair)} を関数メソッドに持つ関数型インタフェースです。<br>
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
     * @param dirInfos フォルダ情報
     * @return Excelブック名のペアのリスト
     */
    public BookNamesPairingInfo pairingBooks(Pair<DirInfo> dirInfos);
}
