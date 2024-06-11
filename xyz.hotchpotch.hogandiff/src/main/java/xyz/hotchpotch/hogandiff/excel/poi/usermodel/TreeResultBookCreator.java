package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.BiFunction;

import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.DirCompareInfo;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.TreeResult;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * フォルダツリー同士の比較結果をExcelファイルの形式に出力する機能を提供します。<br>
 * 
 * @author nmby
 */
public class TreeResultBookCreator {
    
    // [static members] ********************************************************
    
    private static final String templateBookName = "result.xlsx";
    private static final String sheetName = "result";
    
    private static final int ROW_LIST_TEMPLATE = 4;
    private static final int ROW_LIST_START = 6;
    private static final IntPair COL_LEFT = IntPair.of(3, 8);
    private static final int COL_DIFF = 7;
    private static final String DIFF_ONLY_A = "<";
    private static final String DIFF_ONLY_B = ">";
    private static final String DIFF_BOTH = "!";
    private static final String DIFF_FAILED = "?";
    
    // こんなの絶対標準APIにあるはずだけど見つけられていない・・・
    // TODO: 実装改善（標準APIを利用する）
    private static String sanitize(Path path) {
        return path.toString().replace("\\", "/").replace(" ", "%20");
    }
    
    // [instance members] ******************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * フォルダツリー同士の比較結果をExcelファイルの形式で出力して指定されたパスに保存します。<br>
     * 
     * @param dstBookPath 保存先Excelブックのパス
     * @param treeResult フォルダツリー同士の比較結果
     * @param recursively 「子フォルダも含める」の場合は {@code true}
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException {@code dstBookPath}, {@code treeResult} のいずれかが {@code null} の場合
     */
    public void createResultBook(
            Path dstBookPath,
            TreeResult treeResult,
            boolean recursively)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(dstBookPath, "dstBookPath");
        Objects.requireNonNull(treeResult, "treeResult");
        
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
                        treeResult.treeCompareInfo().topDirInfoPair().get(side).dirPath().getParent(),
                        dstBookPath.getParent());
            }
            
            BiFunction<Side, Path, String> relPath = (side, p) -> p.subpath(
                    treeResult.treeCompareInfo().topDirInfoPair().get(side).dirPath().getNameCount() - 1,
                    p.getNameCount())
                    .toString();
            
            int rowNo = ROW_LIST_START - 1;
            
            // 4-2. フォルダペアごとの処理
            for (int j = 0; j < treeResult.treeCompareInfo().dirInfoPairs().size(); j++) {
                Pair<DirInfo> dirInfoPair = treeResult.treeCompareInfo().dirInfoPairs().get(j);
                DirCompareInfo dirCompareInfo = treeResult.treeCompareInfo().dirCompareInfos().get(dirInfoPair).get();
                String dirId = recursively ? Integer.toString(j + 1) : "";
                rowNo++;
                
                // 4-3. フォルダ名と差分シンボルの出力
                Optional<DirResult> dirResult = treeResult.dirResults().get(dirInfoPair);
                
                Pair<String> dirRelNames = Side.map(
                        side -> dirInfoPair.has(side) ? relPath.apply(side, dirInfoPair.get(side).dirPath()) : null);
                
                Pair<Path> outputDirs = Side.map(side -> dirInfoPair.has(side)
                        ? outputDirsMaps.get(side).get(dirInfoPair.get(side).dirPath().getParent())
                                .resolve("【%s%s】%s".formatted(side, dirId,
                                        dirInfoPair.get(side).dirPath().getFileName().toString()))
                        : null);
                
                for (Side side : Side.values()) {
                    if (dirInfoPair.has(side)) {
                        outputDirsMaps.get(side).put(dirInfoPair.get(side).dirPath(), outputDirs.get(side));
                    }
                }
                
                outputDirLine(
                        ch,
                        sheet,
                        rowNo,
                        dirId,
                        outputDirs,
                        dirRelNames,
                        dirInfoPair,
                        dirResult);
                
                // 4-4. セル書式を整える
                copyCellStyles(sheet, rowNo, templateRow);
                
                // 4-5. Excelブック名ペアごとの処理
                for (int i = 0; i < dirCompareInfo.bookNamePairs().size(); i++) {
                    rowNo++;
                    
                    // 4-6. Excelブック名と差分シンボルの出力
                    Pair<String> bookNamePair = dirCompareInfo.bookNamePairs().get(i);
                    Optional<BookResult> bookResult = dirResult
                            .map(DirResult::bookResults)
                            .flatMap(br -> br.get(bookNamePair));
                    
                    outputFileLine(
                            ch,
                            sheet,
                            rowNo,
                            dirId,
                            i + 1,
                            outputDirs,
                            dirRelNames,
                            bookNamePair,
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
            TreeResult treeResult) {
        
        PoiUtil.setCellValue(sheet, 0, 1,
                rb.getString("excel.poi.usermodel.TreeResultBookCreator.010").formatted(Side.A));
        PoiUtil.setCellValue(sheet, 1, 1,
                rb.getString("excel.poi.usermodel.TreeResultBookCreator.010").formatted(Side.B));
        PoiUtil.setCellValue(sheet, 2, 1,
                rb.getString("excel.poi.usermodel.TreeResultBookCreator.020"));
        
        Path topDirA = treeResult.treeCompareInfo().topDirInfoPair().a().dirPath();
        Hyperlink linkA = ch.createHyperlink(HyperlinkType.FILE);
        linkA.setAddress(sanitize(topDirA));
        PoiUtil.setCellValue(sheet, 0, 2, topDirA.toString()).setHyperlink(linkA);
        
        Path topDirB = treeResult.treeCompareInfo().topDirInfoPair().b().dirPath();
        Hyperlink linkB = ch.createHyperlink(HyperlinkType.FILE);
        linkB.setAddress(sanitize(topDirB));
        PoiUtil.setCellValue(sheet, 1, 2, topDirB.toString()).setHyperlink(linkB);
        
        Hyperlink linkW = ch.createHyperlink(HyperlinkType.FILE);
        linkW.setAddress(sanitize(workDir));
        PoiUtil.setCellValue(sheet, 2, 2, workDir.toString()).setHyperlink(linkW);
    }
    
    private void outputDirLine(
            CreationHelper ch,
            Sheet sheet,
            int rowNo,
            String dirId,
            Pair<Path> outputDirs,
            Pair<String> dirRelNames,
            Pair<DirInfo> dirPair,
            Optional<DirResult> dirResult) {
        
        for (Side side : Side.values()) {
            if (dirPair.has(side)) {
                // フォルダパスの出力
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side), "【%s%s】".formatted(side, dirId));
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 1, dirRelNames.get(side));
                
                // ハイパーリンクの設定
                Hyperlink link = ch.createHyperlink(HyperlinkType.FILE);
                link.setAddress(sanitize(outputDirs.get(side)));
                PoiUtil.getCell(sheet, rowNo, COL_LEFT.get(side)).setHyperlink(link);
            }
        }
    }
    
    private void outputFileLine(
            CreationHelper ch,
            Sheet sheet,
            int rowNo,
            String dirId,
            int bookNo,
            Pair<Path> outputDirs,
            Pair<String> dirRelNames,
            Pair<String> bookNames,
            Optional<BookResult> bookResult) {
        
        for (Side side : Side.values()) {
            if (bookNames.has(side)) {
                // フォルダ名とファイル名の出力
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side), "【%s%s】".formatted(side, dirId));
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 1, dirRelNames.get(side));
                
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 2, "【%s%s-%d】".formatted(side, dirId, bookNo));
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 3, bookNames.get(side));
                
                // ハイパーリンクの設定
                Hyperlink dirLink = ch.createHyperlink(HyperlinkType.FILE);
                dirLink.setAddress(sanitize(outputDirs.get(side)));
                PoiUtil.getCell(sheet, rowNo, COL_LEFT.get(side)).setHyperlink(dirLink);
                
                Hyperlink fileLink = ch.createHyperlink(HyperlinkType.FILE);
                fileLink.setAddress(sanitize(outputDirs.get(side)
                        .resolve("【%s%s-%d】%s".formatted(side, dirId, bookNo, bookNames.get(side)))));
                PoiUtil.getCell(sheet, rowNo, COL_LEFT.get(side) + 2).setHyperlink(fileLink);
            }
        }
        
        // 差分記号の出力
        if (bookNames.isOnlyA()) {
            PoiUtil.setCellValue(sheet, rowNo, COL_DIFF, DIFF_ONLY_A);
        } else if (bookNames.isOnlyB()) {
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
                        .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_LEFT.get(side) + ii).getCellStyle()));
            }
        }
        
        PoiUtil.getCellIfPresent(sheet, rowNo, COL_DIFF)
                .ifPresent(c -> c.setCellStyle(templateRow.getCell(COL_DIFF).getCellStyle()));
    }
}
