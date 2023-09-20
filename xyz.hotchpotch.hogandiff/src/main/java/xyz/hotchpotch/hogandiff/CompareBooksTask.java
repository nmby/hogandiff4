package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SheetComparator;
import xyz.hotchpotch.hogandiff.excel.SheetResult;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
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
    protected Void call() throws Exception {
        
        // 0. 処理開始のアナウンス
        announceStart(0, 0);
        
        // 1. 作業用ディレクトリの作成
        Path workDir = createWorkDir(0, 2);
        
        // 2. 比較するシートの組み合わせの決定
        List<Pair<String>> pairs = pairingSheets(2, 5);
        
        // 3. シート同士の比較
        BookResult bResult = compareSheets(pairs, 5, 75);
        
        // 4. 比較結果の表示（テキスト）
        saveAndShowResultText(workDir, bResult.toString(), 75, 80);
        
        // 5. 比較結果の表示（Excelブック）
        paintSaveAndShowBook(workDir, bResult, 80, 98);
        
        // 6. 処理終了のアナウンス
        announceEnd();
        
        return null;
    }
    
    // 0. 処理開始のアナウンス
    private void announceStart(
            int progressBefore,
            int progressAfter) {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        
        BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
        BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
        
        str.append("%s%n[A] %s%n[B] %s%n%n"
                .formatted(rb.getString("CompareBooksTask.010"), bookInfo1, bookInfo2));
        
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
            
            BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
            BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
            
            List<Pair<String>> pairs = getSheetNamePairs(bookInfo1, bookInfo2);
            for (int i = 0; i < pairs.size(); i++) {
                Pair<String> pair = pairs.get(i);
                str.append(BookResult.formatSheetNamesPair(i, pair)).append(BR);
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
            List<Pair<String>> pairs,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareBooksTask.040")).append(BR);
            updateMessage(str.toString());
            
            BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
            BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
            SheetLoader loader1 = factory.sheetLoader(settings, bookInfo1);
            SheetLoader loader2 = isSameBook()
                    ? loader1
                    : factory.sheetLoader(settings, bookInfo2);
            
            SheetComparator comparator = factory.comparator(settings);
            Map<Pair<String>, Optional<SheetResult>> results = new HashMap<>();
            
            for (int i = 0; i < pairs.size(); i++) {
                Pair<String> pair = pairs.get(i);
                
                if (pair.isPaired()) {
                    str.append(BookResult.formatSheetNamesPair(i, pair));
                    updateMessage(str.toString());
                    
                    Set<CellData> cells1 = loader1.loadCells(bookInfo1, pair.a());
                    Set<CellData> cells2 = loader2.loadCells(bookInfo2, pair.b());
                    
                    SheetResult result = comparator.compare(cells1, cells2);
                    results.put(pair, Optional.of(result));
                    
                    str.append("  -  ").append(result.getDiffSummary()).append(BR);
                    updateMessage(str.toString());
                    
                } else {
                    results.put(pair, Optional.empty());
                }
                
                updateProgress(
                        progressBefore + (progressAfter - progressBefore) * (i + 1) / pairs.size(),
                        PROGRESS_MAX);
            }
            
            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return BookResult.of(
                    bookInfo1.bookPath(),
                    bookInfo2.bookPath(),
                    pairs,
                    results);
            
        } catch (Exception e) {
            str.append(rb.getString("CompareBooksTask.050")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("CompareBooksTask.050"), e);
        }
    }
}
