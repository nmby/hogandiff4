package xyz.hotchpotch.hogandiff.excel.common;

import java.util.List;
import java.util.function.Function;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;

public class StrictDirsMatcher extends VStrictDirsMatcherBase {
    
    // [static members] ********************************************************
    
    private static final Matcher<String> coreMatcher = Matcher.identityMatcher();
    
    public static DirsMatcher of() {
        return new StrictDirsMatcher();
    }
    
    // [instance members] ******************************************************
    
    private StrictDirsMatcher() {
        super();
    }
    
    @Override
    protected List<Pair<DirInfo>> pairingDirs(
            List<DirInfo> dirs1,
            List<DirInfo> dirs2) {
        
        assert dirs1 != null;
        assert dirs2 != null;
        
        Function<DirInfo, String> dirToName = d -> d.path().getFileName().toString();
        
        List<IntPair> pairs = coreMatcher.makePairs(
                dirs1.stream().map(dirToName::apply).toList(),
                dirs2.stream().map(dirToName::apply).toList());
        
        return pairs.stream()
                .map(p -> new Pair<>(
                        p.hasA() ? dirs1.get(p.a()) : null,
                        p.hasB() ? dirs2.get(p.b()) : null))
                .toList();
    }
}
