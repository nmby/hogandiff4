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
import xyz.hotchpotch.hogandiff.excel.SResult.Piece;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excepブック同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public class BResult {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
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
        if (idx < 0) {
            throw new IndexOutOfBoundsException(idx);
        }
        Objects.requireNonNull(pair, "pair");
        
        ResourceBundle rb = AppMain.appResource.get();
        
        return "    %d) %s vs %s".formatted(
                idx + 1,
                pair.hasA() ? "A[" + pair.a() + "]" : rb.getString("excel.BResult.010"),
                pair.hasB() ? "B[" + pair.b() + "]" : rb.getString("excel.BResult.010"));
    }
    
    /**
     * Excelブック同士の比較結果を生成して返します。<br>
     * 
     * @param bookPath1 比較対象Excelブックのパス1
     * @param bookPath2 比較対象Excelブックのパス2
     * @param sheetPairs 比較したシート名のペア（片側だけの欠損ペアも含む）
     * @param results Excelシート同士の比較結果（片側だけの欠損ペアも含む）
     * @return Excelブック同士の比較結果
     * @throws NullPointerException
     *          {@code bookPath1}, {@code bookPath2}, {@code sheetPairs}, {@code results}
     *          のいずれかが {@code null} の場合
     */
    public static BResult of(
            Path bookPath1,
            Path bookPath2,
            List<Pair<String>> sheetPairs,
            Map<Pair<String>, Optional<SResult>> results) {
        
        Objects.requireNonNull(bookPath1, "bookPath1");
        Objects.requireNonNull(bookPath2, "bookPath2");
        Objects.requireNonNull(sheetPairs, "sheetPairs");
        Objects.requireNonNull(results, "results");
        
        return new BResult(bookPath1, bookPath2, sheetPairs, results);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<Path> bookPath;
    private final List<Pair<String>> sheetPairs;
    private final Map<Pair<String>, Optional<SResult>> results;
    private final ResourceBundle rb = AppMain.appResource.get();
    
    private BResult(
            Path bookPath1,
            Path bookPath2,
            List<Pair<String>> sheetPairs,
            Map<Pair<String>, Optional<SResult>> results) {
        
        assert bookPath1 != null;
        assert bookPath2 != null;
        assert sheetPairs != null;
        assert results != null;
        
        this.bookPath = Pair.of(bookPath1, bookPath2);
        this.sheetPairs = List.copyOf(sheetPairs);
        this.results = Map.copyOf(results);
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
    
    private String getDiffText(Function<SResult, String> diffDescriptor) {
        StringBuilder str = new StringBuilder();
        
        for (int i = 0; i < sheetPairs.size(); i++) {
            Pair<String> pair = sheetPairs.get(i);
            Optional<SResult> sResult = results.get(pair);
            
            if (!pair.isPaired() || sResult.isEmpty() || !sResult.get().hasDiff()) {
                continue;
            }
            
            str.append(formatSheetNamesPair(i, pair));
            str.append(diffDescriptor.apply(sResult.get()));
            str.append(BR);
        }
        
        return str.isEmpty() ? "    " + rb.getString("excel.BResult.020") + BR : str.toString();
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
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        if (bookPath.isIdentical()) {
            str.append(rb.getString("excel.BResult.030").formatted("")).append(bookPath.a()).append(BR);
        } else {
            str.append(rb.getString("excel.BResult.030").formatted("A")).append(bookPath.a()).append(BR);
            str.append(rb.getString("excel.BResult.030").formatted("B")).append(bookPath.b()).append(BR);
        }
        
        for (int i = 0; i < sheetPairs.size(); i++) {
            Pair<String> pair = sheetPairs.get(i);
            str.append(formatSheetNamesPair(i, pair)).append(BR);
        }
        
        str.append(BR);
        str.append(rb.getString("excel.BResult.040")).append(BR);
        str.append(getDiffSummary()).append(BR);
        str.append(rb.getString("excel.BResult.050")).append(BR);
        str.append(getDiffDetail());
        
        return str.toString();
    }
    
    /**
     * 比較結果のコマンドライン出力用文字列を返します。<br>
     * 
     * @return 比較結果のコマンドライン出力用文字列
     */
    public String getDiff() {
        StringBuilder str = new StringBuilder();
        
        if (bookPath.isIdentical()) {
            str.append("--- ").append(bookPath.a()).append(sheetPairs.get(0).a()).append(BR);
            str.append("+++ ").append(bookPath.b()).append(sheetPairs.get(0).b()).append(BR);
            str.append(BR);
            str.append(results.get(sheetPairs.get(0)).get().getDiff());
            
        } else {
            str.append("--- ").append(bookPath.a()).append(BR);
            str.append("+++ ").append(bookPath.b()).append(BR);
            str.append(BR);
            
            Function<Pair<String>, String> sheetPairToStr = sheetPair -> {
                if (sheetPair.isPaired()) {
                    return "@@ [%s] -> [%s] @@%n".formatted(sheetPair.a(), sheetPair.b())
                            + results.get(sheetPair).get().getDiff();
                    
                } else if (sheetPair.isOnlyA()) {
                    return "@@ -[%s] @@%n".formatted(sheetPair.a());
                    
                } else if (sheetPair.isOnlyB()) {
                    return "@@ +[%s] @@%n".formatted(sheetPair.b());
                    
                } else {
                    throw new AssertionError();
                }
            };
            
            str.append(sheetPairs.stream().map(sheetPairToStr).collect(Collectors.joining(BR)));
        }
        
        return str.toString();
    }
}
