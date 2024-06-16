package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.SheetResult.Stats;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダツリー同士の比較結果を表す不変クラスです。<br>
 * 
 * @author nmby
 * 
 * @param treeCompareInfo フォルダツリー比較情報
 * @param dirResults 比較対象フォルダパスのペアに対するフォルダ比較結果のマップ
 */
public record TreeResult(
        TreeCompareInfo treeCompareInfo,
        Map<Pair<DirInfo>, Optional<DirResult>> dirResults)
        implements Result {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * フォルダペアをユーザー表示用に整形して返します。<br>
     * 
     * @param id このフォルダペアの識別子。
     * @param dirInfoPair フォルダペア情報
     * @return フォルダペアの整形済み文字列
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static String formatDirsInfoPair(
            String id,
            Pair<DirInfo> dirInfoPair) {
        
        Objects.requireNonNull(id);
        Objects.requireNonNull(dirInfoPair);
        
        return "    - %s%n    - %s%n".formatted(
                dirInfoPair.hasA()
                        ? "【A%s】 %s".formatted(id, dirInfoPair.a().dirPath())
                        : rb.getString("excel.TreeResult.010"),
                dirInfoPair.hasB()
                        ? "【B%s】 %s".formatted(id, dirInfoPair.b().dirPath())
                        : rb.getString("excel.TreeResult.010"));
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ<br>
     * 
     * @param treeCompareInfo フォルダツリー比較情報
     * @param dirResults 比較対象フォルダパスのペアに対するフォルダ比較結果のマップ
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public TreeResult(
            TreeCompareInfo treeCompareInfo,
            Map<Pair<DirInfo>, Optional<DirResult>> dirResults) {
        
        Objects.requireNonNull(treeCompareInfo);
        Objects.requireNonNull(dirResults);
        
        this.treeCompareInfo = treeCompareInfo;
        this.dirResults = Map.copyOf(dirResults);
    }
    
    /**
     * この比較結果における差分の有無を返します。<br>
     * 
     * @return 差分ありの場合は {@code true}
     */
    public boolean hasDiff() {
        return treeCompareInfo.childPairs().stream()
                .map(dirResults::get)
                .anyMatch(r -> r.isEmpty() || r.get().hasDiff());
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
        
        for (int i = 0; i < treeCompareInfo.childPairs().size(); i++) {
            Pair<DirInfo> dirInfoPair = treeCompareInfo.childPairs().get(i);
            DirCompareInfo dirCompareInfo = treeCompareInfo.childCompareInfos().get(dirInfoPair).get();
            Optional<DirResult> dirResult = dirResults.get(dirInfoPair);
            
            str.append(formatDirsInfoPair(Integer.toString(i + 1), dirCompareInfo.parentPair()));
            
            if (dirCompareInfo.parentPair().isPaired()) {
                str.append(diffDescriptor.apply(dirResult));
            } else {
                str.append(BR);
            }
        }
        
        return str.toString();
    }
    
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        
        str.append(rb.getString("excel.TreeResult.020").formatted("A"))
                .append(treeCompareInfo.parentPair().a().dirPath())
                .append(BR);
        str.append(rb.getString("excel.TreeResult.020").formatted("B"))
                .append(treeCompareInfo.parentPair().b().dirPath())
                .append(BR);
        
        str.append(BR);
        str.append(rb.getString("excel.TreeResult.030")).append(BR);
        str.append(getDiffSummary());
        str.append(rb.getString("excel.TreeResult.040")).append(BR);
        str.append(getDiffDetail());
        
        return str.toString();
    }
    
    @Override
    public List<Stats> getSheetStats() {
        return dirResults.values().stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(DirResult::getSheetStats)
                .flatMap(List::stream)
                .toList();
    }
}
