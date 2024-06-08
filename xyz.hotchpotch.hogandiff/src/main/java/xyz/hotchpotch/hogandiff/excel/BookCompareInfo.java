package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * Excelブック同士を比較するときのシートの組み合わせ情報を保持する不変クラスです。<br>
 * 
 * @param bookInfoPair 比較対象Excelブックの情報
 * @param sheetNamePairs シート名の組み合わせ
 * @author nmby
 */
public record BookCompareInfo(
        Pair<BookInfo> bookInfoPair,
        List<Pair<String>> sheetNamePairs) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param bookInfoPair 比較対象Excelブックの情報
     * @param sheetNamePairs シート名の組み合わせ
     */
    public BookCompareInfo {
        Objects.requireNonNull(bookInfoPair);
        Objects.requireNonNull(sheetNamePairs);
    }
    
}
