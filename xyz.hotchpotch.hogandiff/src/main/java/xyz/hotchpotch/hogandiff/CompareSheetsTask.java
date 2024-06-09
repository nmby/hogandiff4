package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookCompareInfo;
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
     */
    /*package*/ CompareSheetsTask(Settings settings) {
        super(settings);
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
            
            Pair<Path> bookPathPair = SettingKeys.CURR_BOOK_INFOS.map(settings::get).map(BookInfo::bookPath);
            Pair<String> sheetNamePair = SettingKeys.CURR_SHEET_NAMES.map(settings::get);
            
            str.append(rb.getString("CompareSheetsTask.010")).append(BR);
            str.append(isSameBook()
                    ? "%s%n[A] %s%n[B] %s%n%n".formatted(
                            bookPathPair.a(), sheetNamePair.a(), sheetNamePair.b())
                    : "[A] %s - %s%n[B] %s - %s%n%n".formatted(
                            bookPathPair.a(), sheetNamePair.a(), bookPathPair.b(), sheetNamePair.b()));
            
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
            
            Pair<BookInfo> bookInfoPair = SettingKeys.CURR_BOOK_INFOS.map(settings::get);
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
            Pair<CellsLoader> loaderPair = bookInfoPair.map(BookInfo::bookPath).unsafeMap(
                    bookPath -> Factory.cellsLoader(settings, bookPath, readPasswords.get(bookPath)));
            Pair<String> sheetNamePair = SettingKeys.CURR_SHEET_NAMES.map(settings::get);
            
            str.append(BookResult.formatSheetNamesPair("1", sheetNamePair));
            updateMessage(str.toString());
            
            Pair<Set<CellData>> cellsSetPair = Side.unsafeMap(
                    side -> loaderPair.get(side).loadCells(
                            bookInfoPair.get(side).bookPath(),
                            readPasswords.get(bookInfoPair.get(side).bookPath()),
                            sheetNamePair.get(side)));
            
            SheetComparator comparator = Factory.sheetComparator(settings);
            SheetResult result = comparator.compare(cellsSetPair);
            
            str.append("  -  ").append(result.getDiffSummary()).append(BR).append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return new BookResult(
                    BookCompareInfo.ofSingle(bookInfoPair, sheetNamePair),
                    Map.of(sheetNamePair, Optional.of(result)));
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareSheetsTask.030", "");
        }
    }
}
