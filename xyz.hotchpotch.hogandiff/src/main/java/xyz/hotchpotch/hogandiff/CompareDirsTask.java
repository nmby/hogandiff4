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

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.DirData;
import xyz.hotchpotch.hogandiff.excel.DirLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SheetComparator;
import xyz.hotchpotch.hogandiff.excel.SheetResult;
import xyz.hotchpotch.hogandiff.excel.CellsLoader;
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
    
    /*package*/ CompareDirsTask(
            Settings settings,
            Factory factory) {
        
        super(settings, factory);
    }
    
    @Override
    protected Void call() throws Exception {
        
        // 0. 処理開始のアナウンス
        announceStart(0, 0);
        
        // 1. ディレクトリ情報の抽出
        Pair<DirData> dirData = extractDirData();
        
        // 2. 作業用ディレクトリの作成
        Path workDir = createWorkDir(0, 2);
        
        // 3. 出力用ディレクトリの作成
        Pair<Path> outputDirs = createOutputDirs(workDir, dirData);
        
        // 4. 比較するExcelブックの組み合わせの決定
        List<Pair<String>> pairs = pairingBookNames(dirData, 2, 5);
        
        // 5. フォルダ同士の比較
        DirResult dResult = compareDirs(dirData, outputDirs, pairs, 5, 90);
        
        // 6. 比較結果の表示（テキスト）
        saveAndShowResultText(workDir, dResult.toString(), 95, 97);
        
        // 7. 比較結果の表示（出力フォルダ）
        showOutputDirs(outputDirs, 97, 99);
        
        // 8. 処理終了のアナウンス
        announceEnd();
        
        return null;
    }
    
    // 0. 処理開始のアナウンス
    private void announceStart(
            int progressBefore,
            int progressAfter) {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        
        Path dirPath1 = settings.get(SettingKeys.CURR_DIR_PATH1);
        Path dirPath2 = settings.get(SettingKeys.CURR_DIR_PATH2);
        
        str.append("%s%n[A] %s%n[B] %s%n%n".formatted(
                rb.getString("CompareDirsTask.010"),
                dirPath1,
                dirPath2));
        
        updateMessage(str.toString());
        updateProgress(progressAfter, PROGRESS_MAX);
    }
    
    // 1. ディレクトリ情報の抽出
    private Pair<DirData> extractDirData() throws ExcelHandlingException {
        Path dirPath1 = settings.get(SettingKeys.CURR_DIR_PATH1);
        Path dirPath2 = settings.get(SettingKeys.CURR_DIR_PATH2);
        DirLoader dirLoader = factory.dirLoader();
        DirData dirData1 = dirLoader.loadDir(dirPath1, false);
        DirData dirData2 = dirLoader.loadDir(dirPath2, false);
        
        return Pair.of(dirData1, dirData2);
    }
    
    // 3. 出力用ディレクトリの作成
    private Pair<Path> createOutputDirs(
            Path workDir,
            Pair<DirData> dirData)
            throws ApplicationException {
        
        Path outputDir1 = workDir.resolve("【A】" + dirData.a().getPath().getFileName());
        Path outputDir2 = workDir.resolve("【B】" + dirData.b().getPath().getFileName());
        
        try {
            return Pair.of(
                    Files.createDirectory(outputDir1),
                    Files.createDirectory(outputDir2));
            
        } catch (IOException e) {
            str.append(rb.getString("CompareDirsTask.020")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s%n%s".formatted(rb.getString("CompareDirsTask.020"), outputDir1, outputDir2),
                    e);
        }
    }
    
    // 4. 比較するExcelブック名の組み合わせの決定
    private List<Pair<String>> pairingBookNames(
            Pair<DirData> dirData,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareDirsTask.030")).append(BR);
            updateMessage(str.toString());
            
            List<Pair<String>> pairs = getBookNamePairs(dirData);
            for (int i = 0; i < pairs.size(); i++) {
                Pair<String> pair = pairs.get(i);
                str.append(DirResult.formatBookNamesPair(i, pair)).append(BR);
            }
            
            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return pairs;
            
        } catch (Exception e) {
            str.append(rb.getString("CompareDirsTask.040")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("CompareDirsTask.040"), e);
        }
    }
    
    private List<Pair<String>> getBookNamePairs(Pair<DirData> dirData)
            throws ExcelHandlingException {
        
        Matcher<String> matcher = factory.bookNameMatcher(settings);
        List<IntPair> pairs = matcher.makePairs(
                dirData.a().getFileNames(),
                dirData.b().getFileNames());
        
        return pairs.stream()
                .map(p -> Pair.ofNullable(
                        p.hasA() ? dirData.a().getFileNames().get(p.a()) : null,
                        p.hasB() ? dirData.b().getFileNames().get(p.b()) : null))
                .toList();
    }
    
    // 5. フォルダ同士の比較
    private DirResult compareDirs(
            Pair<DirData> dirData,
            Pair<Path> outputDir,
            List<Pair<String>> pairs,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        str.append(rb.getString("CompareDirsTask.050")).append(BR);
        updateMessage(str.toString());
        
        Map<Pair<String>, Optional<BookResult>> results = new HashMap<>();
        
        for (int i = 0; i < pairs.size(); i++) {
            Pair<String> pair = pairs.get(i);
            
            try {
                if (!pair.isPaired()) {
                    Path src = pair.hasA()
                            ? dirData.a().getPath().resolve(pair.a())
                            : dirData.b().getPath().resolve(pair.b());
                    Path dst = pair.hasA()
                            ? outputDir.a().resolve("【A-%d】%s".formatted(i + 1, pair.a()))
                            : outputDir.b().resolve("【B-%d】%s".formatted(i + 1, pair.b()));
                    
                    Files.copy(src, dst);
                    dst.toFile().setReadable(true, false);
                    dst.toFile().setWritable(true, false);
                    
                    results.put(pair, Optional.empty());
                    continue;
                }
                
                str.append(DirResult.formatBookNamesPair(i, pair));
                updateMessage(str.toString());
                
                BookInfo srcInfo1 = BookInfo.of(dirData.a().getPath().resolve(pair.a()), null);
                BookInfo srcInfo2 = BookInfo.of(dirData.b().getPath().resolve(pair.b()), null);
                BookInfo dstInfo1 = BookInfo.of(outputDir.a().resolve("【A-%d】%s".formatted(i + 1, pair.a())), null);
                BookInfo dstInfo2 = BookInfo.of(outputDir.b().resolve("【B-%d】%s".formatted(i + 1, pair.b())), null);
                
                BookResult result = compareBooks(
                        srcInfo1,
                        srcInfo2,
                        progressBefore + (progressAfter - progressBefore) * i / pairs.size(),
                        progressBefore + (progressAfter - progressBefore) * (i + 1) / pairs.size());
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
                results.putIfAbsent(pair, Optional.empty());
                str.append("  -  ").append(rb.getString("CompareDirsTask.060")).append(BR);
                updateMessage(str.toString());
                e.printStackTrace();
            }
        }
        str.append(BR);
        updateMessage(str.toString());
        updateProgress(progressAfter, PROGRESS_MAX);
        
        return DirResult.of(
                dirData.a(),
                dirData.b(),
                pairs,
                results);
    }
    
    private BookResult compareBooks(
            BookInfo bookInfo1,
            BookInfo bookInfo2,
            int progressBefore,
            int progressAfter)
            throws ExcelHandlingException {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        
        List<Pair<String>> sheetNamePairs = getSheetNamePairs(bookInfo1, bookInfo2);
        
        CellsLoader loader1 = factory.cellsLoader(settings, bookInfo1);
        CellsLoader loader2 = factory.cellsLoader(settings, bookInfo2);
        SheetComparator comparator = factory.comparator(settings);
        Map<Pair<String>, Optional<SheetResult>> results = new HashMap<>();
        
        for (int i = 0; i < sheetNamePairs.size(); i++) {
            Pair<String> pair = sheetNamePairs.get(i);
            
            if (pair.isPaired()) {
                Set<CellData> cells1 = loader1.loadCells(bookInfo1, pair.a());
                Set<CellData> cells2 = loader2.loadCells(bookInfo2, pair.b());
                SheetResult result = comparator.compare(cells1, cells2);
                results.put(pair, Optional.of(result));
                
            } else {
                results.put(pair, Optional.empty());
            }
            
            updateProgress(
                    progressBefore + (progressAfter - progressBefore) * (i + 1) / sheetNamePairs.size(),
                    PROGRESS_MAX);
        }
        
        return BookResult.of(
                bookInfo1.bookPath(),
                bookInfo2.bookPath(),
                sheetNamePairs,
                results);
    }
    
    // 7. 比較結果の表示（出力フォルダ）
    private void showOutputDirs(
            Pair<Path> outputDir,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("CompareDirsTask.070")).append(BR);
                
                Desktop.getDesktop().open(outputDir.a().toFile());
                str.append("    - %s%n".formatted(outputDir.a()));
                
                Desktop.getDesktop().open(outputDir.b().toFile());
                str.append("    - %s%n%n".formatted(outputDir.b()));
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("CompareDirsTask.080")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("CompareDirsTask.080"), e);
        }
    }
}
