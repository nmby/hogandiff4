package xyz.hotchpotch.hogandiff.excel.sax;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.sax.SaxUtil.SheetInfo;

/**
 * SAX (Simple API for XML) を利用して
 * .xlsx/.xlsm 形式のExcelブックから
 * シート名の一覧を抽出する {@link SheetNamesLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM })
public class XSSFSheetNamesLoaderWithSax implements SheetNamesLoader {
    
    // [static members] ********************************************************
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param targetTypes 抽出対象とするシートの種類
     * @return 新しいローダー
     * @throws NullPointerException {@code targetTypes} が {@code null} の場合
     * @throws IllegalArgumentException {@code targetTypes} が空の場合
     */
    public static SheetNamesLoader of(Set<SheetType> targetTypes) {
        Objects.requireNonNull(targetTypes, "targetTypes");
        if (targetTypes.isEmpty()) {
            throw new IllegalArgumentException("targetTypes is empty.");
        }
        
        return new XSSFSheetNamesLoaderWithSax(targetTypes);
    }
    
    // [instance members] ******************************************************
    
    private final Set<SheetType> targetTypes;
    
    private XSSFSheetNamesLoaderWithSax(Set<SheetType> targetTypes) {
        assert targetTypes != null;
        
        this.targetTypes = EnumSet.copyOf(targetTypes);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code bookOpenInfo} が {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookOpenInfo} がサポート対象外の形式の場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックが見つからないとか、ファイル内容がおかしく予期せぬ実行時例外が発生したとか。
    @Override
    public BookInfo loadSheetNames(
            BookOpenInfo bookOpenInfo)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), bookOpenInfo.bookType());
        
        try {
            List<SheetInfo> sheets = SaxUtil.loadSheetInfo(bookOpenInfo);
            
            List<String> sheetNames = sheets.stream()
                    .filter(info -> targetTypes.contains(info.type()))
                    .map(SheetInfo::name)
                    .toList();
            
            return new BookInfo(bookOpenInfo, sheetNames);
            
        } catch (Exception e) {
            throw new ExcelHandlingException("processing failed : %s".formatted(bookOpenInfo), e);
        }
    }
}
