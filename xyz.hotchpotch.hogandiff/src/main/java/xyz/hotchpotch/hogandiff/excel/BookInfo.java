package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Excelブックの情報を保持する不変クラスです。<br>
 * 
 * @author nmby
 * 
 * @param bookPath Excelブックのパス
 * @param readPassword Excelブックの読み取りパスワード
 * @param sheetNames Excelブックに含まれるシート名
 */
public record BookInfo(
        Path bookPath,
        String readPassword,
        List<String> sheetNames) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ<br>
     * 
     * @param bookPath Excelブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @param sheetNames Excelブックに含まれるシート名
     * @throws NullPointerException {@code bookOpenInfo}, {@code sheetNames} のいずれかが {@code null} の場合
     */
    public BookInfo(
            Path bookPath,
            String readPassword,
            List<String> sheetNames) {
        
        Objects.requireNonNull(bookPath, "bookPath");
        // readPassword may be null.
        Objects.requireNonNull(sheetNames, "sheetNames");
        
        this.bookPath = bookPath;
        this.readPassword = readPassword;
        this.sheetNames = List.copyOf(sheetNames);
    }
}
