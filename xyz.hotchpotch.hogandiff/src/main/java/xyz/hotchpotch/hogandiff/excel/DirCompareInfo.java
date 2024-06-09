package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダ同士を比較するときのExcelブックの組み合わせ情報を保持する不変クラスです。<br>
 * 
 * @author nmby
 */
public class DirCompareInfo {
    
    // [static members] ********************************************************
    
    /**
     * コンストラクタ
     * 
     * @param dirInfoPair 比較対象フォルダの情報
     * @param bookNamePairs Excelブック名の組み合わせ
     * @param bookCompareInfos Excelブック比較情報
     * @return フォルダ比較情報
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static DirCompareInfo of(
            Pair<DirInfo> dirInfoPair,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, BookCompareInfo> bookCompareInfos) {
        
        Objects.requireNonNull(dirInfoPair, "dirInfoPair");
        Objects.requireNonNull(bookNamePairs, "bookNamePairs");
        Objects.requireNonNull(bookCompareInfos, "bookCompareInfos");
        
        return new DirCompareInfo(dirInfoPair, bookNamePairs, bookCompareInfos);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> dirInfoPair;
    private final List<Pair<String>> bookNamePairs;
    private final Map<Pair<String>, BookCompareInfo> bookCompareInfos;
    
    private DirCompareInfo(
            Pair<DirInfo> dirInfoPair,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, BookCompareInfo> bookCompareInfos) {
        
        Objects.requireNonNull(dirInfoPair, "dirInfoPair");
        Objects.requireNonNull(bookNamePairs, "bookNamePairs");
        Objects.requireNonNull(bookCompareInfos, "bookCompareInfos");
        
        this.dirInfoPair = dirInfoPair;
        this.bookNamePairs = List.copyOf(bookNamePairs);
        this.bookCompareInfos = Map.copyOf(bookCompareInfos);
    }
    
    /**
     * 比較対象フォルダの情報を返します。<br>
     * 
     * @return 比較対象フォルダの情報
     */
    public Pair<DirInfo> dirInfoPair() {
        return dirInfoPair;
    }
    
    /**
     * Excelブック名の組み合わせを返します。<br>
     * 
     * @return Excelブック名の組み合わせ
     */
    public List<Pair<String>> bookNamePairs() {
        return bookNamePairs;
    }
    
    /**
     * Excelブック比較情報を返します。<br>
     * 
     * @return Excelブック比較情報
     */
    public Map<Pair<String>, BookCompareInfo> bookCompareInfos() {
        return bookCompareInfos;
    }
}
