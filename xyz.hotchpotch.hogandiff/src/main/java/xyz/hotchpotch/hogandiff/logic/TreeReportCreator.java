package xyz.hotchpotch.hogandiff.logic;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiFunction;

import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.logic.poi.PoiUtil;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * フォルダツリー同士の比較結果をExcelファイルの形式に出力する機能を提供します。<br>
 * 
 * @author nmby
 */
public class TreeReportCreator {
    
    // [static members] ********************************************************
    
    private static final String templateBookName = "result_tree.xlsx";
    private static final String sheetName = "result";
    
    private static final int ROW_LIST_TEMPLATE = 5;
    private static final int ROW_LIST_START = 7;
    private static final IntPair COL_LEFT = IntPair.of(3, 8);
    private static final int COL_DIFF = 7;
    private static final String DIFF_ONLY_A = "<";
    private static final String DIFF_ONLY_B = ">";
    private static final String DIFF_BOTH = "!";
    private static final String DIFF_FAILED = "?";
    
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    /**
     * フォルダツリー同士の比較結果をExcelファイルの形式で出力して指定されたパスに保存します。<br>
     * 
     * @param dstBookPath 保存先Excelブックのパス
     * @param treeResult  フォルダツリー同士の比較結果
     * @param recursively 「子フォルダも含める」の場合は {@code true}
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException   パラメータが {@code null} の場合
     */
    public void createResultBook(
            Path dstBookPath,
            ResultOfTrees treeResult,
            boolean recursively)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(dstBookPath);
        Objects.requireNonNull(treeResult);
        
