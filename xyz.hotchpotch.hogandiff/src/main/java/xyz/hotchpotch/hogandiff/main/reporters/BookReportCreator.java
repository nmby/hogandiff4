package xyz.hotchpotch.hogandiff.main.reporters;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.main.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.main.PoiUtil;
import xyz.hotchpotch.hogandiff.main.CellsUtil;
import xyz.hotchpotch.hogandiff.main.models.CellData;
import xyz.hotchpotch.hogandiff.main.models.ResultOfBooks;
import xyz.hotchpotch.hogandiff.main.models.ResultOfSheets;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * Excelブック同士の比較結果をExcelファイルの形式に出力する機能を提供します。<br>
 * 
 * @author nmby
 */
public class BookReportCreator {

    // [static members] ********************************************************

    private static final String templateBookName = "result_book.xlsx";
    private static final String sheetName = "result";
    private static final int MAX_CHARS = 2_000;

    private static final int ROW_TEMPLATE_SHEET_TITLE = 4;
    private static final int ROW_TEMPLATE_NO_DIFF = 5;
    private static final int ROW_TEMPLATE_NO_OPPONENT = 6;
    private static final int ROW_TEMPLATE_FAILED = 7;
    private static final int ROW_TEMPLATE_RROWS_TITLE = 8;
    private static final int ROW_TEMPLATE_RCOLS_TITLE = 12;
    private static final int ROW_TEMPLATE_DCELLS_TITLE = 16;
    private static final int ROW_START = 19;
    private static final IntPair COL_LEFT = IntPair.of(3, 5);

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    // [instance members] ******************************************************

    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();

    /**
     * Excelブック同士の比較結果をExcelファイルの形式で出力して指定されたパスに保存します。<br>
     * 
     * @param dstBookPath 保存先Excelブックのパス
     * @param bookResult  フォルダツリー同士の比較結果
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException   パラメータが {@code null} の場合
     */
    public void createResultBook(
            Path dstBookPath,
            ResultOfBooks bookResult)
            throws ExcelHandlingException {

        Objects.requireNonNull(dstBookPath);
        Objects.requireNonNull(bookResult);

        // 1. テンプレートブックをコピーする。
        try (InputStream srcIs = ClassLoader.getSystemResourceAsStream(templateBookName)) {
            Files.copy(srcIs, dstBookPath);
            dstBookPath.toFile().setReadable(true, false);
            dstBookPath.toFile().setWritable(true, false);

        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "failed to copy template book : %s -> %s".formatted(templateBookName, dstBookPath),
                    e);
        }

