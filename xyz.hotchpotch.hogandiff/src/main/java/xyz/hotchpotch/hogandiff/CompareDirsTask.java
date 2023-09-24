package xyz.hotchpotch.hogandiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import xyz.hotchpotch.hogandiff.excel.BookNamesMatcher;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.TreeResult.DirPairData;
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
        Pair<DirInfo> dirInfoPair = extractDirInfoPair();
        
        // 2. 作業用ディレクトリの作成
        Path workDir = createWorkDir(0, 2);
        
        // 3. 出力用ディレクトリの作成
        Pair<Path> outputDirs = createOutputDirs(workDir, dirInfoPair);
        
        // 4. 比較するExcelブックの組み合わせの決定
        List<Pair<String>> bookNamePairs = pairingBookNames(dirInfoPair, 2, 5);
        
        // 5. フォルダ同士の比較
        DirResult dResult = compareDirs(dirInfoPair, outputDirs, bookNamePairs, 5, 95);
        
        // 6. 比較結果の表示（テキスト）
        saveAndShowResultText(workDir, dResult.toString(), 95, 97);
        
        // 7. 比較結果の表示（出力フォルダ）
        showOutputDirs(workDir, 97, 99);
        
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
    
    // 3. 出力用ディレクトリの作成
    private Pair<Path> createOutputDirs(
            Path workDir,
            Pair<DirInfo> dirInfoPair)
            throws ApplicationException {
        
        Path outputDir1 = workDir.resolve("【A】" + dirInfoPair.a().getPath().getFileName());
        Path outputDir2 = workDir.resolve("【B】" + dirInfoPair.b().getPath().getFileName());
        
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
            Pair<DirInfo> dirInfoPair,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareDirsTask.030")).append(BR);
            updateMessage(str.toString());
            
            BookNamesMatcher matcher = factory.bookNamesMatcher(settings);
            List<Pair<String>> bookNamePairs = matcher.pairingBooks(
                    dirInfoPair.a(),
                    dirInfoPair.b());
            
            for (int i = 0; i < bookNamePairs.size(); i++) {
                Pair<String> bookNamePair = bookNamePairs.get(i);
                str.append(DirResult.formatBookNamesPair("", i, bookNamePair)).append(BR);
            }
            
            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return bookNamePairs;
            
        } catch (Exception e) {
            str.append(rb.getString("CompareDirsTask.040")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("CompareDirsTask.040"), e);
        }
    }
    
    // 5. フォルダ同士の比較
    private DirResult compareDirs(
            Pair<DirInfo> dirPair,
            Pair<Path> outputDirs,
            List<Pair<String>> bookNamePairs,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        str.append(rb.getString("CompareDirsTask.050")).append(BR);
        updateMessage(str.toString());
        updateProgress(progressBefore, PROGRESS_MAX);
        
        return compareDirs(
                "",
                "",
                new DirPairData(0, dirPair, bookNamePairs),
                outputDirs,
                progressBefore,
                progressAfter);
    }
}
