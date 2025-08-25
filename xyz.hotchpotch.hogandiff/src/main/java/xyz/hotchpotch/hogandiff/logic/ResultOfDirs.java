package xyz.hotchpotch.hogandiff.logic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.logic.ResultOfSheets.SheetStats;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダ同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 * 
 * @param dirComparison フォルダ比較情報
 * @param bookResults   Excelブックパスのペアに対応するExcelブック同士の比較結果のマップ
 * @param dirId         フォルダの識別番号
 */
public record ResultOfDirs(
        PairingInfoDirs dirComparison,
        Map<Pair<BookInfo>, Optional<ResultOfBooks>> bookResults,
        String dirId)
        implements Result {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * Excelブックパスペアをユーザー表示用に整形して返します。<br>
     * 
     * @param dirId        親フォルダのペアを示す識別子。
     * @param bookId       このExcelブックパスペアを示す識別子。
     * @param bookInfoPair Excelブックパスペア
     * @return Excelブックパスペアの整形済み文字列
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static String formatBookNamesPair(
            String dirId,
            String bookId,
            Pair<BookInfo> bookInfoPair) {
        
        Objects.requireNonNull(dirId);
        Objects.requireNonNull(bookId);
        Objects.requireNonNull(bookInfoPair);
        
        String bookNameA = bookInfoPair.hasA() ? bookInfoPair.a().bookName() : null;
        String bookNameB = bookInfoPair.hasB() ? bookInfoPair.b().bookName() : null;
        
        return "    %s  vs  %s".formatted(
                bookInfoPair.hasA()
                        ? "【A%s-%s】%s".formatted(dirId, bookId, bookNameA)
                        : rb.getString("excel.DResult.010"),
                bookInfoPair.hasB()
                        ? "【B%s-%s】%s".formatted(dirId, bookId, bookNameB)
                        : rb.getString("excel.DResult.010"));
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ<br>
     * 
     * @param dirComparison フォルダ比較情報
     * @param bookResults   Excelブックパスのペアに対応するExcelブック同士の比較結果のマップ
     * @param dirId         フォルダの識別番号
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public ResultOfDirs {
        Objects.requireNonNull(dirComparison);
        Objects.requireNonNull(bookResults);
        Objects.requireNonNull(dirId);
        
        bookResults = Map.copyOf(bookResults);
    }
    
    /**
     * この比較結果における差分の有無を返します。<br>
     * 
     * @return 差分ありの場合は {@code true}
     */
    public boolean hasDiff() {
        return dirComparison.childBookInfoPairs().stream()
                .map(bookResults::get)
                .anyMatch(r -> r.isEmpty() || r.get().hasDiff());
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
                .filter(ResultOfBooks::hasDiff)
                .count();
        int gapBooks = (int) dirComparison.childBookInfoPairs().stream()
                .filter(Predicate.not(Pair::isPaired))
                .count();
        int failed = (int) dirComparison.childBookInfoPairs().stream()
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
            Function<Optional<ResultOfBooks>, String> diffDescriptor,
            boolean isDetailMode) {
        
        StringBuilder str = new StringBuilder();
        
        if (bookResults.isEmpty()) {
            str.append("    - ").append(rb.getString("excel.DResult.100")).append(BR);
            if (isDetailMode) {
                str.append(BR);
            }
            return str.toString();
        }
        
        for (int i = 0; i < dirComparison.childBookInfoPairs().size(); i++) {
            Pair<BookInfo> bookInfoPair = dirComparison.childBookInfoPairs().get(i);
            Optional<ResultOfBooks> bResult = bookResults.get(bookInfoPair);
            
            str.append(formatBookNamesPair(dirId, Integer.toString(i + 1), bookInfoPair));
            
            if (bookInfoPair.isPaired()) {
                str.append(diffDescriptor.apply(bResult));
            } else {
                str.append(BR);
                if (isDetailMode) {
                    str.append(BR);
                }
            }
        }
        return str.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        str.append(rb.getString("excel.DResult.020").formatted("A"))
                .append(dirComparison.parentDirInfoPair().a().dirPath())
                .append(BR);
        str.append(rb.getString("excel.DResult.020").formatted("B"))
                .append(dirComparison.parentDirInfoPair().b().dirPath())
                .append(BR);
        
        for (int i = 0; i < dirComparison.childBookInfoPairs().size(); i++) {
            Pair<BookInfo> bookInfoPair = dirComparison.childBookInfoPairs().get(i);
            str.append(formatBookNamesPair(dirId, Integer.toString(i + 1), bookInfoPair)).append(BR);
        }
        
        str.append(BR);
        str.append(rb.getString("excel.DResult.030")).append(BR);
        str.append(getDiffSummary()).append(BR);
        str.append(rb.getString("excel.DResult.040")).append(BR);
        str.append(getDiffDetail());
        
        return str.toString();
    }
    
    @Override
    public List<SheetStats> sheetStats() {
        return bookResults.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ResultOfBooks::sheetStats)
                .flatMap(List::stream)
                .toList();
    }
}
