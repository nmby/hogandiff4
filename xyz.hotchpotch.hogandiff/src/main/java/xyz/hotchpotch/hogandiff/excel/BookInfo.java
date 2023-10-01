package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;

/**
 * Excelブックの情報を保持する不変クラスです。<br>
 * 
 * @author nmby
 */
public record BookInfo(
        BookOpenInfo bookOpenInfo,
        List<String> sheetNames) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    public BookInfo(
            BookOpenInfo bookOpenInfo,
            List<String> sheetNames) {
        
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        Objects.requireNonNull(sheetNames, "sheetNames");
        
        this.bookOpenInfo = bookOpenInfo;
        this.sheetNames = List.copyOf(sheetNames);
    }
}
