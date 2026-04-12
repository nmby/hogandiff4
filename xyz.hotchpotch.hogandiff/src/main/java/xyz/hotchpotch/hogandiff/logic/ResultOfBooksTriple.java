package xyz.hotchpotch.hogandiff.logic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import xyz.hotchpotch.hogandiff.logic.ResultOfSheets.SheetStats;
import xyz.hotchpotch.hogandiff.util.Triple;

/**
 * Excelブックの3-way比較結果を表す不変クラスです。<br>
 * <p>
 * 起源（O）に対するA, Bの差分比較結果を保持します。<br>
 *
 * @param bookComparison Excelブック3-way比較情報
 * @param sheetResults Excelシート同士の3-way比較結果（片側だけの欠損トリプルも含む）
 * @author nmby
 */
public record ResultOfBooksTriple(
        PairingInfoBooksTriple bookComparison,
        Map<Triple<String>, Optional<ResultOfSheetsTriple>> sheetResults)
        implements Result {

    // [static members] ********************************************************

    private static final String BR = System.lineSeparator();

    /**
     * シート名トリプルをユーザー表示用に整形して返します。<br>
     *
     * @param id シート名トリプルの識別子
     * @param sheetNameTriple シート名トリプル
     * @return シート名トリプルの整形済み文字列
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static String formatSheetNamesTriple(
            String id,
            Triple<String> sheetNameTriple) {

        Objects.requireNonNull(id);
        Objects.requireNonNull(sheetNameTriple);

        return "    %s) %s  vs  %s  vs  %s".formatted(
                id,
                sheetNameTriple.hasO() ? "O[ " + sheetNameTriple.o() + " ]" : "(なし)",
                sheetNameTriple.hasA() ? "A[ " + sheetNameTriple.a() + " ]" : "(なし)",
                sheetNameTriple.hasB() ? "B[ " + sheetNameTriple.b() + " ]" : "(なし)");
    }

    // [instance members] ******************************************************

    /**
     * コンストラクタ<br>
     *
     * @param bookComparison Excelブック3-way比較情報
     * @param sheetResults Excelシート同士の3-way比較結果（片側だけの欠損トリプルも含む）
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public ResultOfBooksTriple {
        Objects.requireNonNull(bookComparison);
        Objects.requireNonNull(sheetResults);

        sheetResults = Map.copyOf(sheetResults);
    }

    /**
     * この比較結果における差分の有無を返します。<br>
     *
     * @return 差分ありの場合は {@code true}
     */
    public boolean hasDiff() {
        return bookComparison.childSheetNameTriples().stream()
                .map(sheetResults::get)
                .anyMatch(r -> r == null || r.isEmpty() || r.get().hasDiff());
    }

    /**
     * 比較結果を端的に表す差分サマリを返します。<br>
     *
     * @return 比較結果を端的に表す差分サマリ
     */
    public String getDiffSimpleSummary() {
        int diffSheets = (int) bookComparison.childSheetNameTriples().stream()
                .filter(Triple::isPaired)
                .map(sheetResults::get)
                .map(Optional::get)
                .filter(ResultOfSheetsTriple::hasDiff)
                .count();
        int gapSheets = (int) bookComparison.childSheetNameTriples().stream()
                .filter(Predicate.not(Triple::isPaired))
                .count();

        if (diffSheets == 0 && gapSheets == 0) {
            return "差分なし";
        }

        StringBuilder str = new StringBuilder();
        if (0 < diffSheets) {
            str.append("%dシートに差分あり".formatted(diffSheets));
        }
        if (0 < gapSheets) {
            if (!str.isEmpty()) {
                str.append(", ");
            }
            str.append("%dシートが片側のみに存在".formatted(gapSheets));
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

    private String getDiffText(Function<ResultOfSheetsTriple, String> diffDescriptor) {
        StringBuilder str = new StringBuilder();

        for (int i = 0; i < bookComparison.childSheetNameTriples().size(); i++) {
            Triple<String> sheetNameTriple = bookComparison.childSheetNameTriples().get(i);
            Optional<ResultOfSheetsTriple> sResult = sheetResults.get(sheetNameTriple);

            if (sResult == null || !sheetNameTriple.isPaired()
                    || sResult.isEmpty() || !sResult.get().hasDiff()) {
                continue;
            }

            str.append(formatSheetNamesTriple(Integer.toString(i + 1), sheetNameTriple));
            str.append(diffDescriptor.apply(sResult.get()));
            str.append(BR);
        }

        return str.isEmpty()
                ? "    差分なし" + BR + BR
                : str.toString();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();

        Triple<BookInfo> bookInfoTriple = bookComparison.parentBookInfoTriple();
        if (bookInfoTriple.isIdentical()) {
            str.append("ブック: ").append(bookInfoTriple.o().bookPath()).append(BR);
        } else {
            if (bookInfoTriple.hasO()) {
                str.append("[O] ").append(bookInfoTriple.o().bookPath()).append(BR);
            }
            if (bookInfoTriple.hasA()) {
                str.append("[A] ").append(bookInfoTriple.a().bookPath()).append(BR);
            }
            if (bookInfoTriple.hasB()) {
                str.append("[B] ").append(bookInfoTriple.b().bookPath()).append(BR);
            }
        }

        for (int i = 0; i < bookComparison.childSheetNameTriples().size(); i++) {
            Triple<String> sheetNameTriple = bookComparison.childSheetNameTriples().get(i);
            str.append(formatSheetNamesTriple(Integer.toString(i + 1), sheetNameTriple)).append(BR);
        }

        str.append(BR);
        str.append("=== 差分サマリ ===").append(BR);
        str.append(getDiffSummary()).append(BR);
        str.append("=== 差分詳細 ===").append(BR);
        str.append(getDiffDetail());

        return str.toString();
    }

    @Override
    public List<SheetStats> sheetStats() {
        return sheetResults.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(ResultOfSheetsTriple::sheetStats)
                .flatMap(List::stream)
                .toList();
    }
}
