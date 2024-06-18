package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * フォルダツリー比較情報を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public final class TreeCompareInfo implements CompareInfo<DirInfo, DirInfo, DirCompareInfo> {
    
    // [static members] ********************************************************
    
    private static class TreeCompareInfoCreator {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final Matcher<DirInfo> dirInfosMatcher;
        private final Matcher<Path> bookPathsMatcher;
        private final Matcher<String> sheetNamesMatcher;
        private final Map<Path, String> readPasswords;
        
        private List<Pair<DirInfo>> dirInfoPairs;
        private Map<Pair<DirInfo>, Optional<DirCompareInfo>> dirCompareInfos;
        
        private TreeCompareInfoCreator(
                Matcher<DirInfo> dirInfosMatcher,
                Matcher<Path> bookPathsMatcher,
                Matcher<String> sheetNamesMatcher,
                Map<Path, String> readPasswords) {
            
            this.dirInfosMatcher = dirInfosMatcher;
            this.bookPathsMatcher = bookPathsMatcher;
            this.sheetNamesMatcher = sheetNamesMatcher;
            this.readPasswords = readPasswords;
        }
        
        private TreeCompareInfo execute(Pair<DirInfo> topDirInfoPair) {
            this.dirInfoPairs = new ArrayList<>();
            this.dirCompareInfos = new HashMap<>();
            
            doOneFloor(topDirInfoPair);
            
            return new TreeCompareInfo(topDirInfoPair, dirInfoPairs, dirCompareInfos);
        }
        
        private void doOneFloor(Pair<DirInfo> dirInfoPair) {
            assert dirInfoPair.hasA() || dirInfoPair.hasB();
            
            DirCompareInfo dirCompareInfo = DirCompareInfo.calculate(
                    dirInfoPair,
                    dirInfosMatcher,
                    bookPathsMatcher,
                    sheetNamesMatcher,
                    readPasswords);
            
            dirInfoPairs.add(dirInfoPair);
            dirCompareInfos.put(dirInfoPair, Optional.of(dirCompareInfo));
            
            if (dirInfoPair.isPaired()) {
                List<Pair<DirInfo>> childDirInfoPairs = dirInfosMatcher.makeItemPairs(
                        dirInfoPair.a().childDirInfos(),
                        dirInfoPair.b().childDirInfos());
                
                childDirInfoPairs.forEach(this::doOneFloor);
                
            } else {
                Side side = dirInfoPair.hasA() ? Side.A : Side.B;
                dirInfoPair.get(side).childDirInfos().stream()
                        .map(childDirInfo -> Pair.ofOnly(side, childDirInfo))
                        .forEach(this::doOneFloor);
            }
        }
    }
    
    /**
     * 与えられたマッチャーを使用して新たな {@link TreeCompareInfo} インスタンスを生成します。<br>
     * 
     * @param topDirInfoPair 比較対象フォルダの情報
     * @param dirInfosMatcher フォルダの組み合わせを決めるマッチャー
     * @param bookPathsMatcher Excelブックパスの組み合わせを決めるマッチャー
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @param readPasswords 読み取りパスワード
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static TreeCompareInfo calculate(
            Pair<DirInfo> topDirInfoPair,
            Matcher<DirInfo> dirInfosMatcher,
            Matcher<Path> bookPathsMatcher,
            Matcher<String> sheetNamesMatcher,
            Map<Path, String> readPasswords) {
        
        Objects.requireNonNull(topDirInfoPair);
        Objects.requireNonNull(dirInfosMatcher);
        Objects.requireNonNull(bookPathsMatcher);
        Objects.requireNonNull(sheetNamesMatcher);
        Objects.requireNonNull(readPasswords);
        
        TreeCompareInfoCreator creator = new TreeCompareInfoCreator(
                dirInfosMatcher,
                bookPathsMatcher,
                sheetNamesMatcher,
                readPasswords);
        
        return creator.execute(topDirInfoPair);
    }
    
    /**
     * 子フォルダの再帰比較はせずトップフォルダ同士のみ比較する場合の
     * {@link BookCompareInfo} インスタンスを生成します。<br>
     * 
     * @param topDirInfoPair 比較対象フォルダの情報
     * @param dirInfosMatcher フォルダの組み合わせを決めるマッチャー
     * @param bookPathsMatcher Excelブックパスの組み合わせを決めるマッチャー
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @param readPasswords 読み取りパスワード
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static TreeCompareInfo ofSingle(
            Pair<DirInfo> topDirInfoPair,
            Matcher<DirInfo> dirInfosMatcher,
            Matcher<Path> bookPathsMatcher,
            Matcher<String> sheetNamesMatcher,
            Map<Path, String> readPasswords) {
        
        Objects.requireNonNull(topDirInfoPair);
        Objects.requireNonNull(bookPathsMatcher);
        Objects.requireNonNull(sheetNamesMatcher);
        Objects.requireNonNull(readPasswords);
        
        DirCompareInfo dirCompareInfo = DirCompareInfo.calculate(
                topDirInfoPair,
                dirInfosMatcher,
                bookPathsMatcher,
                sheetNamesMatcher,
                readPasswords);
        
        return new TreeCompareInfo(
                topDirInfoPair,
                List.of(topDirInfoPair),
                Map.of(topDirInfoPair, Optional.of(dirCompareInfo)));
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
    
    @Override
    public Pair<DirInfo> parentPair() {
        return topDirInfoPair;
    }
    
    @Override
    public List<Pair<DirInfo>> childPairs() {
        return dirInfoPairs;
    }
    
    @Override
    public Map<Pair<DirInfo>, Optional<DirCompareInfo>> childCompareInfos() {
        return dirCompareInfos;
    }
}
