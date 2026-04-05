package xyz.hotchpotch.hogandiff.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * リスト内における要素の順番に関わりなく、
 * 2つのリスト内の等しい要素同士を対応付ける {@link Matcher} の実装です。<br>
 * <br>
 * <strong>注意：</strong>
 * この実装は、重複要素を持つリストを受け付けません。<br>
 * 
 * @param <T> リストの要素の型
 * @author nmby
 */
/*package*/ class IdentityMatcher<T> extends MatcherBase<T> {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final Function<? super T, ?> idExtractor;

    /**
     * コンストラクタ
     */
    /*package*/ IdentityMatcher() {
        this(Function.identity());
    }
    
    /**
     * コンストラクタ
     * 
     * @param extractor 識別子抽出関数
     */
    /*package*/ IdentityMatcher(Function<? super T, ?> extractor) {
        super(null, null);
        
        assert extractor != null;
        
        this.idExtractor = extractor;
    }
    
    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException {@code listA}, {@code listB} のいずれかに重複要素が含まれる場合
     */
    @Override
    protected void makeIdxPairsPrecheck(
            List<? extends T> listA,
            List<? extends T> listB) {

        // 重複チェックのみ行う（マップは makeIdxPairsMain でローカルに構築する）。
        long distinctA = listA.stream().map(idExtractor).distinct().count();
        long distinctB = listB.stream().map(idExtractor).distinct().count();
        if (listA.size() != distinctA || listB.size() != distinctB) {
            throw new IllegalArgumentException("list has duplicate values.");
        }
    }

    @Override
    protected List<IntPair> makeIdxPairsMain(
            List<? extends T> listA,
            List<? extends T> listB) {

        // 親クラスでバリデーションチェック済み

        // ローカル変数としてマップを構築する（インスタンスフィールドへの副作用を排除）。
        HashMap<Object, Integer> mapA = new HashMap<>();
        for (int i = 0; i < listA.size(); i++) {
            mapA.put(idExtractor.apply(listA.get(i)), i);
        }
        HashMap<Object, Integer> mapB = new HashMap<>();
        for (int j = 0; j < listB.size(); j++) {
            mapB.put(idExtractor.apply(listB.get(j)), j);
        }

        List<IntPair> result = new ArrayList<>();

        mapA.forEach((elemA, i) -> {
            if (mapB.containsKey(elemA)) {
                result.add(IntPair.of(i, mapB.get(elemA)));
                mapB.remove(elemA);
            } else {
                result.add(IntPair.onlyA(i));
            }
        });
        mapB.values().forEach(j -> result.add(IntPair.onlyB(j)));
        
        result.sort(Comparator.naturalOrder());
        return result;
    }
}
