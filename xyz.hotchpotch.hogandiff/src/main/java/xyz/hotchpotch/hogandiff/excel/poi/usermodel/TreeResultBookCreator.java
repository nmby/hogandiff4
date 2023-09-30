package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
            Row templateRow = sheet.getRow(ROW_LIST_TEMPLATE);
            
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
            
            Map<Path, Path> outputDirsA = new HashMap<>();
            Map<Path, Path> outputDirsB = new HashMap<>();
            outputDirsA.put(treeResult.topDirPair().a().path().getParent(), dstBookPath.getParent());
            outputDirsB.put(treeResult.topDirPair().b().path().getParent(), dstBookPath.getParent());
            
            int rowNo = ROW_LIST_START - 1;
            
            // 4-2. フォルダペアごとの処理
            for (DirPairData pairData : pairDataList) {
                rowNo++;
                
                // 4-3. フォルダ名と差分シンボルの出力
                Pair<DirInfo> dirPair = pairData.dirPair();
                Optional<DirResult> dirResult = dirResults.get(dirPair.map(DirInfo::path));
                
                String dirIdA = dirPair.hasA() ? "【A%d】".formatted(pairData.num()) : null;
                String dirNameA = dirPair.hasA() ? relPath.apply(Side.A, dirPair.a().path()) : null;
                String dirIdB = dirPair.hasB() ? "【B%d】".formatted(pairData.num()) : null;
                String dirNameB = dirPair.hasB() ? relPath.apply(Side.B, dirPair.b().path()) : null;
                
                if (dirPair.hasA()) {
                    PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT, dirIdA);
                    PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT + 1, dirNameA);
                }
                if (dirPair.hasB()) {
                    PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT, dirIdB);
                    PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT + 1, dirNameB);
                }
                
                // 4-4. 罫線を整える
                copyCellStyles(sheet, rowNo, templateRow);
                
                // 4-5. Excelブック名ペアごとの処理
                for (int i = 0; i < pairData.bookNamePairs().size(); i++) {
                    rowNo++;
                    
                    // 4-6. Excelブック名と差分シンボルの出力
                    Pair<String> names = pairData.bookNamePairs().get(i);
                    Optional<BookResult> bookResult = dirResult.flatMap(dr -> dr.results().get(names));
                    
                    if (names.hasA()) {
                        PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT, dirIdA);
                        PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT + 1, dirNameA);
                        PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT + 2,
                                "【A%d-%d】".formatted(pairData.num(), i + 1));
                        PoiUtil.setCellValue(sheet, rowNo, COL_A_LEFT + 3, names.a());
                    }
                    if (names.hasB()) {
                        PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT, dirIdB);
                        PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT + 1, dirNameB);
                        PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT + 2,
                                "【B%d-%d】".formatted(pairData.num(), i + 1));
                        PoiUtil.setCellValue(sheet, rowNo, COL_B_LEFT + 3, names.b());
                    }
                    
                    if (names.isOnlyA()) {
                        PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_ONLY_A);
                    } else if (names.isOnlyB()) {
                        PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_ONLY_B);
                    } else if (bookResult.isEmpty()) {
                        PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_FAILED);
                    } else if (bookResult.get().hasDiff()) {
                        PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_BOTH);
                    }
                    
                    // 4-8. 罫線を整える
                    copyCellStyles(sheet, rowNo, templateRow);
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
    
    private void copyCellStyles(Sheet sheet, int rowNo, Row templateRow) {
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_A_LEFT)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_A_LEFT).getCellStyle()));
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_A_LEFT + 1)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_A_LEFT + 1).getCellStyle()));
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_A_LEFT + 2)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_A_LEFT + 2).getCellStyle()));
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_A_LEFT + 3)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_A_LEFT + 3).getCellStyle()));
        
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_DIFF)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_DIFF).getCellStyle()));
        
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_B_LEFT)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_B_LEFT).getCellStyle()));
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_B_LEFT + 1)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_B_LEFT + 1).getCellStyle()));
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_B_LEFT + 2)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_B_LEFT + 2).getCellStyle()));
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_B_LEFT + 3)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_B_LEFT + 3).getCellStyle()));
    }
}
