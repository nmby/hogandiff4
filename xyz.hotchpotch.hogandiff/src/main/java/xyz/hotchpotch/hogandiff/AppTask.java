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
import java.util.Set;
import java.util.function.Predicate;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.excel.BResult;
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
        
        Path bookPath1 = settings.get(SettingKeys.CURR_BOOK_PATH1);
        Path bookPath2 = settings.get(SettingKeys.CURR_BOOK_PATH2);
        
        if (menu == AppMenu.COMPARE_BOOKS) {
            str.append("ブック同士の比較を開始します。%n[A] %s%n[B] %s%n%n"
                    .formatted(bookPath1, bookPath2));
            
        } else {
            String sheetName1 = settings.get(SettingKeys.CURR_SHEET_NAME1);
            String sheetName2 = settings.get(SettingKeys.CURR_SHEET_NAME2);
            
            if (bookPath1.equals(bookPath2)) {
                str.append("シート同士の比較を開始します。%n%s%n[A] %s%n[B] %s%n%n"
                        .formatted(bookPath1, sheetName1, sheetName2));
            } else {
                str.append("シート同士の比較を開始します。%n[A] %s - %s%n[B] %s - %s%n%n"
                        .formatted(bookPath1, sheetName1, bookPath2, sheetName2));
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
            workDir = settings.get(SettingKeys.WORK_DIR_BASE)
                    .resolve(settings.get(SettingKeys.CURR_TIMESTAMP));
            str.append("作業用フォルダを作成しています...%n    - %s%n%n".formatted(workDir));
            updateMessage(str.toString());
            
            workDir = Files.createDirectories(workDir);
            
            updateProgress(progressAfter, PROGRESS_MAX);
            return workDir;
            
        } catch (Exception e) {
            str.append("作業用フォルダの作成に失敗しました。").append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "作業用フォルダの作成に失敗しました。%n%s".formatted(workDir),
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
                str.append("比較するシートの組み合わせを決定しています...").append(BR);
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
            // TODO: サポート対象外の .xlsb やパスワード付きファイルの場合の考慮が必要
            str.append("シートの組み合わせ決定に失敗しました。").append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "シートの組み合わせ決定に失敗しました。", e);
        }
    }
    
    // 3. シート同士の比較
    private BResult compareSheets(
            List<Pair<String>> pairs,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            Path bookPath1 = settings.get(SettingKeys.CURR_BOOK_PATH1);
            Path bookPath2 = settings.get(SettingKeys.CURR_BOOK_PATH2);
            SheetLoader loader1 = factory.sheetLoader(settings, bookPath1);
            SheetLoader loader2 = bookPath1.equals(bookPath2)
                    ? loader1
                    : factory.sheetLoader(settings, bookPath2);
            SComparator comparator = factory.comparator(settings);
            Map<Pair<String>, Optional<SResult>> results = new HashMap<>();
            
            str.append("シートを比較しています...").append(BR);
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
                
                Set<CellData> cells1 = loader1.loadCells(bookPath1, pair.a());
                Set<CellData> cells2 = loader2.loadCells(bookPath2, pair.b());
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
            return BResult.of(bookPath1, bookPath2, pairs, results);
            
        } catch (Exception e) {
            str.append("シートの比較に失敗しました。").append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException("シートの比較に失敗しました。", e);
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
            
            str.append("比較結果テキストを保存しています...%n    - %s%n%n".formatted(textPath));
            updateMessage(str.toString());
            
            try (BufferedWriter writer = Files.newBufferedWriter(textPath)) {
                writer.write(results.toString());
            }
            if (settings.get(SettingKeys.SHOW_RESULT_TEXT)) {
                str.append("比較結果テキストを表示しています...").append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(textPath.toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append("比較結果テキストの保存と表示に失敗しました。").append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "比較結果テキストの保存と表示に失敗しました。%n%s".formatted(textPath),
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
                settings.get(SettingKeys.CURR_BOOK_PATH1),
                settings.get(SettingKeys.CURR_BOOK_PATH2));
        
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
        
        Path dst = null;
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            int progressTotal = progressAfter - progressBefore;
            
            str.append("Excelブックに比較結果の色を付けて保存しています...").append(BR);
            updateMessage(str.toString());
            Path src = settings.get(SettingKeys.CURR_BOOK_PATH1);
            dst = workDir.resolve(src.getFileName());
            str.append("    - %s%n%n".formatted(dst));
            updateMessage(str.toString());
            
            Map<String, Optional<SResult.Piece>> result = new HashMap<>(results.getPiece(Side.A));
            result.putAll(results.getPiece(Side.B));
            BookPainter painter = factory.painter(settings, dst);
            painter.paintAndSave(src, dst, result);
            updateProgress(progressBefore + progressTotal * 4 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append("Excelブックの着色・保存に失敗しました。").append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "Excelブックの着色・保存に失敗しました。", e);
        }
        
        try {
            if (settings.get(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append("比較結果のExcelブックを表示しています...").append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dst.toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append("Excelブックの表示に失敗しました。").append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "Excelブックの表示に失敗しました。", e);
        }
    }
    
    private void showPaintedSheets2(
            Path workDir,
            BResult results,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        int progressTotal = progressAfter - progressBefore;
        Path dst1 = null;
        Path dst2 = null;
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append("Excelブックに比較結果の色を付けて保存しています...").append(BR);
            updateMessage(str.toString());
            
            Path src1 = settings.get(SettingKeys.CURR_BOOK_PATH1);
            dst1 = workDir.resolve("【A】" + src1.getFileName());
            str.append("    - %s%n".formatted(dst1));
            updateMessage(str.toString());
            BookPainter painter1 = factory.painter(settings, dst1);
            painter1.paintAndSave(src1, dst1, results.getPiece(Side.A));
            updateProgress(progressBefore + progressTotal * 2 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append("ExcelブックAの着色・保存に失敗しました。").append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "ExcelブックAの着色・保存に失敗しました。", e);
        }
        
        try {
            Path src2 = settings.get(SettingKeys.CURR_BOOK_PATH2);
            dst2 = workDir.resolve("【B】" + src2.getFileName());
            str.append("    - %s%n%n".formatted(dst2));
            updateMessage(str.toString());
            BookPainter painter2 = factory.painter(settings, dst2);
            painter2.paintAndSave(src2, dst2, results.getPiece(Side.B));
            updateProgress(progressBefore + progressTotal * 4 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append("ExcelブックBの着色・保存に失敗しました。").append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "ExcelブックBの着色・保存に失敗しました。", e);
        }
        
        try {
            if (settings.get(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append("比較結果のExcelブックを表示しています...").append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dst1.toFile());
                Desktop.getDesktop().open(dst2.toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append("Excelブックの表示に失敗しました。").append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "Excelブックの表示に失敗しました。", e);
        }
    }
    
    // 6. 処理終了のアナウンス
    private void announceEnd() {
        str.append("処理が完了しました。");
        updateMessage(str.toString());
        updateProgress(PROGRESS_MAX, PROGRESS_MAX);
    }
}
