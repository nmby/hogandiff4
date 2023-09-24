package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダツリー同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public class TreeResult {
    
    // [static members] ********************************************************
    
    /**
     * フォルダペアをユーザー表示用に整形して返します。<br>
     * 
     * @param id このフォルダペアの識別子。
     * @param data フォルダペア情報
     * @return フォルダペアの整形済み文字列
     * @throws NullPointerException {@code id}, {@code dirPair} のいずれかが {@code null} の場合
     */
    public static String formatDirsPair(String id, Pair<DirInfo> dirPair) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(dirPair, "dirPair");
        
        ResourceBundle rb = AppMain.appResource.get();
        
        return "%s%n%s%n".formatted(
                dirPair.hasA()
                        ? "【A%s】 %s".formatted(id, dirPair.a().getPath())
                        : rb.getString("excel.TreeResult.010"),
                dirPair.hasB()
                        ? "【B%s】 %s".formatted(id, dirPair.b().getPath())
                        : rb.getString("excel.TreeResult.010"));
    }
    
    public static TreeResult of(
            // TODO: Pair<DirInfo>を取る形にした方がよい
            DirInfo topDirInfo1,
            DirInfo topDirInfo2,
            List<Pair<DirInfo>> dirPairs,
            Map<Pair<DirInfo>, Optional<DirResult>> results) {
        
        Objects.requireNonNull(topDirInfo1, "topDirInfo1");
        Objects.requireNonNull(topDirInfo2, "topDirInfo2");
        Objects.requireNonNull(dirPairs, "dirPairs");
        Objects.requireNonNull(results, "results");
        
        return new TreeResult(
                topDirInfo1,
                topDirInfo2,
                dirPairs,
                results);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> topDirInfoPair;
    private final List<Pair<DirInfo>> dirPairs;
    private final Map<Pair<DirInfo>, Optional<DirResult>> results;
    private final ResourceBundle rb = AppMain.appResource.get();
    
    private TreeResult(
            DirInfo topDirInfo1,
            DirInfo topDirInfo2,
            List<Pair<DirInfo>> dirPairs,
            Map<Pair<DirInfo>, Optional<DirResult>> results) {
        
        assert topDirInfo1 != null;
        assert topDirInfo2 != null;
        assert dirPairs != null;
        
        this.topDirInfoPair = Pair.of(topDirInfo1, topDirInfo2);
        this.dirPairs = List.copyOf(dirPairs);
        this.results = Map.copyOf(results);
    }
}
