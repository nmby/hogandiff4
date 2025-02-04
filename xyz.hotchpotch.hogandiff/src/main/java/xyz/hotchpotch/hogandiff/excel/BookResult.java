package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.SheetResult.Piece;
import xyz.hotchpotch.hogandiff.excel.SheetResult.SheetStats;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excepブック同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 * 
 * @param bookComparison Excelブック比較情報
 * @param sheetResults Excelシート同士の比較結果（片側だけの欠損ペアも含む）
 */
public record BookResult(
        BookComparison bookComparison,
        Map<Pair<String>, Optional<SheetResult>> sheetResults)
        implements Result {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * シート名ペアをユーザー表示用に整形して返します。<br>
     * 
     * @param id シート名ペアの識別子。
     * @param sheetNamePair シート名ペア
     * @return シート名ペアの整形済み文字列
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static String formatSheetNamesPair(
            String id,
            Pair<String> sheetNamePair) {
        
        Objects.requireNonNull(id);
        Objects.requireNonNull(sheetNamePair);
        
        //ResourceBundle rb = AppMain.appResource.get();
        
        return "    %s) %s  vs  %s".formatted(
                id,
                sheetNamePair.hasA() ? "A[ " + sheetNamePair.a() + " ]" : rb.getString("excel.BResult.010"),
                sheetNamePair.hasB() ? "B[ " + sheetNamePair.b() + " ]" : rb.getString("excel.BResult.010"));
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ<br>
     * 
     * @param bookComparison Excelブック比較情報
     * @param sheetResults Excelシート同士の比較結果（片側だけの欠損ペアも含む）
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public BookResult(
            BookComparison bookComparison,
            Map<Pair<String>, Optional<SheetResult>> sheetResults) {
        
        Objects.requireNonNull(bookComparison);
        Objects.requireNonNull(sheetResults);
        
        this.bookComparison = bookComparison;
        this.sheetResults = Map.copyOf(sheetResults);
    }
    
    /**
     * 片側のExcelブックについての差分内容を返します。<br>
     * 
     * @param side Excelブックの側
     * @return 片側のExcelブックについての差分内容（シート名とそのシート上の差分個所のマップ）
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public Map<String, Optional<Piece>> getPiece(Side side) {
        Objects.requireNonNull(side);
        
        return sheetResults.entrySet().stream()
                .filter(entry -> entry.getKey().has(side))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().get(side),
                        entry -> entry.getValue().map(s -> s.getPiece(side))));
    }
    
    /**
     * この比較結果における差分の有無を返します。<br>
     * 
     * @return 差分ありの場合は {@code true}
     */
    public boolean hasDiff() {
        return bookComparison.childSheetNamePairs().stream()
                .map(sheetResults::get)
                .anyMatch(r -> r.isEmpty() || r.get().hasDiff());
    }
    
    /**
     * 比較結果を端的に表す差分サマリを返します。<br>
     * 
     * @return 比較結果を端的に表す差分サマリ
     */
    public String getDiffSimpleSummary() {
        int diffSheets = (int) bookComparison.childSheetNamePairs().stream()
                .filter(Pair::isPaired)
                .map(sheetResults::get)
                .map(Optional::get)
                .filter(SheetResult::hasDiff)
                .count();
        int gapSheets = (int) bookComparison.childSheetNamePairs().stream()
                .filter(Predicate.not(Pair::isPaired))
                .count();
        
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
    
    private String getDiffText(Function<SheetResult, String> diffDescriptor) {
        StringBuilder str = new StringBuilder();
        
        for (int i = 0; i < bookComparison.childSheetNamePairs().size(); i++) {
            Pair<String> sheetNamePair = bookComparison.childSheetNamePairs().get(i);
            Optional<SheetResult> sResult = sheetResults.get(sheetNamePair);
            
            if (!sheetNamePair.isPaired() || sResult.isEmpty() || !sResult.get().hasDiff()) {
                continue;
            }
            
            str.append(formatSheetNamesPair(Integer.toString(i + 1), sheetNamePair));
            str.append(diffDescriptor.apply(sResult.get()));
            str.append(BR);
        }
        
        return str.isEmpty()
                ? "    " + rb.getString("excel.BResult.020") + BR + BR
                : str.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        if (bookComparison.parentBookInfoPair().isIdentical()) {
            str.append(rb.getString("excel.BResult.050").formatted(""))
                    .append(bookComparison.parentBookInfoPair().a().bookPath()).append(BR);
        } else {
            str.append(rb.getString("excel.BResult.050").formatted("A"))
                    .append(bookComparison.parentBookInfoPair().a().bookPath()).append(BR);
            str.append(rb.getString("excel.BResult.050").formatted("B"))
                    .append(bookComparison.parentBookInfoPair().b().bookPath()).append(BR);
        }
        
        for (int i = 0; i < bookComparison.childSheetNamePairs().size(); i++) {
            Pair<String> sheetNamePair = bookComparison.childSheetNamePairs().get(i);
            str.append(formatSheetNamesPair(Integer.toString(i + 1), sheetNamePair)).append(BR);
        }
        
        str.append(BR);
        str.append(rb.getString("excel.BResult.060")).append(BR);
        str.append(getDiffSummary()).append(BR);
        str.append(rb.getString("excel.BResult.070")).append(BR);
        str.append(getDiffDetail());
        
        return str.toString();
    }
    
    @Override
    public List<SheetStats> sheetStats() {
        return sheetResults.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(SheetResult::sheetStats)
                .flatMap(List::stream)
                .toList();
    }
}
