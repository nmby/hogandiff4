package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
 * Excelシート同士の比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @author nmby
 */
/*package*/ final class CompareSheetsTask extends AppTaskBase {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param settings 設定セット
     * @param factory ファクトリ
     */
    /*package*/ CompareSheetsTask(
            Settings settings,
            Factory factory) {
        
        super(settings, factory);
    }
    
    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 0);
            
            // 2. シート同士の比較
            BookResult bResult = compareSheets(0, 75);
            
            // 3. 比較結果の表示（テキスト）
            saveAndShowResultText(workDir, bResult.toString(), 75, 80);
            
            // 4. 比較結果の表示（Excelブック）
            paintSaveAndShowBook(workDir, bResult, 80, 98);
            
            // 5. 処理終了のアナウンス
            announceEnd();
            
            return bResult;
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareSheetsTask::call2");
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
            
            Pair<Path> bookPaths = SettingKeys.CURR_BOOK_PATHS.map(settings::get);
            Pair<String> sheetNames = SettingKeys.CURR_SHEET_NAMES.map(settings::get);
            
            str.append(rb.getString("CompareSheetsTask.010")).append(BR);
            str.append(isSameBook()
                    ? "%s%n[A] %s%n[B] %s%n%n".formatted(
                            bookPaths.a(), sheetNames.a(), sheetNames.b())
                    : "[A] %s - %s%n[B] %s - %s%n%n".formatted(
                            bookPaths.a(), sheetNames.a(), bookPaths.b(), sheetNames.b()));
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareSheetsTask::announceStart");
        }
    }
    
    // 2. シート同士の比較
    private BookResult compareSheets(
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareSheetsTask.020")).append(BR);
            updateMessage(str.toString());
            
            Pair<Path> bookPaths = SettingKeys.CURR_BOOK_PATHS.map(settings::get);
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
            Pair<CellsLoader> loaders = bookPaths.unsafeMap(
                    bookPath -> factory.cellsLoader(settings, bookPath, readPasswords.get(bookPath)));
            Pair<String> sheetNames = SettingKeys.CURR_SHEET_NAMES.map(settings::get);
            
            str.append(BookResult.formatSheetNamesPair("1", sheetNames));
            updateMessage(str.toString());
            
            Pair<Set<CellData>> cellsSets = Side.unsafeMap(side -> loaders.get(side).loadCells(
                    bookPaths.get(side),
                    readPasswords.get(bookPaths.get(side)),
                    sheetNames.get(side)));
            
            SheetComparator comparator = factory.comparator(settings);
            SheetResult result = comparator.compare(cellsSets);
            
            str.append("  -  ").append(result.getDiffSummary()).append(BR).append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return new BookResult(
                    bookPaths,
                    List.of(sheetNames),
                    Map.of(sheetNames, Optional.of(result)));
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareSheetsTask.030", "");
        }
    }
}
