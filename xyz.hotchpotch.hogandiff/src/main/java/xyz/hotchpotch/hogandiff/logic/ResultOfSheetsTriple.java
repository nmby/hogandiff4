package xyz.hotchpotch.hogandiff.logic;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.logic.ResultOfSheets.Piece;
import xyz.hotchpotch.hogandiff.logic.ResultOfSheets.SheetStats;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Triple;

/**
 * Excelシートの3-way比較結果を表す不変クラスです。<br>
 * <p>
 * 起源（O）に対するA, Bそれぞれの2-way比較結果（O vs A、O vs B）を保持します。<br>
 *
 * @author nmby
 */
public final class ResultOfSheetsTriple implements Result {

    // [static members] ********************************************************

    /**
     * O ファイル着色用に分類した 3 種類の {@link Piece} を保持するレコードです。<br>
     *
     * @param aOnly    A のみで変更された差分（O ファイル上で赤系に着色）
     * @param bOnly    B のみで変更された差分（O ファイル上で青系に着色）
     * @param conflict A と B の両方で変更された差分（O ファイル上で紫系に着色）
     */
    public record OriginPieces(
            Piece aOnly,
            Piece bOnly,
            Piece conflict) {}

    private static final String BR = System.lineSeparator();

    // [instance members] ******************************************************

    /**
     * O vs A の比較結果。<br>
     * Pair.Side.A が O 側、Pair.Side.B が A 側に対応します。
     */
    private final ResultOfSheets resultOA;

    /**
     * O vs B の比較結果。<br>
     * Pair.Side.A が O 側、Pair.Side.B が B 側に対応します。
     */
    private final ResultOfSheets resultOB;

    /**
     * コンストラクタ<br>
     *
     * @param resultOA O vs A の比較結果（Pair.Side.A = O, Pair.Side.B = A）
     * @param resultOB O vs B の比較結果（Pair.Side.A = O, Pair.Side.B = B）
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public ResultOfSheetsTriple(
            ResultOfSheets resultOA,
            ResultOfSheets resultOB) {

        Objects.requireNonNull(resultOA);
        Objects.requireNonNull(resultOB);

        this.resultOA = resultOA;
        this.resultOB = resultOB;
    }

    /**
     * O vs A の比較結果を返します。<br>
     *
     * @return O vs A の比較結果
     */
    public ResultOfSheets resultOA() {
        return resultOA;
    }

    /**
     * O vs B の比較結果を返します。<br>
     *
     * @return O vs B の比較結果
     */
    public ResultOfSheets resultOB() {
        return resultOB;
    }

    /**
     * 指定された側のシートに関する差分内容を返します。<br>
     * <p>
     * <ul>
     * <li>O側：AおよびBとの差分をすべて含む {@link Piece} を返します。</li>
     * <li>A側：O vs A 比較のA側 {@link Piece} を返します。</li>
     * <li>B側：O vs B 比較のB側 {@link Piece} を返します。</li>
     * </ul>
     *
     * @param side シートの側
     * @return 指定された側のシートに関する差分内容
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public Piece getPiece(Triple.Side side) {
        Objects.requireNonNull(side);

        return switch (side) {
            case O -> resultOA.getPiece(Pair.Side.A);
            case A -> resultOA.getPiece(Pair.Side.B);
            case B -> resultOB.getPiece(Pair.Side.B);
        };
    }

    /**
     * この比較結果における差分の有無を返します。<br>
     *
     * @return 差分ありの場合は {@code true}
     */
    public boolean hasDiff() {
        return resultOA.hasDiff() || resultOB.hasDiff();
    }

    /**
     * 比較結果の差分サマリを返します。<br>
     *
     * @return 比較結果の差分サマリ
     */
    public String getDiffSummary() {
        if (!hasDiff()) {
            return "差分なし";
        }

        StringBuilder str = new StringBuilder();
        if (resultOA.hasDiff()) {
            str.append("O vs A: ").append(resultOA.getDiffSummary());
        }
        if (resultOB.hasDiff()) {
            if (!str.isEmpty()) {
                str.append("  /  ");
            }
            str.append("O vs B: ").append(resultOB.getDiffSummary());
        }
        return str.toString();
    }

    /**
     * 比較結果の差分詳細を返します。<br>
     *
     * @return 比較結果の差分詳細
     */
    public String getDiffDetail() {
        if (!hasDiff()) {
            return "差分なし";
        }

        StringBuilder str = new StringBuilder();
        if (resultOA.hasDiff()) {
            str.append("[O vs A]").append(BR);
            str.append(resultOA.getDiffDetail());
        }
        if (resultOB.hasDiff()) {
            if (!str.isEmpty()) {
                str.append(BR);
            }
            str.append("[O vs B]").append(BR);
            str.append(resultOB.getDiffDetail());
        }
        return str.toString();
    }