        // 1. テンプレートブックをコピーする。
        try (InputStream srcIs = ClassLoader.getSystemResourceAsStream(templateBookName)) {
            Files.copy(srcIs, dstBookPath);
            dstBookPath.toFile().setReadable(true, false);
            dstBookPath.toFile().setWritable(true, false);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "failed to copy template book : %s -> %s".formatted(templateBookName,
                            dstBookPath),
                    e);
        }
        
        // 2. コピーしたファイルをExcelブックとしてロードする。
        try (InputStream is = Files.newInputStream(dstBookPath);
                Workbook book = WorkbookFactory.create(is)) {
            
            CreationHelper ch = book.getCreationHelper();
            Sheet sheet = book.getSheet(sheetName);
            Row templateRow = sheet.getRow(ROW_LIST_TEMPLATE);
            
            // 3. ヘッダ情報を出力する。
            outputHeader(ch, sheet, dstBookPath.getParent(), treeResult);
            
            // 4. フォルダとファイルの比較結果を出力する。
            
            // 4-1. 各種準備
            Pair<Map<Path, Path>> outputDirsMaps = new Pair<>(
                    new HashMap<>(),
                    new HashMap<>());
            
            for (Side side : Side.values()) {
                outputDirsMaps.get(side).put(
                        treeResult.flattenDirComparison().parentDirInfoPair().get(side)
                                .dirPath().getParent(),
                        dstBookPath.getParent());
            }
            
            BiFunction<Side, Path, String> relPath = (side, p) -> p.subpath(
                    treeResult.flattenDirComparison().parentDirInfoPair().get(side).dirPath()
                            .getNameCount() - 1,
                    p.getNameCount())
                    .toString();
            
            int rowNo = ROW_LIST_START - 1;
            
            // 4-2. フォルダペアごとの処理
            for (int j = 0; j < treeResult.flattenDirComparison().dirInfoPairs().size(); j++) {
                Pair<DirInfo> dirInfoPair = treeResult.flattenDirComparison().dirInfoPairs().get(j);
                PairingInfoDirs dirComparison = treeResult.flattenDirComparison().dirComparisons()
                        .get(dirInfoPair)
                        .get();
                String dirId = recursively ? Integer.toString(j + 1) : "";
                rowNo++;
                
                // 4-3. フォルダ名と差分シンボルの出力
                Optional<ResultOfDirs> dirResult = treeResult.dirResults().get(dirInfoPair);
                
                Pair<String> dirRelNamePair = Side.map(
                        side -> dirInfoPair.has(side)
                                ? relPath.apply(side, dirInfoPair.get(side).dirPath())
                                : null);
                
                Pair<Path> outputDirPair = Side.map(side -> dirInfoPair.has(side)
                        ? outputDirsMaps.get(side)
                                .get(dirInfoPair.get(side).dirPath().getParent())
                                .resolve("【%s%s】%s".formatted(side, dirId,
                                        dirInfoPair.get(side).dirPath()
                                                .getFileName()
                                                .toString()))
                        : null);
                
                for (Side side : Side.values()) {
                    if (dirInfoPair.has(side)) {
                        outputDirsMaps.get(side).put(dirInfoPair.get(side).dirPath(),
                                outputDirPair.get(side));
                    }
                }
                
                outputDirLine(
                        ch,
                        sheet,
                        rowNo,
                        dirId,
                        outputDirPair,
                        dirRelNamePair,
                        dirInfoPair,
                        dirResult);
                
                // 4-4. セル書式を整える
                copyCellStyles(sheet, rowNo, templateRow);
                
                // 4-5. Excelブックパスペアごとの処理
                for (int i = 0; i < dirComparison.childBookInfoPairs().size(); i++) {
                    rowNo++;
                    
                    // 4-6. Excelブック名と差分シンボルの出力
                    Pair<BookInfo> bookInfoPair = dirComparison.childBookInfoPairs().get(i);
                    Optional<ResultOfBooks> bookResult = dirResult
                            .map(ResultOfDirs::bookResults)
                            .flatMap(br -> br.get(bookInfoPair));
                    
                    outputFileLine(
                            ch,
                            sheet,
                            rowNo,
                            dirId,
                            i + 1,
                            outputDirPair,
                            dirRelNamePair,
                            bookInfoPair,
                            bookResult);
                    
                    // 4-7. セル書式を整える
                    copyCellStyles(sheet, rowNo, templateRow);
                }
                rowNo++;
            }
            
            sheet.setAutoFilter(new CellRangeAddress(ROW_LIST_START - 1, rowNo, COL_DIFF, COL_DIFF));
            
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
            ResultOfTrees treeResult) {
        
        PoiUtil.setCellValue(sheet, 0, 4,
                rb.getString("excel.poi.usermodel.BookResultBookCreator.010"));
        PoiUtil.setCellValue(sheet, 1, 4,
                rb.getString("excel.poi.usermodel.TreeResultBookCreator.020"));
        PoiUtil.setCellValue(sheet, 2, 4,
                rb.getString("excel.poi.usermodel.TreeResultBookCreator.010").formatted(Side.A));
        PoiUtil.setCellValue(sheet, 3, 4,
                rb.getString("excel.poi.usermodel.TreeResultBookCreator.010").formatted(Side.B));
        
        String timestamp = ar.settings().get(SettingKeys.CURR_TIMESTAMP);
        LocalDateTime localDateTime = LocalDateTime.parse(timestamp, formatter);
        PoiUtil.setCellValue(sheet, 0, 5, localDateTime);
        
        PoiUtil.setHyperlink(
                PoiUtil.setCellValue(sheet, 1, 5, workDir.toString()),
                workDir);
        
        Path topDirA = treeResult.flattenDirComparison().parentDirInfoPair().a().dirPath();
        PoiUtil.setHyperlink(
                PoiUtil.setCellValue(sheet, 2, 5, topDirA.toString()),
                topDirA);
        
        Path topDirB = treeResult.flattenDirComparison().parentDirInfoPair().b().dirPath();
        PoiUtil.setHyperlink(
                PoiUtil.setCellValue(sheet, 3, 5, topDirB.toString()),
                topDirB);
    }
    
    private void outputDirLine(
            CreationHelper ch,
            Sheet sheet,
            int rowNo,
            String dirId,
            Pair<Path> outputDirPair,
            Pair<String> dirRelNamePair,
            Pair<DirInfo> dirPair,
            Optional<ResultOfDirs> dirResult) {
        
        for (Side side : Side.values()) {
            if (dirPair.has(side)) {
                // フォルダパスの出力
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side), "【%s%s】".formatted(side, dirId));
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 1, dirRelNamePair.get(side));
                
                // ハイパーリンクの設定
                PoiUtil.setHyperlink(
                        PoiUtil.getCell(sheet, rowNo, COL_LEFT.get(side)),
                        outputDirPair.get(side));
            }
        }
    }
    
    private void outputFileLine(
            CreationHelper ch,
            Sheet sheet,
            int rowNo,
            String dirId,
            int bookNo,
            Pair<Path> outputDirPair,
            Pair<String> dirRelNamePair,
            Pair<BookInfo> bookInfoPair,
            Optional<ResultOfBooks> bookResult) {
        
        for (Side side : Side.values()) {
            if (bookInfoPair.has(side)) {
                String bookName = bookInfoPair.get(side).bookName();
                
                // フォルダ名とファイル名の出力
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side), "【%s%s】".formatted(side, dirId));
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 1, dirRelNamePair.get(side));
                
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 2,
                        "【%s%s-%d】".formatted(side, dirId, bookNo));
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 3, bookName);
                
                // ハイパーリンクの設定
                PoiUtil.setHyperlink(
                        PoiUtil.getCell(sheet, rowNo, COL_LEFT.get(side)),
                        outputDirPair.get(side));
                
                Path bookPath = outputDirPair.get(side)
                        .resolve("【%s%s-%d】%s".formatted(side, dirId, bookNo, bookName));
                PoiUtil.setHyperlink(
                        PoiUtil.getCell(sheet, rowNo, COL_LEFT.get(side) + 2),
                        bookPath);
            }
        }
        
        // 差分記号の出力
        if (bookInfoPair.isOnlyA()) {
            PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_ONLY_A);
        } else if (bookInfoPair.isOnlyB()) {
            PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_ONLY_B);
        } else if (bookResult.isEmpty()) {
            PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_FAILED);
        } else if (bookResult.get().hasDiff()) {
            PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_BOTH);
        }
    }
    
    private void copyCellStyles(
            Sheet sheet,
            int rowNo,
            Row templateRow) {
        
        for (Side side : Side.values()) {
            for (int i = 0; i < 4; i++) {
                int ii = i;
                PoiUtil.getCellIfPresent(sheet, rowNo, COL_LEFT.get(side) + ii)
                        .ifPresent(c -> c.setCellStyle(templateRow
                                .getCell(COL_LEFT.get(side) + ii).getCellStyle()));
            }
        }
        
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_DIFF)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_DIFF).getCellStyle()));
    }
}
