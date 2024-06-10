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
 * フォルダ同士を比較するときのExcelブックの組み合わせ情報を保持する不変クラスです。<br>
 * 
 * @author nmby
 */
public class DirCompareInfo {
    
    // [static members] ********************************************************
    
    /**
     * コンストラクタ
     * 
     * @param dirInfoPair 比較対象フォルダの情報
     * @param bookNamesMatcher Excelブック名の組み合わせを決めるマッチャー
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @param readPasswords 読み取りパスワード
     * @return フォルダ比較情報
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static DirCompareInfo of(
            Pair<DirInfo> dirInfoPair,
            Matcher<String> bookNamesMatcher,
            Matcher<String> sheetNamesMatcher,
            Map<Path, String> readPasswords) {
        
        Objects.requireNonNull(dirInfoPair);
        Objects.requireNonNull(bookNamesMatcher);
        Objects.requireNonNull(sheetNamesMatcher);
        Objects.requireNonNull(readPasswords);
        
        List<Pair<String>> bookNamePairs;
        if (dirInfoPair.isPaired()) {
            bookNamePairs = bookNamesMatcher.makeItemPairs(
                    dirInfoPair.a().bookNames(),
                    dirInfoPair.b().bookNames());
        } else if (dirInfoPair.hasA()) {
            bookNamePairs = dirInfoPair.a().bookNames().stream()
                    .map(bookName -> Pair.ofOnly(Side.A, bookName))
                    .toList();
        } else if (dirInfoPair.hasB()) {
            bookNamePairs = dirInfoPair.b().bookNames().stream()
                    .map(bookName -> Pair.ofOnly(Side.B, bookName))
                    .toList();
        } else {
            bookNamePairs = List.of();
        }
        
        Map<Pair<String>, Optional<BookCompareInfo>> bookCompareInfos = new HashMap<>();
        
        for (Pair<String> bookNamePair : bookNamePairs) {
            Pair<BookInfo> bookInfoPair = null;
            
            try {
                BookInfo bookInfoA = null;
                if (bookNamePair.hasA()) {
                    Path bookPathA = dirInfoPair.a().dirPath().resolve(bookNamePair.a());
                    String readPasswordA = readPasswords.get(bookPathA);
                    SheetNamesLoader sheetNamesLoaderA = Factory.sheetNamesLoader(bookPathA, readPasswordA);
                    bookInfoA = sheetNamesLoaderA.loadSheetNames(bookPathA, readPasswordA);
                }
                
                BookInfo bookInfoB = null;
                if (bookNamePair.hasB()) {
                    Path bookPathB = dirInfoPair.b().dirPath().resolve(bookNamePair.b());
                    String readPasswordB = readPasswords.get(bookPathB);
                    SheetNamesLoader sheetNamesLoaderB = Factory.sheetNamesLoader(bookPathB, readPasswordB);
                    bookInfoB = sheetNamesLoaderB.loadSheetNames(bookPathB, readPasswordB);
                }
                
                bookInfoPair = Pair.of(bookInfoA, bookInfoB);
                
            } catch (ExcelHandlingException e) {
                // nop
            }
            
            BookCompareInfo bookCompareInfo = bookInfoPair != null
                    ? bookCompareInfo = BookCompareInfo.of(bookInfoPair, sheetNamesMatcher)
                    : null;
            bookCompareInfos.put(bookNamePair, Optional.ofNullable(bookCompareInfo));
        }
        
        return new DirCompareInfo(dirInfoPair, bookNamePairs, bookCompareInfos);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<DirInfo> dirInfoPair;
    private final List<Pair<String>> bookNamePairs;
    private final Map<Pair<String>, Optional<BookCompareInfo>> bookCompareInfos;
    
    private DirCompareInfo(
            Pair<DirInfo> dirInfoPair,
            List<Pair<String>> bookNamePairs,
            Map<Pair<String>, Optional<BookCompareInfo>> bookCompareInfos) {
        
        Objects.requireNonNull(dirInfoPair, "dirInfoPair");
        Objects.requireNonNull(bookNamePairs, "bookNamePairs");
        Objects.requireNonNull(bookCompareInfos, "bookCompareInfos");
        
        this.dirInfoPair = dirInfoPair;
        this.bookNamePairs = List.copyOf(bookNamePairs);
        this.bookCompareInfos = Map.copyOf(bookCompareInfos);
    }
    
    /**
     * 比較対象フォルダの情報を返します。<br>
     * 
     * @return 比較対象フォルダの情報
     */
    public Pair<DirInfo> dirInfoPair() {
        return dirInfoPair;
    }
    
    /**
     * Excelブック名の組み合わせを返します。<br>
     * 
     * @return Excelブック名の組み合わせ
     */
    public List<Pair<String>> bookNamePairs() {
        return bookNamePairs;
    }
    
    /**
     * Excelブック比較情報を返します。<br>
     * 
     * @return Excelブック比較情報
     */
    public Map<Pair<String>, Optional<BookCompareInfo>> bookCompareInfos() {
        return bookCompareInfos;
    }
}
