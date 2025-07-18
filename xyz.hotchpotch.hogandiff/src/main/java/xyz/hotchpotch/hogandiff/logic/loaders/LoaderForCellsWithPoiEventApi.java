package xyz.hotchpotch.hogandiff.logic.loaders;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.poi.hssf.eventusermodel.HSSFEventFactory;
import org.apache.poi.hssf.eventusermodel.HSSFListener;
import org.apache.poi.hssf.eventusermodel.HSSFRequest;
import org.apache.poi.hssf.record.BOFRecord;
import org.apache.poi.hssf.record.BoolErrRecord;
import org.apache.poi.hssf.record.BoundSheetRecord;
import org.apache.poi.hssf.record.CellRecord;
import org.apache.poi.hssf.record.CommonObjectDataSubRecord;
import org.apache.poi.hssf.record.EOFRecord;
import org.apache.poi.hssf.record.FormulaRecord;
import org.apache.poi.hssf.record.LabelSSTRecord;
import org.apache.poi.hssf.record.NoteRecord;
import org.apache.poi.hssf.record.NumberRecord;
import org.apache.poi.hssf.record.ObjRecord;
import org.apache.poi.hssf.record.RKRecord;
import org.apache.poi.hssf.record.Record;
import org.apache.poi.hssf.record.SSTRecord;
import org.apache.poi.hssf.record.StringRecord;
import org.apache.poi.hssf.record.TextObjectRecord;
import org.apache.poi.hssf.record.WSBoolRecord;
import org.apache.poi.hssf.record.common.UnicodeString;
import org.apache.poi.hssf.record.crypto.Biff8EncryptionKey;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.NumberToTextConverter;

import xyz.hotchpotch.hogandiff.logic.BookHandler;
import xyz.hotchpotch.hogandiff.logic.CommonUtil;
import xyz.hotchpotch.hogandiff.logic.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.logic.SheetHandler;
import xyz.hotchpotch.hogandiff.logic.CellsUtil;
import xyz.hotchpotch.hogandiff.logic.models.BookType;
import xyz.hotchpotch.hogandiff.logic.models.CellData;
import xyz.hotchpotch.hogandiff.logic.models.SheetType;

