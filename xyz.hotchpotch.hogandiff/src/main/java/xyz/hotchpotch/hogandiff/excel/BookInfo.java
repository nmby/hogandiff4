package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Excelブック情報を表す不変クラスです。<br>
 * 
 * @author nmby
 * 
 * @param bookPath Excelブックのパス
 * @param sheetNames Excelブックに含まれるシート名
 */
public record BookInfo(
        Path bookPath,
        List<String> sheetNames) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ<br>
     * 
     * @param bookPath Excelブックのパス
     * @param sheetNames Excelブックに含まれるシート名
     * @throws NullPointerException {@code bookPath}, {@code sheetNames} のいずれかが {@code null} の場合
     */
    public BookInfo(
            Path bookPath,
            List<String> sheetNames) {
        
        Objects.requireNonNull(bookPath, "bookPath");
        Objects.requireNonNull(sheetNames, "sheetNames");
        
        this.bookPath = bookPath;
        this.sheetNames = List.copyOf(sheetNames);
    }
    
    @Override
    public String toString() {
        return bookPath.getFileName().toString();
    }
}
