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

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.BResult;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SResult;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較タスクの基底クラスです。<br>
 * 
 * @author nmby
 */
/*package*/ abstract class AppTaskBase extends Task<Void> {
    
    // [static members] ********************************************************
    
    protected static final String BR = System.lineSeparator();
    protected static final int PROGRESS_MAX = 100;
    
    // [instance members] ******************************************************
    
    protected final Settings settings;
    protected final Factory factory;
    protected final StringBuilder str = new StringBuilder();
    protected final ResourceBundle rb = AppMain.appResource.get();
    
    /*package*/ AppTaskBase(
            Settings settings,
            Factory factory) {
        
        assert settings != null;
        assert factory != null;
        
        this.settings = settings;
        this.factory = factory;
    }
    
    protected boolean isSameBook() {
        return Objects.equals(
                settings.get(SettingKeys.CURR_BOOK_INFO1).bookPath(),
                settings.get(SettingKeys.CURR_BOOK_INFO2).bookPath());
    }
    
    protected List<Pair<String>> getSheetNamePairs(BookInfo bookInfo1, BookInfo bookInfo2)
            throws ExcelHandlingException {
        
        BookLoader bookLoader1 = factory.bookLoader(bookInfo1);
        BookLoader bookLoader2 = factory.bookLoader(bookInfo2);
        List<String> sheetNames1 = bookLoader1.loadSheetNames(bookInfo1);
        List<String> sheetNames2 = bookLoader2.loadSheetNames(bookInfo2);
        
        Matcher<String> matcher = factory.sheetNameMatcher(settings);
        List<IntPair> pairs = matcher.makePairs(sheetNames1, sheetNames2);
        
        return pairs.stream()
                .map(p -> Pair.ofNullable(
                        p.hasA() ? sheetNames1.get(p.a()) : null,
                        p.hasB() ? sheetNames2.get(p.b()) : null))
                .toList();
    }
    
    // 作業用ディレクトリの作成
    protected Path createWorkDir(int progressBefore, int progressAfter)
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
    
    // 比較結果の表示（テキスト）
    protected void saveAndShowResultText(
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
    
    // 比較結果の表示（Excelブック）
    protected void saveAndShowPaintedSheets(
            Path workDir,
            BResult results,
            int progressBefore, int progressAfter)
            throws ApplicationException {
        
        if (isSameBook()) {
            saveAndShowPaintedSheets1(workDir, results, progressBefore, progressAfter);
        } else {
            saveAndShowPaintedSheets2(workDir, results, progressBefore, progressAfter);
        }
    }
    
    private void saveAndShowPaintedSheets1(
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
    
    private void saveAndShowPaintedSheets2(
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
    
    // 処理終了のアナウンス
    protected void announceEnd() {
        str.append(rb.getString("AppTask.180"));
        updateMessage(str.toString());
        updateProgress(PROGRESS_MAX, PROGRESS_MAX);
    }
}