/**
 * Apache POI イベントモデル API を利用して、
 * .xls 形式のExcelブックのワークシートから
 * セルデータを抽出する {@link LoaderForCells} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLS })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class LoaderForCellsWithPoiEventApi implements LoaderForCells {

    // [static members] ********************************************************

    /**
     * 内部処理のステップを表す列挙型です。<br>
     *
     * @author nmby
     */
    private static enum ProcessingStep {

        // [static members] ----------------------------------------------------

        /** BOUNDSHEET レコードの中から、目的のシートが何番目に定義されているかを探します。 */
        SEARCHING_SHEET_DEFINITION,

        /** Excelブック共通の SST レコードを読み取ります。 */
        READING_SST_DATA,

        /** 目的のシートが定義される BOF レコードを探します。 */
        SEARCHING_SHEET_BODY,

        /** 目的のシートがワークシートなのかダイアログシートなのかを確認します。 */
        CHECK_WORKSHEET_OR_DIALOGSHEET,

        /** 目的のシートのセル内容物とセルコメントを読み取ります。 */
        READING_CELL_CONTENTS_AND_COMMENTS,

        /** 処理完了。 */
        COMPLETED;

        // [instance members] --------------------------------------------------
    }

    private static class Listener1 implements HSSFListener {

        // [static members] ----------------------------------------------------

        // [instance members] --------------------------------------------------

        private final String sheetName;
        private final boolean extractCachedValue;
        private final Map<String, CellData> cells = new HashMap<>();
        private final Map<Integer, String> comments = new HashMap<>();

        private ProcessingStep step = ProcessingStep.SEARCHING_SHEET_DEFINITION;
        private int sheetIdx = 0;
        private int currIdx = 0;
        private List<String> sst;
        private FormulaRecord prevFormulaRec;
        private CommonObjectDataSubRecord prevFtCmoRec;

        private Listener1(String sheetName, boolean extractCachedValue) {
            assert sheetName != null;

            this.sheetName = sheetName;
            this.extractCachedValue = extractCachedValue;
        }

        /**
         * .xls 形式のExcelブックからセルデータを抽出します。<br>
         * 
         * @param record レコード
         * @throws NoSuchElementException
         *                                       指定された名前のシートが見つからない場合
         * @throws UnsupportedOperationException
         *                                       指定された名前のシートがワークシートではなかった場合
         * @throws UnsupportedOperationException
         *                                       数式セルからキャッシュされた計算値ではなく数式文字列を抽出しようとした場合
         */
        @Override
        public void processRecord(Record record) {
            switch (step) {
                case SEARCHING_SHEET_DEFINITION:
                    searchingSheetDefinition(record);
                    break;

                case READING_SST_DATA:
                    readingSstData(record);
                    break;

                case SEARCHING_SHEET_BODY:
                    searchingSheetBody(record);
                    break;

                case CHECK_WORKSHEET_OR_DIALOGSHEET:
                    checkWorksheetOrDialogsheet(record);
                    break;

                case READING_CELL_CONTENTS_AND_COMMENTS:
                    readingCellContentsAndComments(record);
                    break;

                case COMPLETED:
                    // nop
                    break;

                default:
                    throw new AssertionError(step);
            }
        }

        /**
         * BOUNDSHEET レコードの中から、目的のシートが何番目に定義されているかを探します。<br>
         * 
         * @param record レコード
         * @throws NoSuchElementException 指定された名前のシートが見つからない場合
         */
        private void searchingSheetDefinition(Record record) {
            if (record instanceof BoundSheetRecord bsRec) {
                if (sheetName.equals(bsRec.getSheetname())) {
                    step = ProcessingStep.READING_SST_DATA;
                } else {
                    sheetIdx++;
                }

            } else if (record instanceof EOFRecord) {
                throw new NoSuchElementException(
                        "no such sheet : " + sheetName);
            }
        }

        /**
         * Excelブック共通の SST レコードを読み取ります。<br>
         * 
         * @param record レコード
         */
        private void readingSstData(Record record) {
            if (record instanceof SSTRecord sstRec) {
                sst = IntStream.range(0, sstRec.getNumUniqueStrings())
                        .mapToObj(sstRec::getString)
                        .map(UnicodeString::getString)
                        .toList();
                step = ProcessingStep.SEARCHING_SHEET_BODY;

            } else if (record instanceof EOFRecord) {
                throw new AssertionError("no sst record");
            }
        }

        /**
         * 目的のシートが定義される BOF レコードを探します。<br>
         * 
         * @param record レコード
         * @throws UnsupportedOperationException
         *                                       指定された名前のシートがグラフシートもしくはマクロシートだった場合
         */
        private void searchingSheetBody(Record record) {
            if (record instanceof BOFRecord bofRec) {
                switch (bofRec.getType()) {
                    case BOFRecord.TYPE_WORKSHEET:
                        if (currIdx == sheetIdx) {
                            step = ProcessingStep.CHECK_WORKSHEET_OR_DIALOGSHEET;
                        } else {
                            currIdx++;
                        }
                        break;

                    case BOFRecord.TYPE_CHART:
                    case BOFRecord.TYPE_EXCEL_4_MACRO:
                        if (currIdx == sheetIdx) {
                            throw new UnsupportedOperationException(
                                    "unsupported sheet type : " + bofRec.getType());
                        } else {
                            currIdx++;
                            break;
                        }

                    case BOFRecord.TYPE_WORKBOOK:
                    case BOFRecord.TYPE_WORKSPACE_FILE:
                    case BOFRecord.TYPE_VB_MODULE:
                        // nop
                        break;

                    default:
                        throw new AssertionError("unknown BOF type: " + bofRec.getType());
                }
            }
        }

        /**
         * 目的のシートがワークシートなのかダイアログシートなのかを確認します。<br>
         * 
         * @param record レコード
         * @throws UnsupportedOperationException
         *                                       指定された名前のシートがダイアログシートだった場合
         */
        private void checkWorksheetOrDialogsheet(Record record) {
            if (record instanceof WSBoolRecord wsbRec) {
                if (wsbRec.getDialog()) {
                    // FIXME: [No.01 シート識別不正 - HSSF] ダイアログシートも何故か getDialog() == false が返されるっぽい。
                    throw new UnsupportedOperationException("dialog sheets are not supported");
                }
                step = ProcessingStep.READING_CELL_CONTENTS_AND_COMMENTS;

            } else if (record instanceof EOFRecord) {
                throw new AssertionError("no WSBool record");
            }
        }

        /**
         * 目的のシートのセル内容物とセルコメントを読み取ります。<br>
         * 
         * @param record レコード
         */
        private void readingCellContentsAndComments(Record record) {
            assert record != null;

            if (record instanceof CellRecord && prevFormulaRec != null) {
                throw new AssertionError("no following string record");
            }
            if (record instanceof StringRecord && prevFormulaRec == null) {
                throw new AssertionError("unexpected string record");
            }

            String value = null;

            switch (record) {
                case LabelSSTRecord lRec: // セル内容抽出用
                    value = sst.get(lRec.getSSTIndex());
                    break;

                case NumberRecord nRec: // セル内容抽出用
                    value = NumberToTextConverter.toText(nRec.getValue());
                    break;

                case RKRecord rkRec: // セル内容抽出用
                    value = NumberToTextConverter.toText(rkRec.getRKNumber());
                    break;

                case BoolErrRecord beRec: // セル内容抽出用
                    if (beRec.isBoolean()) {
                        value = Boolean.toString(beRec.getBooleanValue());
                    } else {
                        value = ErrorEval.getText(beRec.getErrorValue());
                    }
                    break;

                case FormulaRecord fRec: // セル内容抽出用
                    value = getValueFromFormulaRecord(fRec);
                    break;

                case StringRecord sRec: // 数式計算値抽出用
                    String calculated = sRec.getString();
                    if (calculated != null && !"".equals(calculated)) {
                        cells.put(
                                CellsUtil.idxToAddress(
                                        prevFormulaRec.getRow(),
                                        prevFormulaRec.getColumn()),
                                new CellData(
                                        prevFormulaRec.getRow(),
                                        prevFormulaRec.getColumn(),
                                        calculated,
                                        null));
                    }
                    prevFormulaRec = null;
                    break;

                case ObjRecord objRec: // セルコメント抽出用
                    Optional<CommonObjectDataSubRecord> ftCmo = objRec.getSubRecords().stream()
                            .filter(sub -> sub instanceof CommonObjectDataSubRecord)
                            .map(sub -> (CommonObjectDataSubRecord) sub)
                            .filter(sub -> sub.getObjectType() == CommonObjectDataSubRecord.OBJECT_TYPE_COMMENT)
                            .findAny();
                    ftCmo.ifPresent(ftCmoRec -> {
                        if (prevFtCmoRec != null) {
                            throw new AssertionError("no following txo record");
                        }
                        prevFtCmoRec = ftCmoRec;
                    });
                    break;

                case TextObjectRecord txoRec: // セルコメント抽出用
                    if (prevFtCmoRec == null) {
                        // throw new AssertionError("no preceding ftCmo record");
                        // FIXME: [No.01 シート識別不正 - HSSF] ダイアログシートの場合もこのパスに流れ込んできてしまう。
                        break;
                    }
                    comments.put(prevFtCmoRec.getObjectId(), txoRec.getStr().getString());
                    prevFtCmoRec = null;
                    break;

                case NoteRecord noteRec: // セルコメント抽出用
                    String address = CellsUtil.idxToAddress(noteRec.getRow(), noteRec.getColumn());
                    String comment = comments.remove(noteRec.getShapeId());

                    if (cells.containsKey(address)) {
                        CellData original = cells.get(address);
                        cells.put(address, original.withComment(comment));
                    } else {
                        cells.put(address, CellData.of(address, "", comment));
                    }
                    break;

                case EOFRecord eofRec: // 次ステップに移行
                    step = ProcessingStep.COMPLETED;
                    break;

                default:
                    // nop
            }

            if (value != null && !"".equals(value)) {
                CellRecord cellRec = (CellRecord) record;
                cells.put(
                        CellsUtil.idxToAddress(
                                cellRec.getRow(),
                                cellRec.getColumn()),
                        new CellData(
                                cellRec.getRow(),
                                cellRec.getColumn(),
                                value,
                                null));
            }
        }

        /**
         * FORMULA レコードからセル格納値を抽出します。<br>
         * 
         * @param fRec レコード
         * @return セル格納値
         * @throws UnsupportedOperationException
         *                                       キャッシュされた計算値ではなく数式文字列を抽出しようとした場合
         */
        private String getValueFromFormulaRecord(FormulaRecord fRec) {
            if (extractCachedValue) {
                if (fRec.hasCachedResultString()) {
                    prevFormulaRec = fRec;
                    return null;
                }

                CellType type = fRec.getCachedResultTypeEnum();

                return switch (type) {
                    case NUMERIC -> NumberToTextConverter.toText(fRec.getValue());
                    case BOOLEAN -> Boolean.toString(fRec.getCachedBooleanValue());
                    case ERROR -> ErrorEval.getText(fRec.getCachedErrorValue());

                    // nop: 空のセルは抽出しない。
                    case BLANK -> null;

                    // 利用者からのレポートによると、このパスに入る場合があるらしい。
                    // 返すべき適切な fRec のメンバが見当たらないため、nullを返しておく。
                    // FIXME: [No.04 数式サポート改善].xlsファイル形式を理解したうえでちゃんとやる
                    case STRING -> null;

                    case _NONE -> throw new AssertionError("_NONE");

                    // キャッシュされた値のタイプが FORMULA というのは無いはず
                    case FORMULA -> throw new AssertionError("FORMULA");

                    default -> throw new AssertionError("unknown cell type: " + type);
                };

            } else {
                // FIXME: [No.04 数式サポート改善] 数式文字列もサポートできるようにする
                throw new UnsupportedOperationException(
                        "extraction of formula strings is not supported");
            }
        }
    }

    // [instance members] ******************************************************

    private final boolean extractCachedValue;

    /**
     * コンストラクタ
     * 
     * @param extractCachedValue
     *                           数式セルからキャッシュされた計算値を抽出する場合は {@code true}、
     *                           数式文字列を抽出する場合は {@code false}
     */
    public LoaderForCellsWithPoiEventApi(boolean extractCachedValue) {
        this.extractCachedValue = extractCachedValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *                                  {@code bookPath}, {@code sheetName} のいずれかが
     *                                  {@code null} の場合
     * @throws IllegalArgumentException
     *                                  {@code bookPath} がサポート対象外の形式の場合
     * @throws ExcelHandlingException
     *                                  処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    // 例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    // 例えば、ブックやシートが見つからないとか、シート種類がサポート対象外とか。
    @Override
    public Set<CellData> loadCells(
            Path bookPath,
            String readPassword,
            String sheetName)
            throws ExcelHandlingException {

        Objects.requireNonNull(bookPath);
        // readPassword may be null.
        Objects.requireNonNull(sheetName);
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(bookPath));

        Biff8EncryptionKey.setCurrentUserPassword(readPassword);
        try (FileInputStream fin = new FileInputStream(bookPath.toFile());
                POIFSFileSystem poifs = new POIFSFileSystem(fin)) {

            HSSFRequest req = new HSSFRequest();
            Listener1 listener1 = new Listener1(sheetName, extractCachedValue);
            req.addListenerForAllRecords(listener1);
            HSSFEventFactory factory = new HSSFEventFactory();
            factory.abortableProcessWorkbookEvents(req, poifs);
            return Set.copyOf(listener1.cells.values());

        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "processing failed : %s - %s".formatted(bookPath, sheetName), e);

        } finally {
            Biff8EncryptionKey.setCurrentUserPassword(null);
        }
    }
}
