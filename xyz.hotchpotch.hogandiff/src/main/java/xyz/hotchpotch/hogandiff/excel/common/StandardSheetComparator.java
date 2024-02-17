package xyz.hotchpotch.hogandiff.excel.common;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.SheetComparator;
import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * {@link SheetComparator} の標準的な実装です。<br>
 *
 * @author nmby
 */
@Deprecated
public class StandardSheetComparator extends SheetComparatorBase {
    
    // [static members] ********************************************************
    
    /**
     *  縦方向の余剰／欠損を考慮しない場合のマッパーを返します。<br>
     * 
     * @param verticality 縦方向の座標を抽出する関数
     * @return 縦方向の要素同士を対応付けるマッパー
     */
    private static Mapper mapper(
            ToIntFunction<CellData> verticality) {
        
        assert verticality != null;
        
        return (cells1, cells2) -> {
            assert cells1 != null;
            assert cells2 != null;
            assert cells1 != cells2;
            
            IntPair range = range(cells1, cells2, verticality);
            return IntStream.rangeClosed(range.a(), range.b())
                    .mapToObj(n -> IntPair.of(n, n))
                    .toList();
        };
    }
    
    /**
     * 縦方向の余剰／欠損を考慮する場合のマッパーを返します。<br>
     * 
     * @param <U> 横方向のソートキーの型
     * @param verticality 縦方向の座標を抽出する関数
     * @param comparator 横方向のソートキーをソートするための比較関数
     * @return 縦方向の要素同士を対応付けるマッパー
     */
    private static <U> Mapper mapper(
            ToIntFunction<CellData> verticality,
            Comparator<CellData> comparator) {
        
        assert verticality != null;
        assert comparator != null;
        
        return (cells1, cells2) -> {
            assert cells1 != null;
            assert cells2 != null;
            assert cells1 != cells2;
            
            int start = range(cells1, cells2, verticality).a();
            List<List<CellData>> cellsList1 = convert(
                    cells1, start, verticality, comparator);
            List<List<CellData>> cellsList2 = convert(
                    cells2, start, verticality, comparator);
            
            Matcher<List<CellData>> matcher = Matcher.minimumEditDistanceMatcherOf(
                    List::size,
                    (list1, list2) -> evaluateDiff(list1, list2, comparator));
            
            return matcher.makeIdxPairs(cellsList1, cellsList2).stream()
                    .map(p -> p.map(i -> i + start))
                    .toList();
        };
    }
    
    /**
     * セルのリスト同士を比較して差分スコアを返します。<br>
     * 
     * @param list1 セルのリスト1
     * @param list2 セルのリスト2
     * @param comparator セルの位置（前後関係）を比較する {@link Comparator}
     * @return 差分スコア
     */
    private static int evaluateDiff(
            List<CellData> list1,
            List<CellData> list2,
            Comparator<CellData> comparator) {
        
        assert list1 != null;
        assert list2 != null;
        assert list1 != list2;
        assert comparator != null;
        
        Iterator<CellData> itr1 = list1.iterator();
        Iterator<CellData> itr2 = list2.iterator();
        
        int diff = 0;
        int comp = 0;
        CellData cell1 = null;
        CellData cell2 = null;
        
        while (itr1.hasNext() && itr2.hasNext()) {
            // 次に比較すべき要素を準備する。
            // 長さ0のリストも有り得るので、最初の要素の取得もループ内で実施している。
            if (comp <= 0) {
                cell1 = itr1.next();
            }
            if (0 <= comp) {
                cell2 = itr2.next();
            }
            
            // セルの位置を比較する。（内容の比較ではない。）
            comp = comparator.compare(cell1, cell2);
            
            if (comp == 0 && !cell1.dataEquals(cell2)) {
                // 位置は同じで内容が異なる場合は差分コスト2とする。
                diff += 2;
                
            } else if (comp != 0) {
                // 位置が異なる場合は差分コスト1とする。
                diff++;
            }
        }
        while (itr1.hasNext()) {
            diff++;
            itr1.next();
        }
        while (itr2.hasNext()) {
            diff++;
            itr2.next();
        }
        
        return diff;
    }
    
