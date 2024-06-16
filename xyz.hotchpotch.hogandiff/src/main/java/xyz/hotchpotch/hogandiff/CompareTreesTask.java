package xyz.hotchpotch.hogandiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.excel.DirCompareInfo;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.Result;
import xyz.hotchpotch.hogandiff.excel.TreeCompareInfo;
import xyz.hotchpotch.hogandiff.excel.TreeResult;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * フォルダツリー同士の比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @author nmby
 */
/*package*/ final class CompareTreesTask extends AppTaskBase {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param settings 設定セット
     */
    /*package*/ CompareTreesTask(Settings settings) {
        super(settings);
    }
    
    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 5);
            
            // 1. フォルダツリー同士の比較
            TreeResult tResult = compareTrees(workDir, 5, 93);
            
            // 2. 比較結果テキストの作成と表示
            saveAndShowResultText(workDir, tResult.toString(), 93, 95);
            
            // 3. 比較結果Excelの作成と表示
            createSaveAndShowResultBook(workDir, tResult, 95, 99);
            
            // 4. 処理終了のアナウンス
            announceEnd();
            
            return tResult;
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareTreesTask::call2");
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
            
            TreeCompareInfo treeCompareInfo = settings.get(SettingKeys.CURR_TREE_COMPARE_INFO);
            
            str.append("%s%n[A] %s%n[B] %s%n".formatted(
                    rb.getString("CompareTreesTask.010"),
                    treeCompareInfo.parentPair().a().dirPath(),
                    treeCompareInfo.parentPair().b().dirPath()));
            
            for (int i = 0; i < treeCompareInfo.childPairs().size(); i++) {
                Pair<DirInfo> dirInfoPair = treeCompareInfo.childPairs().get(i);
                str.append(TreeResult.formatDirsInfoPair(Integer.toString(i + 1), dirInfoPair)).append(BR);
                updateMessage(str.toString());
            }
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareTreesTask::announceStart");
        }
    }
    
    // 1. フォルダツリー同士の比較
    private TreeResult compareTrees(
            Path workDir,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareTreesTask.040")).append(BR);
            updateMessage(str.toString());
            
            TreeCompareInfo treeCompareInfo = settings.get(SettingKeys.CURR_TREE_COMPARE_INFO);
            
            Map<Pair<DirInfo>, Optional<DirResult>> dirResults = new HashMap<>();
            Pair<Map<Path, Path>> outputDirsPair = new Pair<>(new HashMap<>(), new HashMap<>());
            
            double progressDelta = (progressAfter - progressBefore) / (double) treeCompareInfo.childPairs().size();
            
            for (int i = 0; i < treeCompareInfo.childPairs().size(); i++) {
                int ii = i;
                
                Pair<DirInfo> dirInfoPair = treeCompareInfo.childPairs().get(i);
                DirCompareInfo dirCompareInfo = treeCompareInfo.childCompareInfos().get(dirInfoPair).get();
                
                str.append(TreeResult.formatDirsInfoPair(Integer.toString(i + 1), dirCompareInfo.parentPair()));
                updateMessage(str.toString());
                
                Pair<Path> outputDirPair = null;
                
                try {
                    outputDirPair = Side.unsafeMap(side -> {
                        // 出力先ディレクトリの作成
                        if (dirInfoPair.has(side)) {
                            Path targetDirPath = dirInfoPair.get(side).dirPath();
                            Path parentDir = targetDirPath.equals(treeCompareInfo.parentPair().get(side).dirPath())
                                    ? workDir
                                    : outputDirsPair.get(side).get(targetDirPath.getParent());
                            
                            Path outputDir = parentDir
                                    .resolve("【%s%d】%s".formatted(side, ii + 1, targetDirPath.getFileName()));
                            Files.createDirectories(outputDir);
                            outputDirsPair.get(side).put(targetDirPath, outputDir);
                            return outputDir;
                        } else {
                            return null;
                        }
                    });
                    
                } catch (IOException e) {
                    dirResults.putIfAbsent(dirInfoPair, Optional.empty());
                    str.append("  -  ").append(rb.getString("CompareTreesTask.050")).append(BR);
                    updateMessage(str.toString());
                    e.printStackTrace();
                    continue;
                }
                
                if (dirInfoPair.isPaired()) {
                    DirResult dirResult = compareDirs(
                            String.valueOf(i + 1),
                            "      ",
                            dirCompareInfo,
                            outputDirPair,
                            progressBefore + (int) progressDelta * i,
                            progressBefore + (int) progressDelta * (i + 1));
                    dirResults.put(dirInfoPair, Optional.of(dirResult));
                    
                } else {
                    // FIXME: 片フォルダの場合も内部のファイルをコピーする
                    dirResults.put(dirInfoPair, Optional.empty());
                    str.append(BR);
                    updateMessage(str.toString());
                    updateProgress(progressBefore + progressDelta * (i + 1), PROGRESS_MAX);
                }
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return new TreeResult(treeCompareInfo, dirResults);
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareTreesTask.110", "");
        }
    }
}
