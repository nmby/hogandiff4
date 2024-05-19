package xyz.hotchpotch.hogandiff.excel;

import java.util.List;

import xyz.hotchpotch.hogandiff.excel.common.StandardSheetNamesMatcher;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 2つのExcelブックに含まれるシート名同士の対応関係を決めるマッチャーを表します。<br>
 * これは、{@link #pairingSheetNames(Pair)} を関数メソッドに持つ関数型インタフェースです。<br>
 * 
 * @author nmby
 */
@FunctionalInterface
public interface SheetNamesMatcher {
    
    // [static members] ********************************************************
    
    /**
     * 2つのExcelブックに含まれるシート名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param matchNamesStrictly シート名の揺らぎを許容しない場合は {@code true}
     * @return シート名同士の対応関係を決めるマッチャー
     */
    public static SheetNamesMatcher of(boolean matchNamesStrictly) {
        return StandardSheetNamesMatcher.of(matchNamesStrictly);
    }
    
    // [instance members] ******************************************************
    
    /**
     * 2つのExcelブックに含まれるシート同士の組み合わせを決定して返します。<br>
     * 
     * @param bookInfos 比較対象Excelブック情報
     * @return シート同士の組み合わせを表すリスト
     */
    public List<Pair<String>> pairingSheetNames(Pair<BookInfo> bookInfos);
}
