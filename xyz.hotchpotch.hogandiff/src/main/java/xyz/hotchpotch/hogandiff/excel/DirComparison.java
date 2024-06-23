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
 * @param childDirComparisons 子フォルダ比較情報
 * @param childBookInfoPairs 子Excelブックパスの組み合わせ
 * @param childBookComparisons 子Excelブック比較情報
 * @author nmby
 */
public final record DirComparison(
        Pair<DirInfo> parentDirInfoPair,
        List<Pair<DirInfo>> childDirInfoPairs,
        Map<Pair<DirInfo>, Optional<DirComparison>> childDirComparisons,
        List<Pair<BookInfo>> childBookInfoPairs,
        Map<Pair<BookInfo>, Optional<BookComparison>> childBookComparisons)
        implements Comparison {
    
    // [static members] ********************************************************
    
    /**
     * 階層状の {@link DirComparison} の内容を一層に平坦化した情報を保持するレコードです。<br>
     * 
     * @param parentDirInfoPair 比較対象フォルダ情報
     * @param dirInfoPairs 子フォルダ情報
     * @param dirComparisons 子フォルダ比較情報
     */
    public static record FlattenDirComparison(
            Pair<DirInfo> parentDirInfoPair,
            List<Pair<DirInfo>> dirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirComparison>> dirComparisons) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
    }
    
    /**
     * 与えられたマッチャーを使用して新たな {@link DirComparison} インスタンスを生成します。<br>
     * 
     * @param parentDirInfoPair 比較対象フォルダ情報
     * @param dirInfosMatcher フォルダ情報の組み合わせを決めるマッチャー
     * @param bookPathsMatcher Excelブックパスの組み合わせを決めるマッチャー
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @param readPasswords 読み取りパスワード
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static DirComparison calculate(
            Pair<DirInfo> parentDirInfoPair,
            Matcher<DirInfo> dirInfosMatcher,
            Matcher<BookInfo> bookPathsMatcher,
            Matcher<String> sheetNamesMatcher,
            Map<Path, String> readPasswords) {
        
        Objects.requireNonNull(parentDirInfoPair);
        Objects.requireNonNull(bookPathsMatcher);
        Objects.requireNonNull(sheetNamesMatcher);
        Objects.requireNonNull(readPasswords);
        
        List<Pair<DirInfo>> dirInfoPairs;
        List<Pair<BookInfo>> bookInfoPairs;
        
        if (parentDirInfoPair.isPaired()) {
            dirInfoPairs = dirInfosMatcher.makeItemPairs(
                    parentDirInfoPair.a().childDirInfos(),
                    parentDirInfoPair.b().childDirInfos());
            bookInfoPairs = bookPathsMatcher.makeItemPairs(
                    parentDirInfoPair.a().childBookInfos(),
                    parentDirInfoPair.b().childBookInfos());
            
        } else if (parentDirInfoPair.hasA()) {
            dirInfoPairs = parentDirInfoPair.a().childDirInfos().stream()
                    .map(dirInfo -> Pair.ofOnly(Side.A, dirInfo))
                    .toList();
            bookInfoPairs = parentDirInfoPair.a().childBookInfos().stream()
                    .map(bookName -> Pair.ofOnly(Side.A, bookName))
                    .toList();
            
        } else if (parentDirInfoPair.hasB()) {
            dirInfoPairs = parentDirInfoPair.b().childDirInfos().stream()
                    .map(dirInfo -> Pair.ofOnly(Side.B, dirInfo))
                    .toList();
            bookInfoPairs = parentDirInfoPair.b().childBookInfos().stream()
                    .map(bookName -> Pair.ofOnly(Side.B, bookName))
                    .toList();
            
        } else {
            dirInfoPairs = List.of();
            bookInfoPairs = List.of();
        }
        
        Map<Pair<DirInfo>, Optional<DirComparison>> dirComparisons = new HashMap<>();
        Map<Pair<BookInfo>, Optional<BookComparison>> bookComparisons = new HashMap<>();
        
        for (Pair<DirInfo> dirInfoPair : dirInfoPairs) {
            DirComparison dirComparison = DirComparison.calculate(
                    dirInfoPair,
                    dirInfosMatcher,
                    bookPathsMatcher,
                    sheetNamesMatcher,
                    readPasswords);
            dirComparisons.put(dirInfoPair, Optional.ofNullable(dirComparison));
        }
        
        for (Pair<BookInfo> bookInfoPair : bookInfoPairs) {
            BookComparison bookComparison = BookComparison.calculate(bookInfoPair, sheetNamesMatcher);
            bookComparisons.put(bookInfoPair, Optional.ofNullable(bookComparison));
        }
        
        return new DirComparison(
                parentDirInfoPair,
                dirInfoPairs,
                dirComparisons,
                bookInfoPairs,
                bookComparisons);
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param parentDirInfoPair 親フォルダ情報
     * @param childDirInfoPairs 子フォルダ情報の組み合わせ
     * @param childDirComparisons 子フォルダ比較情報
     * @param childBookInfoPairs 子Excelブックパスの組み合わせ
     * @param childBookComparisons 子Excelブック比較情報
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public DirComparison(
            Pair<DirInfo> parentDirInfoPair,
            List<Pair<DirInfo>> childDirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirComparison>> childDirComparisons,
            List<Pair<BookInfo>> childBookInfoPairs,
            Map<Pair<BookInfo>, Optional<BookComparison>> childBookComparisons) {
        
        Objects.requireNonNull(parentDirInfoPair);
        Objects.requireNonNull(childDirInfoPairs);
        Objects.requireNonNull(childDirComparisons);
        Objects.requireNonNull(childBookInfoPairs);
        Objects.requireNonNull(childBookComparisons);
        
        this.parentDirInfoPair = parentDirInfoPair;
        this.childDirInfoPairs = List.copyOf(childDirInfoPairs);
        this.childDirComparisons = Map.copyOf(childDirComparisons);
        this.childBookInfoPairs = List.copyOf(childBookInfoPairs);
        this.childBookComparisons = Map.copyOf(childBookComparisons);
    }
    
    /**
     * ツリー状の本オフジェクトの内容を一層に並べた
     * {@link FlattenDirComparison} オブジェクトに変換して返します。<br>
     * 
     * @return 平坦化されたフォルダ比較情報
     */
    public FlattenDirComparison flatten() {
        List<Pair<DirInfo>> accDirInfoPairs = new ArrayList<>();
        Map<Pair<DirInfo>, Optional<DirComparison>> accDirComparisons = new HashMap<>();
        
        accDirInfoPairs.add(parentDirInfoPair);
        accDirComparisons.put(parentDirInfoPair, Optional.of(this));
        gather(this, accDirInfoPairs, accDirComparisons);
        
        return new FlattenDirComparison(
                parentDirInfoPair,
                accDirInfoPairs,
                accDirComparisons);
    }
    
    private void gather(
            DirComparison dirComparison,
            List<Pair<DirInfo>> accDirInfoPairs,
            Map<Pair<DirInfo>, Optional<DirComparison>> accDirComparisons) {
        
        for (Pair<DirInfo> childDirInfoPair : dirComparison.childDirInfoPairs) {
            Optional<DirComparison> childDirComparison = dirComparison.childDirComparisons
                    .get(childDirInfoPair);
            accDirInfoPairs.add(childDirInfoPair);
            accDirComparisons.put(childDirInfoPair, childDirComparison);
            childDirComparison.ifPresent(info -> gather(info, accDirInfoPairs, accDirComparisons));
        }
    }
}
