package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excelシート同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public record SResult(
        boolean considerRowGaps,
        boolean considerColumnGaps,
        boolean compareCellContents,
        boolean compareCellComments,
        Pair<List<Integer>> redundantRows,
        Pair<List<Integer>> redundantColumns,
        List<Pair<CellData>> diffCells) {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
    /**
     * 片側のシートに関する差分内容を表す不変クラスです。<br>
     *
     * @author nmby
     */
    public static record Piece(
            List<Integer> redundantRows,
            List<Integer> redundantColumns,
            List<CellData> diffCellContents,
            List<CellData> diffCellComments,
            List<CellData> redundantCellComments) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        // java16で正式導入されたRecordを使ってみたいが故にこのクラスをRecordとしているが、
        // 本来はコンストラクタを公開する必要がない。ぐぬぬ
        // recordを使う欲の方が上回ったのでコンストラクタを公開しちゃう。ぐぬぬ
        public Piece {
            Objects.requireNonNull(redundantRows, "redundantRows");
            Objects.requireNonNull(redundantColumns, "redundantColumns");
            Objects.requireNonNull(diffCellContents, "diffCellContents");
            Objects.requireNonNull(diffCellComments, "diffCellComments");
            Objects.requireNonNull(redundantCellComments, "redundantCellComments");
            
            // 一応防御的コピーしておく。
            redundantRows = List.copyOf(redundantRows);
            redundantColumns = List.copyOf(redundantColumns);
            diffCellContents = List.copyOf(diffCellContents);
            diffCellComments = List.copyOf(diffCellComments);
            redundantCellComments = List.copyOf(redundantCellComments);
        }
        
        /**
         * ひとつでも差分があるかを返します。<br>
         * 
         * @return ひとつでも差分がある場合は {@code true}
         */
        public boolean hasDiff() {
            return !redundantRows.isEmpty()
                    || !redundantColumns.isEmpty()
                    || !diffCellContents.isEmpty()
                    || !diffCellComments.isEmpty()
                    || !redundantCellComments.isEmpty();
        }
    }
    
    // [instance members] ******************************************************
    
    /**
     * Excelシート同士の比較結果を生成します。<br>
     * 
     * @param considerRowGaps 比較において行の余剰／欠損を考慮したか
     * @param considerColumnGaps 比較において列の余剰／欠損を考慮したか
     * @param compareCellContents 比較においてセル内容を比較したか
     * @param compareCellComments 比較においてセルコメントを比較したか
     * @param redundantRows 各シートにおける余剰行
     * @param redundantColumns 各シートにおける余剰列
     * @param diffCells 差分セル
     * @return Excelシート同士の比較結果
     * @throws NullPointerException
     *              {@code redundantRows}, {@code redundantColumns}, {@code diffCells}
     *              のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              余剰／欠損の考慮なしにも関わらす余剰／欠損の数が 0 でない場合
     */
    public SResult {
        Objects.requireNonNull(redundantRows, "redundantRows");
        Objects.requireNonNull(redundantColumns, "redundantColumns");
        Objects.requireNonNull(diffCells, "diffCells");
        
        if (!redundantRows.isPaired() || !redundantColumns.isPaired()) {
            throw new IllegalArgumentException("illegal result");
        }
        
        if (!considerRowGaps && (!redundantRows.a().isEmpty() || !redundantRows.b().isEmpty())) {
            throw new IllegalArgumentException("illegal row result");
        }
        if (!considerColumnGaps && (!redundantColumns.a().isEmpty() || !redundantColumns.b().isEmpty())) {
            throw new IllegalArgumentException("illegal column result");
        }
        
        // 一応、防御的コピーしておく。
        if (redundantRows.isPaired()) {
            redundantRows = Pair.of(
                    List.copyOf(redundantRows.a()),
                    List.copyOf(redundantRows.b()));
        }
        if (redundantColumns.isPaired()) {
            redundantColumns = Pair.of(
                    List.copyOf(redundantColumns.a()),
                    List.copyOf(redundantColumns.b()));
        }
        diffCells = List.copyOf(diffCells);
    }
    
    /**
     * 指定された側のシートに関する差分内容を返します。<br>
     * 
     * @param side シートの側
     * @return 指定された側のシートに関する差分内容
     * @throws NullPointerException {@code side} が {@code null} の場合
     */
    public Piece getPiece(Side side) {
        Objects.requireNonNull(side, "side");
        
        List<CellData> diffCellContents = !compareCellContents
                ? List.of()
                : diffCells.stream()
                        .filter(p -> !p.a().contentEquals(p.b()))
                        .map(p -> p.get(side))
                        .toList();
        
        List<CellData> diffCellComments = !compareCellComments
                ? List.of()
                : diffCells.stream()
                        .filter(p -> p.a().hasComment() && p.b().hasComment())
                        .filter(p -> !p.a().commentEquals(p.b()))
                        .map(p -> p.get(side))
                        .toList();
        
        List<CellData> redundantCellComments = !compareCellComments
                ? List.of()
                : diffCells.stream()
                        .filter(p -> p.get(side).hasComment())
                        .filter(p -> !p.get(side.opposite()).hasComment())
                        .map(p -> p.get(side))
                        .toList();
        
        return new Piece(
                redundantRows.get(side),
                redundantColumns.get(side),
                diffCellContents,
                diffCellComments,
                redundantCellComments);
    }
    
    /**
     * この比較結果における差分の有無を返します。<br>
     * 
     * @return 差分ありの場合は {@code true}
     */
    public boolean hasDiff() {
        return !redundantRows.a().isEmpty()
                || !redundantRows.b().isEmpty()
                || !redundantColumns.a().isEmpty()
                || !redundantColumns.b().isEmpty()
                || !diffCells.isEmpty();
    }
    
    /**
     * 比較結果の差分サマリを返します。<br>
     * 
     * @return 比較結果の差分サマリ
     */
    public String getDiffSummary() {
        if (!hasDiff()) {
            return "(差分なし)";
        }
        
        int rows = redundantRows.a().size() + redundantRows.b().size();
        int cols = redundantColumns.a().size() + redundantColumns.b().size();
        int cells = diffCells.size();
        
        StringBuilder str = new StringBuilder();
        if (0 < rows) {
            str.append("余剰行").append(rows);
        }
        if (0 < cols) {
            if (!str.isEmpty()) {
                str.append(", ");
            }
            str.append("余剰列").append(cols);
        }
        if (0 < cells) {
            if (!str.isEmpty()) {
                str.append(", ");
            }
            str.append("差分セル").append(cells);
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
            return "(差分なし)";
        }
        
        StringBuilder str = new StringBuilder();
        
        if (!redundantRows.a().isEmpty() || !redundantRows.b().isEmpty()) {
            for (Side side : Side.values()) {
                List<Integer> rows = redundantRows.get(side);
                if (!rows.isEmpty()) {
                    str.append(String.format("シート%s上の余剰行 : ", side)).append(BR);
                    rows.forEach(row -> str.append("    行").append(row + 1).append(BR));
                }
            }
            str.append(BR);
        }
        if (!redundantColumns.a().isEmpty() || !redundantColumns.b().isEmpty()) {
            for (Side side : Side.values()) {
                List<Integer> cols = redundantColumns.get(side);
                if (!cols.isEmpty()) {
                    str.append(String.format("シート%s上の余剰列 : ", side)).append(BR);
                    cols.forEach(column -> str
                            .append("    ")
                            .append(CellsUtil.columnIdxToStr(column))
                            .append("列").append(BR));
                }
            }
            str.append(BR);
        }
        if (!diffCells.isEmpty()) {
            str.append("差分セル : ");
            diffCells.forEach(pair -> {
                str.append(BR);
                str.append("    ").append(pair.a()).append(BR);
                str.append("    ").append(pair.b()).append(BR);
            });
        }
        
        return str.toString();
    }
    
    @Override
    public String toString() {
        return getDiffDetail();
    }
    
    /**
     * 比較結果のコマンドライン出力用文字列を返します。<br>
     * 
     * @return 比較結果のコマンドライン出力用文字列
     */
    public String getDiff() {
        StringBuilder str = new StringBuilder();
        
        if (!redundantRows.a().isEmpty() || !redundantRows.b().isEmpty()) {
            str.append("Row Gaps :").append(BR);
            
            Function<List<Integer>, String> rowsToStr = rows -> rows.stream()
                    .map(i -> i + 1)
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            
            if (!redundantRows.a().isEmpty()) {
                str.append("- ").append(rowsToStr.apply(redundantRows.a())).append(BR);
            }
            if (!redundantRows.b().isEmpty()) {
                str.append("+ ").append(rowsToStr.apply(redundantRows.b())).append(BR);
            }
            str.append(BR);
        }
        
        if (!redundantColumns.a().isEmpty() || !redundantColumns.b().isEmpty()) {
            str.append("Column Gaps :").append(BR);
            
            Function<List<Integer>, String> columnsToStr = columns -> columns.stream()
                    .map(CellsUtil::columnIdxToStr)
                    .collect(Collectors.joining(", "));
            
            if (!redundantColumns.a().isEmpty()) {
                str.append("- ").append(columnsToStr.apply(redundantColumns.a())).append(BR);
            }
            if (!redundantColumns.b().isEmpty()) {
                str.append("+ ").append(columnsToStr.apply(redundantColumns.b())).append(BR);
            }
            str.append(BR);
        }
        
        if (!diffCells.isEmpty()) {
            str.append("Diff Cells :").append(BR);
            
            str.append(diffCells.stream()
                    .map(diffCell -> String.format("- %s\n+ %s\n", diffCell.a(), diffCell.b()))
                    .collect(Collectors.joining(BR)));
            
        }
        
        return str.toString();
    }
}
