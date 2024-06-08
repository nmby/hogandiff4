package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダ同士を比較するときのExcelブックの組み合わせ情報を保持する不変クラスです。<br>
 * 
 * @param dirInfoPair 比較対象フォルダの情報
 * @param bookNamePairs Excelブック名の組み合わせ
 * @param bookCompareInfos シート名の組み合わせ情報
 * @author nmby
 */
public record DirCompareInfo(
        Pair<DirInfo> dirInfoPair,
        List<Pair<String>> bookNamePairs,
        Map<Pair<String>, BookCompareInfo> bookCompareInfos) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param dirInfoPair 比較対象フォルダの情報
     * @param bookNamePairs Excelブック名の組み合わせ
     * @param bookCompareInfos シート名の組み合わせ情報
     * @throws NullPointerException
     *      {@code dirInfoPair}, {@code bookNamePairs}, {@code sheetNamesPairingInfos}
     *      のいずれかが {@code null} の場合
     */
    public DirCompareInfo {
        Objects.requireNonNull(dirInfoPair, "dirInfoPair");
        Objects.requireNonNull(bookNamePairs, "bookNamePairs");
        Objects.requireNonNull(bookCompareInfos, "bookCompareInfos");
    }
}
