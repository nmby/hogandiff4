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
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher.DirPairData;
import xyz.hotchpotch.hogandiff.excel.Factory;
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
/*package*/ class CompareTreesTask extends AppTaskBase {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /*package*/ CompareTreesTask(
            Settings settings,
            Factory factory) {
        
        super(settings, factory);
    }
    
    @Override
    protected void call2() throws Exception {
        
        // 0. 処理開始のアナウンス
        announceStart(0, 0);
        
        // 1. ディレクトリ情報の抽出
        Pair<DirInfo> topDirPair = extractDirs();
        
        // 3. 比較するフォルダとExcelブック名の組み合わせの決定
        List<DirPairData> pairs = pairingDirsAndBookNames(topDirPair, 2, 5);
        
        // 4. フォルダツリー同士の比較
        TreeResult tResult = compareTrees(workDir, topDirPair, pairs, 5, 93);
        
        // 5. 比較結果テキストの作成と表示
        saveAndShowResultText(workDir, tResult.toString(), 93, 95);
        
        // 6. 比較結果Excelの作成と表示
        createSaveAndShowResultBook(workDir, tResult, 95, 99);
        
        // 7. 処理終了のアナウンス
        announceEnd();
    }
    
    // 0. 処理開始のアナウンス
    private void announceStart(
            int progressBefore,
            int progressAfter) {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        
        Path dirPath1 = settings.get(SettingKeys.CURR_DIR_PATH1);
        Path dirPath2 = settings.get(SettingKeys.CURR_DIR_PATH2);
        
        str.append("%s%n[A] %s%n[B] %s%n%n".formatted(
                rb.getString("CompareTreesTask.010"),
                dirPath1,
                dirPath2));
        
        updateMessage(str.toString());
        updateProgress(progressAfter, PROGRESS_MAX);
    }
    
    // 3. 比較するフォルダとExcelブック名の組み合わせの決定
    private List<DirPairData> pairingDirsAndBookNames(
            Pair<DirInfo> topDirPair,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareTreesTask.020")).append(BR);
            updateMessage(str.toString());
            
            DirsMatcher dirMatcher = factory.dirsMatcher(settings);
            List<Pair<DirInfo>> dirPairs = dirMatcher.pairingDirs(
                    topDirPair.a(),
                    topDirPair.b());
            
            BooksMatcher bookNamesMatcher = factory.bookNamesMatcher(settings);
            BiFunction<Side, Pair<DirInfo>, List<Pair<String>>> bookNamePairs = (side, dirPair) -> dirPair
                    .get(side).bookNames().stream()
                    .map(bookName -> side == Side.A ? new Pair<>(bookName, null) : new Pair<>(null, bookName))
                    .toList();
            
            List<DirPairData> pairDataList = new ArrayList<>();
            
            for (int i = 0; i < dirPairs.size(); i++) {
                Pair<DirInfo> dirPair = dirPairs.get(i);
                
                str.append(TreeResult.formatDirsPair(Integer.toString(i + 1), dirPair)).append(BR);
                updateMessage(str.toString());
                
                DirPairData data = new DirPairData(
                        Integer.toString(i + 1),
                        dirPair,
                        dirPair.isPaired()
                                ? bookNamesMatcher.pairingBooks(dirPair.a(), dirPair.b())
                                : dirPair.isOnlyA()
                                        ? bookNamePairs.apply(Side.A, dirPair)
                                        : bookNamePairs.apply(Side.B, dirPair));
                pairDataList.add(data);
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return pairDataList;
            
        } catch (Exception e) {
            str.append(rb.getString("CompareTreesTask.030")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("CompareTreesTask.030"), e);
        }
    }
    
    // 4. フォルダツリー同士の比較
    private TreeResult compareTrees(
            Path workDir,
            Pair<DirInfo> topDirPair,
            List<DirPairData> pairDataList,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        str.append(rb.getString("CompareTreesTask.040")).append(BR);
        updateMessage(str.toString());
        
        Map<Pair<Path>, Optional<DirResult>> dirResults = new HashMap<>();
        Map<Path, Path> outputDirs1 = new HashMap<>();
        Map<Path, Path> outputDirs2 = new HashMap<>();
        
        int dirPairsCount = (int) pairDataList.stream()
                .filter(data -> data.dirPair().isPaired())
                .count();
        int num = 0;
        
        for (int i = 0; i < pairDataList.size(); i++) {
            DirPairData data = pairDataList.get(i);
            
            str.append(TreeResult.formatDirsPair(Integer.toString(i + 1), data.dirPair()));
            updateMessage(str.toString());
            
            Path outputDir1 = null;
            Path outputDir2 = null;
            
            try {
                // 出力先ディレクトリの作成
                if (data.dirPair().hasA()) {
                    DirInfo targetDir1 = data.dirPair().a();
                    Path parentDir = targetDir1.equals(topDirPair.a())
                            ? workDir
                            : outputDirs1.get(targetDir1.parent().path());
                    outputDir1 = parentDir.resolve("【A%d】%s".formatted(i + 1, targetDir1.path().getFileName()));
                    Files.createDirectories(outputDir1);
                    outputDirs1.put(targetDir1.path(), outputDir1);
                }
                if (data.dirPair().hasB()) {
                    DirInfo targetDir2 = data.dirPair().b();
                    Path parentDir = targetDir2.equals(topDirPair.b())
                            ? workDir
                            : outputDirs2.get(targetDir2.parent().path());
                    outputDir2 = parentDir.resolve("【B%d】%s".formatted(i + 1, targetDir2.path().getFileName()));
                    Files.createDirectories(outputDir2);
                    outputDirs2.put(targetDir2.path(), outputDir2);
                }
            } catch (IOException e) {
                dirResults.putIfAbsent(data.dirPair().map(DirInfo::path), Optional.empty());
                str.append("  -  ").append(rb.getString("CompareTreesTask.050")).append(BR);
                updateMessage(str.toString());
                e.printStackTrace();
                continue;
            }
            
            if (data.dirPair().isPaired()) {
                DirResult dirResult = compareDirs(
                        String.valueOf(i + 1),
                        "      ",
                        data,
                        new Pair<>(outputDir1, outputDir2),
                        progressBefore + (progressAfter - progressBefore) * num / dirPairsCount,
                        progressBefore + (progressAfter - progressBefore) * (num + 1) / dirPairsCount);
                dirResults.put(data.dirPair().map(DirInfo::path), Optional.of(dirResult));
                num++;
                
            } else {
                dirResults.put(data.dirPair().map(DirInfo::path), Optional.empty());
                str.append(BR);
                updateMessage(str.toString());
            }
        }
        
        updateProgress(progressAfter, PROGRESS_MAX);
        
        return new TreeResult(
                topDirPair,
                pairDataList,
                dirResults);
    }
}
