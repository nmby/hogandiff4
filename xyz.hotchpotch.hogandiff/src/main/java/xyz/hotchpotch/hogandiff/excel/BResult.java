package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        this.sheetPairs = sheetPairs;
        this.results = results;
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
                .filter(entry -> entry.getKey().isPresent(side))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().get(side),
                        entry -> entry.getValue().map(s -> s.getPiece(side))));
    }
    
    private String getText(Function<SResult, String> func) {
        
        return sheetPairs.stream().map(pair -> {
            
            StringBuilder str = new StringBuilder();
            str.append(String.format("  - %s vs %s",
                    pair.isPresentA() ? "[" + pair.a() + "]" : "(なし)",
                    pair.isPresentB() ? "[" + pair.b() + "]" : "(なし)"))
                    .append(BR);
            
            if (pair.isPaired()) {
                str.append(func.apply(results.get(pair).get()).indent(8));
            }
            return str;
            
        }).collect(Collectors.joining(BR));
    }
    
    public String getDiffSummary() {
        String diffSummary = sheetPairs.stream()
                .filter(Pair::isPaired)
                .filter(pair -> results.get(pair).get().hasDiff())
                .map(pair -> {
                    
                    StringBuilder str = new StringBuilder();
                    SResult sResult = results.get(pair).get();
                    
                    str.append(String.format("  - [%s] vs [%s]   %s",
                            pair.a(),
                            pair.b(),
                            sResult.getDiffSummary()))
                            .append(BR);
                    
                    return str;
                    
                }).collect(Collectors.joining());
        
        return diffSummary.isEmpty() ? "（差分なし）" + BR : diffSummary;
    }
    
    /**
     * 比較結果のサマリを返します。<br>
     * 
     * @return 比較結果のサマリ
     */
    @Deprecated
    public String getSummary() {
        return getText(SResult::getSummary);
    }
    
    /**
     * 比較結果の詳細を返します。<br>
     * 
     * @return 比較結果の詳細
     */
    public String getDetail() {
        return getText(SResult::getDetail);
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        if (bookPath.isIdentical()) {
            str.append("ブック : ").append(bookPath.a()).append(BR);
        } else {
            str.append("ブックA : ").append(bookPath.a()).append(BR);
            str.append("ブックB : ").append(bookPath.b()).append(BR);
        }
        
        str.append(BR);
        str.append("■差分サマリ ---------------------------------------------------").append(BR);
        str.append(getDiffSummary()).append(BR);
        str.append("■詳細 ---------------------------------------------------------").append(BR);
        str.append(getDetail());
        
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
                    return String.format("@@ [%s] -> [%s] @@\n", sheetPair.a(), sheetPair.b())
                            + results.get(sheetPair).get().getDiff();
                    
                } else if (sheetPair.isOnlyA()) {
                    return String.format("@@ -[%s] @@\n", sheetPair.a());
                    
                } else if (sheetPair.isOnlyB()) {
                    return String.format("@@ +[%s] @@\n", sheetPair.b());
                    
                } else {
                    throw new AssertionError();
                }
            };
            
            str.append(sheetPairs.stream().map(sheetPairToStr).collect(Collectors.joining(BR)));
        }
        
        return str.toString();
    }
}
