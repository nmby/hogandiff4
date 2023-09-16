package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import xyz.hotchpotch.hogandiff.excel.BResult;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SComparator;
import xyz.hotchpotch.hogandiff.excel.SResult;
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
        BResult results = compareSheets(pairs, 5, 75);
        
        // 4. 比較結果の表示（テキスト）
        showResultText(workDir, results, 75, 80);
        
        // 5. 比較結果の表示（Excelブック）
        showPaintedSheets(workDir, results, 80, 98);
        
        // 6. 処理終了のアナウンス
        announceEnd();
        return null;
    }
    
    // 0. 処理開始のアナウンス
    private void announceStart(int progressBefore, int progressAfter) {
        updateProgress(progressBefore, PROGRESS_MAX);
        
        BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
        BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
        
        str.append("%s%n[A] %s%n[B] %s%n%n"
                .formatted(rb.getString("AppTask.010"), bookInfo1, bookInfo2));
        
        updateMessage(str.toString());
        updateProgress(progressAfter, PROGRESS_MAX);
    }
    
    // 2. 比較するシートの組み合わせの決定
    private List<Pair<String>> pairingSheets(int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            str.append(rb.getString("AppTask.050")).append(BR);
            updateMessage(str.toString());
            
            List<Pair<String>> pairs = AppMenu.COMPARE_BOOKS.getSheetNamePairs(settings, factory);
            for (int i = 0; i < pairs.size(); i++) {
                Pair<String> pair = pairs.get(i);
                str.append(BResult.formatSheetNamesPair(i, pair)).append(BR);
            }
            str.append(BR);
            
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return pairs;
            
        } catch (Exception e) {
            // TODO: サポート対象外の .xlsb の場合の考慮が必要
            str.append(rb.getString("AppTask.060")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTask.060"), e);
        }
    }
    
    // 3. シート同士の比較
    private BResult compareSheets(
            List<Pair<String>> pairs,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("AppTask.070")).append(BR);
            updateMessage(str.toString());
            
            BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
            BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
            SheetLoader loader1 = factory.sheetLoader(settings, bookInfo1);
            SheetLoader loader2 = isSameBook()
                    ? loader1
                    : factory.sheetLoader(settings, bookInfo2);
            
            SComparator comparator = factory.comparator(settings);
            Map<Pair<String>, Optional<SResult>> results = new HashMap<>();
            
            int total = progressAfter - progressBefore;
            int numTotalPairs = (int) pairs.stream().filter(Pair::isPaired).count();
            int num = 0;
            
            for (int i = 0; i < pairs.size(); i++) {
                Pair<String> pair = pairs.get(i);
                if (!pair.isPaired()) {
                    continue;
                }
                str.append(BResult.formatSheetNamesPair(i, pair));
                updateMessage(str.toString());
                
                Set<CellData> cells1 = loader1.loadCells(bookInfo1, pair.a());
                Set<CellData> cells2 = loader2.loadCells(bookInfo2, pair.b());
                SResult result = comparator.compare(cells1, cells2);
                results.put(pair, Optional.of(result));
                
                str.append("  -  ").append(result.getDiffSummary()).append(BR);
                updateMessage(str.toString());
                
                num++;
                updateProgress(progressBefore + total * num / numTotalPairs, PROGRESS_MAX);
            }
            str.append(BR);
            
            List<Pair<String>> unpairedPairs = pairs.stream()
                    .filter(Predicate.not(Pair::isPaired))
                    .toList();
            for (Pair<String> pair : unpairedPairs) {
                results.put(pair, Optional.empty());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            return BResult.of(bookInfo1.bookPath(), bookInfo2.bookPath(), pairs, results);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.080")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTask.080"), e);
        }
    }
}
