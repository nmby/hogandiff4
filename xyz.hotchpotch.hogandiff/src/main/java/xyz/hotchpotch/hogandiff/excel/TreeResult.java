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
        
        return "    - %s%n    - %s%n".formatted(
                dirPair.hasA()
                        ? "【A%s】 %s".formatted(id, dirPair.a().getPath())
                        : rb.getString("excel.TreeResult.010"),
                dirPair.hasB()
                        ? "【B%s】 %s".formatted(id, dirPair.b().getPath())
                        : rb.getString("excel.TreeResult.010"));
    }
    
    public static TreeResult of(
            Pair<DirInfo> topDirPair,
            List<Pair<DirInfo>> dirPairs,
            Map<Pair<DirInfo>, Optional<DirResult>> results) {
        
        Objects.requireNonNull(topDirPair, "topDirPair");
        Objects.requireNonNull(dirPairs, "dirPairs");
        Objects.requireNonNull(results, "results");
        
        return new TreeResult(
                topDirPair,
                dirPairs,
                results);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> topDirPair;
    private final List<Pair<DirInfo>> dirPairs;
    private final Map<Pair<DirInfo>, Optional<DirResult>> results;
    private final ResourceBundle rb = AppMain.appResource.get();
    
    private TreeResult(
            Pair<DirInfo> topDirPair,
            List<Pair<DirInfo>> dirPairs,
            Map<Pair<DirInfo>, Optional<DirResult>> results) {
        
        assert topDirPair != null;
        assert dirPairs != null;
        
        this.topDirPair = topDirPair;
        this.dirPairs = List.copyOf(dirPairs);
        this.results = Map.copyOf(results);
    }
}
