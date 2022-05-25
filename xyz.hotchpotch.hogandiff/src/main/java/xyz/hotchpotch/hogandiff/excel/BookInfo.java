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
        
        return of(bookPath, null);
    }
    
    /**
     * 新たなExcelブック情報を生成して返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @return Excelブック情報
     * @throws NullPointerException {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException {@code bookPath} の拡張子が不正な形式の場合
     */
    public static BookInfo of(
            Path bookPath,
            String readPassword) {
        
        Objects.requireNonNull(bookPath, "bookPath");
        
        return new BookInfo(bookPath, readPassword);
    }
    
    // [instance members] ******************************************************
    
    private final Path bookPath;
    private final BookType bookType;
    private final String readPassword;
    
    private BookInfo(
            Path bookPath,
            String readPassword) {
        
        assert bookPath != null;
        
        this.bookPath = bookPath;
        this.bookType = BookType.of(bookPath);
        this.readPassword = readPassword;
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
    
    /**
     * このExcelブックの読み取りパスワードを返します。<br>
     * 読み取りパスワードが登録されていない場合は {@code null} を返します。<br>
     * 
     * @return このExcelブックの読み取りパスワード（登録されていない場合は {@code null}）
     */
    public String getReadPassword() {
        return readPassword;
    }
    
    @Override
    public String toString() {
        return bookPath.toString();
    }
    
    /**
     * このExcelブック情報に指定された読み取りパスワードを追加したExcelブック情報を返します。<br>
     * 
     * @param readPassword Excelブックの読み取りパスワード
     * @return 新たなExcelブック情報
     */
    public BookInfo withReadPassword(String readPassword) {
        return of(this.bookPath, readPassword);
    }
}
