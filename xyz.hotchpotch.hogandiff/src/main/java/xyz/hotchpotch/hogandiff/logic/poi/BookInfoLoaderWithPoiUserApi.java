package xyz.hotchpotch.hogandiff.logic.poi;

import java.nio.file.Path;
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

import xyz.hotchpotch.hogandiff.logic.BookHandler;
import xyz.hotchpotch.hogandiff.logic.BookInfo;
import xyz.hotchpotch.hogandiff.logic.BookType;
import xyz.hotchpotch.hogandiff.logic.BookInfoLoader;
import xyz.hotchpotch.hogandiff.logic.SheetType;
import xyz.hotchpotch.hogandiff.logic.plain.CommonUtil;

/**
 * Apache POI のユーザーモデル API を利用して
 * .xlsx/.xlsm/.xls 形式のExcelブックから
 * シート名の一覧を抽出する {@link BookInfoLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM, BookType.XLS })
public class BookInfoLoaderWithPoiUserApi implements BookInfoLoader {
    
    // [static members] ********************************************************
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param targetTypes 抽出対象とするシートの種類
     * @return 新しいローダー
     * @throws NullPointerException     パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code targetTypes} が空の場合
     */
    public static BookInfoLoader of(Set<SheetType> targetTypes) {
        Objects.requireNonNull(targetTypes);
        if (targetTypes.isEmpty()) {
            throw new IllegalArgumentException("targetTypes is empty.");
        }
        
        return new BookInfoLoaderWithPoiUserApi(targetTypes);
    }
    
    // [instance members] ******************************************************
    
    private final Set<SheetType> targetTypes;
    
    private BookInfoLoaderWithPoiUserApi(Set<SheetType> targetTypes) {
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
     *                                  {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException
     *                                  {@code bookPath} がサポート対象外の形式の場合
     */
    // FIXME: [No.01 シート識別不正 - usermodel] 上記のバグを改修する。（できるのか？）
    //
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
        
        try (Workbook wb = WorkbookFactory.create(
                bookPath.toFile(),
                readPassword,
                true)) {
            
            List<String> sheetNames = StreamSupport.stream(wb.spliterator(), false)
                    .filter(s -> PoiUtil.possibleTypes(s).stream().anyMatch(targetTypes::contains))
                    .map(Sheet::getSheetName)
                    .toList();
            
            return BookInfo.ofLoadCompleted(bookPath, sheetNames);
            
        } catch (LeftoverDataException e) {
            // FIXME: [No.09 書込PW対応] 書き込みpw付きのxlsファイルを開けない
            //
            // 書き込みpw有り/読み込みpw無しのxlsファイルを開こうとすると
            // org.apache.poi.hssf.record.RecordInputStream$LeftoverDataException が発生する。
            // 下記ブログを参考に "VelvetSweatshop" を試してみたが改善されなかった。
            // https://blog.cybozu.io/entry/2017/03/09/080000
            //
            // 暫定対処として、LeftoverDataException はその他の場合にも発生するかもしれないが
            // 書き込みpw付きxlsファイルであると見なしてしまい、
            // 読み取りパスワードが設定されている場合と同様に処理することとする。
            return BookInfo.ofNeedsPassword(bookPath);
            
        } catch (EncryptedDocumentException e) {
            return BookInfo.ofNeedsPassword(bookPath);
            
        } catch (Exception e) {
            return BookInfo.ofLoadFailed(bookPath);
        }
    }
}
