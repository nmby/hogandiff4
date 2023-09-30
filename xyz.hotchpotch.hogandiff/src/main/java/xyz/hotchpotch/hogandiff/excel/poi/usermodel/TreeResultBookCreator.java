package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiFunction;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellUtil;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.TreeResult;
import xyz.hotchpotch.hogandiff.excel.TreeResult.DirPairData;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

public class TreeResultBookCreator {
    
    // [static members] ********************************************************
    
    private static final String templateBookName = "result.xlsx";
    private static final Path templateBookPath;
    static {
        Path tmp = null;
        try {
            tmp = Path.of(AppMain.class.getResource(templateBookName).toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        templateBookPath = tmp;
    }
    private static final String sheetName = "result";
    
    private static final int ROW_LIST_TEMPLATE = 4;
    private static final int ROW_LIST_START = 6;
    private static final int COL_A_LEFT = 3;
    private static final int COL_DIFF = 7;
    private static final int COL_B_LEFT = 8;
    private static final String DIFF_ONLY_A = "<";
    private static final String DIFF_ONLY_B = ">";
    private static final String DIFF_BOTH = "!";
    private static final String DIFF_FAILED = "?";
    
    // [instance members] ******************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    public void createResultBook(
            Path dstBookPath,
            TreeResult treeResult)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(dstBookPath, "dstBookPath");
        Objects.requireNonNull(treeResult, "treeResult");
        
        // 1. テンプレートブックをコピーする。
        try {
            Files.copy(templateBookPath, dstBookPath);
            dstBookPath.toFile().setReadable(true, false);
            dstBookPath.toFile().setWritable(true, false);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "failed to copy template book : %s -> %s".formatted(templateBookPath, dstBookPath),
                    e);
        }
        
