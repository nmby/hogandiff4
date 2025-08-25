package xyz.hotchpotch.hogandiff.logic;

import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.logic.plain.CellsUtil;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excelシート同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public final class ResultOfSheets implements Result {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * 片側のシートに関する差分内容を表す不変クラスです。<br>
     *
     * @author nmby
     * @param redundantRows         余剰行のリスト
     * @param redundantColumns      余剰列のリスト
     * @param diffCellContents      セル内容に差分のあるセルのリスト
     * @param diffCellComments      セルコメントに差分のあるセルのリスト
     * @param redundantCellComments セルコメントが余剰であるセルのリスト
     */
    public static record Piece(
            List<Integer> redundantRows,
            List<Integer> redundantColumns,
            List<CellData> diffCellContents,
            List<CellData> diffCellComments,
            List<CellData> redundantCellComments) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        /**
         * コンストラクタ<br>
         * 
         * @param redundantRows         余剰行のリスト
         * @param redundantColumns      余剰列のリスト
         * @param diffCellContents      セル内容に差分のあるセルのリスト
         * @param diffCellComments      セルコメントに差分のあるセルのリスト
         * @param redundantCellComments セルコメントが余剰であるセルのリスト
         * @throws NullPointerException パラメータが {@code null} の場合
         */
        // java16で正式導入されたRecordを使ってみたいが故にこのクラスをRecordとしているが、
        // 本来はコンストラクタを公開する必要がない。ぐぬぬ
        // recordを使う欲の方が上回ったのでコンストラクタを公開しちゃう。ぐぬぬ
        public Piece {
            Objects.requireNonNull(redundantRows);
            Objects.requireNonNull(redundantColumns);
            Objects.requireNonNull(diffCellContents);
            Objects.requireNonNull(diffCellComments);
            Objects.requireNonNull(redundantCellComments);
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
    
    /**
     * 比較処理の統計情報<br>
     * 
     * @param rows             各比較対象シートの行数
     * @param columns          各比較対象シートの列数
     * @param cells            各比較対象シートのセル数
     * @param redundantRows    各比較対象シートの余剰行数
     * @param redundantColumns 各比較対象シートの余剰列数
     * @param diffCells        差分セル数
     */
    public static record SheetStats(
            IntPair rows,
            IntPair columns,
            IntPair cells,
            IntPair redundantRows,
            IntPair redundantColumns,
            int diffCells) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        /**
         * コンストラクタ
         * 
         * @param rows             各比較対象シートの行数
         * @param columns          各比較対象シートの列数
         * @param cells            各比較対象シートのセル数
         * @param redundantRows    各比較対象シートの余剰行数
         * @param redundantColumns 各比較対象シートの余剰列数
         * @param diffCells        差分セル数
         * @throws NullPointerException パラメータが {@code null} の場合
         */
        public SheetStats {
            Objects.requireNonNull(rows);
            Objects.requireNonNull(columns);
            Objects.requireNonNull(cells);
            Objects.requireNonNull(redundantRows);
            Objects.requireNonNull(redundantColumns);
        }
    }
    
    // [instance members] ******************************************************
    
    private final Pair<List<Integer>> redundantRows;
    private final Pair<List<Integer>> redundantColumns;
    private final List<Pair<CellData>> diffCells;
    private final SheetStats stats;
    
    /**
     * コンストラクタ<br>
     * 
     * @param cellsSetPair     各シートに含まれるセル
     * @param redundantRows    各シートにおける余剰行
     * @param redundantColumns 各シートにおける余剰列
     * @param diffCells        差分セル
     * @throws NullPointerException     パラメータが {@code null} の場合
     * @throws IllegalArgumentException
     *                                  余剰／欠損の考慮なしにも関わらす余剰／欠損の数が 0 でない場合
     */
    public ResultOfSheets(
            Pair<Set<CellData>> cellsSetPair,
            Pair<List<Integer>> redundantRows,
            Pair<List<Integer>> redundantColumns,
            List<Pair<CellData>> diffCells) {
        
        Objects.requireNonNull(cellsSetPair);
        Objects.requireNonNull(redundantRows);
        Objects.requireNonNull(redundantColumns);
        Objects.requireNonNull(diffCells);
        
        if (!redundantRows.isPaired() || !redundantColumns.isPaired()) {
            throw new IllegalArgumentException("illegal result");
        }
        
        this.redundantRows = redundantRows.map(List::copyOf);
        this.redundantColumns = redundantColumns.map(List::copyOf);
        this.diffCells = List.copyOf(diffCells);
        this.stats = new SheetStats(
                IntPair.from(cellsSetPair.map(cells -> cells.stream().mapToInt(CellData::row).max().orElse(0))),
                IntPair.from(cellsSetPair.map(cells -> cells.stream().mapToInt(CellData::column).max().orElse(0))),
                IntPair.from(cellsSetPair.map(Set::size)),
                IntPair.from(redundantRows.map(List::size)),
                IntPair.from(redundantColumns.map(List::size)),
                diffCells.size());
    }
    
    /**
     * 指定された側のシートに関する差分内容を返します。<br>
     * 
     * @param side シートの側
     * @return 指定された側のシートに関する差分内容
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public Piece getPiece(Side side) {
        Objects.requireNonNull(side);
        
        List<CellData> diffCellContents = diffCells.stream()
                .filter(p -> !p.a().contentEquals(p.b()))
                .map(p -> p.get(side))
                .toList();
        
        List<CellData> diffCellComments = diffCells.stream()
                .filter(p -> p.a().hasComment() && p.b().hasComment() && !p.a().commentEquals(p.b()))
                .map(p -> p.get(side))
                .toList();
        
        List<CellData> redundantCellComments = diffCells.stream()
                .filter(p -> p.get(side).hasComment() && !p.get(side.opposite()).hasComment())
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
            return rb.getString("excel.SResult.010");
        }
        
        int rows = redundantRows.a().size() + redundantRows.b().size();
        int cols = redundantColumns.a().size() + redundantColumns.b().size();
        int cells = diffCells.size();
        
        StringBuilder str = new StringBuilder();
        if (0 < rows) {
            str.append(rb.getString("excel.SResult.020").formatted(rows));
        }
        if (0 < cols) {
            if (!str.isEmpty()) {
                str.append(", ");
            }
            str.append(rb.getString("excel.SResult.030").formatted(cols));
        }
        if (0 < cells) {
            if (!str.isEmpty()) {
                str.append(", ");
            }
            str.append(rb.getString("excel.SResult.040").formatted(cells));
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
            return rb.getString("excel.SResult.010");
        }
        
        StringBuilder str = new StringBuilder();
        
        if (!redundantRows.a().isEmpty() || !redundantRows.b().isEmpty()) {
            for (Side side : Side.values()) {
                List<Integer> rows = redundantRows.get(side);
                if (!rows.isEmpty()) {
                    str.append(rb.getString("excel.SResult.050").formatted(side)).append(BR);
                    for (int row : rows) {
                        str.append("    ")
                                .append(rb.getString("excel.SResult.060").formatted(row + 1))
                                .append(BR);
                    }
                }
            }
            str.append(BR);
        }
        if (!redundantColumns.a().isEmpty() || !redundantColumns.b().isEmpty()) {
            for (Side side : Side.values()) {
                List<Integer> cols = redundantColumns.get(side);
                if (!cols.isEmpty()) {
                    str.append(rb.getString("excel.SResult.070").formatted(side)).append(BR);
                    for (int col : cols) {
                        str.append("    ")
                                .append(rb.getString("excel.SResult.080").formatted(CellsUtil.columnIdxToStr(col)))
                                .append(BR);
                    }
                }
            }
            str.append(BR);
        }
        if (!diffCells.isEmpty()) {
            str.append(rb.getString("excel.SResult.090"));
            diffCells.forEach(pair -> {
                str.append(BR);
                str.append("    [A] ").append(pair.a()).append(BR);
                str.append("    [B] ").append(pair.b()).append(BR);
            });
        }
        
        return str.toString();
    }
    
    @Override
    public String toString() {
        return getDiffDetail();
    }
    
    @Override
    public List<SheetStats> sheetStats() {
        return List.of(stats);
    }
    
    /**
     * 余剰行を返します。<br>
     * 
     * @return 余剰行
     */
    public Pair<List<Integer>> redundantRows() {
        return redundantRows;
    }
    
    /**
     * 余剰列を返します。<br>
     * 
     * @return 余剰列
     */
    public Pair<List<Integer>> redundantColumns() {
        return redundantColumns;
    }
    
    /**
     * 差分セルを返します。<br>
     * 
     * @return 差分セル
     */
    public List<Pair<CellData>> diffCells() {
        return diffCells;
    }
}
