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
 * @param status このExcelブック情報の状態
 */
public record BookInfo(
        Path bookPath,
        List<String> sheetNames,
        Status status) {
    
    // [static members] ********************************************************
    
    /**
     * Excelブック情報の状態を表す列挙型です。<br>
     */
    public static enum Status {
        
        // [static members] ----------------------------------------------------
        
        /** Excelブック情報をロード済みであることを表します。 */
        LOAD_COMPLETED,
        
        /** 未だExcelブック情報のロードを試みていないことを表します。 */
        NOT_YET,
        
        /** パスワードロックのためにExcelブック情報のロードに成功していないことを表します。 */
        UNKNOWN_PASSWORD,
        
        /** パスワード以外の何らかの原因でExcelブック情報のロードに失敗したことを表します。 */
        LOAD_FAILED;
        
        // [instance members] --------------------------------------------------
    }
    
    /**
     * ロード済みとマークされたExcelブック情報を返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @param sheetNames Excelブックに含まれるシート名
     * @return ロード済みとマークされたExcelブック情報
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static BookInfo ofLoadCompleted(Path bookPath, List<String> sheetNames) {
        Objects.requireNonNull(bookPath);
        Objects.requireNonNull(sheetNames);
        
        return new BookInfo(bookPath, sheetNames, Status.LOAD_COMPLETED);
    }
    
    /**
     * ロード未試行とマークされたExcelブック情報を返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return ロード未試行とマークされたExcelブック情報
     */
    public static BookInfo ofNotYet(Path bookPath) {
        Objects.requireNonNull(bookPath);
        
        return new BookInfo(bookPath, List.of(), Status.NOT_YET);
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ<br>
     * 
     * @param bookPath Excelブックのパス
     * @param sheetNames Excelブックに含まれるシート名
     * @param status このExcelブック情報の状態
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public BookInfo(
            Path bookPath,
            List<String> sheetNames,
            Status status) {
        
        Objects.requireNonNull(bookPath);
        Objects.requireNonNull(sheetNames);
        Objects.requireNonNull(status);
        
        this.bookPath = bookPath;
        this.sheetNames = List.copyOf(sheetNames);
        this.status = status;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof BookInfo other) {
            return Objects.equals(bookPath, other.bookPath);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(bookPath);
    }
    
    @Override
    public String toString() {
        return bookPath.getFileName().toString();
    }
}
