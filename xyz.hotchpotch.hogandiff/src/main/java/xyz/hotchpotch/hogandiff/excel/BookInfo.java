package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;

/**
 * Excelブックの情報を保持する不変クラスです。<br>
 * 
 * @author nmby
 * 
 * @param bookOpenInfo Excelブックオープン情報
 * @param sheetNames Excelブックに含まれるシート名
 */
public record BookInfo(
        BookOpenInfo bookOpenInfo,
        List<String> sheetNames) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ<br>
     * 
     * @param bookOpenInfo Excelブックオープン情報
     * @param sheetNames Excelブックに含まれるシート名
     * @throws NullPointerException {@code bookOpenInfo}, {@code sheetNames} のいずれかが {@code null} の場合
     */
    public BookInfo(
            BookOpenInfo bookOpenInfo,
            List<String> sheetNames) {
        
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        Objects.requireNonNull(sheetNames, "sheetNames");
        
        this.bookOpenInfo = bookOpenInfo;
        this.sheetNames = List.copyOf(sheetNames);
    }
}
