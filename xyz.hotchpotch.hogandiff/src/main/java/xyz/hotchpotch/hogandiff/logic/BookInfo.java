package xyz.hotchpotch.hogandiff.logic;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo;

/**
 * Excelブック情報を表す不変クラスです。<br>
 * 
 * @param bookPath Excelブックのローカルパス
 * @param sheetNames Excelブックに含まれるシート名
 * @param status このExcelブック情報の状態
 * @param googleFileInfo GoogleDrive上のファイル情報。ローカルファイルの場合は {@code null}
 * @author nmby
 */
public record BookInfo(
        Path bookPath,
        List<String> sheetNames,
        Status status,
        GoogleFileInfo googleFileInfo) {
    
    // [static members] ********************************************************
    
    /**
     * Excelブック情報の状態を表す列挙型です。<br>
     */
    public static enum Status {
        
        // [static members] ----------------------------------------------------
        
        /** Excelブック情報をロード済みであることを表します。 */
        LOAD_COMPLETED,
        
        /** パスワード以外の何らかの原因でExcelブック情報のロードに失敗したことを表します。 */
        LOAD_FAILED,
        
        /** パスワードロックのためにExcelブック情報のロードに成功していないことを表します。 */
        NEEDS_PASSWORD;
        
        // [instance members] --------------------------------------------------
    }
    
    /**
     * ロード済みとマークされたExcelブック情報を返します。<br>
     * 
     * @param bookPath   Excelブックのパス
     * @param sheetNames Excelブックに含まれるシート名
     * @return ロード済みとマークされたExcelブック情報
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static BookInfo ofLoadCompleted(Path bookPath, List<String> sheetNames) {
        Objects.requireNonNull(bookPath);
        Objects.requireNonNull(sheetNames);
        
        return new BookInfo(bookPath, sheetNames, Status.LOAD_COMPLETED, null);
    }
    
    /**
     * パスワード以外の何らかの減でロード失敗とマークされたExcelブック情報を返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return パスワード以外の何らかの減でロード失敗とマークされたExcelブック情報
     */
    public static BookInfo ofLoadFailed(Path bookPath) {
        Objects.requireNonNull(bookPath);
        
        return new BookInfo(bookPath, List.of(), Status.LOAD_FAILED, null);
    }
    
    /**
     * パスワードロックのためロード未成功とマークされたExcelブック情報を返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return パスワードロックのためロード未成功とマークされたExcelブック情報
     */
    public static BookInfo ofNeedsPassword(Path bookPath) {
        Objects.requireNonNull(bookPath);
        
        return new BookInfo(bookPath, List.of(), Status.NEEDS_PASSWORD, null);
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ。<br>
     * 
     * @param bookPath Excelブックのローカルパス
     * @param sheetNames Excelブックに含まれるシート名
     * @param status このExcelブック情報の状態
     * @param googleFileInfo GoogleDrive上のファイル情報。ローカルファイルの場合は {@code null}
     * @throws NullPointerException パラメータが {@code null} の場合（ただし {@code googleFileInfo} は除く）
     */
    public BookInfo {
        Objects.requireNonNull(bookPath);
        Objects.requireNonNull(sheetNames);
        Objects.requireNonNull(status);
        // googleFileInfo は null 許容
        
        sheetNames = List.copyOf(sheetNames);
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
        return bookName();
    }
    
    /**
     * Excelブック名、Googleスプレッドシートファイル名を返します。<br>
     * Googleスプレッドシートの場合は拡張子の無いファイル名が返されます。<br>
     * 
     * @return Excelブック名／Googleスプレッドシートファイル名
     */
    public String bookName() {
        return googleFileInfo == null
                ? bookPath.getFileName().toString()
                : googleFileInfo.metadata().name();
    }
    
    /**
     * 比較結果として出力すべきファイル名を返します。<br>
     * Googleスプレッドシートは拡張子を持たないため、ローカル保存ように拡張子を追加して返します。<br>
     * 
     * @return 比較結果として出力すべきファイル名
     */
    public String bookNameWithExtension() {
        String bookName = bookName();
        try {
            BookType.of(Path.of(bookName));
            return bookName;
        } catch (IllegalArgumentException e) {
            return bookName + ".xlsx";
        }
    }
    
    /**
     * ユーザー向けに表示するパス情報を返します。<br>
     * ローカルファイルの場合はパスを、GoogleDrive上のファイルの場合はファイルIDとリビジョン名を返します。<br>
     * 
     * @return ユーザー向けに表示するパス情報
     */
    public String dispPathInfo() {
        return googleFileInfo == null
                ? bookPath.toString()
                : "GoogleDrive :  %s  [%s]".formatted(googleFileInfo.metadata().name(), googleFileInfo.revision().desc());
    }
    
    /**
     * このExcelブック情報にGoogleDrive上のファイル情報を設定した新しいインスタンスを返します。<br>
     * 
     * @param googleFileInfo GoogleDrive上のファイル情報
     * @return GoogleDrive上のファイル情報を設定した新しいインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     * @throws IllegalStateException 既にGoogleDrive上のファイル情報が設定されている場合
     */
    public BookInfo withGoogleFileInfo(GoogleFileInfo googleFileInfo) {
        Objects.requireNonNull(googleFileInfo);
        if (this.googleFileInfo != null) {
            throw new IllegalStateException("googleFileInfo is already set.");
        }
        return new BookInfo(bookPath, sheetNames, status, googleFileInfo);
    }
}
