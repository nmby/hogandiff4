package xyz.hotchpotch.hogandiff;

import java.util.HashMap;
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
 * Excelブック同士の比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @author nmby
 */
/*package*/ class CompareBooksTask extends AppTaskBase {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /*package*/ CompareBooksTask(
            Settings settings,
            Factory factory) {
        
        super(settings, factory);
    }
    
    @Override
    protected void call2() throws Exception {
        
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
    }
    
    // 0. 処理開始のアナウンス
    private void announceStart(
            int progressBefore,
            int progressAfter) {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        
        BookOpenInfo bookOpenInfo1 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1);
        BookOpenInfo bookOpenInfo2 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2);
        
        str.append("%s%n[A] %s%n[B] %s%n%n"
                .formatted(rb.getString("CompareBooksTask.010"), bookOpenInfo1, bookOpenInfo2));
        
        updateMessage(str.toString());
        updateProgress(progressAfter, PROGRESS_MAX);
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
            
            BookOpenInfo bookOpenInfo1 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1);
            BookOpenInfo bookOpenInfo2 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2);
            
            List<Pair<String>> pairs = getSheetNamePairs(bookOpenInfo1, bookOpenInfo2);
            for (int i = 0; i < pairs.size(); i++) {
                Pair<String> pair = pairs.get(i);
                str.append(BookResult.formatSheetNamesPair(Integer.toString(i + 1), pair)).append(BR);
            }
            str.append(BR);
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return pairs;
            
        } catch (Exception e) {
            str.append(rb.getString("CompareBooksTask.030")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("CompareBooksTask.030"), e);
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
            
            BookOpenInfo bookOpenInfo1 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1);
            BookOpenInfo bookOpenInfo2 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2);
            CellsLoader loader1 = factory.cellsLoader(settings, bookOpenInfo1);
            CellsLoader loader2 = isSameBook()
                    ? loader1
                    : factory.cellsLoader(settings, bookOpenInfo2);
            
            SheetComparator comparator = factory.comparator(settings);
            Map<Pair<String>, Optional<SheetResult>> results = new HashMap<>();
            
            for (int i = 0; i < sheetNamePairs.size(); i++) {
                Pair<String> sheetNamePair = sheetNamePairs.get(i);
                
                if (sheetNamePair.isPaired()) {
                    str.append(BookResult.formatSheetNamesPair(Integer.toString(i + 1), sheetNamePair));
                    updateMessage(str.toString());
                    
                    Set<CellData> cells1 = loader1.loadCells(bookOpenInfo1, sheetNamePair.a());
                    Set<CellData> cells2 = loader2.loadCells(bookOpenInfo2, sheetNamePair.b());
                    
                    SheetResult result = comparator.compare(cells1, cells2);
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
                    new Pair<>(
                            bookOpenInfo1.bookPath(),
                            bookOpenInfo2.bookPath()),
                    sheetNamePairs,
                    results);
            
        } catch (Exception e) {
            str.append(rb.getString("CompareBooksTask.050")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("CompareBooksTask.050"), e);
        }
    }
}
