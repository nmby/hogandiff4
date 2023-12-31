package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.record.RecordInputStream.LeftoverDataException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.PasswordHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;

/**
 * Apache POI のユーザーモデル API を利用して
 * .xlsx/.xlsm/.xls 形式のExcelブックから
 * シート名の一覧を抽出する {@link SheetNamesLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM, BookType.XLS })
public class SheetNamesLoaderWithPoiUserApi implements SheetNamesLoader {
    
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
        
        return new SheetNamesLoaderWithPoiUserApi(targetTypes);
    }
    
    // [instance members] ******************************************************
    
    private final Set<SheetType> targetTypes;
    
    private SheetNamesLoaderWithPoiUserApi(Set<SheetType> targetTypes) {
        assert targetTypes != null;
        
        this.targetTypes = EnumSet.copyOf(targetTypes);
    }
    
    /**
     * {@inheritDoc}
     * <br>
     * <strong>注意：</strong>
     * この実装には、目的の種類以外のシートも抽出されてしまうというバグがあります。
     * ごめんなさい m(_ _)m <br>
     * 
     * @throws NullPointerException
     *              {@code bookOpenInfo} が {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookOpenInfo} がサポート対象外の形式の場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // FIXME: [No.1 シート識別不正 - usermodel] 上記のバグを改修する。（できるのか？）
    //
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
        
        try (Workbook wb = WorkbookFactory.create(
                bookOpenInfo.bookPath().toFile(),
                bookOpenInfo.readPassword(),
                true)) {
            
            List<String> sheetNames = StreamSupport.stream(wb.spliterator(), false)
                    .filter(s -> PoiUtil.possibleTypes(s).stream().anyMatch(targetTypes::contains))
                    .map(Sheet::getSheetName)
                    .toList();
            
            return new BookInfo(bookOpenInfo, sheetNames);
            
        } catch (LeftoverDataException e) {
            // FIXME: [No.7 POI関連] 書き込みpw付きのxlsファイルを開けない
            // 
            // 書き込みpw有り/読み込みpw無しのxlsファイルを開こうとすると
            // org.apache.poi.hssf.record.RecordInputStream$LeftoverDataException が発生する。
            // 下記ブログを参考に "VelvetSweatshop" を試してみたが改善されなかった。
            // https://blog.cybozu.io/entry/2017/03/09/080000
            //
            // 暫定対処として、LeftoverDataException はその他の場合にも発生するかもしれないが
            // 書き込みpw付きxlsファイルであると見なしてしまい、
            // 本当は読み取り専用で読み込めてほしいが
            // サポート対象外であるとユーザーに案内することにする。
            throw new PasswordHandlingException(
                    (bookOpenInfo.readPassword() == null
                            ? "book is encrypted : %s"
                            : "password is incorrect : %s")
                                    .formatted(bookOpenInfo),
                    e);
            
        } catch (EncryptedDocumentException e) {
            throw new PasswordHandlingException(
                    (bookOpenInfo.readPassword() == null
                            ? "book is encrypted : %s"
                            : "password is incorrect : %s")
                                    .formatted(bookOpenInfo),
                    e);
            
        } catch (Exception e) {
            throw new ExcelHandlingException("processing failed : %s".formatted(bookOpenInfo), e);
        }
    }
}