        // 2. コピーしたファイルをExcelブックとしてロードする。
        try (InputStream is = Files.newInputStream(dstBookPath);
                Workbook book = WorkbookFactory.create(is)) {
            
            Sheet sheet = book.getSheet(sheetName);
            
            // 3. ヘッダ情報を出力する。
            PoiUtil.setCellValue(sheet, 0, 1,
                    rb.getString("excel.poi.usermodel.TreeResultBookCreator.010").formatted(Side.A));
            PoiUtil.setCellValue(sheet, 1, 1,
                    rb.getString("excel.poi.usermodel.TreeResultBookCreator.010").formatted(Side.B));
            PoiUtil.setCellValue(sheet, 2, 1,
                    rb.getString("excel.poi.usermodel.TreeResultBookCreator.020"));
            
            PoiUtil.setCellValue(sheet, 0, 2, treeResult.topDirPair().a().path().toString());
            PoiUtil.setCellValue(sheet, 1, 2, treeResult.topDirPair().b().path().toString());
            PoiUtil.setCellValue(sheet, 2, 2, dstBookPath.getParent().toString());
            
            // 4. フォルダとファイルの比較結果を出力する。
            
            // 4-1. 各種準備
            List<DirPairData> pairDataList = treeResult.pairDataList();
            Map<Pair<Path>, Optional<DirResult>> dirResults = treeResult.results();
            
            BiFunction<Side, Path, String> relPath = (side, p) -> p.subpath(
                    treeResult.topDirPair().get(side).path().getNameCount() - 1,
                    p.getNameCount())
                    .toString();
            
            Row templateRow = sheet.getRow(ROW_LIST_TEMPLATE);
            Row currRow = null;
            int rowNo = ROW_LIST_START - 1;
            
            // 4-2. フォルダペアごとの処理
            for (DirPairData pairData : pairDataList) {
                rowNo++;
                currRow = CellUtil.getRow(rowNo, sheet);
                
                // 4-3. フォルダ名と差分シンボルの出力
                Pair<DirInfo> dirPair = pairData.dirPair();
                Optional<DirResult> dirResult = dirResults.get(dirPair.map(DirInfo::path));
                
                String dirIdA = dirPair.hasA() ? "【A%d】".formatted(pairData.num()) : "";
                String dirNameA = dirPair.hasA() ? relPath.apply(Side.A, dirPair.a().path()) : "";
                String dirIdB = dirPair.hasB() ? "【B%d】".formatted(pairData.num()) : "";
                String dirNameB = dirPair.hasB() ? relPath.apply(Side.B, dirPair.b().path()) : "";
                
                PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT, dirIdA);
                PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT + 1, dirNameA);
                PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT, dirIdB);
                PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT + 1, dirNameB);
                PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, "");
                
                // 4-4. 罫線を整える
                copyCellStyles(templateRow, currRow);
                
                // 4-5. Excelブック名ペアごとの処理
                for (int i = 0; i < pairData.bookNamePairs().size(); i++) {
                    rowNo++;
                    currRow = CellUtil.getRow(rowNo, sheet);
                    
                    // 4-6. Excelブック名と差分シンボルの出力
                    Pair<String> names = pairData.bookNamePairs().get(i);
                    Optional<BookResult> bookResult = dirResult.flatMap(dr -> dr.results().get(names));
                    
                    String bookIdA = names.hasA() ? "【A%d-%d】".formatted(pairData.num(), i + 1) : "";
                    String bookNameA = names.hasA() ? names.a() : "";
                    String bookIdB = names.hasB() ? "【B%d-%d】".formatted(pairData.num(), i + 1) : "";
                    String bookNameB = names.hasB() ? names.b() : "";
                    
                    PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT + 2, bookIdA);
                    PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT + 3, bookNameA);
                    PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT + 2, bookIdB);
                    PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT + 3, bookNameB);
                    
                    if (names.isOnlyA()) {
                        PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_ONLY_A);
                    } else if (names.isOnlyB()) {
                        PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_ONLY_B);
                    } else if (bookResult.isEmpty()) {
                        PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_FAILED);
                    } else if (bookResult.get().hasDiff()) {
                        PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_BOTH);
                    } else {
                        PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, "");
                    }
                    
                    // 4-7. Excelブック名の行にもフォルダ名を出力する。
                    PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT, names.hasA() ? dirIdA : "");
                    PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT + 1, names.hasA() ? dirNameA : "");
                    PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT, names.hasB() ? dirIdB : "");
                    PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT + 1, names.hasB() ? dirNameB : "");
                    
                    // 4-8. 罫線を整える
                    copyCellStyles(templateRow, currRow);
                }
                rowNo++;
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
    
    private void copyCellStyles(Row templateRow, Row currRow) {
        CellUtil.getCell(currRow, COL_A_LEFT).setCellStyle(templateRow.getCell(COL_A_LEFT).getCellStyle());
        CellUtil.getCell(currRow, COL_A_LEFT + 1).setCellStyle(templateRow.getCell(COL_A_LEFT + 1).getCellStyle());
        CellUtil.getCell(currRow, COL_A_LEFT + 2).setCellStyle(templateRow.getCell(COL_A_LEFT + 2).getCellStyle());
        CellUtil.getCell(currRow, COL_A_LEFT + 3).setCellStyle(templateRow.getCell(COL_A_LEFT + 3).getCellStyle());
        CellUtil.getCell(currRow, COL_DIFF).setCellStyle(templateRow.getCell(COL_DIFF).getCellStyle());
        CellUtil.getCell(currRow, COL_B_LEFT).setCellStyle(templateRow.getCell(COL_B_LEFT).getCellStyle());
        CellUtil.getCell(currRow, COL_B_LEFT + 1).setCellStyle(templateRow.getCell(COL_B_LEFT + 1).getCellStyle());
        CellUtil.getCell(currRow, COL_B_LEFT + 2).setCellStyle(templateRow.getCell(COL_B_LEFT + 2).getCellStyle());
        CellUtil.getCell(currRow, COL_B_LEFT + 3).setCellStyle(templateRow.getCell(COL_B_LEFT + 3).getCellStyle());
    }
}
