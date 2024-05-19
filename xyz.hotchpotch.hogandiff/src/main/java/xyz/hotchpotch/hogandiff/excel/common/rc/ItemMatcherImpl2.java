package xyz.hotchpotch.hogandiff.excel.common.rc;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 縦方向の挿入／削除を考慮して縦方向の対応付けを行う {@link ItemMatcher} の実装です。<br>
 * 横方向の挿入／削除を考慮しない場合と考慮する場合の双方に対応できます。<br>
 * 横方向の要素について重みづけをして評価します。<br>
 * 
 * @author nmby
 */
public class ItemMatcherImpl2 implements ItemMatcher {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final Function<CellData, Integer> vertical;
    private final Function<CellData, Integer> horizontal;
    private final Comparator<CellData> horizontalComparator;
    
    /* package */ ItemMatcherImpl2(
            Function<CellData, Integer> vertical,
            Function<CellData, Integer> horizontal,
            Comparator<CellData> horizontalComparator) {
        
        assert vertical != null;
        assert horizontal != null;
        assert horizontalComparator != null;
        
        this.vertical = vertical;
        this.horizontal = horizontal;
        this.horizontalComparator = horizontalComparator;
    }
    
    @Override
    public List<IntPair> makePairs(
            Pair<Set<CellData>> cellsSets,
            List<IntPair> horizontalPairs) {
        
        Objects.requireNonNull(cellsSets, "cellsSets");
        
        Pair<Set<Integer>> horizontalRedundants = Side.map(
                side -> horizontalPairs == null
                        ? null
                        : horizontalPairs.stream()
                                .filter(pair -> pair.isOnly(side))
                                .map(pair -> pair.get(side))
                                .collect(Collectors.toSet()));
        
        Pair<List<List<CellData>>> lists = Side.unsafeMap(
                side -> convert(cellsSets.get(side), horizontalRedundants.get(side)));
        
        Pair<Map<Integer, Double>> weights = Side.map(
                side -> weights(cellsSets.get(side), horizontalRedundants.get(side)));
        
        Matcher<List<CellData>> matcher = Matcher.minimumEditDistanceMatcherOf(
                gapEvaluator(weights.a()),
                gapEvaluator(weights.b()),
                diffEvaluator(horizontalComparator, weights));
        
        return matcher.makeIdxPairs(lists.a(), lists.b());
    }
    
    private List<List<CellData>> convert(
            Set<CellData> cells,
            Set<Integer> horizontalRedundants) {
        
        Map<Integer, List<CellData>> map = cells.parallelStream()
                .filter(cell -> horizontalRedundants == null || !horizontalRedundants.contains(horizontal.apply(cell)))
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
    
    private Map<Integer, Double> weights(
            Set<CellData> cells,
            Set<Integer> horizontalRedundants) {
        
        Map<Integer, Set<String>> map = cells.parallelStream()
                .filter(cell -> horizontalRedundants == null || !horizontalRedundants.contains(horizontal.apply(cell)))
                .collect(Collectors.groupingBy(
                        horizontal,
                        Collectors.mapping(CellData::content, Collectors.toSet())));
        
        return map.entrySet().parallelStream()
                .map(entry -> {
                    int key = entry.getKey();
                    Set<String> strs = entry.getValue();
                    int sumLen = strs.parallelStream().mapToInt(String::length).sum();
                    return Map.entry(key, Math.sqrt(sumLen));
                })
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }
    
    private ToIntFunction<List<CellData>> gapEvaluator(Map<Integer, Double> weights) {
        return (list) -> (int) list.parallelStream()
                .mapToInt(horizontal::apply)
                .mapToDouble(weights::get)
                .sum();
    }
    
    private ToIntBiFunction<List<CellData>, List<CellData>> diffEvaluator(
            Comparator<CellData> horizontalComparator,
            Pair<Map<Integer, Double>> weights) {
        
        return (list1, list2) -> {
            int comp = 0;
            double cost = 0d;
            CellData cell1 = null;
            CellData cell2 = null;
            Iterator<CellData> itr1 = list1.iterator();
            Iterator<CellData> itr2 = list2.iterator();
            
            while (itr1.hasNext() && itr2.hasNext()) {
                if (comp <= 0) {
                    cell1 = itr1.next();
                }
                if (0 <= comp) {
                    cell2 = itr2.next();
                }
                
                comp = horizontalComparator.compare(cell1, cell2);
                
                if (comp < 0) {
                    cost += weights.a().get(horizontal.apply(cell1));
                } else if (0 < comp) {
                    cost += weights.b().get(horizontal.apply(cell2));
                } else if (!cell1.contentEquals(cell2)) {
                    // TODO: セルコメント加味の要否について再検討する。
                    cost += weights.a().get(horizontal.apply(cell1)) + weights.b().get(horizontal.apply(cell2));
                }
            }
            
            while (itr1.hasNext()) {
                cell1 = itr1.next();
                cost += weights.a().get(horizontal.apply(cell1));
            }
            while (itr2.hasNext()) {
                cell2 = itr2.next();
                cost += weights.b().get(horizontal.apply(cell2));
            }
            return (int) cost;
        };
    }
}
