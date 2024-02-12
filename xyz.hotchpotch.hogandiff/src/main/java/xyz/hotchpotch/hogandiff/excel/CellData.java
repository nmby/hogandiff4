package xyz.hotchpotch.hogandiff.excel;

import java.util.Comparator;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * Excelシート上のセルを表します。<br>
 *
 * @author nmby
 */
public record CellData(
        int row,
        int column,
        String content,
        String comment) {
    
    // [static members] ********************************************************
    
    /**
     * 新たなセルデータを生成します。<br>
     * 
     * @param address セルアドレス（{@code "A1"} 形式）
     * @param content セル内容
     * @param comment セルコメント
     * @return 新たなセルデータ
     * @throws NullPointerException {@code address}, {@code content} のいずれかが {@code null} の場合
     */
    public static CellData of(String address, String content, String comment) {
        Objects.requireNonNull(address, "address");
        
        IntPair idx = CellsUtil.addressToIdx(address);
        return new CellData(idx.a(), idx.b(), content, comment);
    }
    
    /**
     * 新たな空のセルデータを生成します。<br>
     * 
     * @param row 行インデックス（0開始）
     * @param column 列インデックス（0開始）
     * @return 新たな空のセルデータ
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static CellData empty(int row, int column) {
        return new CellData(row, column, "", null);
    }
    
    // [instance members] ******************************************************
    
    public CellData {
        Objects.requireNonNull(content, "content");
        if (row < 0 || column < 0) {
            throw new IndexOutOfBoundsException("(%d, %d)".formatted(row, column));
        }
    }
    
    /**
     * セルアドレス（{@code "A1"} 形式）を返します。<br>
     * 
     * @return セルアドレス（{@code "A1"} 形式）
     */
    public String address() {
        return CellsUtil.idxToAddress(row(), column());
    }
    
    /**
     * このセルデータがセルコメントを保持するか否かを返します。<br>
     * 
     * @return セルコメントを保持する場合は {@code true}
     */
    public boolean hasComment() {
        return comment != null;
    }
    
    /**
     * このセルデータにセルコメントを追加して出来るセルデータを新たに生成して返します。<br>
     * 
     * @param comment セルコメント
     * @return 新たなセルデータ
     * @throws NullPointerException {@code comment} が {@code null} の場合
     * @throws IllegalStateException このセルデータが既にセルコメントを保持する場合
     */
    public CellData withComment(String comment) {
        Objects.requireNonNull(comment, "comment");
        if (this.comment != null) {
            throw new IllegalStateException();
        }
        
        return new CellData(row, column, content, comment);
    }
    
    /**
     * このセルデータと指定されたセルデータのセル内容が等価か否かを返します。<br>
     * 
     * @param cell 比較対象のセルデータ
     * @return セル内容が等価な場合は {@code true}
     * @throws NullPointerException {@code cell} が {@code null} の場合
     */
    public boolean contentEquals(CellData cell) {
        Objects.requireNonNull(cell, "cell");
        
        return content.equals(cell.content);
    }
    
    /**
     * このセルデータと指定されたセルデータのセルコメントが等価か否かを返します。<br>
     * 
     * @param cell 比較対象のセルデータ
     * @return セルコメントが等価な場合は {@code true}
     * @throws NullPointerException {@code cell} が {@code null} の場合
     */
    public boolean commentEquals(CellData cell) {
        return comment == null
                ? cell.comment == null
                : comment.equals(cell.comment);
    }
    
    /**
     * このセルデータと指定されたセルデータのデータ内容（セル内容とセルコメント）が等価か否かを返します。<br>
     * 
     * @param cell 比較対象のセルデータ
     * @return データ内容が等価な場合は {@code true}
     * @throws NullPointerException {@code cell} が {@code null} の場合
     */
    public boolean dataEquals(CellData cell) {
        return contentEquals(cell) && commentEquals(cell);
    }
    
    /**
     * このセルデータと指定されたセルデータのデータ内容（セル内容とセルコメント）の大小関係を返します。<br>
     * 
     * @param cell 比較対象のセルデータ
     * @return このセルデータのデータ内容が小さい場合は負の整数、等しい場合はゼロ、大きい場合は正の整数
     * @throws NullPointerException {@code cell} が {@code null} の場合
     */
    public int dataCompareTo(CellData cell) {
        return !contentEquals(cell)
                ? content.compareTo(cell.content)
                : Objects.compare(
                        comment,
                        cell.comment,
                        Comparator.nullsFirst(Comparator.naturalOrder()));
    }
    
    @Override
    public String toString() {
        return "%s: %s%s".formatted(
                address(),
                content,
                comment == null ? "" : " [comment: " + comment + "]");
    }
}
