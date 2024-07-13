package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.CellsUtil;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetResult;
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
    
    private static final int ROW_TEMPLATE_SHEET_TITLE = 4;
    private static final int ROW_TEMPLATE_NO_DIFF = 5;
    private static final int ROW_TEMPLATE_FAILED = 6;
    private static final int ROW_TEMPLATE_RROWS_TITLE = 7;
    private static final int ROW_TEMPLATE_RCOLS_TITLE = 9;
    private static final int ROW_TEMPLATE_DCELLS_TITLE = 11;
    private static final int ROW_START = 14;
    private static final IntPair COL_LEFT = IntPair.of(3, 5);
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    
    // こんなの絶対標準APIにあるはずだけど見つけられていない・・・
    // TODO: 実装改善（標準APIを利用する）
    private static String sanitize(Path path) {
        return path.toString().replace("\\", "/").replace(" ", "%20");
    }
    
    // [instance members] ******************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * Excelブック同士の比較結果をExcelファイルの形式で出力して指定されたパスに保存します。<br>
     * 
     * @param dstBookPath 保存先Excelブックのパス
     * @param bookResult フォルダツリー同士の比較結果
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public void createResultBook(
            Path dstBookPath,
            BookResult bookResult)
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
                SheetResult sheetResult = bookResult.sheetResults().get(sheetNamePair).orElse(null);
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
            BookResult bookResult) {
        
        PoiUtil.setCellValue(sheet, 0, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.010"));
        PoiUtil.setCellValue(sheet, 1, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.020"));
        PoiUtil.setCellValue(sheet, 2, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.030").formatted(Side.A));
        PoiUtil.setCellValue(sheet, 3, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.030").formatted(Side.B));
        
        // TODO: フォルダ名から日時取るのはスマートじゃないので改善する
        LocalDateTime localDateTime = LocalDateTime.parse(workDir.getFileName().toString(), formatter);
        PoiUtil.setCellValue(sheet, 0, COL_LEFT.a() + 1, localDateTime);
        
        Hyperlink linkW = ch.createHyperlink(HyperlinkType.FILE);
        linkW.setAddress(sanitize(workDir));
        PoiUtil.setCellValue(sheet, 1, COL_LEFT.a() + 1, workDir.toString()).setHyperlink(linkW);
        
        Path bookPathA = bookResult.bookComparison().parentBookInfoPair().a().bookPath();
        Hyperlink linkA = ch.createHyperlink(HyperlinkType.FILE);
        linkA.setAddress(sanitize(bookPathA));
        PoiUtil.setCellValue(sheet, 2, COL_LEFT.a() + 1, bookPathA.toString()).setHyperlink(linkA);
        
        Path bookPathB = bookResult.bookComparison().parentBookInfoPair().b().bookPath();
        Hyperlink linkB = ch.createHyperlink(HyperlinkType.FILE);
        linkB.setAddress(sanitize(bookPathB));
        PoiUtil.setCellValue(sheet, 3, COL_LEFT.a() + 1, bookPathB.toString()).setHyperlink(linkB);
        
        PoiUtil.setCellValue(sheet, ROW_TEMPLATE_NO_DIFF, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.040"));
        PoiUtil.setCellValue(sheet, ROW_TEMPLATE_FAILED, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.050"));
        PoiUtil.setCellValue(sheet, ROW_TEMPLATE_RROWS_TITLE, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.060"));
        PoiUtil.setCellValue(sheet, ROW_TEMPLATE_RCOLS_TITLE, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.070"));
        PoiUtil.setCellValue(sheet, ROW_TEMPLATE_DCELLS_TITLE, COL_LEFT.a(),
                rb.getString("excel.poi.usermodel.BookResultBookCreator.080"));
    }
    
    private int outputSheetResult(
            Sheet sheet,
            int sheetIdx,
            int rowIdx,
            Pair<String> sheetNamePair,
            SheetResult sheetResult) {
        
        PoiUtil.copyRow(sheet, ROW_TEMPLATE_SHEET_TITLE, rowIdx);
        PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a() - 1, sheetIdx + 1);
        PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a() + 1,
                sheetNamePair.hasA() ? sheetNamePair.a()
                        : rb.getString("excel.poi.usermodel.BookResultBookCreator.090"));
        PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b() + 1,
                sheetNamePair.hasB() ? sheetNamePair.b()
                        : rb.getString("excel.poi.usermodel.BookResultBookCreator.090"));
        rowIdx++;
        
        if (!sheetNamePair.isPaired()) {
            return rowIdx;
        }
        
        if (sheetResult == null) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_FAILED, rowIdx);
            rowIdx++;
            return rowIdx;
        }
        
        if (!sheetResult.hasDiff()) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_NO_DIFF, rowIdx);
            rowIdx++;
            return rowIdx;
        }
        
        if (!sheetResult.redundantRows().a().isEmpty() || !sheetResult.redundantRows().b().isEmpty()) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_RROWS_TITLE, rowIdx);
            rowIdx++;
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_RROWS_TITLE + 1, rowIdx);
            
            Pair<String> rRows = sheetResult.redundantRows()
                    .map(rows -> rows.stream().map(i -> String.valueOf(i + 1)).collect(Collectors.joining(", ")));
            PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a() + 1, rRows.a());
            PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b() + 1, rRows.b());
            rowIdx++;
        }
        
        if (!sheetResult.redundantColumns().a().isEmpty() || !sheetResult.redundantColumns().b().isEmpty()) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_RCOLS_TITLE, rowIdx);
            rowIdx++;
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_RCOLS_TITLE + 1, rowIdx);
            
            Pair<String> rCols = sheetResult.redundantColumns()
                    .map(cols -> cols.stream().map(CellsUtil::columnIdxToStr).collect(Collectors.joining(", ")));
            PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a() + 1, rCols.a());
            PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b() + 1, rCols.b());
            rowIdx++;
        }
        
        if (!sheetResult.diffCells().isEmpty()) {
            PoiUtil.copyRow(sheet, ROW_TEMPLATE_DCELLS_TITLE, rowIdx);
            rowIdx++;
            
            for (Pair<CellData> pair : sheetResult.diffCells()) {
                PoiUtil.copyRow(sheet, ROW_TEMPLATE_DCELLS_TITLE + 1, rowIdx);
                
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a(), pair.a().address());
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b(), pair.b().address());
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.a() + 1, pair.a().dataString());
                PoiUtil.setCellValue(sheet, rowIdx, COL_LEFT.b() + 1, pair.b().dataString());
                rowIdx++;
            }
        }
        return rowIdx;
    }
}
