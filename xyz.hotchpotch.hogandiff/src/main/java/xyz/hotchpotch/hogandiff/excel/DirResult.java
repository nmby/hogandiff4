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
public class DirResult {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
    /**
     * Excelブック名ペアをユーザー表示用に整形して返します。<br>
     * 
     * @param id このExcelブック名ペアを示す識別子。
     * @param pair Excelブック名ペア
     * @return Excelブック名ペアの整形済み文字列
     * @throws NullPointerException {@code id}, {@code pair} のいずれかが {@code null} の場合
     */
    public static String formatBookNamesPair(String id, Pair<String> pair) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(pair, "pair");
        
        ResourceBundle rb = AppMain.appResource.get();
        
        return "    【%s】 %s  vs  %s".formatted(
                id,
                pair.hasA() ? "A【 " + pair.a() + " 】" : rb.getString("excel.DResult.010"),
                pair.hasB() ? "B【 " + pair.b() + " 】" : rb.getString("excel.DResult.010"));
    }
    
    public static DirResult of(
            // TODO: Pair<DirInfo>を取る形にした方がよい
            DirInfo dirInfo1,
            DirInfo dirInfo2,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, Optional<BookResult>> results) {
        
        Objects.requireNonNull(dirInfo1, "dirInfo1");
        Objects.requireNonNull(dirInfo2, "dirInfo2");
        Objects.requireNonNull(bookNamePairs, "bookNamePairs");
        Objects.requireNonNull(results, "results");
        
        return new DirResult(dirInfo1, dirInfo2, bookNamePairs, results);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> dirInfoPair;
    private final List<Pair<String>> bookNamePairs;
    private final Map<Pair<String>, Optional<BookResult>> results;
    private final ResourceBundle rb = AppMain.appResource.get();
    
    private DirResult(
            DirInfo dirInfo1,
            DirInfo dirInfo2,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, Optional<BookResult>> results) {
        
        assert dirInfo1 != null;
        assert dirInfo2 != null;
        assert bookNamePairs != null;
        
        this.dirInfoPair = Pair.of(dirInfo1, dirInfo2);
        this.bookNamePairs = List.copyOf(bookNamePairs);
        this.results = Map.copyOf(results);
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        str.append(rb.getString("excel.DResult.020").formatted("A"))
                .append(dirInfoPair.a().getPath())
                .append(BR);
        str.append(rb.getString("excel.DResult.020").formatted("B"))
                .append(dirInfoPair.b().getPath())
                .append(BR);
        
        for (int i = 0; i < bookNamePairs.size(); i++) {
            Pair<String> bookNamePair = bookNamePairs.get(i);
            str.append(formatBookNamesPair(String.valueOf(i), bookNamePair)).append(BR);
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
                : rb.getString("excel.DResult.050")));
    }
    
    private String getDiffDetail() {
        return getDiffText(bResult -> bResult.isPresent()
                ? BR + bResult.get().getDiffDetail().indent(4).replace("\n", BR)
                : "");
    }
    
    private String getDiffText(Function<Optional<BookResult>, String> diffDescriptor) {
        StringBuilder str = new StringBuilder();
        
        for (int i = 0; i < bookNamePairs.size(); i++) {
            Pair<String> bookNamePair = bookNamePairs.get(i);
            Optional<BookResult> bResult = results.get(bookNamePair);
            
            if (!bookNamePair.isPaired() || (bResult.isPresent() && !bResult.get().hasDiff())) {
                continue;
            }
            
            str.append(formatBookNamesPair(String.valueOf(i), bookNamePair));
            str.append(diffDescriptor.apply(bResult));
        }
        
        return str.isEmpty() ? "    " + rb.getString("excel.DResult.060") + BR : str.toString();
    }
}
