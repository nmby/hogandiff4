package xyz.hotchpotch.hogandiff.logic.plain.matchers;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.logic.CellData;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * 縦方向の挿入／削除を考慮して縦方向の対応付けを行う {@link ItemMatcher} の実装です。<br>
 * 横方向の挿入／削除を考慮しない場合と考慮する場合の双方に対応できます。<br>
 * 横方向の要素の重みづけは行いません。<br>
 * 
 * @author nmby
 */
public class ItemMatcherImpl1 implements ItemMatcher {
    
    // [static members] ********************************************************
    
    /** 余剰評価関数 */
    private static final ToIntFunction<List<CellData>> gapEvaluator = List::size;
    
    /**
     * 差分評価関数を返します。<br>
     * 
     * @param horizontalComparator 横方向の比較関数
     * @return 差分評価関数
     */
    private static ToIntBiFunction<List<CellData>, List<CellData>> diffEvaluator(
            Comparator<CellData> horizontalComparator) {
        
        assert horizontalComparator != null;
        
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
    
    private final ToIntFunction<CellData> vertical;
    private final ToIntFunction<CellData> horizontal;
    private final Comparator<CellData> horizontalComparator;
    private final Matcher<List<CellData>> matcher;
    
    /**
     * コンストラクタ
     * 
     * @param vertical             縦インデックス抽出関数
     * @param horizontal           横インデックス抽出関数
     * @param horizontalComparator 横方向比較関数
     */
    /* package */ ItemMatcherImpl1(
            ToIntFunction<CellData> vertical,
            ToIntFunction<CellData> horizontal,
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
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException {@code cellsSetPair} が {@code null} の場合
     */
    @Override
    public List<IntPair> makePairs(
            Pair<Set<CellData>> cellsSetPair,
            List<IntPair> horizontalPairs) {
        
        Objects.requireNonNull(cellsSetPair);
        
        Pair<Set<Integer>> horizontalRedundants = ItemMatcher.extractRedundants(horizontalPairs);
        
        Pair<List<List<CellData>>> listPair = ItemMatcher.convertCellsToList(
                cellsSetPair,
                horizontalRedundants,
                vertical,
                horizontal,
                List.of(),
                list -> {
                    list.sort(horizontalComparator);
                    return list;
                });
        
        return matcher.makeIdxPairs(listPair.a(), listPair.b());
    }
}
