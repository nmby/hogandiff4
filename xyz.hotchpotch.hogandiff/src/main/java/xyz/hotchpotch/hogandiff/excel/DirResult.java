package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;

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
     * @param bookId 親フォルダのペアを示す識別子。
     * @param i このExcelブック名ペアの番号。{@code i + 1} をユーザー向けに表示します。
     * @param pair Excelブック名ペア
     * @return Excelブック名ペアの整形済み文字列
     * @throws NullPointerException {@code id}, {@code pair} のいずれかが {@code null} の場合
     */
    public static String formatBookNamesPair(
            String bookId,
            int i,
            Pair<String> pair) {
        
        Objects.requireNonNull(bookId, "bookId");
        Objects.requireNonNull(pair, "pair");
        if (i < 0) {
            throw new IllegalArgumentException("i: %d".formatted(i));
        }
        
        ResourceBundle rb = AppMain.appResource.get();
        
        return "    %s  vs  %s".formatted(
                pair.hasA() ? "【A%s-%d】%s".formatted(bookId, i + 1, pair.a()) : rb.getString("excel.DResult.010"),
                pair.hasB() ? "【B%s-%d】%s".formatted(bookId, i + 1, pair.b()) : rb.getString("excel.DResult.010"));
    }
    
    public static DirResult of(
            Pair<DirInfo> dirPair,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, Optional<BookResult>> results) {
        
        Objects.requireNonNull(dirPair, "dirPair");
        Objects.requireNonNull(bookNamePairs, "bookNamePairs");
        Objects.requireNonNull(results, "results");
        
        return new DirResult(dirPair, bookNamePairs, results);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> dirPair;
    private final List<Pair<String>> bookNamePairs;
    private final Map<Pair<String>, Optional<BookResult>> results;
    private final ResourceBundle rb = AppMain.appResource.get();
    
    private DirResult(
            Pair<DirInfo> dirPair,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, Optional<BookResult>> results) {
        
        assert dirPair != null;
        assert bookNamePairs != null;
        
        this.dirPair = dirPair;
        this.bookNamePairs = List.copyOf(bookNamePairs);
        this.results = Map.copyOf(results);
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        str.append(rb.getString("excel.DResult.020").formatted("A"))
                .append(dirPair.a().path())
                .append(BR);
        str.append(rb.getString("excel.DResult.020").formatted("B"))
                .append(dirPair.b().path())
                .append(BR);
        
        for (int i = 0; i < bookNamePairs.size(); i++) {
            Pair<String> bookNamePair = bookNamePairs.get(i);
            str.append(formatBookNamesPair("", i, bookNamePair)).append(BR);
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
                : rb.getString("excel.DResult.050")),
                false);
    }
    
    public String getDiffDetail() {
        return getDiffText(bResult -> bResult.isPresent()
                ? BR + bResult.get().getDiffDetail().indent(4).replace("\n", BR)
                : BR + "        " + rb.getString("excel.DResult.050") + BR + BR,
                true);
    }
    
    private String getDiffText(
            Function<Optional<BookResult>, String> diffDescriptor,
            boolean isDetailMode) {
        
        StringBuilder str = new StringBuilder();
        
        if (results.isEmpty()) {
            str.append("    - ").append(rb.getString("excel.DResult.100")).append(BR);
            if (isDetailMode) {
                // TODO: とても不細工なのでどうにかしたい
                str.append(BR);
            }
            return str.toString();
        }
        
        for (int i = 0; i < bookNamePairs.size(); i++) {
            Pair<String> bookNamePair = bookNamePairs.get(i);
            Optional<BookResult> bResult = results.get(bookNamePair);
            
            str.append(formatBookNamesPair("", i, bookNamePair));
            
            if (bookNamePair.isPaired()) {
                str.append(diffDescriptor.apply(bResult));
            } else {
                str.append(BR);
                if (isDetailMode) {
                    // TODO: とても不細工なのでどうにかしたい
                    str.append(BR);
                }
            }
        }
        return str.toString();
    }
    
    public String getDiffSimpleSummary() {
        if (results.isEmpty()) {
            return rb.getString("excel.DResult.100");
        }
        
        int diffBooks = (int) results.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(BookResult::hasDiff)
                .count();
        int gapBooks = (int) bookNamePairs.stream()
                .filter(Predicate.not(Pair::isPaired))
                .count();
        int failed = (int) bookNamePairs.stream()
                .filter(Pair::isPaired)
                .filter(p -> !results.containsKey(p) || results.get(p).isEmpty())
                .count();
        
        if (diffBooks == 0 && gapBooks == 0 && failed == 0) {
            return rb.getString("excel.DResult.060");
        }
        
        StringBuilder str = new StringBuilder();
        if (0 < diffBooks) {
            str.append(rb.getString("excel.DResult.070").formatted(diffBooks));
        }
        if (0 < gapBooks) {
            if (!str.isEmpty()) {
                str.append(", ");
            }
            str.append(rb.getString("excel.DResult.080").formatted(gapBooks));
        }
        if (0 < failed) {
            if (!str.isEmpty()) {
                str.append(", ");
            }
            str.append(rb.getString("excel.DResult.090").formatted(failed));
        }
        
        return str.toString();
    }
}
