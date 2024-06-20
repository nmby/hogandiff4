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
 * フォルダ比較情報を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public final class DirCompareInfo implements CompareInfo<DirInfo, Path, BookCompareInfo> {
    
    // [static members] ********************************************************
    
    /**
     * 階層状の {@link DirCompareInfo} の内容を一層に平坦化した情報を保持するレコードです。<br>
     * 
     * @param topDirInfoPair 比較対象フォルダ情報
     * @param dirInfoPairs 子フォルダ情報
     * @param dirCompareInfos 子フォルダ比較情報
     */
    public static record FlattenDirCompareInfo(
            Pair<DirInfo> topDirInfoPair,
            List<Pair<DirInfo>> dirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirCompareInfo>> dirCompareInfos) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
    }
    
    /**
     * 与えられたマッチャーを使用して新たな {@link DirCompareInfo} インスタンスを生成します。<br>
     * 
     * @param topDirInfoPair 比較対象フォルダ情報
     * @param dirInfosMatcher フォルダ情報の組み合わせを決めるマッチャー
     * @param bookPathsMatcher Excelブックパスの組み合わせを決めるマッチャー
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @param readPasswords 読み取りパスワード
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static DirCompareInfo calculate(
            Pair<DirInfo> topDirInfoPair,
            Matcher<DirInfo> dirInfosMatcher,
            Matcher<Path> bookPathsMatcher,
            Matcher<String> sheetNamesMatcher,
            Map<Path, String> readPasswords) {
        
        Objects.requireNonNull(topDirInfoPair);
        Objects.requireNonNull(bookPathsMatcher);
        Objects.requireNonNull(sheetNamesMatcher);
        Objects.requireNonNull(readPasswords);
        
        List<Pair<DirInfo>> dirInfoPairs;
        List<Pair<Path>> bookPathPairs;
        
        if (topDirInfoPair.isPaired()) {
            dirInfoPairs = dirInfosMatcher.makeItemPairs(
                    topDirInfoPair.a().childDirInfos(),
                    topDirInfoPair.b().childDirInfos());
            bookPathPairs = bookPathsMatcher.makeItemPairs(
                    topDirInfoPair.a().childBookPaths(),
                    topDirInfoPair.b().childBookPaths());
            
        } else if (topDirInfoPair.hasA()) {
            dirInfoPairs = topDirInfoPair.a().childDirInfos().stream()
                    .map(dirInfo -> Pair.ofOnly(Side.A, dirInfo))
                    .toList();
            bookPathPairs = topDirInfoPair.a().childBookPaths().stream()
                    .map(bookName -> Pair.ofOnly(Side.A, bookName))
                    .toList();
            
        } else if (topDirInfoPair.hasB()) {
            dirInfoPairs = topDirInfoPair.b().childDirInfos().stream()
                    .map(dirInfo -> Pair.ofOnly(Side.B, dirInfo))
                    .toList();
            bookPathPairs = topDirInfoPair.b().childBookPaths().stream()
                    .map(bookName -> Pair.ofOnly(Side.B, bookName))
                    .toList();
            
        } else {
            dirInfoPairs = List.of();
            bookPathPairs = List.of();
        }
        
        Map<Pair<DirInfo>, Optional<DirCompareInfo>> dirCompareInfos = new HashMap<>();
        Map<Pair<Path>, Optional<BookCompareInfo>> bookCompareInfos = new HashMap<>();
        
        for (Pair<DirInfo> dirInfoPair : dirInfoPairs) {
            DirCompareInfo dirCompareInfo = DirCompareInfo.calculate(
                    dirInfoPair,
                    dirInfosMatcher,
                    bookPathsMatcher,
                    sheetNamesMatcher,
                    readPasswords);
            dirCompareInfos.put(dirInfoPair, Optional.ofNullable(dirCompareInfo));
        }
        
        for (Pair<Path> bookPathPair : bookPathPairs) {
            BookCompareInfo bookCompareInfo = null;
            try {
                Pair<BookInfo> bookInfoPair = Side.unsafeMap(
                        side -> {
                            if (bookPathPair.has(side)) {
                                Path bookPath = bookPathPair.get(side);
                                String readPassword = readPasswords.get(bookPath);
                                SheetNamesLoader sheetNamesLoader = Factory.sheetNamesLoader(bookPath, readPassword);
                                return sheetNamesLoader.loadSheetNames(bookPath, readPassword);
                            } else {
                                return null;
                            }
                        });
                bookCompareInfo = bookInfoPair != null
                        ? BookCompareInfo.calculate(bookInfoPair, sheetNamesMatcher)
                        : null;
                
            } catch (ExcelHandlingException e) {
                // nop
            }
            bookCompareInfos.put(bookPathPair, Optional.ofNullable(bookCompareInfo));
        }
        
        return new DirCompareInfo(
                topDirInfoPair,
                dirInfoPairs,
                dirCompareInfos,
                bookPathPairs,
                bookCompareInfos);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> topDirInfoPair;
    private final List<Pair<DirInfo>> childDirInfoPairs;
    private final Map<Pair<DirInfo>, Optional<DirCompareInfo>> childDirCompareInfos;
    private final List<Pair<Path>> childBookPathPairs;
    private final Map<Pair<Path>, Optional<BookCompareInfo>> childBookCompareInfos;
    
    private DirCompareInfo(
            Pair<DirInfo> topDirInfoPair,
            List<Pair<DirInfo>> childDirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirCompareInfo>> childDirCompareInfos,
            List<Pair<Path>> childBookPathPairs,
            Map<Pair<Path>, Optional<BookCompareInfo>> childBookCompareInfos) {
        
        assert topDirInfoPair != null;
        assert childDirInfoPairs != null;
        assert childDirCompareInfos != null;
        assert childBookPathPairs != null;
        assert childBookCompareInfos != null;
        
        this.topDirInfoPair = topDirInfoPair;
        this.childDirInfoPairs = List.copyOf(childDirInfoPairs);
        this.childDirCompareInfos = Map.copyOf(childDirCompareInfos);
        this.childBookPathPairs = List.copyOf(childBookPathPairs);
        this.childBookCompareInfos = Map.copyOf(childBookCompareInfos);
    }
    
    @Override
    public Pair<DirInfo> parentPair() {
        return topDirInfoPair;
    }
    
    @Override
    public List<Pair<Path>> childPairs() {
        return childBookPathPairs;
    }
    
    @Override
    public Map<Pair<Path>, Optional<BookCompareInfo>> childCompareInfos() {
        return childBookCompareInfos;
    }
    
    /**
     * ツリー状の本オフジェクトの内容を一層に並べた
     * {@link FlattenDirCompareInfo} オブジェクトに変換して返します。<br>
     * 
     * @return 平坦化されたフォルダ比較情報
     */
    public FlattenDirCompareInfo flatten() {
        List<Pair<DirInfo>> accDirInfoPairs = new ArrayList<>();
        Map<Pair<DirInfo>, Optional<DirCompareInfo>> accDirCompareInfos = new HashMap<>();
        
        accDirInfoPairs.add(topDirInfoPair);
        accDirCompareInfos.put(topDirInfoPair, Optional.of(this));
        gather(this, accDirInfoPairs, accDirCompareInfos);
        
        return new FlattenDirCompareInfo(
                topDirInfoPair,
                accDirInfoPairs,
                accDirCompareInfos);
    }
    
    private void gather(
            DirCompareInfo dirCompareInfo,
            List<Pair<DirInfo>> accDirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirCompareInfo>> accDirCompareInfos) {
        
        for (Pair<DirInfo> childDirInfoPair : dirCompareInfo.childDirInfoPairs) {
            Optional<DirCompareInfo> childDirCompareInfo = dirCompareInfo.childDirCompareInfos.get(childDirInfoPair);
            accDirInfoPairs.add(childDirInfoPair);
            accDirCompareInfos.put(childDirInfoPair, childDirCompareInfo);
            childDirCompareInfo.ifPresent(info -> gather(info, accDirInfoPairs, accDirCompareInfos));
        }
    }
}
