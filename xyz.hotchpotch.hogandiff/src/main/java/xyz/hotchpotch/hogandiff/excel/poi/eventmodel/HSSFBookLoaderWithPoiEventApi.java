package xyz.hotchpotch.hogandiff.excel.poi.eventmodel;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.WSBoolRecord;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;

/**
 * Apache POI イベントモデル API を利用して
 * .xls 形式のExcelブックから
 * シート名の一覧を抽出する {@link BookLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLS })
public class HSSFBookLoaderWithPoiEventApi implements BookLoader {
    
    // [static members] ********************************************************
    
    private static class SheetInfo {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final String sheetName;
        private Set<SheetType> possibleTypes;
        
        private SheetInfo(String sheetName) {
            assert sheetName != null;
            
            this.sheetName = sheetName;
        }
    }
    
    private static class Listener1 implements HSSFListener {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private List<SheetInfo> sheets = new ArrayList<>();
        private int idx = -1;
        
        @Override
        public void processRecord(Record record) {
            assert record != null;
            
            switch (record) {
                case BoundSheetRecord bsRec:
                    sheets.add(new SheetInfo(bsRec.getSheetname()));
                    break;
                
                case BOFRecord bof:
                    switch (bof.getType()) {
                        case BOFRecord.TYPE_WORKBOOK:
                        case BOFRecord.TYPE_WORKSPACE_FILE:
                            // nop
                            break;
                        
                        case BOFRecord.TYPE_WORKSHEET:
                            ++idx;
                            sheets.get(idx).possibleTypes = EnumSet.of(SheetType.WORKSHEET, SheetType.DIALOG_SHEET);
                            break;
                        
                        case BOFRecord.TYPE_CHART:
                            ++idx;
                            sheets.get(idx).possibleTypes = EnumSet.of(SheetType.CHART_SHEET);
                            break;
                        
                        case BOFRecord.TYPE_EXCEL_4_MACRO:
                            ++idx;
                            sheets.get(idx).possibleTypes = EnumSet.of(SheetType.MACRO_SHEET);
                            break;
                        
                        case BOFRecord.TYPE_VB_MODULE:
                            ++idx;
                            sheets.get(idx).possibleTypes = Set.of();
                            break;
                        
                        default:
                            throw new AssertionError("unknown BOF type: " + bof.getType());
                    }
                    break;
                
                case WSBoolRecord wsbRec:
                    if (wsbRec.getDialog()) {
                        // FIXME: [No.01 シート識別不正 - HSSF] ダイアログシートであっても何故かここに入ってくれない
                        sheets.get(idx).possibleTypes = EnumSet.of(SheetType.DIALOG_SHEET);
                    } else {
                        sheets.get(idx).possibleTypes.remove(SheetType.DIALOG_SHEET);
                    }
                    break;
                
                default:
                    // nop
            }
        }
        
        private List<String> getSheetNames(Set<SheetType> targetTypes) {
            assert targetTypes != null;
            
            return sheets.stream()
                    .filter(s -> s.possibleTypes.stream().anyMatch(targetTypes::contains))
                    .map(s -> s.sheetName)
                    .toList();
        }
    }
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param targetTypes 抽出対象とするシートの種類
     * @return 新しいローダー
     * @throws NullPointerException パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code targetTypes} が空の場合
     */
    public static BookLoader of(Set<SheetType> targetTypes) {
        Objects.requireNonNull(targetTypes);
        if (targetTypes.isEmpty()) {
            throw new IllegalArgumentException("targetTypes is empty.");
        }
        
        return new HSSFBookLoaderWithPoiEventApi(targetTypes);
    }
    
    // [instance members] ******************************************************
    
    private final Set<SheetType> targetTypes;
    
    private HSSFBookLoaderWithPoiEventApi(Set<SheetType> targetTypes) {
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
     *              {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookPath} がサポート対象外の形式の場合
     */
    // FIXME: [No.01 シート識別不正 - usermodel] 上記のバグを改修する。（できるのか？）
    // 
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・抽出処理中に発生したあらゆる例外は catch し、呼出元には必ず {@link BookInfo} オブジェクトを返却する。
    @Override
    public BookInfo loadBookInfo(
            Path bookPath,
            String readPassword) {
        
        Objects.requireNonNull(bookPath);
        // readPassword may be null.
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(bookPath));
        
        Biff8EncryptionKey.setCurrentUserPassword(readPassword);
        try (FileInputStream fin = new FileInputStream(bookPath.toFile());
                POIFSFileSystem poifs = new POIFSFileSystem(fin)) {
            
            HSSFRequest req = new HSSFRequest();
            Listener1 listener1 = new Listener1();
            req.addListenerForAllRecords(listener1);
            HSSFEventFactory factory = new HSSFEventFactory();
            factory.abortableProcessWorkbookEvents(req, poifs);
            
            return BookInfo.ofLoadCompleted(
                    bookPath,
                    listener1.getSheetNames(targetTypes));
            
        } catch (EncryptedDocumentException e) {
            return BookInfo.ofNeedsPassword(bookPath);
            
        } catch (Exception e) {
            return BookInfo.ofLoadFailed(bookPath);
            
        } finally {
            Biff8EncryptionKey.setCurrentUserPassword(null);
        }
    }
}
