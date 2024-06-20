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
     * @param parentDirInfoPair 比較対象フォルダ情報
     * @param dirInfoPairs 子フォルダ情報
     * @param dirCompareInfos 子フォルダ比較情報
     */
    public static record FlattenDirCompareInfo(
            Pair<DirInfo> parentDirInfoPair,
            List<Pair<DirInfo>> dirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirCompareInfo>> dirCompareInfos) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
    }
    
    /**
     * 与えられたマッチャーを使用して新たな {@link DirCompareInfo} インスタンスを生成します。<br>
     * 
     * @param parentDirInfoPair 比較対象フォルダ情報
     * @param dirInfosMatcher フォルダ情報の組み合わせを決めるマッチャー
     * @param bookPathsMatcher Excelブックパスの組み合わせを決めるマッチャー
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @param readPasswords 読み取りパスワード
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static DirCompareInfo calculate(
            Pair<DirInfo> parentDirInfoPair,
            Matcher<DirInfo> dirInfosMatcher,
            Matcher<Path> bookPathsMatcher,
            Matcher<String> sheetNamesMatcher,
            Map<Path, String> readPasswords) {
        
        Objects.requireNonNull(parentDirInfoPair);
        Objects.requireNonNull(bookPathsMatcher);
        Objects.requireNonNull(sheetNamesMatcher);
        Objects.requireNonNull(readPasswords);
        
        List<Pair<DirInfo>> dirInfoPairs;
        List<Pair<Path>> bookPathPairs;
        
        if (parentDirInfoPair.isPaired()) {
            dirInfoPairs = dirInfosMatcher.makeItemPairs(
                    parentDirInfoPair.a().childDirInfos(),
                    parentDirInfoPair.b().childDirInfos());
            bookPathPairs = bookPathsMatcher.makeItemPairs(
                    parentDirInfoPair.a().childBookPaths(),
                    parentDirInfoPair.b().childBookPaths());
            
        } else if (parentDirInfoPair.hasA()) {
            dirInfoPairs = parentDirInfoPair.a().childDirInfos().stream()
                    .map(dirInfo -> Pair.ofOnly(Side.A, dirInfo))
                    .toList();
            bookPathPairs = parentDirInfoPair.a().childBookPaths().stream()
                    .map(bookName -> Pair.ofOnly(Side.A, bookName))
                    .toList();
            
        } else if (parentDirInfoPair.hasB()) {
            dirInfoPairs = parentDirInfoPair.b().childDirInfos().stream()
                    .map(dirInfo -> Pair.ofOnly(Side.B, dirInfo))
                    .toList();
            bookPathPairs = parentDirInfoPair.b().childBookPaths().stream()
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
                parentDirInfoPair,
                dirInfoPairs,
                dirCompareInfos,
                bookPathPairs,
                bookCompareInfos);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> parentDirInfoPair;
    private final List<Pair<DirInfo>> childDirInfoPairs;
    private final Map<Pair<DirInfo>, Optional<DirCompareInfo>> childDirCompareInfos;
    private final List<Pair<Path>> childBookPathPairs;
    private final Map<Pair<Path>, Optional<BookCompareInfo>> childBookCompareInfos;
    
    private DirCompareInfo(
            Pair<DirInfo> parentDirInfoPair,
            List<Pair<DirInfo>> childDirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirCompareInfo>> childDirCompareInfos,
            List<Pair<Path>> childBookPathPairs,
            Map<Pair<Path>, Optional<BookCompareInfo>> childBookCompareInfos) {
        
        assert parentDirInfoPair != null;
        assert childDirInfoPairs != null;
        assert childDirCompareInfos != null;
        assert childBookPathPairs != null;
        assert childBookCompareInfos != null;
        
        this.parentDirInfoPair = parentDirInfoPair;
        this.childDirInfoPairs = List.copyOf(childDirInfoPairs);
        this.childDirCompareInfos = Map.copyOf(childDirCompareInfos);
        this.childBookPathPairs = List.copyOf(childBookPathPairs);
        this.childBookCompareInfos = Map.copyOf(childBookCompareInfos);
    }
    
    /**
     * 比較対象フォルダ情報を返します。<br>
     * 
     * @return 比較対象フォルダ情報
     */
    public Pair<DirInfo> parentDirInfoPair() {
        return parentDirInfoPair;
    }
    
    /**
     * 子Excelブック情報を返します。<br>
     * 
     * @return 子Excelブック情報
     */
    public List<Pair<Path>> childBookPathPairs() {
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
        
        accDirInfoPairs.add(parentDirInfoPair);
        accDirCompareInfos.put(parentDirInfoPair, Optional.of(this));
        gather(this, accDirInfoPairs, accDirCompareInfos);
        
        return new FlattenDirCompareInfo(
                parentDirInfoPair,
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
