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
 * 
 * @param dirPair 比較対象のフォルダ情報のペア
 * @param bookNamePairs 比較対象のExcelブック名のペアのリスト
 * @param bookResults Excelブック名のペアに対応するExcelブック同士の比較結果のマップ
 * @param dirId フォルダの識別番号
 */
public record DirResult(
        Pair<DirInfo> dirPair,
        List<Pair<String>> bookNamePairs,
        Map<Pair<String>, Optional<BookResult>> bookResults,
        String dirId) {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * Excelブック名ペアをユーザー表示用に整形して返します。<br>
     * 
     * @param dirId 親フォルダのペアを示す識別子。
     * @param i このExcelブック名ペアの番号。{@code i + 1} をユーザー向けに表示します。
     * @param pair Excelブック名ペア
     * @return Excelブック名ペアの整形済み文字列
     * @throws NullPointerException {@code id}, {@code pair} のいずれかが {@code null} の場合
     */
    public static String formatBookNamesPair(
            String dirId,
            int i,
            Pair<String> pair) {
        
        Objects.requireNonNull(dirId, "dirId");
        Objects.requireNonNull(pair, "pair");
        if (i < 0) {
            throw new IllegalArgumentException("i: %d".formatted(i));
        }
        
        return "    %s  vs  %s".formatted(
                pair.hasA() ? "【A%s-%d】%s".formatted(dirId, i + 1, pair.a()) : rb.getString("excel.DResult.010"),
                pair.hasB() ? "【B%s-%d】%s".formatted(dirId, i + 1, pair.b()) : rb.getString("excel.DResult.010"));
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ<br>
     * 
     * @param dirPair 比較対象のフォルダ情報のペア
     * @param bookNamePairs 比較対象のExcelブック名のペアのリスト
     * @param bookResults Excelブック名のペアに対応するExcelブック同士の比較結果のマップ
     * @param dirId フォルダの識別番号
     * @throws NullPointerException
     *          {@code dirPair}, {@code bookNamePairs}, {@code bookResults}, {@code dirId}
     *          のいずれかが {@code null} の場合
     */
    public DirResult(
            Pair<DirInfo> dirPair,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, Optional<BookResult>> bookResults,
            String dirId) {
        
        Objects.requireNonNull(dirPair, "dirPair");
        Objects.requireNonNull(bookNamePairs, "bookNamePairs");
        Objects.requireNonNull(bookResults, "bookResults");
        Objects.requireNonNull(dirId, "dirId");
        
        this.dirPair = dirPair;
        this.bookNamePairs = List.copyOf(bookNamePairs);
        this.bookResults = Map.copyOf(bookResults);
        this.dirId = dirId;
    }
    
    /**
     * 差分ありの場合は {@code true}<br>
     * 
     * @return 差分ありの場合は {@code true}
     */
    public boolean hasDiff() {
        return bookNamePairs.stream()
                .map(bookResults::get)
                .anyMatch(r -> r.isEmpty() || r.get().hasDiff());
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
            str.append(formatBookNamesPair(dirId, i, bookNamePair)).append(BR);
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
    
    /**
     * 差分内容の詳細を返します。<br>
     * 
     * @return 差分内容の詳細を表す文字列
     */
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
        
        if (bookResults.isEmpty()) {
            str.append("    - ").append(rb.getString("excel.DResult.100")).append(BR);
            if (isDetailMode) {
                // TODO: とても不細工なのでどうにかしたい
                str.append(BR);
            }
            return str.toString();
        }
        
        for (int i = 0; i < bookNamePairs.size(); i++) {
            Pair<String> bookNamePair = bookNamePairs.get(i);
            Optional<BookResult> bResult = bookResults.get(bookNamePair);
            
            str.append(formatBookNamesPair(dirId, i, bookNamePair));
            
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
    
    /**
     * 差分内容のサマリを返します。<br>
     * 
     * @return 差分内容のサマリ
     */
    public String getDiffSimpleSummary() {
        if (bookResults.isEmpty()) {
            return rb.getString("excel.DResult.100");
        }
        
        int diffBooks = (int) bookResults.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(BookResult::hasDiff)
                .count();
        int gapBooks = (int) bookNamePairs.stream()
                .filter(Predicate.not(Pair::isPaired))
                .count();
        int failed = (int) bookNamePairs.stream()
                .filter(Pair::isPaired)
                .filter(p -> !bookResults.containsKey(p) || bookResults.get(p).isEmpty())
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
