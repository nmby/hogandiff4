package xyz.hotchpotch.hogandiff;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import xyz.hotchpotch.hogandiff.excel.BooksMatcher;
import xyz.hotchpotch.hogandiff.excel.DirCompareInfo;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher;
import xyz.hotchpotch.hogandiff.excel.Factory;
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
/*package*/ final class CompareTreesTask extends AppTaskBase {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param settings 設定セット
     * @param factory ファクトリ
     */
    /*package*/ CompareTreesTask(
            Settings settings,
            Factory factory) {
        
        super(settings, factory);
    }
    
    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 0);
            
            // 1. ディレクトリ情報の抽出
            Pair<DirInfo> topDirPair = SettingKeys.CURR_DIR_INFOS.map(settings::get);
            
            // 3. 比較するフォルダとExcelブック名の組み合わせの決定
            List<DirCompareInfo> dirCompareInfos = pairingDirsAndBookNames(topDirPair, 2, 5);
            
            // 4. フォルダツリー同士の比較
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
            TreeResult tResult = compareTrees(workDir, topDirPair, dirCompareInfos, readPasswords, 5, 93);
            
            // 5. 比較結果テキストの作成と表示
            saveAndShowResultText(workDir, tResult.toString(), 93, 95);
            
            // 6. 比較結果Excelの作成と表示
            createSaveAndShowResultBook(workDir, tResult, 95, 99);
            
            // 7. 処理終了のアナウンス
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
            
            Pair<Path> dirPathPair = SettingKeys.CURR_DIR_INFOS.map(settings::get).map(DirInfo::dirPath);
            
            str.append("%s%n[A] %s%n[B] %s%n%n".formatted(
                    rb.getString("CompareTreesTask.010"),
                    dirPathPair.a(),
                    dirPathPair.b()));
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareTreesTask::announceStart");
        }
    }
    
    // 3. 比較するフォルダとExcelブック名の組み合わせの決定
    private List<DirCompareInfo> pairingDirsAndBookNames(
            Pair<DirInfo> topDirInfoPair,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareTreesTask.020")).append(BR);
            updateMessage(str.toString());
            
            DirsMatcher dirMatcher = factory.dirsMatcher(settings);
            List<Pair<DirInfo>> dirInfoPairs = dirMatcher.pairingDirs(topDirInfoPair);
            
            BooksMatcher bookNamesMatcher = factory.bookNamesMatcher(settings);
            BiFunction<Side, Pair<DirInfo>, List<Pair<String>>> onesideBookNamePairs = (side, dirPair) -> dirPair
                    .get(side).bookNames().stream()
                    .map(bookName -> side == Side.A ? new Pair<>(bookName, null) : new Pair<>(null, bookName))
                    .toList();
            
            List<DirCompareInfo> dirCompareInfos = new ArrayList<>();
            
            for (int i = 0; i < dirInfoPairs.size(); i++) {
                Pair<DirInfo> dirInfoPair = dirInfoPairs.get(i);
                
                str.append(TreeResult.formatDirsPair(Integer.toString(i + 1), dirInfoPair)).append(BR);
                updateMessage(str.toString());
                
                DirCompareInfo dirCompareInfo = new DirCompareInfo(
                        dirInfoPair,
                        dirInfoPair.isPaired()
                                ? bookNamesMatcher.pairingBooks(dirInfoPair).bookNamePairs()
                                : dirInfoPair.isOnlyA()
                                        ? onesideBookNamePairs.apply(Side.A, dirInfoPair)
                                        : onesideBookNamePairs.apply(Side.B, dirInfoPair),
                        Map.of());
                dirCompareInfos.add(dirCompareInfo);
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return dirCompareInfos;
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareTreesTask.030", "");
        }
    }
    
    // 4. フォルダツリー同士の比較
    private TreeResult compareTrees(
            Path workDir,
            Pair<DirInfo> topDirInfoPair,
            List<DirCompareInfo> dirCompareInfos,
            Map<Path, String> readPasswords,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareTreesTask.040")).append(BR);
            updateMessage(str.toString());
            
            Map<Pair<Path>, Optional<DirResult>> dirResults = new HashMap<>();
            Pair<Map<Path, Path>> outputDirsPair = new Pair<>(new HashMap<>(), new HashMap<>());
            
            int dirPairsCount = (int) dirCompareInfos.stream()
                    .filter(data -> data.dirInfoPair().isPaired())
                    .count();
            int num = 0;
            
            for (int i = 0; i < dirCompareInfos.size(); i++) {
                int ii = i;
                
                DirCompareInfo dirCompareInfo = dirCompareInfos.get(i);
                
                str.append(TreeResult.formatDirsPair(Integer.toString(i + 1), dirCompareInfo.dirInfoPair()));
                updateMessage(str.toString());
                
                Pair<Path> outputDirPair = null;
                
                try {
                    outputDirPair = Side.unsafeMap(side -> {
                        // 出力先ディレクトリの作成
                        if (dirCompareInfo.dirInfoPair().has(side)) {
                            DirInfo targetDirInfo = dirCompareInfo.dirInfoPair().get(side);
                            Path parentDir = targetDirInfo.equals(topDirInfoPair.get(side))
                                    ? workDir
                                    : outputDirsPair.get(side).get(targetDirInfo.parent().dirPath());
                            
                            Path outputDir = parentDir
                                    .resolve("【%s%d】%s".formatted(side, ii + 1, targetDirInfo.dirPath().getFileName()));
                            Files.createDirectories(outputDir);
                            outputDirsPair.get(side).put(targetDirInfo.dirPath(), outputDir);
                            return outputDir;
                        } else {
                            return null;
                        }
                    });
                    
                } catch (IOException e) {
                    dirResults.putIfAbsent(dirCompareInfo.dirInfoPair().map(DirInfo::dirPath), Optional.empty());
                    str.append("  -  ").append(rb.getString("CompareTreesTask.050")).append(BR);
                    updateMessage(str.toString());
                    e.printStackTrace();
                    continue;
                }
                
                if (dirCompareInfo.dirInfoPair().isPaired()) {
                    DirResult dirResult = compareDirs(
                            String.valueOf(i + 1),
                            "      ",
                            dirCompareInfo,
                            readPasswords,
                            outputDirPair,
                            progressBefore + (progressAfter - progressBefore) * num / dirPairsCount,
                            progressBefore + (progressAfter - progressBefore) * (num + 1) / dirPairsCount);
                    dirResults.put(dirCompareInfo.dirInfoPair().map(DirInfo::dirPath), Optional.of(dirResult));
                    num++;
                    
                } else {
                    dirResults.put(dirCompareInfo.dirInfoPair().map(DirInfo::dirPath), Optional.empty());
                    str.append(BR);
                    updateMessage(str.toString());
                }
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return new TreeResult(
                    topDirInfoPair,
                    dirCompareInfos,
                    dirResults);
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareTreesTask.110", "");
        }
    }
}
