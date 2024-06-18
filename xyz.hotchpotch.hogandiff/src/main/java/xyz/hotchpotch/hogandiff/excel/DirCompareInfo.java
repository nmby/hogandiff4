package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
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
    
    private final Pair<DirInfo> dirInfoPair;
    private final List<Pair<DirInfo>> childDirInfoPairs;
    private final Map<Pair<DirInfo>, Optional<DirCompareInfo>> childDirCompareInfos;
    private final List<Pair<Path>> childBookPathPairs;
    private final Map<Pair<Path>, Optional<BookCompareInfo>> childBookCompareInfos;
    
    private DirCompareInfo(
            Pair<DirInfo> dirInfoPair,
            List<Pair<DirInfo>> childDirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirCompareInfo>> childDirCompareInfos,
            List<Pair<Path>> childBookPathPairs,
            Map<Pair<Path>, Optional<BookCompareInfo>> childBookCompareInfos) {
        
        assert dirInfoPair != null;
        assert childDirInfoPairs != null;
        assert childDirCompareInfos != null;
        assert childBookPathPairs != null;
        assert childBookCompareInfos != null;
        
        this.dirInfoPair = dirInfoPair;
        this.childDirInfoPairs = List.copyOf(childDirInfoPairs);
        this.childDirCompareInfos = Map.copyOf(childDirCompareInfos);
        this.childBookPathPairs = List.copyOf(childBookPathPairs);
        this.childBookCompareInfos = Map.copyOf(childBookCompareInfos);
    }
    
    @Override
    public Pair<DirInfo> parentPair() {
        return dirInfoPair;
    }
    
    @Override
    public List<Pair<Path>> childPairs() {
        return childBookPathPairs;
    }
    
    @Override
    public Map<Pair<Path>, Optional<BookCompareInfo>> childCompareInfos() {
        return childBookCompareInfos;
    }
}
