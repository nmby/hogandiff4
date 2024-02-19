package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 縦方向の挿入／削除を考慮せずに縦方向の対応付けを行う {@link ItemMatcher} の実装です。<br>
 * 
 * @author nmby
 */
/* package */ class ItemMatcherImpl0 implements ItemMatcher {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final ToIntFunction<CellData> vertical;
    
    /* package */ ItemMatcherImpl0(ToIntFunction<CellData> vertical) {
        assert vertical != null;
        
        this.vertical = vertical;
    }
    
    @Override
    public List<IntPair> makePairs(Set<CellData> cells1, Set<CellData> cells2) {
        Objects.requireNonNull(cells1, "cells1");
        Objects.requireNonNull(cells2, "cells2");
        
        int min1 = cells1.parallelStream().mapToInt(vertical).min().orElse(0);
        int max1 = cells1.parallelStream().mapToInt(vertical).max().orElse(0);
        int min2 = cells2.parallelStream().mapToInt(vertical).min().orElse(0);
        int max2 = cells2.parallelStream().mapToInt(vertical).max().orElse(0);
        
        return IntStream.rangeClosed(Math.min(min1, min2), Math.max(max1, max2))
                .mapToObj(n -> IntPair.of(n, n))
                .toList();
    }
}
