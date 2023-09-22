package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Excelブックを開くための情報を保持する不変クラスです。<br>
 * 
 * @author nmby
 */
public record BookOpenInfo(
        Path bookPath,
        String readPassword) {
    
    // [static members] ********************************************************
    
    public static boolean isSameBook(
            BookOpenInfo bookOpenInfo1,
            BookOpenInfo bookOpenInfo2) {
        
        Objects.requireNonNull(bookOpenInfo1, "bookOpenInfo1");
        Objects.requireNonNull(bookOpenInfo2, "bookOpenInfo2");
        
        return Objects.equals(
                bookOpenInfo1.bookPath,
                bookOpenInfo2.bookPath);
    }
    
    // [instance members] ******************************************************
    
    public BookOpenInfo {
        Objects.requireNonNull(bookPath, "bookPath");
    }
    
    /**
     * このExcelブックの形式を返します。<br>
     * 
     * @return このExcelブックの形式
     */
    public BookType bookType() {
        return BookType.of(bookPath);
    }
    
    @Override
    public String toString() {
        return bookPath.toString();
    }
    
    /**
     * このExcelブックオープン情報に指定された読み取りパスワードを追加した
     * 新たなExcelブックオープン情報を返します。<br>
     * 
     * @param readPassword Excelブックの読み取りパスワード
     * @return 新たなExcelブックオープン情報
     */
    public BookOpenInfo withReadPassword(String readPassword) {
        return new BookOpenInfo(this.bookPath, readPassword);
    }
}
