package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Excelブックを特定するための情報を保持する不変クラスです。<br>
 * 
 * @author nmby
 */
public class BookInfo {
    
    // [static members] ********************************************************
    
    /**
     * 新たなExcelブック情報を生成して返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return Excelブック情報
     * @throws NullPointerException {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException {@code bookPath} の拡張子が不正な形式の場合
     */
    public static BookInfo of(
            Path bookPath) {
        
        Objects.requireNonNull(bookPath, "bookPath");
        
        return new BookInfo(bookPath);
    }
    
    // [instance members] ******************************************************
    
    private final Path bookPath;
    private final BookType bookType;
    
    private BookInfo(
            Path bookPath) {
        
        assert bookPath != null;
        
        this.bookPath = bookPath;
        this.bookType = BookType.of(bookPath);
    }
    
    /**
     * このExcelブックのパスを返します。<br>
     * 
     * @return このExcelブックのパス
     */
    public Path bookPath() {
        return bookPath;
    }
    
    /**
     * このExcelブックの形式を返します。<br>
     * 
     * @return このExcelブックの形式
     */
    public BookType bookType() {
        return bookType;
    }
    
    @Override
    public String toString() {
        return bookPath.toString();
    }
}
