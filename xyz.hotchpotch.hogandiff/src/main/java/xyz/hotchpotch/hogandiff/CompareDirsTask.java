package xyz.hotchpotch.hogandiff;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.excel.BooksMatcher;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher.DirPairData;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.Result;
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
     * @param factory ファクトリ
     */
    /*package*/ CompareDirsTask(
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
            Pair<DirInfo> dirPair = extractDirs();
            
            // 3. 出力用ディレクトリの作成
            Pair<Path> outputDirPair = createOutputDirs(workDir, dirPair);
            
            // 4. 比較するExcelブックの組み合わせの決定
            List<Pair<String>> bookNamePairs = pairingBookNames(dirPair, 2, 5);
            
            // 5. フォルダ同士の比較
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
            DirResult dResult = compareDirs(dirPair, outputDirPair, bookNamePairs, readPasswords, 5, 93);
            
            // 6. 比較結果テキストの作成と表示
            saveAndShowResultText(workDir, dResult.toString(), 93, 95);
            
            // 7. 比較結果Excelの作成と表示
            TreeResult tResult = new TreeResult(
                    dirPair,
                    List.of(new DirPairData("", dirPair, bookNamePairs)),
                    Map.of(dirPair.map(DirInfo::path), Optional.of(dResult)));
            
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
            
            Pair<Path> dirPathPair = SettingKeys.CURR_DIR_PATHS.map(settings::get);
            
            str.append("%s%n[A] %s%n[B] %s%n%n".formatted(
                    rb.getString("CompareDirsTask.010"),
                    dirPathPair.a(),
                    dirPathPair.b()));
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareDirsTask::announceStart");
        }
    }
    
    // 3. 出力用ディレクトリの作成
    private Pair<Path> createOutputDirs(
            Path workDir,
            Pair<DirInfo> dirPair)
            throws ApplicationException {
        
        Pair<Path> outputDirPair = null;
        try {
            outputDirPair = Side.map(
                    side -> workDir.resolve("【%s】%s".formatted(side, dirPair.get(side).path().getFileName())));
            
            return outputDirPair.unsafeMap(Files::createDirectory);
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareDirsTask.020", "");
        }
    }
    
    // 4. 比較するExcelブック名の組み合わせの決定
    private List<Pair<String>> pairingBookNames(
            Pair<DirInfo> dirPair,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareDirsTask.030")).append(BR);
            updateMessage(str.toString());
            
            BooksMatcher matcher = factory.bookNamesMatcher(settings);
            List<Pair<String>> bookNamePairs = matcher.pairingBooks(dirPair);
            
            if (bookNamePairs.size() == 0) {
                str.append("    - ").append(rb.getString("CompareDirsTask.070")).append(BR);
            }
            for (int i = 0; i < bookNamePairs.size(); i++) {
                Pair<String> bookNamePair = bookNamePairs.get(i);
                str.append(DirResult.formatBookNamesPair("", Integer.toString(i + 1), bookNamePair)).append(BR);
            }
            
            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return bookNamePairs;
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareDirsTask.040", "");
        }
    }
    
    // 5. フォルダ同士の比較
    private DirResult compareDirs(
            Pair<DirInfo> dirPair,
            Pair<Path> outputDirs,
            List<Pair<String>> bookNamePairs,
            Map<Path, String> readPasswords,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            if (0 < bookNamePairs.size()) {
                str.append(rb.getString("CompareDirsTask.050")).append(BR);
                updateMessage(str.toString());
                return compareDirs(
                        "",
                        "",
                        new DirPairData("", dirPair, bookNamePairs),
                        readPasswords,
                        outputDirs,
                        progressBefore,
                        progressAfter);
                
            } else {
                return new DirResult(
                        dirPair,
                        bookNamePairs,
                        bookNamePairs.stream().collect(Collectors.toMap(
                                Function.identity(),
                                name -> Optional.empty())),
                        "");
            }
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareDirsTask.080", "");
        }
    }
}
