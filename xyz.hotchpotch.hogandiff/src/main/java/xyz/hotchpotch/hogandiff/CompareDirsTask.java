package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.List;

import xyz.hotchpotch.hogandiff.core.Matcher;
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
        
        // 2. 比較するシートの組み合わせの決定
        List<Pair<String>> pairs = pairingBookNames(2, 5);
        
//        // 3. シート同士の比較
//        BResult results = compareSheets(pairs, 5, 75);
//        
//        // 4. 比較結果の表示（テキスト）
//        showResultText(workDir, results, 75, 80);
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
    private List<Pair<String>> pairingBookNames(int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            str.append(rb.getString("AppTask.200")).append(BR);
            updateMessage(str.toString());
            
            List<Pair<String>> pairs = getBookNamePairs();
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
    
    private List<Pair<String>> getBookNamePairs()
            throws ExcelHandlingException {
        
        Path dirPath1 = settings.get(SettingKeys.CURR_DIR_PATH1);
        Path dirPath2 = settings.get(SettingKeys.CURR_DIR_PATH2);
        DirLoader dirLoader = factory.dirLoader();
        DirData dirData1 = dirLoader.loadDir(dirPath1, false);
        DirData dirData2 = dirLoader.loadDir(dirPath2, false);
        
        Matcher<String> matcher = factory.bookNameMatcher(settings);
        List<IntPair> pairs = matcher.makePairs(dirData1.fileNames(), dirData2.fileNames());
        
        return pairs.stream()
                .map(p -> Pair.ofNullable(
                        p.hasA() ? dirData1.fileNames().get(p.a()) : null,
                        p.hasB() ? dirData2.fileNames().get(p.b()) : null))
                .toList();
    }
}
