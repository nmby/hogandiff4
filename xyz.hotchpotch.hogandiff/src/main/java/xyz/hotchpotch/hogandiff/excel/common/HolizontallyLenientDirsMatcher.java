package xyz.hotchpotch.hogandiff.excel.common;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

public class HolizontallyLenientDirsMatcher extends VerticallyStrictDirsMatcher {
    
    // [static members] ********************************************************
    
    private static final int BASE_WEIGHT = 5;
    private static final int DIR_WEIGHT = 25;
    private static final int BOOK_WEIGHT = 10;
    
    private static final Matcher<String> namesMatcher = Matcher.identityMatcher();
    
    private static final ToIntFunction<DirInfo> gapCost = dirInfo -> 0
            + dirInfo.children().size() * DIR_WEIGHT
            + dirInfo.bookNames().size() * BOOK_WEIGHT;
    
    private static final ToIntBiFunction<DirInfo, DirInfo> diffCost = (dirInfo1, dirInfo2) -> {
        String dirName1 = dirInfo1.path().getFileName().toString();
        String dirName2 = dirInfo2.path().getFileName().toString();
        if (dirName1.equals(dirName2)) {
            // フォルダ名が同じならばフォルダの内容を加味せずにマッチングさせる。
            return 0;
        }
        
        List<String> childNames1 = dirInfo1.children().stream().map(c -> c.path().getFileName().toString()).toList();
        List<String> childNames2 = dirInfo2.children().stream().map(c -> c.path().getFileName().toString()).toList();
        List<IntPair> childPairs = namesMatcher.makePairs(childNames1, childNames2);
        List<IntPair> bookNamePairs = namesMatcher.makePairs(dirInfo1.bookNames(), dirInfo2.bookNames());
        
        int unmatchDirs = (int) childPairs.stream().filter(Predicate.not(IntPair::isPaired)).count();
        int unmatchBooks = (int) bookNamePairs.stream().filter(Predicate.not(IntPair::isPaired)).count();
        
        return BASE_WEIGHT + unmatchDirs * DIR_WEIGHT + unmatchBooks * BOOK_WEIGHT;
    };
    
    private static final Matcher<DirInfo> coreMatcher = Matcher.greedyMatcherOf(
            gapCost::applyAsInt,
            diffCost::applyAsInt);
    
    public static DirsMatcher of() {
        return new HolizontallyLenientDirsMatcher();
    }
    
    // [instance members] ******************************************************
    
    private HolizontallyLenientDirsMatcher() {
        super();
        System.out.println("★★★ HolizontallyLenientDirsMatcher ★★★");
    }
    
    @Override
    protected List<Pair<DirInfo>> pairingDirs(
            List<DirInfo> dirs1,
            List<DirInfo> dirs2) {
        
        assert dirs1 != null;
        assert dirs2 != null;
        
        List<IntPair> pairs = coreMatcher.makePairs(dirs1, dirs2);
        
        return pairs.stream()
                .map(p -> new Pair<>(
                        p.hasA() ? dirs1.get(p.a()) : null,
                        p.hasB() ? dirs2.get(p.b()) : null))
                .toList();
    }
}
