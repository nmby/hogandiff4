package xyz.hotchpotch.hogandiff.excel;

import java.util.Objects;

import org.apache.poi.ss.util.CellReference;

import xyz.hotchpotch.hogandiff.util.IntPair;

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
            throw new IndexOutOfBoundsException("row:%d, column:%d".formatted(row, column));
        }
        return "%S%d".formatted(columnIdxToStr(column), row + 1);
    }
    
    /**
     * セルアドレス（{@code "A1"} 形式）を
     * 行・列のインデックス（{@code (0, 0)} 形式）に変換します。<br>
     * 
     * @param address セルアドレス（{@code "A1"} 形式）
     * @return 行・列のインデックスのペア
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static IntPair addressToIdx(String address) {
        Objects.requireNonNull(address);
        
        int i = 0;
        for (; i < address.length(); i++) {
            if (Character.isDigit(address.charAt(i))) {
                break;
            }
        }
        if (i == 0 || address.length() <= i) {
            throw new IllegalArgumentException(address);
        }
        
        String colStr = address.substring(0, i);
        String rowStr = address.substring(i);
        
        return IntPair.of(
                Integer.parseInt(rowStr) - 1,
                CellReference.convertColStringToIndex(colStr));
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
            throw new IndexOutOfBoundsException("column:%d".formatted(column));
        }
        
        return CellReference.convertNumToColString(column);
    }
    
    /**
     * 列の記号（{@code "A"} など）を列のインデックス（{@code 0} など）に変換します。<br>
     * 
     * @param columnStr 列の記号（{@code "A"} など）
     * @return 列インデックス（0 開始）
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static int columnStrToIdx(String columnStr) {
        Objects.requireNonNull(columnStr);
        
        return CellReference.convertColStringToIndex(columnStr);
    }
    
    // [instance members] ******************************************************
    
    private CellsUtil() {
    }
}
