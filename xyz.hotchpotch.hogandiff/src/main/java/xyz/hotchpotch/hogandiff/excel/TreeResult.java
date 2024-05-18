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
 * 
 * @param topDirPair 比較対象のトップフォルダのペア
 * @param pairDataList 比較するフォルダ同士の組み合わせを表すリスト
 * @param dirResults 比較対象フォルダパスのペアに対するフォルダ比較結果のマップ
 */
public record TreeResult(
        Pair<DirInfo> topDirPair,
        List<DirPairData> pairDataList,
        Map<Pair<Path>, Optional<DirResult>> dirResults)
        implements Result {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * フォルダペアをユーザー表示用に整形して返します。<br>
     * 
     * @param id このフォルダペアの識別子。
     * @param dirPair フォルダペア情報
     * @return フォルダペアの整形済み文字列
     * @throws NullPointerException {@code id}, {@code dirPair} のいずれかが {@code null} の場合
     */
    public static String formatDirsPair(
            String id,
            Pair<DirInfo> dirPair) {
        
        Objects.requireNonNull(dirPair, "dirPair");
        
        return "    - %s%n    - %s%n".formatted(
                dirPair.hasA()
                        ? "【A%s】 %s".formatted(id, dirPair.a().path())
                        : rb.getString("excel.TreeResult.010"),
                dirPair.hasB()
                        ? "【B%s】 %s".formatted(id, dirPair.b().path())
                        : rb.getString("excel.TreeResult.010"));
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ<br>
     * 
     * @param topDirPair 比較対象のトップフォルダのペア
     * @param pairDataList 比較するフォルダ同士の組み合わせを表すリスト
     * @param dirResults 比較対象フォルダパスのペアに対するフォルダ比較結果のマップ
     * @throws NullPointerException
     *          {@code topDirPair}, {@code pairDataList}, {@code dirResults} のいずれかが {@code null} の場合
     */
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
    
    /**
     * この比較結果における差分の有無を返します。<br>
     * 
     * @return 差分ありの場合は {@code true}
     */
    public boolean hasDiff() {
        return pairDataList.stream()
                .map(DirPairData::dirPair)
                .map(p -> p.map(DirInfo::path))
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
        
        for (int i = 0; i < pairDataList.size(); i++) {
            DirPairData pairData = pairDataList.get(i);
            Optional<DirResult> dirResult = dirResults.get(pairData.dirPair().map(DirInfo::path));
            
            str.append(formatDirsPair(pairData.id(), pairData.dirPair()));
            
            if (pairData.dirPair().isPaired()) {
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
}
