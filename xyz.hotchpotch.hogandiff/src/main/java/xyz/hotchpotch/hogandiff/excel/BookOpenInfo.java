package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Excelブックを開くための情報を保持する不変クラスです。<br>
 * 
 * @author nmby
 * 
 * @param bookPath Excelブックのパス
 * @param readPassword Excelブックの読み取りパスワード
 */
@Deprecated
public record BookOpenInfo(
        Path bookPath,
        String readPassword) {
    
    // [static members] ********************************************************
    
    /**
     * 2つのExcelブックが同一のブックを指すかを返します。<br>
     * 本メソッドは、{@link #bookPath()} の値に基づいて同一性を判断します。<br>
     * 
     * @param bookOpenInfo1 Excelブックオープン情報1
     * @param bookOpenInfo2 Excelブックオープン情報2
     * @return 2つのExcelブックが同一のブックを指す場合は {@code true}
     * @throws NullPointerException {@code bookOpenInfo1}, {@code bookOpenInfo2} のいずれかが {@code null} の場合
     */
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
    
    /**
     * コンストラクタ<br>
     * 
     * @param bookPath Excelブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @throws NullPointerException {@code bookPath} が {@code null} の場合
     */
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
