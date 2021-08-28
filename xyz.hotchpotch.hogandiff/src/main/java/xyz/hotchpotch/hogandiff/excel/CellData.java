package xyz.hotchpotch.hogandiff.excel;

import java.util.Objects;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * Excelシート上のセルを表します。<br>
 *
 * @author nmby
 */
public interface CellData {
    
    // [static members] ********************************************************
    
    /**
     * 新たなセルデータを生成します。<br>
     * 
     * @param row 行インデックス（0開始）
     * @param column 列インデックス（0開始）
     * @param content セル内容
     * @param saveMemory 省メモリモードの場合は {@code true}
     * @return 新たなセルデータ
     * @throws NullPointerException {@code content} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static CellData of(
            int row,
            int column,
            String content,
            boolean saveMemory) {
        
        Objects.requireNonNull(content, "content");
        if (row < 0 || column < 0) {
            throw new IndexOutOfBoundsException(String.format("(%d, %d)", row, column));
        }
        
        return saveMemory
                ? new CellHashData(row, column, content.hashCode(), 0)
                : new CellStringData(row, column, content, null);
    }
    
    /**
     * 新たなセルデータを生成します。<br>
     * 
     * @param address セルアドレス（{@code "A1"} 形式）
     * @param content セル内容
     * @param saveMemory 省メモリモードの場合は {@code true}
     * @return 新たなセルデータ
     * @throws NullPointerException {@code address}, {@code content} のいずれかが {@code null} の場合
     */
    public static CellData of(
            String address,
            String content,
            boolean saveMemory) {
        
        Objects.requireNonNull(address, "address");
        
        IntPair idx = CellsUtil.addressToIdx(address);
        return CellData.of(idx.a(), idx.b(), content, saveMemory);
    }
    
    /**
     * 新たな空のセルデータを生成します。<br>
     * 
     * @param row 行インデックス（0開始）
     * @param column 列インデックス（0開始）
     * @param saveMemory 省メモリモードの場合は {@code true}
     * @return 新たな空のセルデータ
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static CellData empty(
            int row,
            int column,
            boolean saveMemory) {
        
        return CellData.of(row, column, "", saveMemory);
    }
    
    /**
     * 新たな空のセルデータを生成します。<br>
     * 
     * @param address セルアドレス（{@code "A1"} 形式）
     * @param saveMemory 省メモリモードの場合は {@code true}
     * @return 新たな空のセルデータ
     * @throws NullPointerException {@code address} が {@code null} の場合
     */
    public static CellData empty(
            String address,
            boolean saveMemory) {
        
        return CellData.of(address, "", saveMemory);
    }
    
    // [instance members] ******************************************************
    
    /**
     * 行インデックス（0開始）を返します。<br>
     * 
     * @return 行インデックス（0開始）
     */
    int row();
    
    /**
     * 列インデックス（0開始）を返します。<br>
     * 
     * @return 列インデックス（0開始）
     */
    int column();
    
    /**
     * セルアドレス（{@code "A1"} 形式）を返します。<br>
     * 
     * @return セルアドレス（{@code "A1"} 形式）
     */
    default String address() {
        return CellsUtil.idxToAddress(row(), column());
    }
    
    /**
     * このセルデータがセルコメントを保持するか否かを返します。<br>
     * 
     * @return セルコメントを保持する場合は {@code true}
     */
    boolean hasComment();
    
    /**
     * このセルデータにセルコメントを追加して出来るセルデータを新たに生成して返します。<br>
     * 
     * @param comment セルコメント
     * @return 新たなセルデータ
     */
    CellData addComment(String comment);
    
    /**
     * このセルデータと指定されたセルデータのセル内容が等価か否かを返します。<br>
     * 
     * @param cell 比較対象のセルデータ
     * @return セル内容が等価な場合は {@code true}
     */
    boolean contentEquals(CellData cell);
    
    /**
     * このセルデータと指定されたセルデータのセルコメントが等価か否かを返します。<br>
     * 
     * @param cell 比較対象のセルデータ
     * @return セルコメントが等価な場合は {@code true}
     */
    boolean commentEquals(CellData cell);
    
    /**
     * このセルデータと指定されたセルデータのデータ内容（セル内容とセルコメント）が等価か否かを返します。<br>
     * 
     * @param cell 比較対象のセルデータ
     * @return データ内容が等価な場合は {@code true}
     */
    default boolean dataEquals(CellData cell) {
        return contentEquals(cell) && commentEquals(cell);
    }
    
    /**
     * このセルデータと指定されたセルデータのデータ内容（セル内容とセルコメント）の大小関係を返します。<br>
     * 
     * @param cell 比較対象のセルデータ
     * @return このセルデータのデータ内容が小さい場合は負の整数、等しい場合はゼロ、大きい場合は正の整数
     */
    int dataCompareTo(CellData cell);
}
