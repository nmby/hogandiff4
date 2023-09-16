package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダ同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public class DResult {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final Pair<DirData> dirData;
    private final List<Pair<String>> bookNamePairs;
    private final Map<Pair<String>, Optional<BResult>> results;
    
    private DResult(
            DirData dirData1,
            DirData dirData2,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, Optional<BResult>> results) {
        
        assert dirData1 != null;
        assert dirData2 != null;
        assert bookNamePairs != null;
        
        this.dirData = Pair.of(dirData1, dirData2);
        this.bookNamePairs = List.copyOf(bookNamePairs);
        this.results = Map.copyOf(results);
    }
}
