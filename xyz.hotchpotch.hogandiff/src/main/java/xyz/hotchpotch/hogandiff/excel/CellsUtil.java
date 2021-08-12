package xyz.hotchpotch.hogandiff.excel;

import java.util.Objects;

import org.apache.poi.ss.util.CellAddress;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * セルに関するユーティリティクラスです。<br>
 *
 * @author nmby
 */
public class CellsUtil {
    
    // [static members] ********************************************************
    
    /**
     * 行・列のインデックス（{@code (0, 0)} 形式）を
     * セルアドレス（{@code "A1"} 形式）に変換します。<br>
     * 
     * @param row 行インデックス（0 開始）
     * @param column 列インデックス（0 開始）
     * @return セルアドレス（{@code "A1"} 形式）
     * @throws IndexOutOfBoundsException {@code row}, {@code column} のいずれかが 0 未満の場合
     */
    public static String idxToAddress(int row, int column) {
        if (row < 0 || column < 0) {
            throw new IndexOutOfBoundsException(String.format("row:%d, column:%d", row, column));
        }
        return new CellAddress(row, column).formatAsString();
    }
    
    /**
     * セルアドレス（{@code "A1"} 形式）を
     * 行・列のインデックス（{@code (0, 0)} 形式）に変換します。<br>
     * 
     * @param address セルアドレス（{@code "A1"} 形式）
     * @return 行・列のインデックスのペア
     * @throws NullPointerException {@code address} が {@code null} の場合
     */
    public static Pair<Integer> addressToIdx(String address) {
        Objects.requireNonNull(address, "address");
        
        CellAddress ca = new CellAddress(address);
        return Pair.of(ca.getRow(), ca.getColumn());
    }
    
    /**
     * 列のインデックス（{@code 0} など）を記号（{@code "A"} など）に変換します。<br>
     * 
     * @param column 列インデックス（0 開始）
     * @return 列の記号（{@code "A"} など）
     * @throws IndexOutOfBoundsException {@code column} が 0 未満の場合
     */
    public static String columnIdxToStr(int column) {
        if (column < 0) {
            throw new IndexOutOfBoundsException(String.format("column:%d", column));
        }
        
        String address = new CellAddress(0, column).formatAsString();
        return address.substring(0, address.length() - 1);
    }
    
    /**
     * 列の記号（{@code "A"} など）を列のインデックス（{@code 0} など）に変換します。<br>
     * 
     * @param columnStr 列の記号（{@code "A"} など）
     * @return 列インデックス（0 開始）
     * @throws NullPointerException {@code columnStr} が {@code null} の場合
     */
    public static int columnStrToIdx(String columnStr) {
        Objects.requireNonNull(columnStr, "columnStr");
        
        CellAddress ca = new CellAddress(columnStr + "1");
        return ca.getColumn();
    }
    
    /**
     * 指定された2つのセルの {@code #row()}, {@code #column()} を除く属性値が等しいかを返します。<br>
     * 
     * @param cell1 比較対象のセル1（{@code null} 許容）
     * @param cell2 比較対象のセル2（{@code null} 許容）
     * @return {@code cell1}, {@code cell2} が同一か属性値が等しい場合は {@code true}
     */
    public static boolean attrEquals(CellReplica cell1, CellReplica cell2) {
        return cell1 == cell2 || (cell1 != null && cell1.attrEquals(cell2));
    }
    
    /**
     * 指定された2つのセルの {@code #row()}, {@code #column()} を除く属性値の大小関係を返します。<br>
     * 
     * @param cell1 比較対象のセル1
     * @param cell2 比較対象のセル2
     * @return セル1の属性値がセル2の属性値より小さい場合は負の整数、等しい場合は0、大きい場合は正の整数
     * @throws NullPointerException {@code cell1}, {@code cell2} のいずれかが {@code null} の場合
     */
    public static int attrCompare(CellReplica cell1, CellReplica cell2) {
        Objects.requireNonNull(cell1, "cell1");
        Objects.requireNonNull(cell2, "cell2");
        
        return cell1.attrCompareTo(cell2);
    }
    
    // [instance members] ******************************************************
    
    private CellsUtil() {
    }
}
