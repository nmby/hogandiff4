package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.CellsLoader;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.Result;
import xyz.hotchpotch.hogandiff.excel.SheetComparator;
import xyz.hotchpotch.hogandiff.excel.SheetResult;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * Excelブック同士の比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @author nmby
 */
/*package*/ final class CompareBooksTask extends AppTaskBase {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param settings 設定セット
     * @param factory ファクトリ
     */
    /*package*/ CompareBooksTask(
            Settings settings,
            Factory factory) {
        
        super(settings, factory);
    }
    
    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 0);
            
            // 2. 比較するシートの組み合わせの決定
            List<Pair<String>> sheetNamePairs = pairingSheets(0, 3);
            
            // 3. シート同士の比較
            BookResult bResult = compareSheets(sheetNamePairs, 3, 75);
            
            // 4. 比較結果の表示（テキスト）
            saveAndShowResultText(workDir, bResult.toString(), 75, 80);
            
            // 5. 比較結果の表示（Excelブック）
            paintSaveAndShowBook(workDir, bResult, 80, 98);
            
            // 6. 処理終了のアナウンス
            announceEnd();
            
            return bResult;
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareBooksTask::call2");
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
            
            Pair<Path> bookPathPair = SettingKeys.CURR_BOOK_INFOS.map(settings::get).map(BookInfo::bookPath);
            
            str.append("%s%n[A] %s%n[B] %s%n%n"
                    .formatted(rb.getString("CompareBooksTask.010"), bookPathPair.a(), bookPathPair.b()));
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareBooksTask::announceStart");
        }
    }
    
    // 2. 比較するシートの組み合わせの決定
    private List<Pair<String>> pairingSheets(
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            str.append(rb.getString("CompareBooksTask.020")).append(BR);
            updateMessage(str.toString());
            
            List<Pair<String>> sheetNamePairs = settings.get(SettingKeys.CURR_SHEETS_PAIRING);
            
            for (int i = 0; i < sheetNamePairs.size(); i++) {
                Pair<String> pair = sheetNamePairs.get(i);
                str.append(BookResult.formatSheetNamesPair(Integer.toString(i + 1), pair)).append(BR);
            }
            str.append(BR);
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return sheetNamePairs;
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareBooksTask.030", "");
        }
    }
    
    // 3. シート同士の比較
    private BookResult compareSheets(
            List<Pair<String>> sheetNamePairs,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareBooksTask.040")).append(BR);
            updateMessage(str.toString());
            
            Pair<Path> bookPathPair = SettingKeys.CURR_BOOK_INFOS.map(settings::get).map(BookInfo::bookPath);
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
            Pair<CellsLoader> loaderPair = bookPathPair.unsafeMap(
                    bookPath -> factory.cellsLoader(settings, bookPath, readPasswords.get(bookPath)));
            
            SheetComparator comparator = factory.comparator(settings);
            Map<Pair<String>, Optional<SheetResult>> results = new HashMap<>();
            
            for (int i = 0; i < sheetNamePairs.size(); i++) {
                Pair<String> sheetNamePair = sheetNamePairs.get(i);
                
                if (sheetNamePair.isPaired()) {
                    str.append(BookResult.formatSheetNamesPair(Integer.toString(i + 1), sheetNamePair));
                    updateMessage(str.toString());
                    
                    Pair<Set<CellData>> cellsSetPair = Side.unsafeMap(
                            side -> loaderPair.get(side).loadCells(
                                    bookPathPair.get(side),
                                    readPasswords.get(bookPathPair.get(side)),
                                    sheetNamePair.get(side)));
                    
                    SheetResult result = comparator.compare(cellsSetPair);
                    results.put(sheetNamePair, Optional.of(result));
                    
                    str.append("  -  ").append(result.getDiffSummary()).append(BR);
                    updateMessage(str.toString());
                    
                } else {
                    results.put(sheetNamePair, Optional.empty());
                }
                
                updateProgress(
                        progressBefore + (progressAfter - progressBefore) * (i + 1) / sheetNamePairs.size(),
                        PROGRESS_MAX);
            }
            
            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return new BookResult(
                    bookPathPair,
                    sheetNamePairs,
                    results);
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareBooksTask.050", "");
        }
    }
}
