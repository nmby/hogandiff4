package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import xyz.hotchpotch.hogandiff.excel.BookNamesMatcher;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher;
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
    
    private static record DirPairData(
            int num,
            Pair<DirInfo> dirPair,
            List<Pair<String>> bookNamePairs) {
    }
    
    // [instance members] ******************************************************
    
    /*package*/ CompareTreesTask(
            Settings settings,
            Factory factory) {
        
        super(settings, factory);
    }
    
    @Override
    protected Void call() throws Exception {
        
        // 0. 処理開始のアナウンス
        announceStart(0, 0);
        
        // 1. ディレクトリ情報の抽出
        Pair<DirInfo> topDirPair = extractDirInfoPair();
        
        // 2. 作業用ディレクトリの作成
        Path workDir = createWorkDir(0, 2);
        
        // 3. 比較するフォルダとExcelブック名の組み合わせの決定
        List<DirPairData> pairs = pairingDirsAndBookNames(topDirPair, 2, 5);
        
        // 4. フォルダツリー同士の比較
        TreeResult tResult = compareTrees(workDir, topDirPair, pairs, 5, 95);
        
        // 5. 比較結果の表示（テキスト）
        saveAndShowResultText(workDir, tResult.toString(), 95, 97);
        
        // 6. 比較結果の表示（出力フォルダ）
        showOutputDirs(workDir, 97, 99);
        
        // 7. 処理終了のアナウンス
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
                rb.getString("CompareTreesTask.010"),
                dirPath1,
                dirPath2));
        
        updateMessage(str.toString());
        updateProgress(progressAfter, PROGRESS_MAX);
    }
    
    // 3. 比較するフォルダとExcelブック名の組み合わせの決定
    private List<DirPairData> pairingDirsAndBookNames(
            Pair<DirInfo> dirInfoPair,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareTreesTask.020")).append(BR);
            updateMessage(str.toString());
            
            DirsMatcher dirMatcher = factory.dirsMatcher(settings);
            List<Pair<DirInfo>> dirPairs = dirMatcher.pairingDirs(
                    dirInfoPair.a(),
                    dirInfoPair.b());
            
            BookNamesMatcher bookNamesMatcher = factory.bookNamesMatcher(settings);
            BiFunction<Side, Pair<DirInfo>, List<Pair<String>>> bookNamePairs = (side, dirPair) -> dirPair
                    .get(side).getBookNames().stream()
                    .map(bookName -> Pair.ofOnly(side, bookName))
                    .toList();
            
            List<DirPairData> dirPairDataList = new ArrayList<>();
            
            for (int i = 0; i < dirPairs.size(); i++) {
                Pair<DirInfo> dirPair = dirPairs.get(i);
                
                String id = String.valueOf(i + 1);
                str.append(TreeResult.formatDirsPair(id, dirPair));
                
                DirPairData data = new DirPairData(
                        i + 1,
                        dirPair,
                        dirPair.isPaired()
                                ? bookNamesMatcher.pairingBooks(dirPair.a(), dirPair.b())
                                : dirPair.isOnlyA()
                                        ? bookNamePairs.apply(Side.A, dirPair)
                                        : bookNamePairs.apply(Side.B, dirPair));
                dirPairDataList.add(data);
                
                for (int j = 0; j < data.bookNamePairs().size(); j++) {
                    Pair<String> bookNamePair = data.bookNamePairs().get(j);
                    
                    str.append(DirResult.formatBookNamesPair(
                            "%s-%d".formatted(id, j + 1),
                            bookNamePair));
                    str.append(BR);
                }
                str.append(BR);
            }
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return dirPairDataList;
            
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
        
        Map<Pair<DirInfo>, Optional<DirResult>> dirResults = new HashMap<>();
        Map<DirInfo, Path> outputDirs1 = new HashMap<>();
        Map<DirInfo, Path> outputDirs2 = new HashMap<>();
        
        int bookPairsCount = (int) pairDataList.stream()
                .filter(data -> data.dirPair().isPaired())
                .flatMap(data -> data.bookNamePairs().stream())
                .filter(Pair::isPaired)
                .count();
        int num = 0;
        
        for (int i = 0; i < pairDataList.size(); i++) {
            DirPairData data = pairDataList.get(i);
            
            str.append(TreeResult.formatDirsPair(String.valueOf(i + 1), data.dirPair));
            updateMessage(str.toString());
            
            Path outputDir1 = null;
            Path outputDir2 = null;
            
            try {
                // 出力先ディレクトリの作成
                if (data.dirPair.hasA()) {
                    DirInfo targetDir1 = data.dirPair.a();
                    Path parentDir = targetDir1.equals(topDirPair.a())
                            ? workDir
                            : outputDirs1.get(targetDir1.getParent());
                    outputDir1 = parentDir.resolve("【A%d】%s".formatted(i + 1, targetDir1.getPath().getFileName()));
                    Files.createDirectories(outputDir1);
                    outputDirs1.put(targetDir1, outputDir1);
                }
                if (data.dirPair.hasB()) {
                    DirInfo targetDir2 = data.dirPair.b();
                    Path parentDir = targetDir2.equals(topDirPair.b())
                            ? workDir
                            : outputDirs2.get(targetDir2.getParent());
                    outputDir2 = parentDir.resolve("【B%d】%s".formatted(i + 1, targetDir2.getPath().getFileName()));
                    Files.createDirectories(outputDir2);
                    outputDirs2.put(targetDir2, outputDir2);
                }
            } catch (IOException e) {
                dirResults.putIfAbsent(data.dirPair, Optional.empty());
                str.append("  -  ").append(rb.getString("CompareTreesTask.050")).append(BR);
                updateMessage(str.toString());
                e.printStackTrace();
                continue;
            }
            
            Map<Pair<String>, Optional<BookResult>> bookResults = new HashMap<>();
            
            for (int j = 0; j < data.bookNamePairs().size(); j++) {
                Pair<String> bookNamePair = data.bookNamePairs().get(j);
                
                str.append(DirResult.formatBookNamesPair(
                        "%d-%d".formatted(i + 1, j + 1),
                        bookNamePair));
                updateMessage(str.toString());
                
                try {
                    if (bookNamePair.isPaired()) {
                        BookOpenInfo srcInfo1 = new BookOpenInfo(
                                data.dirPair.a().getPath().resolve(bookNamePair.a()), null);
                        BookOpenInfo srcInfo2 = new BookOpenInfo(
                                data.dirPair.b().getPath().resolve(bookNamePair.b()), null);
                        BookOpenInfo dstInfo1 = new BookOpenInfo(
                                outputDir1.resolve("【A%d-%d】%s".formatted(i + 1, j + 1, bookNamePair.a())), null);
                        BookOpenInfo dstInfo2 = new BookOpenInfo(
                                outputDir2.resolve("【B%d-%d】%s".formatted(i + 1, j + 1, bookNamePair.b())), null);
                        
                        BookResult bookResult = compareBooks(
                                srcInfo1,
                                srcInfo2,
                                progressBefore + (progressAfter - progressBefore) * num / bookPairsCount,
                                progressBefore + (progressAfter - progressBefore) * (num + 1) / bookPairsCount);
                        bookResults.put(bookNamePair, Optional.of(bookResult));
                        
                        BookPainter painter1 = factory.painter(settings, srcInfo1);
                        BookPainter painter2 = factory.painter(settings, srcInfo2);
                        painter1.paintAndSave(srcInfo1, dstInfo1, bookResult.getPiece(Side.A));
                        painter2.paintAndSave(srcInfo2, dstInfo2, bookResult.getPiece(Side.B));
                        
                        str.append("  -  ").append(bookResult.getDiffSimpleSummary()).append(BR);
                        updateMessage(str.toString());
                        
                        num++;
                        updateProgress(
                                progressBefore + (progressAfter - progressBefore) * num / bookPairsCount,
                                PROGRESS_MAX);
                    } else {
                        if (!bookNamePair.isPaired()) {
                            Path src = bookNamePair.hasA()
                                    ? data.dirPair.a().getPath().resolve(bookNamePair.a())
                                    : data.dirPair.b().getPath().resolve(bookNamePair.b());
                            Path dst = bookNamePair.hasA()
                                    ? outputDir1.resolve("【A%d-%d】%s".formatted(i + 1, j + 1, bookNamePair.a()))
                                    : outputDir2.resolve("【B%d-%d】%s".formatted(i + 1, j + 1, bookNamePair.b()));
                            
                            Files.copy(src, dst);
                            dst.toFile().setReadable(true, false);
                            dst.toFile().setWritable(true, false);
                            
                            bookResults.put(bookNamePair, Optional.empty());
                            
                            str.append(BR);
                            updateMessage(str.toString());
                        }
                    }
                } catch (Exception e) {
                    bookResults.putIfAbsent(bookNamePair, Optional.empty());
                    str.append("  -  ").append(rb.getString("CompareTreesTask.050")).append(BR);
                    updateMessage(str.toString());
                    e.printStackTrace();
                    continue;
                }
            }
            
            dirResults.put(
                    data.dirPair,
                    data.dirPair.isPaired()
                            ? Optional.of(DirResult.of(
                                    data.dirPair.a(),
                                    data.dirPair.b(),
                                    data.bookNamePairs,
                                    bookResults))
                            : Optional.empty());
            
            str.append(BR);
            updateMessage(str.toString());
        }
        
        updateProgress(progressAfter, PROGRESS_MAX);
        
        return TreeResult.of(
                topDirPair.a(),
                topDirPair.b(),
                pairDataList.stream().map(DirPairData::dirPair).toList(),
                dirResults);
    }
    
    // 6. 比較結果の表示（出力フォルダ）
    private void showOutputDirs(
            Path workDir,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("CompareDirsTask.070")).append(BR);
                
                List<Path> outputDirs = Files.list(workDir)
                        .filter(f -> Files.isDirectory(f, LinkOption.NOFOLLOW_LINKS))
                        .sorted()
                        .toList();
                
                Desktop.getDesktop().open(outputDirs.get(0).toFile());
                str.append("    - %s%n".formatted(outputDirs.get(0)));
                
                Desktop.getDesktop().open(outputDirs.get(1).toFile());
                str.append("    - %s%n%n".formatted(outputDirs.get(1)));
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
