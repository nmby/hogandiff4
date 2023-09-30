package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
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

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.TreeResult;
import xyz.hotchpotch.hogandiff.excel.TreeResult.DirPairData;
import xyz.hotchpotch.hogandiff.util.IntPair;
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
    private static final IntPair COL_LEFT = IntPair.of(3, 8);
    private static final int COL_DIFF = 7;
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
            
            CreationHelper ch = book.getCreationHelper();
            Sheet sheet = book.getSheet(sheetName);
            Row templateRow = sheet.getRow(ROW_LIST_TEMPLATE);
            
            // 3. ヘッダ情報を出力する。
            outputHeader(sheet, treeResult, dstBookPath.getParent());
            
            // 4. フォルダとファイルの比較結果を出力する。
            
            // 4-1. 各種準備
            Pair<Map<Path, Path>> outputDirsMaps = new Pair<>(
                    new HashMap<>(),
                    new HashMap<>());
            
            for (Side side : Side.values()) {
                outputDirsMaps.get(side).put(
                        treeResult.topDirPair().a().path().getParent(),
                        dstBookPath.getParent());
            }
            
            BiFunction<Side, Path, String> relPath = (side, p) -> p.subpath(
                    treeResult.topDirPair().get(side).path().getNameCount() - 1,
                    p.getNameCount())
                    .toString();
            
            int rowNo = ROW_LIST_START - 1;
            
            // 4-2. フォルダペアごとの処理
            for (DirPairData pairData : treeResult.pairDataList()) {
                rowNo++;
                
                // 4-3. フォルダ名と差分シンボルの出力
                Pair<DirInfo> dirPair = pairData.dirPair();
                Optional<DirResult> dirResult = treeResult.results().get(dirPair.map(DirInfo::path));
                
                Pair<String> dirRelNames = new Pair<>(
                        dirPair.hasA() ? relPath.apply(Side.A, dirPair.a().path()) : null,
                        dirPair.hasB() ? relPath.apply(Side.B, dirPair.b().path()) : null);
                
                Pair<Path> outputDirs = new Pair<>(Side.A, Side.B)
                        .map(side -> dirPair.has(side)
                                ? outputDirsMaps.get(side).get(dirPair.get(side).path().getParent()).resolve(
                                        "【%s%d】%s".formatted(side, pairData.num(),
                                                dirPair.get(side).path().getFileName().toString()))
                                : null);
                
                for (Side side : Side.values()) {
                    if (dirPair.has(side)) {
                        outputDirsMaps.get(side).put(dirPair.get(side).path(), outputDirs.get(side));
                    }
                }
                
                outputDirLine(
                        ch,
                        sheet,
                        rowNo,
                        pairData.num(),
                        outputDirs,
                        dirRelNames,
                        dirPair,
                        dirResult);
                
                // 4-4. セル書式を整える
                copyCellStyles(sheet, rowNo, templateRow);
                
                // 4-5. Excelブック名ペアごとの処理
                for (int i = 0; i < pairData.bookNamePairs().size(); i++) {
                    rowNo++;
                    
                    // 4-6. Excelブック名と差分シンボルの出力
                    Pair<String> bookNames = pairData.bookNamePairs().get(i);
                    Optional<BookResult> bookResult = dirResult.flatMap(dr -> dr.results().get(bookNames));
                    
                    outputFileLine(
                            ch,
                            sheet,
                            rowNo,
                            pairData.num(),
                            i + 1,
                            outputDirs,
                            dirRelNames,
                            bookNames,
                            bookResult);
                    
                    // 4-7. セル書式を整える
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
    
    private void outputHeader(
            Sheet sheet,
            TreeResult treeResult,
            Path workDir) {
        
        PoiUtil.setCellValue(sheet, 0, 1,
                rb.getString("excel.poi.usermodel.TreeResultBookCreator.010").formatted(Side.A));
        PoiUtil.setCellValue(sheet, 1, 1,
                rb.getString("excel.poi.usermodel.TreeResultBookCreator.010").formatted(Side.B));
        PoiUtil.setCellValue(sheet, 2, 1,
                rb.getString("excel.poi.usermodel.TreeResultBookCreator.020"));
        
        PoiUtil.setCellValue(sheet, 0, 2, treeResult.topDirPair().a().path().toString());
        PoiUtil.setCellValue(sheet, 1, 2, treeResult.topDirPair().b().path().toString());
        PoiUtil.setCellValue(sheet, 2, 2, workDir.toString());
        
    }
    
    private void outputDirLine(
            CreationHelper ch,
            Sheet sheet,
            int rowNo,
            int dirNo,
            Pair<Path> outputDirs,
            Pair<String> dirRelNames,
            Pair<DirInfo> dirPair,
            Optional<DirResult> dirResult) {
        
        for (Side side : Side.values()) {
            if (dirPair.has(side)) {
                // フォルダパスの出力
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side), "【%s%d】".formatted(side, dirNo));
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 1, dirRelNames.get(side));
                
                // ハイパーリンクの設定
                Hyperlink link = ch.createHyperlink(HyperlinkType.FILE);
                link.setAddress(outputDirs.get(side).toString().replace("\\", "/"));
                PoiUtil.getCell(sheet, rowNo, COL_LEFT.get(side)).setHyperlink(link);
            }
        }
    }
    
    private void outputFileLine(
            CreationHelper ch,
            Sheet sheet,
            int rowNo,
            int dirNo,
            int bookNo,
            Pair<Path> outputDirs,
            Pair<String> dirRelNames,
            Pair<String> bookNames,
            Optional<BookResult> bookResult) {
        
        for (Side side : Side.values()) {
            if (bookNames.has(side)) {
                // ファイル名の出力
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side), "【%s%d】".formatted(side, dirNo));
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 1, dirRelNames.get(side));
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 2, "【%s%d-%d】".formatted(side, dirNo, bookNo));
                PoiUtil.setCellValue(sheet, rowNo, COL_LEFT.get(side) + 3, bookNames.get(side));
                
                // ハイパーリンクの設定
                Hyperlink link = ch.createHyperlink(HyperlinkType.FILE);
                link.setAddress(
                        // TODO: URI周りの処理をもっとスマートにできるはず..
                        outputDirs.get(side).resolve("【%s%d-%d】%s".formatted(side, dirNo, bookNo, bookNames.get(side)))
                                .toString().replace("\\", "/").replace(" ", "%20"));
                PoiUtil.getCell(sheet, rowNo, COL_LEFT.get(side) + 2).setHyperlink(link);
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
