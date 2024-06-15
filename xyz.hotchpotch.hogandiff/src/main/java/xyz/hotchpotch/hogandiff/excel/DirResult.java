package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Predicate;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.SheetResult.Stats;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダ同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 * 
 * @param dirCompareInfoProp フォルダ比較情報
 * @param bookResults Excelブック名のペアに対応するExcelブック同士の比較結果のマップ
 * @param dirId フォルダの識別番号
 */
public record DirResult(
        DirCompareInfo dirCompareInfo,
        Map<Pair<String>, Optional<BookResult>> bookResults,
        String dirId)
        implements Result {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * Excelブック名ペアをユーザー表示用に整形して返します。<br>
     * 
     * @param dirId 親フォルダのペアを示す識別子。
     * @param bookId このExcelブック名ペアを示す識別子。
     * @param pair Excelブック名ペア
     * @return Excelブック名ペアの整形済み文字列
     * @throws NullPointerException {@code dirId}, {@code bookId}, {@code pair} のいずれかが {@code null} の場合
     */
    public static String formatBookNamesPair(
            String dirId,
            String bookId,
            Pair<String> pair) {
        
        Objects.requireNonNull(dirId, "dirId");
        Objects.requireNonNull(bookId, "bookId");
        Objects.requireNonNull(pair, "pair");
        
        return "    %s  vs  %s".formatted(
                pair.hasA() ? "【A%s-%s】%s".formatted(dirId, bookId, pair.a()) : rb.getString("excel.DResult.010"),
                pair.hasB() ? "【B%s-%s】%s".formatted(dirId, bookId, pair.b()) : rb.getString("excel.DResult.010"));
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ<br>
     * 
     * @param dirCompareInfo フォルダ比較情報
     * @param bookResults Excelブック名のペアに対応するExcelブック同士の比較結果のマップ
     * @param dirId フォルダの識別番号
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public DirResult(
            DirCompareInfo dirCompareInfo,
            Map<Pair<String>, Optional<BookResult>> bookResults,
            String dirId) {
        
        Objects.requireNonNull(dirCompareInfo);
        Objects.requireNonNull(bookResults);
        Objects.requireNonNull(dirId);
        
        this.dirCompareInfo = dirCompareInfo;
        this.bookResults = Map.copyOf(bookResults);
        this.dirId = dirId;
    }
    
    /**
     * この比較結果における差分の有無を返します。<br>
     * 
     * @return 差分ありの場合は {@code true}
     */
    public boolean hasDiff() {
        return dirCompareInfo.childPairs().stream()
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
                .filter(BookResult::hasDiff)
                .count();
        int gapBooks = (int) dirCompareInfo.childPairs().stream()
                .filter(Predicate.not(Pair::isPaired))
                .count();
        int failed = (int) dirCompareInfo.childPairs().stream()
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
        
        for (int i = 0; i < dirCompareInfo.childPairs().size(); i++) {
            Pair<String> bookNamePair = dirCompareInfo.childPairs().get(i);
            Optional<BookResult> bResult = bookResults.get(bookNamePair);
            
            str.append(formatBookNamesPair(dirId, Integer.toString(i + 1), bookNamePair));
            
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
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        str.append(rb.getString("excel.DResult.020").formatted("A"))
                .append(dirCompareInfo.parentPair().a().dirPath())
                .append(BR);
        str.append(rb.getString("excel.DResult.020").formatted("B"))
                .append(dirCompareInfo.parentPair().b().dirPath())
                .append(BR);
        
        for (int i = 0; i < dirCompareInfo.childPairs().size(); i++) {
            Pair<String> bookNamePair = dirCompareInfo.childPairs().get(i);
            str.append(formatBookNamesPair(dirId, Integer.toString(i + 1), bookNamePair)).append(BR);
        }
        
        str.append(BR);
        str.append(rb.getString("excel.DResult.030")).append(BR);
        str.append(getDiffSummary()).append(BR);
        str.append(rb.getString("excel.DResult.040")).append(BR);
        str.append(getDiffDetail());
        
        return str.toString();
    }
    
    @Override
    public List<Stats> getSheetStats() {
        return bookResults.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(BookResult::getSheetStats)
                .flatMap(List::stream)
                .toList();
    }
}