    /**
     * O ファイル着色用に差分を A-only / B-only / conflict に分類して返します。<br>
     * <p>
     * 差分の座標はいずれも O 座標系で表されます。<br>
     *
     * @return O ファイル着色用に分類した差分情報
     */
    public OriginPieces computeOriginPieces() {
        // O の視点（Pair.Side.A = O 側）で両比較から Piece を取得する
        Piece oPieceFromA = resultOA.getPiece(Pair.Side.A);
        Piece oPieceFromB = resultOB.getPiece(Pair.Side.A);

        // (row, column) 位置を表すローカルレコード
        record Pos(int row, int col) {}

        // --- 余剰行 ---
        Set<Integer> aRows = new HashSet<>(oPieceFromA.redundantRows());
        Set<Integer> bRows = new HashSet<>(oPieceFromB.redundantRows());
        Set<Integer> conflictRows = aRows.stream()
                .filter(bRows::contains)
                .collect(Collectors.toSet());
        List<Integer> aOnlyRows = oPieceFromA.redundantRows().stream()
                .filter(r -> !conflictRows.contains(r)).toList();
        List<Integer> bOnlyRows = oPieceFromB.redundantRows().stream()
                .filter(r -> !conflictRows.contains(r)).toList();
        List<Integer> conflictRowList = oPieceFromA.redundantRows().stream()
                .filter(conflictRows::contains).toList();

        // --- 余剰列 ---
        Set<Integer> aCols = new HashSet<>(oPieceFromA.redundantColumns());
        Set<Integer> bCols = new HashSet<>(oPieceFromB.redundantColumns());
        Set<Integer> conflictCols = aCols.stream()
                .filter(bCols::contains)
                .collect(Collectors.toSet());
        List<Integer> aOnlyCols = oPieceFromA.redundantColumns().stream()
                .filter(c -> !conflictCols.contains(c)).toList();
        List<Integer> bOnlyCols = oPieceFromB.redundantColumns().stream()
                .filter(c -> !conflictCols.contains(c)).toList();
        List<Integer> conflictColList = oPieceFromA.redundantColumns().stream()
                .filter(conflictCols::contains).toList();

        // --- 差分セル内容 ---
        Set<Pos> aContentPos = oPieceFromA.diffCellContents().stream()
                .map(c -> new Pos(c.row(), c.column()))
                .collect(Collectors.toSet());
        Set<Pos> bContentPos = oPieceFromB.diffCellContents().stream()
                .map(c -> new Pos(c.row(), c.column()))
                .collect(Collectors.toSet());
        Set<Pos> conflictContentPos = aContentPos.stream()
                .filter(bContentPos::contains)
                .collect(Collectors.toSet());
        List<CellData> aOnlyContents = oPieceFromA.diffCellContents().stream()
                .filter(c -> !conflictContentPos.contains(new Pos(c.row(), c.column()))).toList();
        List<CellData> bOnlyContents = oPieceFromB.diffCellContents().stream()
                .filter(c -> !conflictContentPos.contains(new Pos(c.row(), c.column()))).toList();
        List<CellData> conflictContents = oPieceFromA.diffCellContents().stream()
                .filter(c -> conflictContentPos.contains(new Pos(c.row(), c.column()))).toList();

        // --- 差分セルコメント ---
        Set<Pos> aCommentPos = oPieceFromA.diffCellComments().stream()
                .map(c -> new Pos(c.row(), c.column()))
                .collect(Collectors.toSet());
        Set<Pos> bCommentPos = oPieceFromB.diffCellComments().stream()
                .map(c -> new Pos(c.row(), c.column()))
                .collect(Collectors.toSet());
        Set<Pos> conflictCommentPos = aCommentPos.stream()
                .filter(bCommentPos::contains)
                .collect(Collectors.toSet());
        List<CellData> aOnlyComments = oPieceFromA.diffCellComments().stream()
                .filter(c -> !conflictCommentPos.contains(new Pos(c.row(), c.column()))).toList();
        List<CellData> bOnlyComments = oPieceFromB.diffCellComments().stream()
                .filter(c -> !conflictCommentPos.contains(new Pos(c.row(), c.column()))).toList();
        List<CellData> conflictComments = oPieceFromA.diffCellComments().stream()
                .filter(c -> conflictCommentPos.contains(new Pos(c.row(), c.column()))).toList();

        // --- 余剰セルコメント ---
        Set<Pos> aRedundantComPos = oPieceFromA.redundantCellComments().stream()
                .map(c -> new Pos(c.row(), c.column()))
                .collect(Collectors.toSet());
        Set<Pos> bRedundantComPos = oPieceFromB.redundantCellComments().stream()
                .map(c -> new Pos(c.row(), c.column()))
                .collect(Collectors.toSet());
        Set<Pos> conflictRedundantComPos = aRedundantComPos.stream()
                .filter(bRedundantComPos::contains)
                .collect(Collectors.toSet());
        List<CellData> aOnlyRedundantComs = oPieceFromA.redundantCellComments().stream()
                .filter(c -> !conflictRedundantComPos.contains(new Pos(c.row(), c.column()))).toList();
        List<CellData> bOnlyRedundantComs = oPieceFromB.redundantCellComments().stream()
                .filter(c -> !conflictRedundantComPos.contains(new Pos(c.row(), c.column()))).toList();
        List<CellData> conflictRedundantComs = oPieceFromA.redundantCellComments().stream()
                .filter(c -> conflictRedundantComPos.contains(new Pos(c.row(), c.column()))).toList();

        return new OriginPieces(
                new Piece(aOnlyRows, aOnlyCols, aOnlyContents, aOnlyComments, aOnlyRedundantComs),
                new Piece(bOnlyRows, bOnlyCols, bOnlyContents, bOnlyComments, bOnlyRedundantComs),
                new Piece(conflictRowList, conflictColList, conflictContents, conflictComments,
                        conflictRedundantComs));
    }

    @Override
    public String toString() {
        return getDiffDetail();
    }

    @Override
    public List<SheetStats> sheetStats() {
        return List.of(
                resultOA.sheetStats().get(0),
                resultOB.sheetStats().get(0));
    }
}
