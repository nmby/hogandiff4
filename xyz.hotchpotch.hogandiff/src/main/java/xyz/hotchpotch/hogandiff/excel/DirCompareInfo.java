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
     * @param dirInfoPair 比較対象フォルダ情報
     * @param bookPathsMatcher Excelブックパスの組み合わせを決めるマッチャー
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @param readPasswords 読み取りパスワード
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static DirCompareInfo calculate(
            Pair<DirInfo> dirInfoPair,
            Matcher<Path> bookPathsMatcher,
            Matcher<String> sheetNamesMatcher,
            Map<Path, String> readPasswords) {
        
        Objects.requireNonNull(dirInfoPair);
        Objects.requireNonNull(bookPathsMatcher);
        Objects.requireNonNull(sheetNamesMatcher);
        Objects.requireNonNull(readPasswords);
        
        List<Pair<Path>> bookPathPairs;
        if (dirInfoPair.isPaired()) {
            bookPathPairs = bookPathsMatcher.makeItemPairs(
                    dirInfoPair.a().childBookPaths(),
                    dirInfoPair.b().childBookPaths());
        } else if (dirInfoPair.hasA()) {
            bookPathPairs = dirInfoPair.a().childBookPaths().stream()
                    .map(bookName -> Pair.ofOnly(Side.A, bookName))
                    .toList();
        } else if (dirInfoPair.hasB()) {
            bookPathPairs = dirInfoPair.b().childBookPaths().stream()
                    .map(bookName -> Pair.ofOnly(Side.B, bookName))
                    .toList();
        } else {
            bookPathPairs = List.of();
        }
        
        Map<Pair<Path>, Optional<BookCompareInfo>> bookCompareInfos = new HashMap<>();
        
        for (Pair<Path> bookPathPair : bookPathPairs) {
            assert bookPathPair.hasA() || bookPathPair.hasB();
            
            Pair<BookInfo> bookInfoPair = null;
            
            try {
                BookInfo bookInfoA = null;
                if (bookPathPair.hasA()) {
                    Path bookPathA = bookPathPair.a();
                    String readPasswordA = readPasswords.get(bookPathA);
                    SheetNamesLoader sheetNamesLoaderA = Factory.sheetNamesLoader(bookPathA, readPasswordA);
                    bookInfoA = sheetNamesLoaderA.loadSheetNames(bookPathA, readPasswordA);
                }
                
                BookInfo bookInfoB = null;
                if (bookPathPair.hasB()) {
                    Path bookPathB = bookPathPair.b();
                    String readPasswordB = readPasswords.get(bookPathB);
                    SheetNamesLoader sheetNamesLoaderB = Factory.sheetNamesLoader(bookPathB, readPasswordB);
                    bookInfoB = sheetNamesLoaderB.loadSheetNames(bookPathB, readPasswordB);
                }
                
                bookInfoPair = Pair.of(bookInfoA, bookInfoB);
                
            } catch (ExcelHandlingException e) {
                // nop
            }
            
            BookCompareInfo bookCompareInfo = bookInfoPair != null
                    ? BookCompareInfo.calculate(bookInfoPair, sheetNamesMatcher)
                    : null;
            bookCompareInfos.put(bookPathPair, Optional.ofNullable(bookCompareInfo));
        }
        
        return new DirCompareInfo(dirInfoPair, bookPathPairs, bookCompareInfos);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> dirInfoPair;
    private final List<Pair<Path>> bookPathPairs;
    private final Map<Pair<Path>, Optional<BookCompareInfo>> bookCompareInfos;
    
    private DirCompareInfo(
            Pair<DirInfo> dirInfoPair,
            List<Pair<Path>> bookPathPairs,
            Map<Pair<Path>, Optional<BookCompareInfo>> bookCompareInfos) {
        
        assert dirInfoPair != null;
        assert bookPathPairs != null;
        assert bookCompareInfos != null;
        
        this.dirInfoPair = dirInfoPair;
        this.bookPathPairs = List.copyOf(bookPathPairs);
        this.bookCompareInfos = Map.copyOf(bookCompareInfos);
    }
    
    @Override
    public Pair<DirInfo> parentPair() {
        return dirInfoPair;
    }
    
    @Override
    public List<Pair<Path>> childPairs() {
        return bookPathPairs;
    }
    
    @Override
    public Map<Pair<Path>, Optional<BookCompareInfo>> childCompareInfos() {
        return bookCompareInfos;
    }
}
