package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 縦方向の挿入／削除を考慮せずに単純に縦インデックスで縦方向の対応付けを行う
 * {@link ItemMatcher} の実装です。<br>
 * 
 * @author nmby
 */
/* package */ class ItemMatcherImpl0 implements ItemMatcher {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final Function<CellData, Integer> vertical;
    
    /**
     * コンストラクタ
     * 
     * @param vertical 縦インデックス抽出関数
     */
    /* package */ ItemMatcherImpl0(Function<CellData, Integer> vertical) {
        assert vertical != null;
        
        this.vertical = vertical;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException {@code cellsSets} が {@code null} の場合
     */
    @Override
    public List<IntPair> makePairs(
            Pair<Set<CellData>> cellsSets,
            List<IntPair> horizontalPairs) {
        
        Objects.requireNonNull(cellsSets, "cellsSets");
        
        int min1 = cellsSets.a().parallelStream().mapToInt(cell -> vertical.apply(cell)).min().orElse(0);
        int max1 = cellsSets.a().parallelStream().mapToInt(cell -> vertical.apply(cell)).max().orElse(0);
        int min2 = cellsSets.b().parallelStream().mapToInt(cell -> vertical.apply(cell)).min().orElse(0);
        int max2 = cellsSets.b().parallelStream().mapToInt(cell -> vertical.apply(cell)).max().orElse(0);
        
        return IntStream.rangeClosed(Math.min(min1, min2), Math.max(max1, max2))
                .mapToObj(n -> IntPair.of(n, n))
                .toList();
    }
}
