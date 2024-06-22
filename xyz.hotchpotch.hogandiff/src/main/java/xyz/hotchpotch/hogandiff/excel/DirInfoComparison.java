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
 * @param parentDirInfoPair 親フォルダ情報
 * @param childDirInfoPairs 子フォルダ情報の組み合わせ
 * @param childDirInfoComparisons 子フォルダ比較情報
 * @param childBookPathPairs 子Excelブックパスの組み合わせ
 * @param childBookInfoComparisons 子Excelブック比較情報
 * @author nmby
 */
public final record DirInfoComparison(
        Pair<DirInfo> parentDirInfoPair,
        List<Pair<DirInfo>> childDirInfoPairs,
        Map<Pair<DirInfo>, Optional<DirInfoComparison>> childDirInfoComparisons,
        List<Pair<Path>> childBookPathPairs,
        Map<Pair<Path>, Optional<BookInfoComparison>> childBookInfoComparisons)
        implements CompareInfo {
    
    // [static members] ********************************************************
    
    /**
     * 階層状の {@link DirInfoComparison} の内容を一層に平坦化した情報を保持するレコードです。<br>
     * 
     * @param parentDirInfoPair 比較対象フォルダ情報
     * @param dirInfoPairs 子フォルダ情報
     * @param dirInfoComparisons 子フォルダ比較情報
     */
    public static record FlattenDirInfoComparison(
            Pair<DirInfo> parentDirInfoPair,
            List<Pair<DirInfo>> dirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirInfoComparison>> dirInfoComparisons) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
    }
    
    /**
     * 与えられたマッチャーを使用して新たな {@link DirInfoComparison} インスタンスを生成します。<br>
     * 
     * @param parentDirInfoPair 比較対象フォルダ情報
     * @param dirInfosMatcher フォルダ情報の組み合わせを決めるマッチャー
     * @param bookPathsMatcher Excelブックパスの組み合わせを決めるマッチャー
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @param readPasswords 読み取りパスワード
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static DirInfoComparison calculate(
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
        
        Map<Pair<DirInfo>, Optional<DirInfoComparison>> dirCompareInfos = new HashMap<>();
        Map<Pair<Path>, Optional<BookInfoComparison>> bookCompareInfos = new HashMap<>();
        
        for (Pair<DirInfo> dirInfoPair : dirInfoPairs) {
            DirInfoComparison dirInfoComparison = DirInfoComparison.calculate(
                    dirInfoPair,
                    dirInfosMatcher,
                    bookPathsMatcher,
                    sheetNamesMatcher,
                    readPasswords);
            dirCompareInfos.put(dirInfoPair, Optional.ofNullable(dirInfoComparison));
        }
        
        for (Pair<Path> bookPathPair : bookPathPairs) {
            BookInfoComparison bookInfoComparison = null;
            try {
                Pair<BookInfo> bookInfoPair = Side.unsafeMap(
                        side -> {
                            if (bookPathPair.has(side)) {
                                Path bookPath = bookPathPair.get(side);
                                String readPassword = readPasswords.get(bookPath);
                                BookInfoLoader sheetNamesLoader = Factory.bookInfoLoader(bookPath, readPassword);
                                return sheetNamesLoader.loadBookInfo(bookPath, readPassword);
                            } else {
                                return null;
                            }
                        });
                bookInfoComparison = bookInfoPair != null
                        ? BookInfoComparison.calculate(bookInfoPair, sheetNamesMatcher)
                        : null;
                
            } catch (ExcelHandlingException e) {
                // nop
            }
            bookCompareInfos.put(bookPathPair, Optional.ofNullable(bookInfoComparison));
        }
        
        return new DirInfoComparison(
                parentDirInfoPair,
                dirInfoPairs,
                dirCompareInfos,
                bookPathPairs,
                bookCompareInfos);
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param parentDirInfoPair 親フォルダ情報
     * @param childDirInfoPairs 子フォルダ情報の組み合わせ
     * @param childDirInfoComparisons 子フォルダ比較情報
     * @param childBookPathPairs 子Excelブックパスの組み合わせ
     * @param childBookInfoComparisons 子Excelブック比較情報
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public DirInfoComparison {
        Objects.requireNonNull(parentDirInfoPair);
        Objects.requireNonNull(childDirInfoPairs);
        Objects.requireNonNull(childDirInfoComparisons);
        Objects.requireNonNull(childBookPathPairs);
        Objects.requireNonNull(childBookInfoComparisons);
        
        childDirInfoPairs = List.copyOf(childDirInfoPairs);
        childDirInfoComparisons = Map.copyOf(childDirInfoComparisons);
        childBookPathPairs = List.copyOf(childBookPathPairs);
        childBookInfoComparisons = Map.copyOf(childBookInfoComparisons);
    }
    
    /**
     * ツリー状の本オフジェクトの内容を一層に並べた
     * {@link FlattenDirInfoComparison} オブジェクトに変換して返します。<br>
     * 
     * @return 平坦化されたフォルダ比較情報
     */
    public FlattenDirInfoComparison flatten() {
        List<Pair<DirInfo>> accDirInfoPairs = new ArrayList<>();
        Map<Pair<DirInfo>, Optional<DirInfoComparison>> accDirCompareInfos = new HashMap<>();
        
        accDirInfoPairs.add(parentDirInfoPair);
        accDirCompareInfos.put(parentDirInfoPair, Optional.of(this));
        gather(this, accDirInfoPairs, accDirCompareInfos);
        
        return new FlattenDirInfoComparison(
                parentDirInfoPair,
                accDirInfoPairs,
                accDirCompareInfos);
    }
    
    private void gather(
            DirInfoComparison dirInfoComparison,
            List<Pair<DirInfo>> accDirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirInfoComparison>> accDirCompareInfos) {
        
        for (Pair<DirInfo> childDirInfoPair : dirInfoComparison.childDirInfoPairs) {
            Optional<DirInfoComparison> childDirInfoComparison = dirInfoComparison.childDirInfoComparisons
                    .get(childDirInfoPair);
            accDirInfoPairs.add(childDirInfoPair);
            accDirCompareInfos.put(childDirInfoPair, childDirInfoComparison);
            childDirInfoComparison.ifPresent(info -> gather(info, accDirInfoPairs, accDirCompareInfos));
        }
    }
}