        // 2. コピーしたファイルをExcelブックとしてロードする。
        try (InputStream is = Files.newInputStream(dstBookPath);
                Workbook book = WorkbookFactory.create(is)) {

            CreationHelper ch = book.getCreationHelper();
            Sheet sheet = book.getSheet(sheetName);

            // 3. ヘッダ情報を出力する。
            outputHeader(ch, sheet, dstBookPath.getParent(), bookResult);

            // 4. シートごとの比較結果を出力する。

            // 4-1. 各種準備
            int rowIdx = ROW_START - 1;

            // 4-2. シートペアごとの処理
            for (int j = 0; j < bookResult.bookComparison().childSheetNamePairs().size(); j++) {
                Pair<String> sheetNamePair = bookResult.bookComparison().childSheetNamePairs().get(j);
                ResultOfSheets sheetResult = bookResult.sheetResults().get(sheetNamePair).orElse(null);
                rowIdx++;

                // 4-3. シートペアの差分出力
                rowIdx = outputSheetResult(sheet, j, rowIdx, sheetNamePair, sheetResult);
            }

            // 5. Excelブックを上書き保存する。
            try (OutputStream os = Files.newOutputStream(dstBookPath)) {
                book.write(os);
            }

        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "failed to create and save result book : %s".formatted(dstBookPath), e);
        }
    }

    private void outputHeader(
            CreationHelper ch,
            Sheet sheet,
            Path workDir,
            ResultOfBooks bookResult) {

        PoiUtil.setCellValue(sheet, 0, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.010"));
        PoiUtil.setCellValue(sheet, 1, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.020"));
        PoiUtil.setCellValue(sheet, 2, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.030").formatted(Side.A));
        PoiUtil.setCellValue(sheet, 3, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.030").formatted(Side.B));

        String timestamp = ar.settings().get(SettingKeys.CURR_TIMESTAMP);
        LocalDateTime localDateTime = LocalDateTime.parse(timestamp, formatter);
        PoiUtil.setCellValue(sheet, 0, COL_LEFT.a() + 1, localDateTime);

        PoiUtil.setHyperlink(
                PoiUtil.setCellValue(sheet, 1, COL_LEFT.a() + 1, workDir.toString()),
                workDir);

        Path bookPathA = bookResult.bookComparison().parentBookInfoPair().a().bookPath();
        PoiUtil.setHyperlink(
                PoiUtil.setCellValue(sheet, 2, COL_LEFT.a() + 1, bookPathA.toString()),
                bookPathA);

        Path bookPathB = bookResult.bookComparison().parentBookInfoPair().b().bookPath();
        PoiUtil.setHyperlink(
                PoiUtil.setCellValue(sheet, 3, COL_LEFT.a() + 1, bookPathB.toString()),
                bookPathB);

        PoiUtil.setCellValue(sheet, ROW_TEMPLATE_NO_DIFF, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.040"));
        PoiUtil.setCellValue(sheet, ROW_TEMPLATE_NO_OPPONENT, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.090"));
        PoiUtil.setCellValue(sheet, ROW_TEMPLATE_FAILED, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.050"));
    }

    private int outputSheetResult(
            Sheet sheet,
            int sheetIdx,
            int rowIdx,
            Pair<String> sheetNamePair,
            ResultOfSheets sheetResult) {

        PoiUtil.copyRow(sheet, ROW_TEMPLATE_SHEET_TITLE, rowIdx);
        PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a() - 1, sheetIdx + 1);
        PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a() + 1,
                sheetNamePair.hasA() ? sheetNamePair.a()
                        : rb.getString("excel.poi.usermodel.BookResultBookCreator.090"));
        PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b() + 1,
                sheetNamePair.hasB() ? sheetNamePair.b()
                        : rb.getString("excel.poi.usermodel.BookResultBookCreator.090"));
        rowIdx++;

        // 比較対象なしの場合は「比較対象なし」の旨を出力する。
        if (!sheetNamePair.isPaired()) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_NO_OPPONENT, rowIdx);
            rowIdx++;
            return rowIdx;
        }

        // 比較結果なしの場合は「失敗」の旨を出力する。
        if (sheetResult == null) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_FAILED, rowIdx);
            rowIdx++;
            return rowIdx;
        }

        // 差分なしの場合は「差分なし」の旨を出力する。
        if (!sheetResult.hasDiff()) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_NO_DIFF, rowIdx);
            rowIdx++;
            return rowIdx;
        }

        // 余剰行ありの場合
        if (!sheetResult.redundantRows().a().isEmpty() || !sheetResult.redundantRows().b().isEmpty()) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_RROWS_TITLE, rowIdx);
            PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a(),
                    rb.getString("excel.poi.usermodel.BookResultBookCreator.060").formatted(
                            sheetResult.redundantRows().a().size(),
                            sheetResult.redundantRows().b().size()));
            rowIdx++;

            Pair<String> rRows = sheetResult.redundantRows()
                    .map(rows -> rows.stream().map(i -> String.valueOf(i + 1)).collect(Collectors.joining(", ")));
            String rRowsA = rRows.a();
            String rRowsB = rRows.b();
            int rRowsStart = rowIdx;

            while (!rRowsA.isEmpty() || !rRowsB.isEmpty()) {
                boolean isLast = rRowsA.length() <= MAX_CHARS && rRowsB.length() <= MAX_CHARS;
                PoiUtil.copyRow(sheet, ROW_TEMPLATE_RROWS_TITLE + (isLast ? 3 : 2), rowIdx);
                if (rowIdx == rRowsStart) {
                    PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a(), "【A】");
                    PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b(), "【B】");
                }

                String a = null;
                String b = null;
                if (rRowsA.length() <= MAX_CHARS) {
                    a = rRowsA;
                    rRowsA = "";
                } else {
                    int idx = rRowsA.lastIndexOf(", ", MAX_CHARS) + 2;
                    a = rRowsA.substring(0, idx);
                    rRowsA = rRowsA.substring(idx);
                }
                if (rRowsB.length() <= MAX_CHARS) {
                    b = rRowsB;
                    rRowsB = "";
                } else {
                    int idx = rRowsB.lastIndexOf(", ", MAX_CHARS) + 2;
                    b = rRowsB.substring(0, idx);
                    rRowsB = rRowsB.substring(idx);
                }
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a() + 1, a);
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b() + 1, b);
                rowIdx++;
            }
            sheet.groupRow(rRowsStart, rowIdx - 1);
        }

        // 余剰列ありの場合
        if (!sheetResult.redundantColumns().a().isEmpty() || !sheetResult.redundantColumns().b().isEmpty()) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_RCOLS_TITLE, rowIdx);
            PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a(),
                    rb.getString("excel.poi.usermodel.BookResultBookCreator.070").formatted(
                            sheetResult.redundantColumns().a().size(),
                            sheetResult.redundantColumns().b().size()));
            rowIdx++;

            Pair<String> rCols = sheetResult.redundantColumns()
                    .map(cols -> cols.stream().map(CellsUtil::columnIdxToStr).collect(Collectors.joining(", ")));
            String rColsA = rCols.a();
            String rColsB = rCols.b();
            int rColsStart = rowIdx;

            while (!rColsA.isEmpty() || !rColsB.isEmpty()) {
                boolean isLast = rColsA.length() <= MAX_CHARS && rColsB.length() <= MAX_CHARS;
                PoiUtil.copyRow(sheet, ROW_TEMPLATE_RCOLS_TITLE + (isLast ? 3 : 2), rowIdx);
                if (rowIdx == rColsStart) {
                    PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a(), "【A】");
                    PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b(), "【B】");
                }

                String a = null;
                String b = null;
                if (rColsA.length() <= MAX_CHARS) {
                    a = rColsA;
                    rColsA = "";
                } else {
                    int idx = rColsA.lastIndexOf(", ", MAX_CHARS) + 2;
                    a = rColsA.substring(0, idx);
                    rColsA = rColsA.substring(idx);
                }
                if (rColsB.length() <= MAX_CHARS) {
                    b = rColsB;
                    rColsB = "";
                } else {
                    int idx = rColsB.lastIndexOf(", ", MAX_CHARS) + 2;
                    b = rColsB.substring(0, idx);
                    rColsB = rColsB.substring(idx);
                }
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a() + 1, a);
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b() + 1, b);
                rowIdx++;
            }
            sheet.groupRow(rColsStart, rowIdx - 1);
        }

        // 差分セルありの場合
        if (!sheetResult.diffCells().isEmpty()) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_DCELLS_TITLE, rowIdx);
            PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a(),
                    rb.getString("excel.poi.usermodel.BookResultBookCreator.080").formatted(
                            sheetResult.diffCells().size()));
            rowIdx++;
            int start = rowIdx;

            for (Pair<CellData> pair : sheetResult.diffCells()) {
                PoiUtil.copyRow(sheet, ROW_TEMPLATE_DCELLS_TITLE + 1, rowIdx);

                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a(), pair.a().address());
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b(), pair.b().address());
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a() + 1, pair.a().dataString());
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b() + 1, pair.b().dataString());
                rowIdx++;
            }
            sheet.groupRow(start, rowIdx - 1);
        }
        return rowIdx;
    }
}
