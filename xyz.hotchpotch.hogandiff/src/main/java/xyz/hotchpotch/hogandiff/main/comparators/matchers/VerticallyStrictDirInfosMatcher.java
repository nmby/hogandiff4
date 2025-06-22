package xyz.hotchpotch.hogandiff.main.comparators.matchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.main.misc.excel.DirInfosMatcher;
import xyz.hotchpotch.hogandiff.main.models.DirInfo;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 同一階層のフォルダ同士をペアリングする {@link DirInfosMatcher} の実装です。<br>
 * 
 * @author nmby
 */
// FIXME: [No.11 機能改善] フォルダ階層の変更にも対応できる柔軟な {@link DirInfosMatcher} も実装する
public class VerticallyStrictDirInfosMatcher implements DirInfosMatcher {

    // [static members] ********************************************************

    private static final Function<DirInfo, String> dirNameExtractor = d -> d.dirPath().getFileName().toString();

    private static final Matcher<DirInfo> strictDirNamesMatcher = Matcher.identityMatcherOf(dirNameExtractor);

    private static final Matcher<DirInfo> fuzzyButSimpleDirsMatcher = Matcher.minimumCostFlowMatcherOf(
            d -> d.childDirInfos().size() + d.childBookInfos().size(),
            (d1, d2) -> {
                List<String> childrenNames1 = d1.childDirInfos().stream().map(dirNameExtractor).toList();
                List<String> childrenNames2 = d2.childDirInfos().stream().map(dirNameExtractor).toList();

                int gapChildren = (int) Matcher.identityMatcherOf().makeIdxPairs(childrenNames1, childrenNames2)
                        .stream().filter(Predicate.not(IntPair::isPaired)).count();
                int gapBookNames = (int) Matcher.identityMatcherOf()
                        .makeIdxPairs(d1.childBookInfos(), d2.childBookInfos()).stream()
                        .filter(Predicate.not(IntPair::isPaired)).count();

                return gapChildren + gapBookNames;
            });

    /**
     * {@link DirInfosMatcher} のインスタンスを返します。<br>
     * 
     * @param matchNamesStrictly フォルダ名の曖昧一致を許さない場合は {@code true}
     * @return マッチャー
     */
    public static DirInfosMatcher of(boolean matchNamesStrictly) {
        return new VerticallyStrictDirInfosMatcher(matchNamesStrictly
                ? strictDirNamesMatcher
                : Matcher.combinedMatcherOf(List.of(
                        strictDirNamesMatcher,
                        fuzzyButSimpleDirsMatcher)));
    }

    // [instance members] ******************************************************

    private final Matcher<DirInfo> coreMatcher;

    private VerticallyStrictDirInfosMatcher(Matcher<DirInfo> coreMatcher) {
        assert coreMatcher != null;
        this.coreMatcher = coreMatcher;
    }

    @Override
    public List<Pair<DirInfo>> pairingDirs(Pair<DirInfo> topDirInfos) {
        Objects.requireNonNull(topDirInfos);

        List<Pair<DirInfo>> resultPairs = new ArrayList<>();

        resultPairs.add(topDirInfos);

        pairingDirs2(resultPairs, topDirInfos);

        return List.copyOf(resultPairs);
    }

    private void pairingDirs2(
            List<Pair<DirInfo>> resultPairs,
            Pair<DirInfo> dirInfos) {

        assert resultPairs != null;
        assert dirInfos != null;

        List<Pair<DirInfo>> dirPairs = coreMatcher.makeItemPairs(
                dirInfos.a().childDirInfos(),
                dirInfos.b().childDirInfos());

        for (Pair<DirInfo> dirPair : dirPairs) {
            if (dirPair.isPaired()) {
                resultPairs.add(dirPair);
                pairingDirs2(resultPairs, dirPair);

            } else if (dirPair.isOnlyA()) {
                setAloneDirs(resultPairs, dirPair.a(), Side.A);

            } else if (dirPair.isOnlyB()) {
                setAloneDirs(resultPairs, dirPair.b(), Side.B);
            }
        }
    }

    private void setAloneDirs(
            List<Pair<DirInfo>> pairs,
            DirInfo dirInfo,
            Side side) {

        pairs.add(Pair.ofOnly(side, dirInfo));

        dirInfo.childDirInfos().forEach(d -> setAloneDirs(pairs, d, side));
    }
}
