package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 縦方向の挿入／削除を考慮して縦方向の対応付けを行う {@link ItemMatcher} の実装です。<br>
 * 横方向の挿入／削除を考慮しない場合と考慮する場合の双方に対応できます。<br>
 * 
 * @author nmby
 */
public class ItemMatcherImpl1 implements ItemMatcher {
    
    // [static members] ********************************************************
    
    private static final ToIntFunction<List<CellData>> gapEvaluator = List::size;
    
    private static ToIntBiFunction<List<CellData>, List<CellData>> diffEvaluator(
            Comparator<CellData> horizontalComparator) {
        
        return (list1, list2) -> {
            int idx1 = 0;
            int idx2 = 0;
            int comp = 0;
            int cost = 0;
            CellData cell1 = null;
            CellData cell2 = null;
            
            while (idx1 < list1.size() && idx2 < list2.size()) {
                if (comp <= 0) {
                    cell1 = list1.get(idx1);
                    idx1++;
                }
                if (0 <= comp) {
                    cell2 = list2.get(idx2);
                    idx2++;
                }
                
                comp = horizontalComparator.compare(cell1, cell2);
                
                if (comp < 0) {
                    cost++;
                } else if (0 < comp) {
                    cost++;
                } else if (!cell1.contentEquals(cell2)) {
                    // TODO: セルコメント加味の要否について再検討する。
                    cost += 2;
                }
            }
            
            if (idx1 < list1.size()) {
                cost += list1.size() - idx1;
            }
            if (idx2 < list2.size()) {
                cost += list2.size() - idx2;
            }
            return cost;
        };
    }
    
    // [instance members] ******************************************************
    
    private final Function<CellData, Integer> vertical;
    private final Function<CellData, Integer> horizontal;
    private final Comparator<CellData> horizontalComparator;
    private final Matcher<List<CellData>> matcher;
    
    /* package */ ItemMatcherImpl1(
            Function<CellData, Integer> vertical,
            Function<CellData, Integer> horizontal,
            Comparator<CellData> horizontalComparator) {
        
        assert vertical != null;
        assert horizontal != null;
        assert horizontalComparator != null;
        
        this.vertical = vertical;
        this.horizontal = horizontal;
        this.horizontalComparator = horizontalComparator;
        this.matcher = Matcher.minimumEditDistanceMatcherOf(
                gapEvaluator,
                diffEvaluator(horizontalComparator));
    }
    
    @Override
    public List<IntPair> makePairs(
            Set<CellData> cells1,
            Set<CellData> cells2,
            List<IntPair> horizontalPairs) {
        
        Objects.requireNonNull(cells1, "cells1");
        Objects.requireNonNull(cells2, "cells2");
        
        Set<Integer> redundants1 = horizontalPairs == null
                ? null
                : horizontalPairs.stream()
                        .filter(IntPair::isOnlyA)
                        .map(IntPair::a)
                        .collect(Collectors.toSet());
        List<List<CellData>> list1 = convert(cells1, redundants1);
        
        Set<Integer> redundants2 = horizontalPairs == null
                ? null
                : horizontalPairs.stream()
                        .filter(IntPair::isOnlyB)
                        .map(IntPair::b)
                        .collect(Collectors.toSet());
        List<List<CellData>> list2 = convert(cells2, redundants2);
        
        return matcher.makeIdxPairs(list1, list2);
    }
    
    private List<List<CellData>> convert(
            Set<CellData> cells,
            Set<Integer> redundants) {
        
        Map<Integer, List<CellData>> map = cells.parallelStream()
                .filter(cell -> redundants == null || !redundants.contains(horizontal.apply(cell)))
                .collect(Collectors.groupingBy(vertical));
        
        int max = map.keySet().stream().mapToInt(n -> n).max().orElse(0);
        
        return IntStream.rangeClosed(0, max).parallel()
                .mapToObj(i -> {
                    if (map.containsKey(i)) {
                        List<CellData> list = map.get(i);
                        list.sort(horizontalComparator);
                        return list;
                    } else {
                        return List.<CellData> of();
                    }
                })
                .toList();
    }
}