    /**
     * セルのセットを、セルのリストのリストに変換します。<br>
     * 一次元目のリストは verticality でソートしたものであり、
     * 二次元目のリストは同一 verticality 値のものを
     * extractor と comparator でソートしたものです。<br>
     * 
     * @param <U> 横方向のソートキーの型
     * @param cells セルセット
     * @param start セルセットのリスト化を始める最小インデックス値
     * @param verticality 縦方向の値（行または列）の抽出関数
     * @param comparator 横方向のソートキーの比較関数
     * @return セルのリストのリスト
     */
    private static <U> List<List<CellData>> convert(
            Set<CellData> cells,
            int start,
            ToIntFunction<CellData> verticality,
            Comparator<CellData> comparator) {
        
        assert cells != null;
        assert 0 <= start;
        assert verticality != null;
        assert comparator != null;
        
        Map<Integer, List<CellData>> map = cells.stream()
                .collect(Collectors.groupingBy(verticality::applyAsInt));
        
        int end = range(cells, verticality).b();
        return IntStream.rangeClosed(start, end).parallel()
                .mapToObj(i -> {
                    if (map.containsKey(i)) {
                        List<CellData> list = map.get(i);
                        list.sort(comparator);
                        return list;
                    } else {
                        return List.<CellData> of();
                    }
                })
                .toList();
    }
    
    private static IntPair range(
            Set<CellData> cells,
            ToIntFunction<CellData> axis) {
        
        assert cells != null;
        assert axis != null;
        
        int min = cells.stream()
                .mapToInt(axis)
                .min().orElse(0);
        int max = cells.stream()
                .mapToInt(axis)
                .max().orElse(0);
        
        return IntPair.of(min, max);
    }
    
    private static IntPair range(
            Set<CellData> cells1,
            Set<CellData> cells2,
            ToIntFunction<CellData> axis) {
        
        assert cells1 != null;
        assert cells2 != null;
        assert cells1 != cells2;
        assert axis != null;
        
        IntPair range1 = range(cells1, axis);
        IntPair range2 = range(cells2, axis);
        
        return IntPair.of(
                Math.min(range1.a(), range2.a()),
                Math.max(range1.b(), range2.b()));
    }
    
    /**
     * 新しいコンパレータを返します。<br>
     * 
     * @param considerRowGaps 比較において行の余剰／欠損を考慮する場合は {@code true}
     * @param considerColumnGaps 比較において列の余剰／欠損を考慮する場合は {@code true}
     * @return 新しいコンパレータ
     */
    public static SheetComparator of(boolean considerRowGaps, boolean considerColumnGaps) {
        return new StandardSheetComparator(considerRowGaps, considerColumnGaps);
    }
    
    // [instance members] ******************************************************
    
    private StandardSheetComparator(boolean considerRowGaps, boolean considerColumnGaps) {
        super(considerRowGaps, considerColumnGaps);
    }
    
    @Override
    protected Mapper rowsMapper() {
        if (considerRowGaps && considerColumnGaps) {
            return mapper(CellData::row, CellData::dataCompareTo);
        } else if (considerRowGaps) {
            return mapper(CellData::row, Comparator.comparingInt(CellData::column));
        } else if (considerColumnGaps) {
            return mapper(CellData::row);
        } else {
            return mapper(CellData::row);
        }
    }
    
    @Override
    protected Mapper columnsMapper() {
        if (considerRowGaps && considerColumnGaps) {
            return mapper(CellData::column, CellData::dataCompareTo);
        } else if (considerRowGaps) {
            return mapper(CellData::column);
        } else if (considerColumnGaps) {
            return mapper(CellData::column, Comparator.comparingInt(CellData::row));
        } else {
            return mapper(CellData::column);
        }
    }
}
