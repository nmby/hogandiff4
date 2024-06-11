package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * フォルダツリー比較情報を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public class TreeCompareInfo {
    
    // [static members] ********************************************************
    
    /**
     * 与えられたマッチャーを使用して新たな {@link TreeCompareInfo} インスタンスを生成します。<br>
     * 
     * @param topDirInfoPair 比較対象フォルダの情報
     * @param dirsMatcher フォルダの組み合わせを決めるマッチャー
     * @param bookNamesMatcher Excelブック名の組み合わせを決めるマッチャー
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @param readPasswords 読み取りパスワード
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static TreeCompareInfo of(
            Pair<DirInfo> topDirInfoPair,
            Matcher<DirInfo> dirsMatcher,
            Matcher<String> bookNamesMatcher,
            Matcher<String> sheetNamesMatcher,
            Map<Path, String> readPasswords) {
        
        Objects.requireNonNull(topDirInfoPair);
        Objects.requireNonNull(dirsMatcher);
        Objects.requireNonNull(bookNamesMatcher);
        Objects.requireNonNull(sheetNamesMatcher);
        Objects.requireNonNull(readPasswords);
        
        List<Pair<DirInfo>> dirInfoPairs;
        if (topDirInfoPair.isPaired()) {
            dirInfoPairs = dirsMatcher.makeItemPairs(
                    topDirInfoPair.a().children(),
                    topDirInfoPair.b().children());
        } else if (topDirInfoPair.hasA()) {
            dirInfoPairs = topDirInfoPair.a().children().stream()
                    .map(dirInfo -> Pair.ofOnly(Side.A, dirInfo))
                    .toList();
        } else if (topDirInfoPair.hasB()) {
            dirInfoPairs = topDirInfoPair.b().children().stream()
                    .map(dirInfo -> Pair.ofOnly(Side.B, dirInfo))
                    .toList();
        } else {
            dirInfoPairs = List.of();
        }
        
        Map<Pair<DirInfo>, Optional<DirCompareInfo>> dirCompareInfos = dirInfoPairs.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        dirInfoPair -> Optional.of(DirCompareInfo.of(
                                dirInfoPair,
                                bookNamesMatcher,
                                sheetNamesMatcher,
                                readPasswords))));
        
        return new TreeCompareInfo(
                topDirInfoPair,
                dirInfoPairs,
                dirCompareInfos);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> topDirInfoPair;
    private final List<Pair<DirInfo>> dirInfoPairs;
    private final Map<Pair<DirInfo>, Optional<DirCompareInfo>> dirCompareInfos;
    
    /**
     * コンストラクタ
     * 
     * @param topDirInfoPair 比較対象トップフォルダの情報
     * @param dirInfoPairs フォルダの組み合わせ
     * @param dirCompareInfos Excelブックの組み合わせ情報
     * @throws NullPointerException
     *      {@code topDirInfoPair}, {@code dirCompareInfos} のいずれかが {@code null} の場合
     */
    private TreeCompareInfo(
            Pair<DirInfo> topDirInfoPair,
            List<Pair<DirInfo>> dirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirCompareInfo>> dirCompareInfos) {
        
        assert topDirInfoPair != null;
        assert dirInfoPairs != null;
        assert dirCompareInfos != null;
        
        this.topDirInfoPair = topDirInfoPair;
        this.dirInfoPairs = List.copyOf(dirInfoPairs);
        this.dirCompareInfos = Map.copyOf(dirCompareInfos);
    }
    
    /**
     * 比較対象トップフォルダ情報を返します。<br>
     * 
     * @return 比較対象トップフォルダ情報
     */
    public Pair<DirInfo> topDirInfoPair() {
        return topDirInfoPair;
    }
    
    /**
     * フォルダの組み合わせを返します。<br>
     * 
     * @return フォルダの組み合わせ
     */
    public List<Pair<DirInfo>> dirInfoPairs() {
        return dirInfoPairs;
    }
    
    /**
     * フォルダ比較情報を返します。<br>
     * 
     * @return フォルダ比較情報
     */
    public Map<Pair<DirInfo>, Optional<DirCompareInfo>> dirCompareInfos() {
        return dirCompareInfos;
    }
}
