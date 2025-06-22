package xyz.hotchpotch.hogandiff.main.misc.excel.common.rc;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.main.misc.SheetComparator;
import xyz.hotchpotch.hogandiff.main.misc.excel.CellsUtil;
import xyz.hotchpotch.hogandiff.main.models.CellData;
import xyz.hotchpotch.hogandiff.main.models.ResultOfSheets;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 行同士の対応関係と列同士の対応関係をそれぞれ求めることによりセル同士の対応関係を決定する
 * {@link SheetComparator} の実装です。<br>
 *
 * @author nmby
 */
public class RCSheetComparator implements SheetComparator {

        // [static members] ********************************************************

        private static final Pair<List<Integer>> EMPTY_PAIR = new Pair<>(List.of(), List.of());

        /**
         * 新たなコンパレータを返します。<br>
         * 
         * @param considerRowGaps    行の挿入／削除を考慮する場合は {@code true}
         * @param considerColumnGaps 列の挿入／削除を考慮する場合は {@code true}
         * @param prioritizeSpeed    比較処理の速度を優先する場合は {@code true}
         * @return 新たなコンパレータ
         */
        public static RCSheetComparator of(
                        boolean considerRowGaps,
                        boolean considerColumnGaps,
                        boolean prioritizeSpeed) {

                return new RCSheetComparator(
                                RCMatcher.of(
                                                considerRowGaps,
                                                considerColumnGaps,
                                                prioritizeSpeed));
        }

        // [instance members] ******************************************************

        private final RCMatcher rcMatcher;

        private RCSheetComparator(RCMatcher rcMatcher) {
                assert rcMatcher != null;

                this.rcMatcher = rcMatcher;
        }

        /**
         * {@inheritDoc}
         * 
         * @throws NullPointerException
         *                                  {@code cellsSetPair} が {@code null} の場合
         * @throws IllegalArgumentException
         *                                  {@code cellsSetPair} に含まれるセルセットが同一インスタンスの場合
         */
        @Override
        public ResultOfSheets compare(Pair<Set<CellData>> cellsSetPair) {
                Objects.requireNonNull(cellsSetPair);

                if (cellsSetPair.a() == cellsSetPair.b()) {
                        if (cellsSetPair.a().isEmpty()) {
                                return new ResultOfSheets(
                                                cellsSetPair,
                                                EMPTY_PAIR,
                                                EMPTY_PAIR,
                                                List.of());
                        } else {
                                throw new IllegalArgumentException("cells1 == cells2");
                        }
                }

                Pair<List<IntPair>> pairs = rcMatcher.make2Pairs(cellsSetPair);
                List<IntPair> rowPairs = pairs.a();
                List<IntPair> columnPairs = pairs.b();

                // 余剰行の収集
                Pair<List<Integer>> redundantRows = Side.map(side -> rowPairs.stream()
                                .filter(pair -> pair.isOnly(side))
                                .map(pair -> pair.get(side))
                                .toList());

                // 余剰列の収集
                Pair<List<Integer>> redundantColumns = Side.map(side -> columnPairs.stream()
                                .filter(pair -> pair.isOnly(side))
                                .map(pair -> pair.get(side))
                                .toList());

                // 差分セルの収集
                List<Pair<CellData>> diffCells = extractDiffs(cellsSetPair, rowPairs, columnPairs);

                return new ResultOfSheets(
                                cellsSetPair,
                                redundantRows,
                                redundantColumns,
                                diffCells);
        }

        private List<Pair<CellData>> extractDiffs(
                        Pair<Set<CellData>> cellsSetPair,
                        List<IntPair> rowPairs,
                        List<IntPair> columnPairs) {

                assert cellsSetPair != null;
                assert cellsSetPair.a() != cellsSetPair.b();
                assert rowPairs != null;
                assert columnPairs != null;

                Pair<Map<String, CellData>> maps = cellsSetPair.map(cells -> cells.stream()
                                .collect(Collectors.toMap(CellData::address, Function.identity())));

                List<IntPair> columnPairsFiltered = columnPairs.stream().filter(IntPair::isPaired).toList();

                return rowPairs.parallelStream().filter(IntPair::isPaired).flatMap(
                                rows -> columnPairsFiltered.stream().map(columns -> {
                                        Pair<String> addressPair = Side.map(
                                                        side -> CellsUtil.idxToAddress(rows.get(side),
                                                                        columns.get(side)));
                                        Pair<CellData> cellPair = Side
                                                        .map(side -> maps.get(side).get(addressPair.get(side)));

                                        return (cellPair.a() != null && cellPair.b() != null
                                                        && cellPair.a().dataEquals(cellPair.b())
                                                        || cellPair.a() == null && cellPair.b() == null)
                                                                        ? null
                                                                        : Side.map(side -> cellPair.get(side) != null
                                                                                        ? cellPair.get(side)
                                                                                        : CellData.empty(rows.get(side),
                                                                                                        columns.get(side)));
                                }).filter(Objects::nonNull)).toList();
        }
}
