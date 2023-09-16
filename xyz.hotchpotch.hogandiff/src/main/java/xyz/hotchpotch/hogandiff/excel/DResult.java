package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダ同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public class DResult {
    
    // [static members] ********************************************************
    
    /**
     * Excelブック名ペアをユーザー表示用に整形して返します。<br>
     * 
     * @param idx Excelブック名ペアのインデックス。{@code idx + 1} が番号として表示されます。
     * @param pair Excelブック名ペア
     * @return Excelブック名ペアの整形済み文字列
     * @throws NullPointerException {@code pair} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code idx} が {@code 0} 未満の場合
     */
    public static String formatBookNamesPair(int idx, Pair<String> pair) {
        Objects.requireNonNull(pair, "pair");
        if (idx < 0) {
            throw new IndexOutOfBoundsException(idx);
        }
        
        ResourceBundle rb = AppMain.appResource.get();
        
        return "    %d) %s vs %s".formatted(
                idx + 1,
                pair.hasA() ? "A[" + pair.a() + "]" : rb.getString("excel.DResult.010"),
                pair.hasB() ? "B[" + pair.b() + "]" : rb.getString("excel.DResult.010"));
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirData> dirData;
    private final List<Pair<String>> bookNamePairs;
    private final Map<Pair<String>, Optional<BResult>> results;
    
    private DResult(
            DirData dirData1,
            DirData dirData2,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, Optional<BResult>> results) {
        
        assert dirData1 != null;
        assert dirData2 != null;
        assert bookNamePairs != null;
        
        this.dirData = Pair.of(dirData1, dirData2);
        this.bookNamePairs = List.copyOf(bookNamePairs);
        this.results = Map.copyOf(results);
    }
}
