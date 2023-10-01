package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.SheetResult.Piece;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excepブック同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public record BookResult(
        Pair<Path> bookPaths,
        List<Pair<String>> sheetPairs,
        Map<Pair<String>, Optional<SheetResult>> results) {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * シート名ペアをユーザー表示用に整形して返します。<br>
     * 
     * @param idx シート名ペアのインデックス。{@code idx + 1} が番号として表示されます。
     * @param pair シート名ペア
     * @return シート名ペアの整形済み文字列
     * @throws NullPointerException {@code pair} が {@code null} の場合
     * @throws IndexOutOfBoundsException {@code idx} が {@code 0} 未満の場合
     */
    public static String formatSheetNamesPair(int idx, Pair<String> pair) {
        Objects.requireNonNull(pair, "pair");
        if (idx < 0) {
            throw new IndexOutOfBoundsException(idx);
        }
        
        //ResourceBundle rb = AppMain.appResource.get();
        
        return "    %d) %s  vs  %s".formatted(
                idx + 1,
                pair.hasA() ? "A[ " + pair.a() + " ]" : rb.getString("excel.BResult.010"),
                pair.hasB() ? "B[ " + pair.b() + " ]" : rb.getString("excel.BResult.010"));
    }
    
    // [instance members] ******************************************************
    
    /**
     * Excelブック同士の比較結果を生成して返します。<br>
     * 
     * @param bookPaths 比較対象Excelブックパスのペア（片側だけの欠損ペアもあり得る）
     * @param sheetPairs 比較したシート名のペア（片側だけの欠損ペアも含む）
     * @param results Excelシート同士の比較結果（片側だけの欠損ペアも含む）
     * @return Excelブック同士の比較結果
     * @throws NullPointerException
     *          {@code bookPaths}, {@code sheetPairs}, {@code results} のいずれかが {@code null} の場合
     */
    public BookResult(
            Pair<Path> bookPaths,
            List<Pair<String>> sheetPairs,
            Map<Pair<String>, Optional<SheetResult>> results) {
        
        Objects.requireNonNull(bookPaths, "bookPaths");
        Objects.requireNonNull(sheetPairs, "sheetPairs");
        Objects.requireNonNull(results, "results");
        
        this.bookPaths = bookPaths;
        this.sheetPairs = List.copyOf(sheetPairs);
        this.results = Map.copyOf(results);
    }
    
    /**
     * ひとつでも差分があるかを返します。<br>
     * 
     * @return ひとつでも差分がある場合は {@code true}
     */
    public boolean hasDiff() {
        return sheetPairs.stream()
                .map(results::get)
                .anyMatch(r -> r.isEmpty() || r.get().hasDiff());
    }
    
    /**
     * 片側のExcelブックについての差分内容を返します。<br>
     * 
     * @param side Excelブックの側
     * @return 片側のExcelブックについての差分内容（シート名とそのシート上の差分個所のマップ）
     */
    public Map<String, Optional<Piece>> getPiece(Side side) {
        Objects.requireNonNull(side, "side");
        
        return results.entrySet().stream()
                .filter(entry -> entry.getKey().has(side))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().get(side),
                        entry -> entry.getValue().map(s -> s.getPiece(side))));
    }
    
    private String getDiffText(Function<SheetResult, String> diffDescriptor) {
        StringBuilder str = new StringBuilder();
        
        for (int i = 0; i < sheetPairs.size(); i++) {
            Pair<String> pair = sheetPairs.get(i);
            Optional<SheetResult> sResult = results.get(pair);
            
            if (!pair.isPaired() || sResult.isEmpty() || !sResult.get().hasDiff()) {
                continue;
            }
            
            str.append(formatSheetNamesPair(i, pair));
            str.append(diffDescriptor.apply(sResult.get()));
            str.append(BR);
        }
        
        return str.isEmpty()
                ? "    " + rb.getString("excel.BResult.020") + BR
                : str.toString();
    }
    
    /**
     * 比較結果の差分サマリを返します。<br>
     * 
     * @return 比較結果の差分サマリ
     */
    public String getDiffSummary() {
        return getDiffText(sResult -> "  -  " + sResult.getDiffSummary());
    }
    
    /**
     * 比較結果の差分詳細を返します。<br>
     * 
     * @return 比較結果の差分詳細
     */
    public String getDiffDetail() {
        return getDiffText(sResult -> BR + sResult.getDiffDetail().indent(8).replace("\n", BR));
    }
    
    public String getDiffSimpleSummary() {
        int diffSheets = (int) sheetPairs.stream()
                .filter(Pair::isPaired).map(p -> results.get(p).get()).filter(SheetResult::hasDiff).count();
        int gapSheets = (int) sheetPairs.stream().filter(p -> !p.isPaired()).count();
        
        if (diffSheets == 0 && gapSheets == 0) {
            return rb.getString("excel.BResult.020");
        }
        
        StringBuilder str = new StringBuilder();
        if (0 < diffSheets) {
            str.append(rb.getString("excel.BResult.030").formatted(diffSheets));
        }
        if (0 < gapSheets) {
            if (!str.isEmpty()) {
                str.append(", ");
            }
            str.append(rb.getString("excel.BResult.040").formatted(gapSheets));
        }
        
        return str.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        if (bookPaths.isIdentical()) {
            str.append(rb.getString("excel.BResult.050").formatted("")).append(bookPaths.a()).append(BR);
        } else {
            str.append(rb.getString("excel.BResult.050").formatted("A")).append(bookPaths.a()).append(BR);
            str.append(rb.getString("excel.BResult.050").formatted("B")).append(bookPaths.b()).append(BR);
        }
        
        for (int i = 0; i < sheetPairs.size(); i++) {
            Pair<String> pair = sheetPairs.get(i);
            str.append(formatSheetNamesPair(i, pair)).append(BR);
        }
        
        str.append(BR);
        str.append(rb.getString("excel.BResult.060")).append(BR);
        str.append(getDiffSummary()).append(BR);
        str.append(rb.getString("excel.BResult.070")).append(BR);
        str.append(getDiffDetail());
        
        return str.toString();
    }
}
