package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダ同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public class DResult {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
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
        
        return "    【%d】 %s  vs  %s".formatted(
                idx + 1,
                pair.hasA() ? "A【 " + pair.a() + " 】" : rb.getString("excel.DResult.010"),
                pair.hasB() ? "B【 " + pair.b() + " 】" : rb.getString("excel.DResult.010"));
    }
    
    public static DResult of(
            DirData dirData1,
            DirData dirData2,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, Optional<BResult>> results) {
        
        Objects.requireNonNull(dirData1, "dirData1");
        Objects.requireNonNull(dirData2, "dirData2");
        Objects.requireNonNull(bookNamePairs, "bookNamePairs");
        Objects.requireNonNull(results, "results");
        
        return new DResult(dirData1, dirData2, bookNamePairs, results);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirData> dirData;
    private final List<Pair<String>> bookNamePairs;
    private final Map<Pair<String>, Optional<BResult>> results;
    private final ResourceBundle rb = AppMain.appResource.get();
    
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
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        str.append(rb.getString("excel.DResult.020").formatted("A")).append(dirData.a().path()).append(BR);
        str.append(rb.getString("excel.DResult.020").formatted("B")).append(dirData.b().path()).append(BR);
        
        for (int i = 0; i < bookNamePairs.size(); i++) {
            Pair<String> pair = bookNamePairs.get(i);
            str.append(formatBookNamesPair(i, pair)).append(BR);
        }
        
        str.append(BR);
        str.append(rb.getString("excel.DResult.030")).append(BR);
        str.append(getDiffSummary()).append(BR);
        str.append(rb.getString("excel.DResult.040")).append(BR);
        str.append(getDiffDetail());
        
        return str.toString();
    }
    
    private String getDiffSummary() {
        return getDiffText(bResult -> "  -  %s%n".formatted(bResult.isPresent()
                ? bResult.get().getDiffSimpleSummary()
                : rb.getString("excel.DResult.080")));
    }
    
    private String getDiffDetail() {
        return getDiffText(bResult -> bResult.isPresent()
                ? BR + bResult.get().getDiffDetail().indent(4).replace("\n", BR)
                : "");
    }
    
    private String getDiffText(Function<Optional<BResult>, String> diffDescriptor) {
        StringBuilder str = new StringBuilder();
        
        for (int i = 0; i < bookNamePairs.size(); i++) {
            Pair<String> pair = bookNamePairs.get(i);
            Optional<BResult> bResult = results.get(pair);
            
            if (!pair.isPaired() || (bResult.isPresent() && !bResult.get().hasDiff())) {
                continue;
            }
            
            str.append(formatBookNamesPair(i, pair));
            str.append(diffDescriptor.apply(bResult));
        }
        
        return str.isEmpty() ? "    " + rb.getString("excel.DResult.050") + BR : str.toString();
    }
}
