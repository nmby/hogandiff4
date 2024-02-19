package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;

/* package */ class RCMatcherUtil {
    
    // [static members] ********************************************************
    
    private static final ToIntFunction<List<CellData>> fastGapEvaluator = List::size;
    
    private static final ToIntFunction<List<CellData>> accurateGapEvaluator = list -> list.stream()
            // セルコメントも加味すべきかもしれないが、一旦セルコンテンツだけでやってみる。
            // TODO: セルコメント加味の要否について再検討する。
            .mapToInt(c -> c.content().length())
            .sum();
    
    private static ToIntBiFunction<List<CellData>, List<CellData>> diffEvaluator(
            Comparator<CellData> horizontalSorter,
            ToIntFunction<CellData> cellGapEvaluator,
            ToIntBiFunction<CellData, CellData> cellDiffEvaluator) {
        
        return (list1, list2) -> {
            Iterator<CellData> itr1 = list1.iterator();
            Iterator<CellData> itr2 = list2.iterator();
            CellData cell1 = null;
            CellData cell2 = null;
            int comp = 0;
            int cost = 0;
            
            while (itr1.hasNext() && itr2.hasNext()) {
                if (comp <= 0) {
                    cell1 = itr1.next();
                }
                if (0 <= comp) {
                    cell2 = itr2.next();
                }
                
                comp = horizontalSorter.compare(cell1, cell2);
                
                if (comp < 0) {
                    cost += cellGapEvaluator.applyAsInt(cell1);
                } else if (0 < comp) {
                    cost += cellGapEvaluator.applyAsInt(cell2);
                } else {
                    // TODO: セルコメント加味の要否について再検討する。
                    cost += cell1.contentEquals(cell2)
                            ? 0
                            : cellDiffEvaluator.applyAsInt(cell1, cell2);
                }
            }
            
            while (itr1.hasNext()) {
                cell1 = itr1.next();
                cost += cellGapEvaluator.applyAsInt(cell1);
            }
            while (itr2.hasNext()) {
                cell2 = itr2.next();
                cost += cellGapEvaluator.applyAsInt(cell2);
            }
            return cost;
        };
    }
    
    private static BiFunction<Set<CellData>, Set<CellData>, List<IntPair>> matcherOf(
            ToIntFunction<CellData> vertical,
            ToIntFunction<CellData> horizontal,
            boolean considerVGaps,
            boolean considerHGaps,
            boolean quicknessFirst) {
        
        if (!considerVGaps) {
            return new V0(vertical);
        }
        
        Comparator<CellData> horizontalComparator = considerHGaps
                // TODO: セルコメント加味の要否について再検討する。
                ? Comparator.comparing(CellData::content)
                : Comparator.comparingInt(horizontal);
        
        return new V1(
                vertical,
                horizontalComparator,
                quicknessFirst
                        ? RCMatcherUtil.fastGapEvaluator
                        : RCMatcherUtil.accurateGapEvaluator,
                quicknessFirst
                        ? RCMatcherUtil.diffEvaluator(
                                horizontalComparator,
                                c -> 1,
                                (c1, c2) -> 2)
                        : RCMatcherUtil.diffEvaluator(
                                horizontalComparator,
                                // TODO: セルコメント加味の要否について再検討する。
                                c -> c.content().length(),
                                (c1, c2) -> StringDiffUtil.levenshteinDistance(c1.content(), c2.content())));
    }
    
    public static BiFunction<Set<CellData>, Set<CellData>, List<IntPair>> rowsMatcherOf(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean quicknessFirst) {
        
        return matcherOf(
                CellData::row,
                CellData::column,
                considerRowGaps,
                considerColumnGaps,
                quicknessFirst);
    }
    
    public static BiFunction<Set<CellData>, Set<CellData>, List<IntPair>> columnsMatcherOf(
            boolean considerRowGaps,
            boolean considerColumnGaps,
            boolean quicknessFirst) {
        
        return matcherOf(
                CellData::column,
                CellData::row,
                considerColumnGaps,
                considerRowGaps,
                quicknessFirst);
    }
    
    // [instance members] ******************************************************
    
    private RCMatcherUtil() {
    }
}
