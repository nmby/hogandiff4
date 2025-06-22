package xyz.hotchpotch.hogandiff.main.models;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Excelブック情報を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public class BookInfo {

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

        return new BookInfo(bookPath, sheetNames, Status.LOAD_COMPLETED);
    }

    /**
     * パスワード以外の何らかの減でロード失敗とマークされたExcelブック情報を返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return パスワード以外の何らかの減でロード失敗とマークされたExcelブック情報
     */
    public static BookInfo ofLoadFailed(Path bookPath) {
        Objects.requireNonNull(bookPath);

        return new BookInfo(bookPath, List.of(), Status.LOAD_FAILED);
    }

    /**
     * パスワードロックのためロード未成功とマークされたExcelブック情報を返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return パスワードロックのためロード未成功とマークされたExcelブック情報
     */
    public static BookInfo ofNeedsPassword(Path bookPath) {
        Objects.requireNonNull(bookPath);

        return new BookInfo(bookPath, List.of(), Status.NEEDS_PASSWORD);
    }

    // [instance members] ******************************************************

    private final Path bookPath;
    private final List<String> sheetNames;
    private final Status status;

    private BookInfo(
            Path bookPath,
            List<String> sheetNames,
            Status status) {

        assert bookPath != null;
        assert sheetNames != null;
        assert status != null;

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
        return bookName();
    }

    /** @return Excelブックのパス */
    public Path bookPath() {
        return bookPath;
    }

    /** @return Excelブックのファイル名 */
    public String bookName() {
        return bookPath.getFileName().toString();
    }

    /** @return Excelブックに含まれるシート名 */
    public List<String> sheetNames() {
        return sheetNames;
    }

    /** @return このExcelブック情報の状態 */
    public Status status() {
        return status;
    }
}
