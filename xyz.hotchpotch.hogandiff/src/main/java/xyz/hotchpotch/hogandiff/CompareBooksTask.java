package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.HashMap;
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
     */
    /*package*/ CompareBooksTask(Settings settings) {
        super(settings);
    }
    
    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 3);
            
            // 1. シート同士の比較
            BookResult bResult = compareSheets(3, 75);
            
            // 2. 比較結果の表示（テキスト）
            saveAndShowResultText(workDir, bResult.toString(), 75, 80);
            
            // 3. 比較結果の表示（Excelブック）
            paintSaveAndShowBook(workDir, bResult, 80, 98);
            
            // 4. 処理終了のアナウンス
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
            
            BookCompareInfo bookCompareInfo = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO);
            Pair<Path> bookPathPair = bookCompareInfo.bookInfoPair().map(BookInfo::bookPath);
            
            str.append("%s%n[A] %s%n[B] %s%n"
                    .formatted(rb.getString("CompareBooksTask.010"), bookPathPair.a(), bookPathPair.b()));
            
            for (int i = 0; i < bookCompareInfo.sheetNamePairs().size(); i++) {
                Pair<String> sheetNamePair = bookCompareInfo.sheetNamePairs().get(i);
                str.append(BookResult.formatSheetNamesPair(Integer.toString(i + 1), sheetNamePair)).append(BR);
            }
            
            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareBooksTask::announceStart");
        }
    }
    
    // 1. シート同士の比較
    private BookResult compareSheets(
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareBooksTask.040")).append(BR);
            updateMessage(str.toString());
            
            BookCompareInfo bookCompareInfo = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO);
            Pair<BookInfo> bookInfoPair = bookCompareInfo.bookInfoPair();
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
            Pair<CellsLoader> loaderPair = bookInfoPair
                    .map(BookInfo::bookPath)
                    .unsafeMap(bookPath -> Factory.cellsLoader(settings, bookPath, readPasswords.get(bookPath)));
            
            SheetComparator sheetComparator = Factory.sheetComparator(settings);
            Map<Pair<String>, Optional<SheetResult>> results = new HashMap<>();
            
            double progressDelta = (progressAfter - progressBefore) / (double) bookCompareInfo.sheetNamePairs().size();
            
            for (int i = 0; i < bookCompareInfo.sheetNamePairs().size(); i++) {
                Pair<String> sheetNamePair = bookCompareInfo.sheetNamePairs().get(i);
                SheetResult result = null;
                
                try {
                    if (sheetNamePair.isPaired()) {
                        str.append(BookResult.formatSheetNamesPair(Integer.toString(i + 1), sheetNamePair));
                        updateMessage(str.toString());
                        
                        Pair<Set<CellData>> cellsSetPair = Side.unsafeMap(
                                side -> loaderPair.get(side).loadCells(
                                        bookInfoPair.get(side).bookPath(),
                                        readPasswords.get(bookInfoPair.get(side).bookPath()),
                                        sheetNamePair.get(side)));
                        
                        result = sheetComparator.compare(cellsSetPair);
                        
                        str.append("  -  ").append(result.getDiffSummary()).append(BR);
                        updateMessage(str.toString());
                    }
                } catch (Exception e) {
                    str.append("  -  ").append(rb.getString("AppTaskBase.150")).append(BR);
                }
                
                results.put(sheetNamePair, Optional.ofNullable(result));
                updateProgress(progressBefore + progressDelta * (i + 1), PROGRESS_MAX);
            }
            
            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return new BookResult(bookCompareInfo, results);
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareBooksTask.050", "");
        }
    }
}
