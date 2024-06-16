package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * Excelブック比較情報を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public final class SheetCompareInfo implements CompareInfo<BookInfo, String, Void> {
    
    // [static members] ********************************************************
    
    /**
     * 指定した内容で {@link SheetCompareInfo} インスタンスを生成します。<br>
     * 
     * @param bookInfoPair Excelブック情報
     * @param sheetNamePair シート名の組み合わせ
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static SheetCompareInfo of(
            Pair<BookInfo> bookInfoPair,
            Pair<String> sheetNamePair) {
        
        Objects.requireNonNull(bookInfoPair);
        Objects.requireNonNull(sheetNamePair);
        
        return new SheetCompareInfo(bookInfoPair, sheetNamePair);
    }
    
    // [instance members] ******************************************************
    
    private final Pair<BookInfo> bookInfoPair;
    private final Pair<String> sheetNamePair;
    
    private SheetCompareInfo(
            Pair<BookInfo> bookInfoPair,
            Pair<String> sheetNamePair) {
        
        assert bookInfoPair != null;
        assert sheetNamePair != null;
        
        this.bookInfoPair = bookInfoPair;
        this.sheetNamePair = sheetNamePair;
    }
    
    @Override
    public Pair<BookInfo> parentPair() {
        return bookInfoPair;
    }
    
    @Override
    public List<Pair<String>> childPairs() {
        return List.of(sheetNamePair);
    }
    
    @Override
    public Map<Pair<String>, Optional<Void>> childCompareInfos() {
        throw new UnsupportedOperationException();
    }
}
