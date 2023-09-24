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
    
    public static record DirPairData(
            int num,
            Pair<DirInfo> dirPair,
            List<Pair<String>> bookNamePairs) {
    }
    
    /**
     * フォルダペアをユーザー表示用に整形して返します。<br>
     * 
     * @param i このフォルダペアの識別番号。{@code i + 1} をユーザー向けに表示します。
     * @param data フォルダペア情報
     * @return フォルダペアの整形済み文字列
     * @throws NullPointerException {@code id}, {@code dirPair} のいずれかが {@code null} の場合
     */
    public static String formatDirsPair(int i, Pair<DirInfo> dirPair) {
        Objects.requireNonNull(dirPair, "dirPair");
        
        ResourceBundle rb = AppMain.appResource.get();
        
        return "    - %s%n    - %s%n".formatted(
                dirPair.hasA()
                        ? "【A%d】 %s".formatted(i + 1, dirPair.a().getPath())
                        : rb.getString("excel.TreeResult.010"),
                dirPair.hasB()
                        ? "【B%d】 %s".formatted(i + 1, dirPair.b().getPath())
                        : rb.getString("excel.TreeResult.010"));
    }
    
    public static TreeResult of(
            Pair<DirInfo> topDirPair,
            List<DirPairData> pairDataList,
            Map<Pair<DirInfo>, Optional<DirResult>> results) {
        
        Objects.requireNonNull(topDirPair, "topDirPair");
        Objects.requireNonNull(pairDataList, "pairDataList");
        Objects.requireNonNull(results, "results");
        
        return new TreeResult(
                topDirPair,
                pairDataList,
                results);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> topDirPair;
    private final List<DirPairData> pairDataList;
    private final Map<Pair<DirInfo>, Optional<DirResult>> results;
    private final ResourceBundle rb = AppMain.appResource.get();
    
    private TreeResult(
            Pair<DirInfo> topDirPair,
            List<DirPairData> pairDataList,
            Map<Pair<DirInfo>, Optional<DirResult>> results) {
        
        assert topDirPair != null;
        assert pairDataList != null;
        
        this.topDirPair = topDirPair;
        this.pairDataList = List.copyOf(pairDataList);
        this.results = Map.copyOf(results);
    }
}
