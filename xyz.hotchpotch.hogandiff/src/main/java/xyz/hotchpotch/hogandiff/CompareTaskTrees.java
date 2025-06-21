package xyz.hotchpotch.hogandiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.excel.DirComparison;
import xyz.hotchpotch.hogandiff.excel.DirComparison.FlattenDirComparison;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.Result;
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
/*package*/ final class CompareTaskTrees extends CompareTask {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param settings 設定セット
     */
    /*package*/ CompareTaskTrees(Settings settings) {
        super(settings);
    }
    
    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 5);
            
            // 1. フォルダツリー同士の比較
            TreeResult tResult = compareTrees(workDir, 5, 93);
            
            Exception failed = null;
            
            // 2. 比較結果レポート（Excelブック）の保存と表示
            try {
                createSaveAndShowResultBook(workDir, tResult, 93, 97);
            } catch (Exception e) {
                failed = e;
            }
            
            // 3. 比較結果レポート（テキスト）の保存
            try {
                saveResultText(workDir, tResult.toString(), 97, 99);
            } catch (Exception e) {
                if (failed == null) {
                    failed = e;
                } else {
                    failed.addSuppressed(e);
                }
            }
            
            // 4. 処理終了のアナウンス
            announceEnd();
            
            if (failed != null) {
                throw failed;
            }
            
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
            
            FlattenDirComparison flattenDirComparison = settings.get(SettingKeys.CURR_TREE_COMPARE_INFO)
                    .flatten();
            
            str.append("%s%n[A] %s%n[B] %s%n".formatted(
                    rb.getString("CompareTreesTask.010"),
                    flattenDirComparison.parentDirInfoPair().a().dirPath(),
                    flattenDirComparison.parentDirInfoPair().b().dirPath()));
            
            for (int i = 0; i < flattenDirComparison.dirInfoPairs().size(); i++) {
                Pair<DirInfo> dirInfoPair = flattenDirComparison.dirInfoPairs().get(i);
                str.append(TreeResult.formatDirInfoPair(Integer.toString(i + 1), dirInfoPair)).append(BR);
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
            
            FlattenDirComparison flattenDirComparison = settings.get(SettingKeys.CURR_TREE_COMPARE_INFO)
                    .flatten();
            
            Map<Pair<DirInfo>, Optional<DirResult>> dirResults = new HashMap<>();
            Pair<Map<Path, Path>> outputDirsPair = new Pair<>(new HashMap<>(), new HashMap<>());
            
            double progressDelta = (progressAfter - progressBefore)
                    / (double) flattenDirComparison.dirInfoPairs().size();
            
            for (int i = 0; i < flattenDirComparison.dirInfoPairs().size(); i++) {
                int ii = i;
                
                Pair<DirInfo> dirInfoPair = flattenDirComparison.dirInfoPairs().get(i);
                DirComparison dirComparison = flattenDirComparison.dirComparisons().get(dirInfoPair).get();
                
                str.append(
                        TreeResult.formatDirInfoPair(Integer.toString(i + 1), dirComparison.parentDirInfoPair()));
                updateMessage(str.toString());
                
                Pair<Path> outputDirPair = null;
                
                try {
                    outputDirPair = Side.unsafeMap(side -> {
                        // 出力先ディレクトリの作成
                        if (dirInfoPair.has(side)) {
                            Path targetDirPath = dirInfoPair.get(side).dirPath();
                            Path parentDir = targetDirPath
                                    .equals(flattenDirComparison.parentDirInfoPair().get(side).dirPath())
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
                    // FIXME: [No.X 内部実装改善] この辺の見通しが非常に悪いので改善する
                    DirResult dirResult = compareDirs(
                            String.valueOf(i + 1),
                            "      ",
                            dirComparison,
                            outputDirPair,
                            progressBefore + (int) progressDelta * i,
                            progressBefore + (int) progressDelta * (i + 1));
                    dirResults.put(dirInfoPair, Optional.of(dirResult));
                    
                } else {
                    // FIXME: [No.11 機能改善] 片フォルダの場合も内部のファイルをコピーする
                    dirResults.put(dirInfoPair, Optional.empty());
                    str.append(BR);
                    updateMessage(str.toString());
                    updateProgress(progressBefore + progressDelta * (i + 1), PROGRESS_MAX);
                }
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return new TreeResult(flattenDirComparison, dirResults);
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareTreesTask.110", "");
        }
    }
}
