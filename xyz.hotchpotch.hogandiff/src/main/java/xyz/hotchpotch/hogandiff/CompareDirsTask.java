package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.excel.BookNamesMatcher;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.Factory;
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
                str.append(DirResult.formatBookNamesPair(String.valueOf(i + 1), bookNamePair)).append(BR);
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
            Pair<DirInfo> dirInfoPair,
            Pair<Path> outputDir,
            List<Pair<String>> bookNamePairs,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        str.append(rb.getString("CompareDirsTask.050")).append(BR);
        updateMessage(str.toString());
        
        Map<Pair<String>, Optional<BookResult>> results = new HashMap<>();
        
        for (int i = 0; i < bookNamePairs.size(); i++) {
            Pair<String> bookNamePair = bookNamePairs.get(i);
            
            try {
                str.append(DirResult.formatBookNamesPair(String.valueOf(i + 1), bookNamePair));
                updateMessage(str.toString());
                
                if (bookNamePair.isPaired()) {
                    BookOpenInfo srcInfo1 = new BookOpenInfo(
                            dirInfoPair.a().getPath().resolve(bookNamePair.a()), null);
                    BookOpenInfo srcInfo2 = new BookOpenInfo(
                            dirInfoPair.b().getPath().resolve(bookNamePair.b()), null);
                    BookOpenInfo dstInfo1 = new BookOpenInfo(
                            outputDir.a().resolve("【A-%d】%s".formatted(i + 1, bookNamePair.a())), null);
                    BookOpenInfo dstInfo2 = new BookOpenInfo(
                            outputDir.b().resolve("【B-%d】%s".formatted(i + 1, bookNamePair.b())), null);
                    
                    BookResult result = compareBooks(
                            srcInfo1,
                            srcInfo2,
                            progressBefore + (progressAfter - progressBefore) * i / bookNamePairs.size(),
                            progressBefore + (progressAfter - progressBefore) * (i + 1) / bookNamePairs.size());
                    results.put(bookNamePair, Optional.of(result));
                    
                    BookPainter painter1 = factory.painter(settings, srcInfo1);
                    BookPainter painter2 = factory.painter(settings, srcInfo2);
                    painter1.paintAndSave(srcInfo1, dstInfo1, result.getPiece(Side.A));
                    painter2.paintAndSave(srcInfo2, dstInfo2, result.getPiece(Side.B));
                    
                    str.append("  -  ").append(result.getDiffSimpleSummary()).append(BR);
                    updateMessage(str.toString());
                    
                    updateProgress(
                            progressBefore + (progressAfter - progressBefore) * (i + 1) / bookNamePairs.size(),
                            PROGRESS_MAX);
                    
                } else {
                    Path src = bookNamePair.hasA()
                            ? dirInfoPair.a().getPath().resolve(bookNamePair.a())
                            : dirInfoPair.b().getPath().resolve(bookNamePair.b());
                    Path dst = bookNamePair.hasA()
                            ? outputDir.a().resolve("【A-%d】%s".formatted(i + 1, bookNamePair.a()))
                            : outputDir.b().resolve("【B-%d】%s".formatted(i + 1, bookNamePair.b()));
                    
                    Files.copy(src, dst);
                    dst.toFile().setReadable(true, false);
                    dst.toFile().setWritable(true, false);
                    
                    results.put(bookNamePair, Optional.empty());
                    
                    str.append(BR);
                    updateMessage(str.toString());
                }
                
            } catch (Exception e) {
                results.putIfAbsent(bookNamePair, Optional.empty());
                str.append("  -  ").append(rb.getString("CompareDirsTask.060")).append(BR);
                updateMessage(str.toString());
                e.printStackTrace();
            }
        }
        str.append(BR);
        updateMessage(str.toString());
        updateProgress(progressAfter, PROGRESS_MAX);
        
        return DirResult.of(
                dirInfoPair.a(),
                dirInfoPair.b(),
                bookNamePairs,
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
