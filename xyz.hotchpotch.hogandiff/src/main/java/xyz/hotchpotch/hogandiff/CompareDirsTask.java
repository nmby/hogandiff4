package xyz.hotchpotch.hogandiff;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.excel.DirCompareInfo;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.Result;
import xyz.hotchpotch.hogandiff.excel.TreeCompareInfo;
import xyz.hotchpotch.hogandiff.excel.TreeResult;
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
/*package*/ final class CompareDirsTask extends AppTaskBase {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param settings 設定セット
     */
    /*package*/ CompareDirsTask(Settings settings) {
        super(settings);
    }
    
    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 5);
            
            // 3. 出力用ディレクトリの作成
            Pair<Path> outputDirPair = createOutputDirs(workDir);
            
            // 5. フォルダ同士の比較
            DirResult dResult = compareDirs(outputDirPair, 5, 93);
            
            // 6. 比較結果テキストの作成と表示
            saveAndShowResultText(workDir, dResult.toString(), 93, 95);
            
            // 7. 比較結果Excelの作成と表示
            DirCompareInfo dirCompareInfo = settings.get(SettingKeys.CURR_DIR_COMPARE_INFO);
            Pair<DirInfo> dirInfoPair = dirCompareInfo.dirInfoPair();
            TreeResult tResult = new TreeResult(
                    TreeCompareInfo.ofSingle(
                            dirInfoPair,
                            Factory.bookNamesMatcher2(settings),
                            Factory.sheetNamesMatcher2(settings),
                            settings.get(SettingKeys.CURR_READ_PASSWORDS)),
                    Map.of(dirInfoPair.map(DirInfo::dirPath), Optional.of(dResult)));
            
            createSaveAndShowResultBook(workDir, tResult, 95, 99);
            
            // 8. 処理終了のアナウンス
            announceEnd();
            
            return tResult;
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareDirsTask::call2");
        }
    }
    
    //■ タスクステップ ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
    
    // 0. 処理開始のアナウンス
    private void announceStart(
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            DirCompareInfo dirCompareInfo = settings.get(SettingKeys.CURR_DIR_COMPARE_INFO);
            Pair<Path> dirPathPair = dirCompareInfo.dirInfoPair().map(DirInfo::dirPath);
            List<Pair<String>> bookNamePairs = dirCompareInfo.bookNamePairs();
            
            str.append("%s%n[A] %s%n[B] %s%n".formatted(
                    rb.getString("CompareDirsTask.010"),
                    dirPathPair.a(),
                    dirPathPair.b()));
            
            if (bookNamePairs.size() == 0) {
                str.append("    - ").append(rb.getString("CompareDirsTask.070")).append(BR);
            }
            for (int i = 0; i < bookNamePairs.size(); i++) {
                Pair<String> bookNamePair = bookNamePairs.get(i);
                str.append(DirResult.formatBookNamesPair("", Integer.toString(i + 1), bookNamePair)).append(BR);
            }
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareDirsTask::announceStart");
        }
    }
    
    // 3. 出力用ディレクトリの作成
    private Pair<Path> createOutputDirs(Path workDir)
            throws ApplicationException {
        
        DirCompareInfo dirCompareInfo = settings.get(SettingKeys.CURR_DIR_COMPARE_INFO);
        Pair<DirInfo> dirInfoPair = dirCompareInfo.dirInfoPair();
        Pair<Path> outputDirPair = null;
        
        try {
            outputDirPair = Side.map(
                    side -> workDir.resolve("【%s】%s".formatted(side, dirInfoPair.get(side).dirPath().getFileName())));
            
            return outputDirPair.unsafeMap(Files::createDirectory);
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareDirsTask.020", "");
        }
    }
    
    // 5. フォルダ同士の比較
    private DirResult compareDirs(
            Pair<Path> outputDirPair,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            DirCompareInfo dirCompareInfo = settings.get(SettingKeys.CURR_DIR_COMPARE_INFO);
            
            if (0 < dirCompareInfo.bookNamePairs().size()) {
                str.append(BR).append(rb.getString("CompareDirsTask.050")).append(BR);
                updateMessage(str.toString());
                return compareDirs(
                        "",
                        "",
                        dirCompareInfo,
                        outputDirPair,
                        progressBefore,
                        progressAfter);
                
            } else {
                return new DirResult(
                        dirCompareInfo,
                        dirCompareInfo.bookNamePairs().stream().collect(Collectors.toMap(
                                Function.identity(),
                                name -> Optional.empty())),
                        "");
            }
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareDirsTask.080", "");
        }
    }
}
