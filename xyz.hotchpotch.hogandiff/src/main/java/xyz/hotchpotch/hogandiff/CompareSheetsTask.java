package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.CellsLoader;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SheetComparator;
import xyz.hotchpotch.hogandiff.excel.SheetResult;
import xyz.hotchpotch.hogandiff.util.Pair;
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
/*package*/ class CompareSheetsTask extends AppTaskBase {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /*package*/ CompareSheetsTask(
            Settings settings,
            Factory factory) {
        
        super(settings, factory);
    }
    
    @Override
    protected Void call() throws Exception {
        
        // 0. 処理開始のアナウンス
        announceStart(0, 0);
        
        // 1. 作業用ディレクトリの作成
        Path workDir = createWorkDir(0, 2);
        
        // 2. シート同士の比較
        BookResult bResult = compareSheets(5, 75);
        
        // 3. 比較結果の表示（テキスト）
        saveAndShowResultText(workDir, bResult.toString(), 75, 80);
        
        // 4. 比較結果の表示（Excelブック）
        paintSaveAndShowBook(workDir, bResult, 80, 98);
        
        // 5. 処理終了のアナウンス
        announceEnd();
        
        return null;
    }
    
    // 0. 処理開始のアナウンス
    private void announceStart(
            int progressBefore,
            int progressAfter) {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        
        BookOpenInfo bookOpenInfo1 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1);
        BookOpenInfo bookOpenInfo2 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2);
        String sheetName1 = settings.get(SettingKeys.CURR_SHEET_NAME1);
        String sheetName2 = settings.get(SettingKeys.CURR_SHEET_NAME2);
        
        str.append(rb.getString("CompareSheetsTask.010")).append(BR);
        str.append(isSameBook()
                ? "%s%n[A] %s%n[B] %s%n%n".formatted(bookOpenInfo1, sheetName1, sheetName2)
                : "[A] %s - %s%n[B] %s - %s%n%n".formatted(bookOpenInfo1, sheetName1, bookOpenInfo2, sheetName2));
        
        updateMessage(str.toString());
        updateProgress(progressAfter, PROGRESS_MAX);
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
            
            BookOpenInfo bookOpenInfo1 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1);
            BookOpenInfo bookOpenInfo2 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2);
            CellsLoader loader1 = factory.cellsLoader(settings, bookOpenInfo1);
            CellsLoader loader2 = isSameBook()
                    ? loader1
                    : factory.cellsLoader(settings, bookOpenInfo2);
            
            Pair<String> pair = new Pair<>(
                    settings.get(SettingKeys.CURR_SHEET_NAME1),
                    settings.get(SettingKeys.CURR_SHEET_NAME2));
            
            str.append(BookResult.formatSheetNamesPair(0, pair));
            updateMessage(str.toString());
            
            Set<CellData> cells1 = loader1.loadCells(bookOpenInfo1, pair.a());
            Set<CellData> cells2 = loader2.loadCells(bookOpenInfo2, pair.b());
            
            SheetComparator comparator = factory.comparator(settings);
            SheetResult result = comparator.compare(cells1, cells2);
            
            str.append("  -  ").append(result.getDiffSummary()).append(BR).append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return new BookResult(
                    new Pair<>(
                            bookOpenInfo1.bookPath(),
                            bookOpenInfo2.bookPath()),
                    List.of(pair),
                    Map.of(pair, Optional.of(result)));
            
        } catch (Exception e) {
            str.append(rb.getString("CompareSheetsTask.030")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("CompareSheetsTask.030"), e);
        }
    }
}
