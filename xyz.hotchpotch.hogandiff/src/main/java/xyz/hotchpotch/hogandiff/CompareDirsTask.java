package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.BResult;
import xyz.hotchpotch.hogandiff.excel.DResult;
import xyz.hotchpotch.hogandiff.excel.DirData;
import xyz.hotchpotch.hogandiff.excel.DirLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
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
        
        // 1. 作業用ディレクトリの作成
        Path workDir = createWorkDir(0, 2);
        
        Path dirPath1 = settings.get(SettingKeys.CURR_DIR_PATH1);
        Path dirPath2 = settings.get(SettingKeys.CURR_DIR_PATH2);
        DirLoader dirLoader = factory.dirLoader();
        DirData dirData1 = dirLoader.loadDir(dirPath1, false);
        DirData dirData2 = dirLoader.loadDir(dirPath2, false);
        
        // 2. 比較するExcelブックの組み合わせの決定
        List<Pair<String>> pairs = pairingBookNames(dirData1, dirData2, 2, 5);
        
        // 3. Excelブック同士の比較
        DResult results = compareDirs(dirData1, dirData2, pairs, 5, 80);
        
        //        // 4. 比較結果の表示（テキスト）
        //        showResultText(workDir, results, 80, 85);
        //        
        //        // 5. 比較結果の表示（Excelブック）
        //        showPaintedSheets(workDir, results, 80, 98);
        //        
        //        // 6. 処理終了のアナウンス
        //        announceEnd();
        
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
            List<Pair<String>> pairs,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("AppTask.220")).append(BR);
            updateMessage(str.toString());
            
            Map<Pair<String>, Optional<BResult>> results = new HashMap<>();
            
            int total = progressAfter - progressBefore;
            int numTotalPairs = (int) pairs.stream().filter(Pair::isPaired).count();
            int num = 0;
            
            for (int i = 0; i < pairs.size(); i++) {
                Pair<String> pair = pairs.get(i);
                if (!pair.isPaired()) {
                    continue;
                }
                str.append(DResult.formatBookNamesPair(i, pair));
                updateMessage(str.toString());
                
                BResult result = compareBooks(
                        dirData1.path().resolve(pair.a()),
                        dirData2.path().resolve(pair.b()));
                results.put(pair, Optional.of(result));
                
                str.append("  -  ").append(result.getDiffSimpleSummary()).append(BR);
                updateMessage(str.toString());
                
                num++;
                updateProgress(progressBefore + total * num / numTotalPairs, PROGRESS_MAX);
            }
            str.append(BR);
            
            List<Pair<String>> unpairedPairs = pairs.stream()
                    .filter(Predicate.not(Pair::isPaired))
                    .toList();
            for (Pair<String> pair : unpairedPairs) {
                results.put(pair, Optional.empty());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            return DResult.of(dirData1, dirData2, pairs, results);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.230")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTask.230"), e);
        }
    }
    
    private BResult compareBooks(Path bookPath1, Path bookPath2) {
        // TODO: coding
        return null;
    }
}
