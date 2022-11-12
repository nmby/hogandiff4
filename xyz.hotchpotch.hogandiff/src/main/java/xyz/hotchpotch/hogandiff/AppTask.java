package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.excel.BResult;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SComparator;
import xyz.hotchpotch.hogandiff.excel.SResult;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @author nmby
 */
/*package*/ class AppTask extends Task<Void> {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    private static final int PROGRESS_MAX = 100;
    
    // [instance members] ******************************************************
    
    private final Settings settings;
    private final Factory factory;
    private final AppMenu menu;
    private final StringBuilder str = new StringBuilder();
    private final ResourceBundle rb = AppMain.appResource.get();
    
    /*package*/ AppTask(
            Settings settings,
            Factory factory) {
        
        assert settings != null;
        assert factory != null;
        
        this.settings = settings;
        this.factory = factory;
        this.menu = settings.get(SettingKeys.CURR_MENU);
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
        
        if (menu == AppMenu.COMPARE_BOOKS) {
            str.append("%s%n[A] %s%n[B] %s%n%n"
                    .formatted(rb.getString("AppTask.010"), bookInfo1, bookInfo2));
            
        } else {
            String sheetName1 = settings.get(SettingKeys.CURR_SHEET_NAME1);
            String sheetName2 = settings.get(SettingKeys.CURR_SHEET_NAME2);
            
            if (Objects.equals(bookInfo1.bookPath(), bookInfo2.bookPath())) {
                str.append("%s%n%s%n[A] %s%n[B] %s%n%n"
                        .formatted(rb.getString("AppTask.020"), bookInfo1, sheetName1, sheetName2));
            } else {
                str.append("%s%n[A] %s - %s%n[B] %s - %s%n%n"
                        .formatted(rb.getString("AppTask.020"), bookInfo1, sheetName1, bookInfo2, sheetName2));
            }
        }
        updateMessage(str.toString());
        
        updateProgress(progressAfter, PROGRESS_MAX);
    }
    
    // 1. 作業用ディレクトリの作成
    private Path createWorkDir(int progressBefore, int progressAfter)
            throws ApplicationException {
        
        Path workDir = null;
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            workDir = settings.getOrDefault(SettingKeys.WORK_DIR_BASE)
                    .resolve(settings.getOrDefault(SettingKeys.CURR_TIMESTAMP));
            str.append("%s%n    - %s%n%n".formatted(rb.getString("AppTask.030"), workDir));
            updateMessage(str.toString());
            
            workDir = Files.createDirectories(workDir);
            
            updateProgress(progressAfter, PROGRESS_MAX);
            return workDir;
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.040")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s".formatted(rb.getString("AppTask.040"), workDir),
                    e);
        }
    }
    
    // 2. 比較するシートの組み合わせの決定
    private List<Pair<String>> pairingSheets(int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            List<Pair<String>> pairs;
            if (menu == AppMenu.COMPARE_BOOKS) {
                str.append(rb.getString("AppTask.050")).append(BR);
                updateMessage(str.toString());
                
                pairs = menu.getSheetNamePairs(settings, factory);
                for (int i = 0; i < pairs.size(); i++) {
                    Pair<String> pair = pairs.get(i);
                    str.append(BResult.formatSheetNamesPair(i, pair)).append(BR);
                }
                str.append(BR);
                
                updateMessage(str.toString());
                
            } else {
                pairs = menu.getSheetNamePairs(settings, factory);
            }
            
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
            
            BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
            BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
            SheetLoader loader1 = factory.sheetLoader(settings, bookInfo1);
            SheetLoader loader2 = Objects.equals(bookInfo1.bookPath(), bookInfo2.bookPath())
                    ? loader1
                    : factory.sheetLoader(settings, bookInfo2);
            SComparator comparator = factory.comparator(settings);
            Map<Pair<String>, Optional<SResult>> results = new HashMap<>();
            
            str.append(rb.getString("AppTask.070")).append(BR);
            updateMessage(str.toString());
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
    
    // 4. 比較結果の表示（テキスト）
    private void showResultText(
            Path workDir,
            BResult results,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        Path textPath = null;
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            textPath = workDir.resolve("result.txt");
            
            str.append("%s%n    - %s%n%n".formatted(rb.getString("AppTask.090"), textPath));
            updateMessage(str.toString());
            
            try (BufferedWriter writer = Files.newBufferedWriter(textPath)) {
                writer.write(results.toString());
            }
            if (settings.getOrDefault(SettingKeys.SHOW_RESULT_TEXT)) {
                str.append(rb.getString("AppTask.100")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(textPath.toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.110")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s".formatted(rb.getString("AppTask.110"), textPath),
                    e);
        }
    }
    
    // 5. 比較結果の表示（Excelブック）
    private void showPaintedSheets(
            Path workDir,
            BResult results,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        boolean isSameBook = Objects.equals(
                settings.get(SettingKeys.CURR_BOOK_INFO1).bookPath(),
                settings.get(SettingKeys.CURR_BOOK_INFO2).bookPath());
        
        if (isSameBook) {
            showPaintedSheets1(workDir, results, progressBefore, progressAfter);
        } else {
            showPaintedSheets2(workDir, results, progressBefore, progressAfter);
        }
    }
    
    private void showPaintedSheets1(
            Path workDir,
            BResult results,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        BookInfo dst = null;
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            int progressTotal = progressAfter - progressBefore;
            
            str.append(rb.getString("AppTask.120")).append(BR);
            updateMessage(str.toString());
            BookInfo src = settings.get(SettingKeys.CURR_BOOK_INFO1);
            dst = BookInfo.of(
                    workDir.resolve(src.bookPath().getFileName()),
                    src.getReadPassword());
            str.append("    - %s%n%n".formatted(dst));
            updateMessage(str.toString());
            
            Map<String, Optional<SResult.Piece>> result = new HashMap<>(results.getPiece(Side.A));
            result.putAll(results.getPiece(Side.B));
            BookPainter painter = factory.painter(settings, dst);
            painter.paintAndSave(src, dst, result);
            updateProgress(progressBefore + progressTotal * 4 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.130")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTask.130"), e);
        }
        
        try {
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("AppTask.140")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dst.bookPath().toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.150")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTask.150"), e);
        }
    }
    
    private void showPaintedSheets2(
            Path workDir,
            BResult results,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        int progressTotal = progressAfter - progressBefore;
        BookInfo dst1 = null;
        BookInfo dst2 = null;
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("AppTask.120")).append(BR);
            updateMessage(str.toString());
            
            BookInfo src1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
            dst1 = BookInfo.of(
                    workDir.resolve("【A】" + src1.bookPath().getFileName()),
                    src1.getReadPassword());
            str.append("    - %s%n".formatted(dst1));
            updateMessage(str.toString());
            BookPainter painter1 = factory.painter(settings, dst1);
            painter1.paintAndSave(src1, dst1, results.getPiece(Side.A));
            updateProgress(progressBefore + progressTotal * 2 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.160")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTask.160"), e);
        }
        
        try {
            BookInfo src2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
            dst2 = BookInfo.of(
                    workDir.resolve("【B】" + src2.bookPath().getFileName()),
                    src2.getReadPassword());
            str.append("    - %s%n%n".formatted(dst2));
            updateMessage(str.toString());
            BookPainter painter2 = factory.painter(settings, dst2);
            painter2.paintAndSave(src2, dst2, results.getPiece(Side.B));
            updateProgress(progressBefore + progressTotal * 4 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.170")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTask.170"), e);
        }
        
        try {
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("AppTask.140")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dst1.bookPath().toFile());
                Desktop.getDesktop().open(dst2.bookPath().toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTask.150")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTask.150"), e);
        }
    }
    
    // 6. 処理終了のアナウンス
    private void announceEnd() {
        str.append(rb.getString("AppTask.180"));
        updateMessage(str.toString());
        updateProgress(PROGRESS_MAX, PROGRESS_MAX);
    }
}
