package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher.DirPairData;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダツリー同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public record TreeResult(
        Pair<DirInfo> topDirPair,
        List<DirPairData> pairDataList,
        Map<Pair<Path>, Optional<DirResult>> dirResults) {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final ResourceBundle rb = AppMain.appResource.get();
    
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
        
        return "    - %s%n    - %s%n".formatted(
                dirPair.hasA()
                        ? "【A%d】 %s".formatted(i + 1, dirPair.a().path())
                        : rb.getString("excel.TreeResult.010"),
                dirPair.hasB()
                        ? "【B%d】 %s".formatted(i + 1, dirPair.b().path())
                        : rb.getString("excel.TreeResult.010"));
    }
    
    // [instance members] ******************************************************
    
    public TreeResult(
            Pair<DirInfo> topDirPair,
            List<DirPairData> pairDataList,
            Map<Pair<Path>, Optional<DirResult>> dirResults) {
        
        Objects.requireNonNull(topDirPair, "topDirPair");
        Objects.requireNonNull(pairDataList, "pairDataList");
        Objects.requireNonNull(dirResults, "dirResults");
        
        this.topDirPair = topDirPair;
        this.pairDataList = List.copyOf(pairDataList);
        this.dirResults = Map.copyOf(dirResults);
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        str.append(rb.getString("excel.TreeResult.020").formatted("A"))
                .append(topDirPair.a().path())
                .append(BR);
        str.append(rb.getString("excel.TreeResult.020").formatted("B"))
                .append(topDirPair.b().path())
                .append(BR);
        
        str.append(BR);
        str.append(rb.getString("excel.TreeResult.030")).append(BR);
        str.append(getDiffSummary());
        str.append(rb.getString("excel.TreeResult.040")).append(BR);
        str.append(getDiffDetail());
        
        return str.toString();
    }
    
    private String getDiffSummary() {
        return getDiffText(dirResult -> "        - %s%n%n".formatted(dirResult.isPresent()
                ? dirResult.get().getDiffSimpleSummary()
                : rb.getString("excel.TreeResult.050")));
    }
    
    private String getDiffDetail() {
        return getDiffText(dirResult -> dirResult.isPresent()
                ? dirResult.get().getDiffDetail().indent(4).replace("\n", BR)
                : "        " + rb.getString("excel.TreeResult.050") + BR + BR);
    }
    
    private String getDiffText(Function<Optional<DirResult>, String> diffDescriptor) {
        StringBuilder str = new StringBuilder();
        
        for (int i = 0; i < pairDataList.size(); i++) {
            DirPairData pairData = pairDataList.get(i);
            Optional<DirResult> dirResult = dirResults.get(pairData.dirPair().map(DirInfo::path));
            
            str.append(formatDirsPair(pairData.num() - 1, pairData.dirPair()));
            
            if (pairData.dirPair().isPaired()) {
                str.append(diffDescriptor.apply(dirResult));
            } else {
                str.append(BR);
            }
        }
        
        return str.toString();
    }
}
