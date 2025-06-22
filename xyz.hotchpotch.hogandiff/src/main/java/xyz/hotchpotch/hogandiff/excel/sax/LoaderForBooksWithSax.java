package xyz.hotchpotch.hogandiff.excel.sax;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.PasswordHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.sax.SaxUtil.SheetInfo;
import xyz.hotchpotch.hogandiff.task.BookInfo;
import xyz.hotchpotch.hogandiff.task.LoaderForBooks;

/**
 * SAX (Simple API for XML) を利用して
 * .xlsx/.xlsm 形式のExcelブックから
 * シート名の一覧を抽出する {@link LoaderForBooks} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM })
public class LoaderForBooksWithSax implements LoaderForBooks {

    // [static members] ********************************************************

    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param targetTypes 抽出対象とするシートの種類
     * @return 新しいローダー
     * @throws NullPointerException     パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code targetTypes} が空の場合
     */
    public static LoaderForBooks of(Set<SheetType> targetTypes) {
        Objects.requireNonNull(targetTypes);
        if (targetTypes.isEmpty()) {
            throw new IllegalArgumentException("targetTypes is empty.");
        }

        return new LoaderForBooksWithSax(targetTypes);
    }

    // [instance members] ******************************************************

    private final Set<SheetType> targetTypes;

    private LoaderForBooksWithSax(Set<SheetType> targetTypes) {
        assert targetTypes != null;

        this.targetTypes = EnumSet.copyOf(targetTypes);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *                                  {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException
     *                                  {@code bookPath} がサポート対象外の形式の場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    // 例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・抽出処理中に発生したあらゆる例外は catch し、呼出元には必ず {@link BookInfo} オブジェクトを返却する。
    @Override
    public BookInfo loadBookInfo(
            Path bookPath,
            String readPassword) {

        Objects.requireNonNull(bookPath);
        // readPassword may be null.
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(bookPath));

        try {
            List<SheetInfo> sheets = SaxUtil.loadSheetInfos(bookPath, readPassword);

            List<String> sheetNames = sheets.stream()
                    .filter(info -> targetTypes.contains(info.type()))
                    .map(SheetInfo::sheetName)
                    .toList();

            return BookInfo.ofLoadCompleted(bookPath, sheetNames);

        } catch (PasswordHandlingException e) {
            return BookInfo.ofNeedsPassword(bookPath);

        } catch (Exception e) {
            return BookInfo.ofLoadFailed(bookPath);
        }
    }
}
