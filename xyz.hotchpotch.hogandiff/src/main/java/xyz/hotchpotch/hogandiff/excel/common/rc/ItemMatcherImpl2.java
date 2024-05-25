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
    private final ToIntFunction<CellData> horizontal;
    private final Comparator<CellData> horizontalComparator;
    
    /* package */ ItemMatcherImpl2(
            Function<CellData, Integer> vertical,
            ToIntFunction<CellData> horizontal,
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
        
        Pair<Set<Integer>> horizontalRedundants = horizontalPairs == null
                ? new Pair<>(Set.of(), Set.of())
                : Side.map(side -> horizontalPairs.stream()
                        .filter(pair -> pair.isOnly(side))
                        .map(pair -> pair.get(side))
                        .collect(Collectors.toSet()));
        
        List<List<CellData>> listA = convert(cellsSets.a(), horizontalRedundants.a());
        List<List<CellData>> listB = convert(cellsSets.b(), horizontalRedundants.b());
        
        double[] weightsA = weights(cellsSets.a(), horizontalRedundants.a());
        double[] weightsB = weights(cellsSets.b(), horizontalRedundants.b());
        
        Matcher<List<CellData>> matcher = Matcher.minimumEditDistanceMatcherOf(
                gapEvaluator(weightsA),
                gapEvaluator(weightsB),
                diffEvaluator(weightsA, weightsB));
        
        return matcher.makeIdxPairs(listA, listB);
    }
    
    private List<List<CellData>> convert(
            Set<CellData> cells,
            Set<Integer> horizontalRedundants) {
        
        Map<Integer, List<CellData>> map = cells.parallelStream()
                .filter(cell -> !horizontalRedundants.contains(horizontal.applyAsInt(cell)))
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
    
    private double[] weights(
            Set<CellData> cells,
            Set<Integer> horizontalRedundants) {
        
        Map<Integer, Set<String>> map = cells.parallelStream()
                .filter(cell -> !horizontalRedundants.contains(horizontal.applyAsInt(cell)))
                .collect(Collectors.groupingBy(
                        horizontal::applyAsInt,
                        Collectors.mapping(CellData::content, Collectors.toSet())));
        
        int max = map.keySet().stream().mapToInt(i -> i).max().orElse(0);
        double[] weights = new double[max + 1];
        
        map.entrySet().parallelStream().forEach(entry -> {
            int key = entry.getKey();
            Set<String> strs = entry.getValue();
            int sumLen = strs.parallelStream().mapToInt(String::length).sum();
            weights[key] = Math.sqrt(sumLen);
        });
        
        return weights;
    }
    
    private ToIntFunction<List<CellData>> gapEvaluator(double[] weights) {
        return (list) -> (int) list.parallelStream()
                .mapToInt(horizontal)
                .mapToDouble(i -> weights[i])
                .sum();
    }
    
    private ToIntBiFunction<List<CellData>, List<CellData>> diffEvaluator(
            double[] weightsA,
            double[] weightsB) {
        
        return (list1, list2) -> {
            int comp = 0;
            double cost = 0d;
            CellData cell1 = null;
            CellData cell2 = null;
            int idx1 = 0;
            int idx2 = 0;
            
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
                    cost += weightsA[horizontal.applyAsInt(cell1)];
                } else if (0 < comp) {
                    cost += weightsB[horizontal.applyAsInt(cell2)];
                } else if (!cell1.contentEquals(cell2)) {
                    // TODO: セルコメント加味の要否について再検討する。
                    cost += weightsA[horizontal.applyAsInt(cell1)] + weightsB[horizontal.applyAsInt(cell2)];
                }
            }
            
            if (idx1 < list1.size()) {
                cost += list1.subList(idx1, list1.size()).stream()
                        .mapToDouble(c1 -> weightsA[horizontal.applyAsInt(c1)])
                        .sum();
            }
            if (idx2 < list2.size()) {
                cost += list2.subList(idx2, list2.size()).stream()
                        .mapToDouble(c2 -> weightsB[horizontal.applyAsInt(c2)])
                        .sum();
            }
            return (int) cost;
        };
    }
}
