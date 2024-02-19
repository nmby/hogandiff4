package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 縦方向の挿入／削除を考慮して縦方向の対応付けを行う {@link ItemMatcher} の実装です。<br>
 * 
 * @author nmby
 */
public class ItemMatcherImpl1 implements ItemMatcher {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final ToIntFunction<CellData> vertical;
    private final Comparator<CellData> horizontalComparator;
    private final Matcher<List<CellData>> matcher;
    
    /* package */ ItemMatcherImpl1(
            ToIntFunction<CellData> vertical,
            Comparator<CellData> horizontalComparator,
            ToIntFunction<List<CellData>> gapEvaluator,
            ToIntBiFunction<List<CellData>, List<CellData>> diffEvaluator) {
        
        assert vertical != null;
        assert horizontalComparator != null;
        assert gapEvaluator != null;
        assert diffEvaluator != null;
        
        this.vertical = vertical;
        this.horizontalComparator = horizontalComparator;
        this.matcher = Matcher.minimumEditDistanceMatcherOf(
                gapEvaluator,
                diffEvaluator);
    }
    
    @Override
    public List<IntPair> makePairs(Set<CellData> cells1, Set<CellData> cells2) {
        Objects.requireNonNull(cells1, "cells1");
        Objects.requireNonNull(cells2, "cells2");
        
        List<List<CellData>> list1 = convert(cells1);
        List<List<CellData>> list2 = convert(cells2);
        
        return matcher.makeIdxPairs(list1, list2);
    }
    
    private List<List<CellData>> convert(Set<CellData> cells) {
        Map<Integer, List<CellData>> map = cells.parallelStream()
                .collect(Collectors.groupingBy(cell -> vertical.applyAsInt(cell)));
        
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
