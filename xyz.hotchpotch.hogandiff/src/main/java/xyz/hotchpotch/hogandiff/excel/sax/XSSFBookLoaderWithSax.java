package xyz.hotchpotch.hogandiff.excel.sax;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.sax.SaxUtil.SheetInfo;

/**
 * SAX (Simple API for XML) を利用して
 * .xlsx/.xlsm 形式のExcelブックから
 * シート名の一覧を抽出する {@link BookLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM })
public class XSSFBookLoaderWithSax implements BookLoader {
    
    // [static members] ********************************************************
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param targetTypes 抽出対象とするシートの種類
     * @return 新しいローダー
     * @throws NullPointerException {@code targetTypes} が {@code null} の場合
     * @throws IllegalArgumentException {@code targetTypes} が空の場合
     */
    public static BookLoader of(Set<SheetType> targetTypes) {
        Objects.requireNonNull(targetTypes, "targetTypes");
        if (targetTypes.isEmpty()) {
            throw new IllegalArgumentException("targetTypes is empty.");
        }
        
        return new XSSFBookLoaderWithSax(targetTypes);
    }
    
    // [instance members] ******************************************************
    
    private final Set<SheetType> targetTypes;
    
    private XSSFBookLoaderWithSax(Set<SheetType> targetTypes) {
        assert targetTypes != null;
        
        this.targetTypes = EnumSet.copyOf(targetTypes);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookPath} がサポート対象外の形式の場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックが見つからないとか、ファイル内容がおかしく予期せぬ実行時例外が発生したとか。
    @Override
    public BookInfo loadBookInfo(
            Path bookPath,
            String readPassword)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookPath, "bookPath");
        // readPassword may be null.
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(bookPath));
        
        try {
            List<SheetInfo> sheets = SaxUtil.loadSheetInfo(bookPath, readPassword);
            
            List<String> sheetNames = sheets.stream()
                    .filter(info -> targetTypes.contains(info.type()))
                    .map(SheetInfo::name)
                    .toList();
            
            return BookInfo.ofLoadCompleted(bookPath, sheetNames);
            
        } catch (Exception e) {
            return BookInfo.ofLoadFailed(bookPath);
        }
    }
}