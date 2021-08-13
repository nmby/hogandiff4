package xyz.hotchpotch.hogandiff.excel;

import java.util.Objects;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * Excelシート上のセルを表します。<br>
 *
 * @author nmby
 */
public interface CellData {
    
    // [static members] ********************************************************
    
    /**
     * 新たなセルレプリカを生成します。<br>
     * 
     * @param row 行インデックス（0開始）
     * @param column 列インデックス（0開始）
     * @param content セル内容
     * @return 新たなセルレプリカ
     * @throws NullPointerException {@code content} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static CellData of(int row, int column, String content) {
        Objects.requireNonNull(content, "content");
        if (row < 0 || column < 0) {
            throw new IndexOutOfBoundsException(String.format("(%d, %d)", row, column));
        }
        
        return new CellStringData(row, column, content, null);
    }
    
    /**
     * 新たなセルレプリカを生成します。<br>
     * 
     * @param address セルアドレス（{@code "A1"} 形式）
     * @param content セル内容
     * @return 新たなセルレプリカ
     * @throws NullPointerException {@code address}, {@code content} のいずれかが {@code null} の場合
     */
    public static CellData of(String address, String content) {
        Objects.requireNonNull(address, "address");
        
        Pair<Integer> idx = CellsUtil.addressToIdx(address);
        return CellData.of(idx.a(), idx.b(), content);
    }
    
    /**
     * 新たな空のセルレプリカを生成します。<br>
     * 
     * @param row 行インデックス（0開始）
     * @param column 列インデックス（0開始）
     * @return 新たな空のセルレプリカ
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static CellData empty(int row, int column) {
        return CellData.of(row, column, "");
    }
    
    /**
     * 新たな空のセルレプリカを生成します。<br>
     * 
     * @param address セルアドレス（{@code "A1"} 形式）
     * @return 新たな空のセルレプリカ
     * @throws NullPointerException {@code address} が {@code null} の場合
     */
    public static CellData empty(String address) {
        return CellData.of(address, "");
    }
    
    // [instance members] ******************************************************
    
    int row();
    
    int column();
    
    /**
     * セルアドレス（{@code "A1"} 形式）を返します。<br>
     * 
     * @return セルアドレス（{@code "A1"} 形式）
     */
    default String address() {
        return CellsUtil.idxToAddress(row(), column());
    }
    
    String content();
    
    String comment();
    
    CellData addComment(String comment);
    
    /**
     * {@code #row()}, {@code #column()} を除く属性について、
     * このセルと指定されたセルの属性値が等しいかを返します。<br>
     * 
     * @param cell 比較対象のセル（{@code null} 許容）
     * @return {@code cell} が {@code null} でなく属性値が等しい場合は {@code true}
     */
    default boolean dataEquals(CellData cell) {
        return cell != null
                && Objects.equals(content(), cell.content())
                && Objects.equals(comment(), cell.comment());
    }
    
    /**
     * {@code #row()}, {@code #column()} を除く属性について、
     * このセルと指定されたセルの属性値の大小関係を返します。<br>
     * 
     * @param cell 比較対象のセル
     * @return このセルの属性値が指定されたセルの属性値より
     *          小さい場合は負の整数、等しい場合はゼロ、大きい場合は正の整数
     * @throws NullPointerException {@code cell} が {@code null} の場合
     */
    default int dataCompareTo(CellData cell) {
        Objects.requireNonNull(cell, "cell");
        
        return !content().equals(cell.content())
                ? content().compareTo(cell.content())
                : comment() != null && cell.comment() != null
                        ? comment().compareTo(cell.comment())
                        : 0;
    }
}
