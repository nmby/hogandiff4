package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.BResult;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.DResult;
import xyz.hotchpotch.hogandiff.excel.DirData;
import xyz.hotchpotch.hogandiff.excel.DirLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SComparator;
import xyz.hotchpotch.hogandiff.excel.SResult;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * フォルダ同士の比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @author nmby
 */
/*package*/ class CompareDirsTask extends AppTaskBase {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /*package*/ CompareDirsTask(Settings settings, Factory factory) {
        super(settings, factory);
    }
    
    @Override
    protected Void call() throws Exception {
        
        // 0. 処理開始のアナウンス
        announceStart(0, 0);
        
        Path dirPath1 = settings.get(SettingKeys.CURR_DIR_PATH1);
        Path dirPath2 = settings.get(SettingKeys.CURR_DIR_PATH2);
        DirLoader dirLoader = factory.dirLoader();
        DirData dirData1 = dirLoader.loadDir(dirPath1, false);
        DirData dirData2 = dirLoader.loadDir(dirPath2, false);
        
        // 1. 作業用ディレクトリの作成
        Path workDir = createWorkDir(0, 2);
        Path outputDir1 = createOutputDir(workDir, dirData1, "【A】");
        Path outputDir2 = createOutputDir(workDir, dirData2, "【B】");
        
        // 2. 比較するExcelブックの組み合わせの決定
        List<Pair<String>> pairs = pairingBookNames(dirData1, dirData2, 2, 5);
        
        // 3. フォルダ同士の比較
        DResult dResult = compareDirs(dirData1, dirData2, outputDir1, outputDir2, pairs, 5, 90);
        
        // 4. 比較結果の表示（テキスト）
        saveAndShowResultText(workDir, dResult.toString(), 95, 97);
        
        // 5. 比較結果の表示（出力フォルダ）
        showOutputDirs(outputDir1, outputDir2, 97, 99);
        
        // 6. 処理終了のアナウンス
        announceEnd();
        
        return null;
    }
    
    // 0. 処理開始のアナウンス
    private void announceStart(int progressBefore, int progressAfter) {
        updateProgress(progressBefore, PROGRESS_MAX);
        
        Path dirPath1 = settings.get(SettingKeys.CURR_DIR_PATH1);
        Path dirPath2 = settings.get(SettingKeys.CURR_DIR_PATH2);
        
        str.append("%s%n[A] %s%n[B] %s%n%n"
                .formatted(rb.getString("AppTask.190"), dirPath1, dirPath2));
        
        updateMessage(str.toString());
        updateProgress(progressAfter, PROGRESS_MAX);
    }
    
    private Path createOutputDir(Path workDir, DirData dirData, String prefix)
            throws ApplicationException {
        
        Path outputDir = workDir.resolve(prefix + dirData.path().getFileName());
        try {
            return Files.createDirectory(outputDir);
            
        } catch (IOException e) {
            str.append(rb.getString("AppTask.040")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s".formatted(rb.getString("AppTask.040"), outputDir),
                    e);
        }
    }
    
    // 2. 比較するExcelブック名の組み合わせの決定
    private List<Pair<String>> pairingBookNames(
            DirData dirData1, DirData dirData2,
            int progressBefore, int progressAfter)
            
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            str.append(rb.getString("AppTask.200")).append(BR);
            updateMessage(str.toString());
            
            List<Pair<String>> pairs = getBookNamePairs(dirData1, dirData2);
            for (int i = 0; i < pairs.size(); i++) {
                Pair<String> pair = pairs.get(i);
                str.append(DResult.formatBookNamesPair(i, pair)).append(BR);
            }
            str.append(BR);
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return pairs;
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.210")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTask.210"), e);
        }
    }
    
    private List<Pair<String>> getBookNamePairs(DirData dirData1, DirData dirData2)
            throws ExcelHandlingException {
        
        Matcher<String> matcher = factory.bookNameMatcher(settings);
        List<IntPair> pairs = matcher.makePairs(dirData1.fileNames(), dirData2.fileNames());
        
        return pairs.stream()
                .map(p -> Pair.ofNullable(
                        p.hasA() ? dirData1.fileNames().get(p.a()) : null,
                        p.hasB() ? dirData2.fileNames().get(p.b()) : null))
                .toList();
    }
    
    // 3. フォルダ同士の比較
    private DResult compareDirs(
            DirData dirData1, DirData dirData2,
            Path outputDir1, Path outputDir2,
            List<Pair<String>> pairs,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        Map<Pair<String>, Optional<BResult>> results = new HashMap<>();
        
        updateProgress(progressBefore, PROGRESS_MAX);
        str.append(rb.getString("AppTask.220")).append(BR);
        updateMessage(str.toString());
        
        for (int i = 0; i < pairs.size(); i++) {
            Pair<String> pair = pairs.get(i);
            
            try {
                if (!pair.isPaired()) {
                    Path src = pair.hasA()
                            ? dirData1.path().resolve(pair.a())
                            : dirData2.path().resolve(pair.b());
                    Path dst = pair.hasA()
                            ? outputDir1.resolve("【A-%d】%s".formatted(i + 1, pair.a()))
                            : outputDir2.resolve("【B-%d】%s".formatted(i + 1, pair.b()));
                    
                    Files.copy(src, dst);
                    dst.toFile().setReadable(true, false);
                    dst.toFile().setWritable(true, false);
                    
                    results.put(pair, Optional.empty());
                    str.append(BR);
                    updateMessage(str.toString());
                    continue;
                }
                
                str.append(DResult.formatBookNamesPair(i, pair));
                updateMessage(str.toString());
                
                BookInfo srcInfo1 = BookInfo.of(dirData1.path().resolve(pair.a()), null);
                BookInfo srcInfo2 = BookInfo.of(dirData2.path().resolve(pair.b()), null);
                BookInfo dstInfo1 = BookInfo.of(outputDir1.resolve("【A-%d】%s".formatted(i + 1, pair.a())), null);
                BookInfo dstInfo2 = BookInfo.of(outputDir2.resolve("【B-%d】%s".formatted(i + 1, pair.b())), null);
                
                BResult result = compareBooks(srcInfo1, srcInfo2);
                results.put(pair, Optional.of(result));
                
                BookPainter painter1 = factory.painter(settings, srcInfo1);
                BookPainter painter2 = factory.painter(settings, srcInfo2);
                painter1.paintAndSave(srcInfo1, dstInfo1, result.getPiece(Side.A));
                painter2.paintAndSave(srcInfo2, dstInfo2, result.getPiece(Side.B));
                
                str.append("  -  ").append(result.getDiffSimpleSummary()).append(BR);
                updateMessage(str.toString());
                
                updateProgress(
                        progressBefore + (progressAfter - progressBefore) * (i + 1) / pairs.size(),
                        PROGRESS_MAX);
            } catch (Exception e) {
                str.append("  -  ").append(rb.getString("AppTask.240")).append(BR);
                updateMessage(str.toString());
                e.printStackTrace();
            }
        }
        str.append(BR);
        
        updateProgress(progressAfter, PROGRESS_MAX);
        return DResult.of(dirData1, dirData2, pairs, results);
    }
    
    private BResult compareBooks(BookInfo bookInfo1, BookInfo bookInfo2)
            throws ExcelHandlingException {
        
        List<Pair<String>> sheetNamePairs = getSheetNamePairs(bookInfo1, bookInfo2);
        
        SheetLoader loader1 = factory.sheetLoader(settings, bookInfo1);
        SheetLoader loader2 = factory.sheetLoader(settings, bookInfo2);
        SComparator comparator = factory.comparator(settings);
        Map<Pair<String>, Optional<SResult>> results = new HashMap<>();
        
        for (Pair<String> pair : sheetNamePairs) {
            if (!pair.isPaired()) {
                continue;
            }
            Set<CellData> cells1 = loader1.loadCells(bookInfo1, pair.a());
            Set<CellData> cells2 = loader2.loadCells(bookInfo2, pair.b());
            SResult result = comparator.compare(cells1, cells2);
            results.put(pair, Optional.of(result));
        }
        
        List<Pair<String>> unpairedPairs = sheetNamePairs.stream()
                .filter(Predicate.not(Pair::isPaired))
                .toList();
        for (Pair<String> pair : unpairedPairs) {
            results.put(pair, Optional.empty());
        }
        
        return BResult.of(bookInfo1.bookPath(), bookInfo2.bookPath(), sheetNamePairs, results);
    }
    
    // 5. 比較結果の表示（出力フォルダ）
    private void showOutputDirs(
            Path outputDir1,
            Path outputDir2,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("excel.DResult.060")).append(BR);
                
                Desktop.getDesktop().open(outputDir1.toFile());
                str.append("    - %s%n".formatted(outputDir1));
                
                Desktop.getDesktop().open(outputDir2.toFile());
                str.append("    - %s%n%n".formatted(outputDir2));
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("excel.DResult.070")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("excel.DResult.070"), e);
        }
    }
}
