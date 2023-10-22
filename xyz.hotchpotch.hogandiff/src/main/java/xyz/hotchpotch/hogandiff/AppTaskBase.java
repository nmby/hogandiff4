package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.CellsLoader;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirLoader;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.DirsMatcher.DirPairData;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SheetComparator;
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.excel.SheetNamesMatcher;
import xyz.hotchpotch.hogandiff.excel.SheetResult;
import xyz.hotchpotch.hogandiff.excel.TreeResult;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.TreeResultBookCreator;
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
    
    /** 今回の実行における各種設定を保持する設定セット */
    protected final Settings settings;
    
    /** 各種インスタンスのファクトリ */
    protected final Factory factory;
    
    /** 今回の実行における作業用ディレクトリ */
    protected final Path workDir;
    
    /** ユーザー向け表示文字列を保持する {@link StringBuilder} */
    protected final StringBuilder str = new StringBuilder();
    
    /** このアプリケーションのリソースバンドル */
    protected final ResourceBundle rb = AppMain.appResource().get();
    
    /*package*/ AppTaskBase(
            Settings settings,
            Factory factory) {
        
        assert settings != null;
        assert factory != null;
        
        this.settings = settings;
        this.factory = factory;
        this.workDir = settings.get(SettingKeys.WORK_DIR_BASE)
                .resolve(settings.get(SettingKeys.CURR_TIMESTAMP));
    }
    
    @Override
    protected Void call() throws Exception {
        try {
            call2();
            return null;
            
        } catch (OutOfMemoryError e) {
            str.append(BR).append(BR).append(rb.getString("AppTaskBase.170")).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.170"), e);
        }
    }
    
    protected abstract void call2() throws Exception;
    
    /**
     * このタスクの比較対象Excelブックが同一ブックかを返します。<br>
     * 
     * @return 同一ブックの場合は {@code true}
     * @throws IllegalStateException 今回の実行メニューがブック同士の比較でもシート同士の比較でもない場合
     */
    protected boolean isSameBook() {
        AppMenu menu = settings.getOrDefault(SettingKeys.CURR_MENU);
        
        switch (menu) {
            case COMPARE_BOOKS:
            case COMPARE_SHEETS:
                return BookOpenInfo.isSameBook(
                        settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1),
                        settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2));
            
            default:
                throw new IllegalStateException("not suitable for " + menu);
        }
    }
    
    // 実装メモ：
    // 子クラスで使ういろんな機能を親に詰め込むというのはオブジェクト志向として間違ってる！
    // というのは分かりつつも、、、まぁそのうち整理するので許して。。。
    // TODO: AppTask周りの機能構成を整理する
    
    /**
     * 指定された2つのExcelブックに含まれるシート名をロードし、
     * 設定内容に基づいてシート名をペアリングして返します。<br>
     * 
     * @param bookOpenInfo1 Excelブック情報1
     * @param bookOpenInfo2 Excelブック情報2
     * @return シート名のペアのリスト
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    protected List<Pair<String>> getSheetNamePairs(
            BookOpenInfo bookOpenInfo1,
            BookOpenInfo bookOpenInfo2)
            throws ExcelHandlingException {
        
        assert bookOpenInfo1 != null;
        assert bookOpenInfo2 != null;
        assert !Objects.equals(bookOpenInfo1.bookPath(), bookOpenInfo2.bookPath());
        
        SheetNamesLoader bookLoader1 = factory.sheetNamesLoader(bookOpenInfo1);
        SheetNamesLoader bookLoader2 = factory.sheetNamesLoader(bookOpenInfo2);
        BookInfo bookInfo1 = bookLoader1.loadSheetNames(bookOpenInfo1);
        BookInfo bookInfo2 = bookLoader2.loadSheetNames(bookOpenInfo2);
        
        SheetNamesMatcher matcher = factory.sheetNamesMatcher(settings);
        return matcher.pairingSheetNames(bookInfo1, bookInfo2);
    }
    
    /**
     * 比較対象のフォルダもしくはフォルダツリーを抽出し、
     * トップフォルダの情報のペアを返します。<br>
     * 
     * @return 比較対象フォルダ・フォルダツリーのトップフォルダの情報のペア
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    protected Pair<DirInfo> extractDirs() throws ExcelHandlingException {
        Path dirPath1 = settings.get(SettingKeys.CURR_DIR_PATH1);
        Path dirPath2 = settings.get(SettingKeys.CURR_DIR_PATH2);
        DirLoader dirLoader = factory.dirLoader(settings);
        DirInfo dirInfo1 = dirLoader.loadDir(dirPath1);
        DirInfo dirInfo2 = dirLoader.loadDir(dirPath2);
        
        return new Pair<>(dirInfo1, dirInfo2);
    }
    
    protected BookResult compareBooks(
            BookOpenInfo bookOpenInfo1,
            BookOpenInfo bookOpenInfo2,
            int progressBefore,
            int progressAfter)
            throws ExcelHandlingException {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        
        List<Pair<String>> sheetNamePairs = getSheetNamePairs(bookOpenInfo1, bookOpenInfo2);
        
        CellsLoader loader1 = factory.cellsLoader(settings, bookOpenInfo1);
        CellsLoader loader2 = factory.cellsLoader(settings, bookOpenInfo2);
        SheetComparator comparator = factory.comparator(settings);
        Map<Pair<String>, Optional<SheetResult>> results = new HashMap<>();
        
        for (int i = 0; i < sheetNamePairs.size(); i++) {
            Pair<String> sheetNamePair = sheetNamePairs.get(i);
            
            if (sheetNamePair.isPaired()) {
                Set<CellData> cells1 = loader1.loadCells(bookOpenInfo1, sheetNamePair.a());
                Set<CellData> cells2 = loader2.loadCells(bookOpenInfo2, sheetNamePair.b());
                SheetResult result = comparator.compare(cells1, cells2);
                results.put(sheetNamePair, Optional.of(result));
                
            } else {
                results.put(sheetNamePair, Optional.empty());
            }
            
            updateProgress(
                    progressBefore + (progressAfter - progressBefore) * (i + 1) / sheetNamePairs.size(),
                    PROGRESS_MAX);
        }
        
        return new BookResult(
                new Pair<>(
                        bookOpenInfo1.bookPath(),
                        bookOpenInfo2.bookPath()),
                sheetNamePairs,
                results);
    }
    
    protected DirResult compareDirs(
            String dirId,
            String indent,
            DirPairData data,
            Pair<Path> outputDirs,
            int progressBefore,
            int progressAfter) {
        
        Map<Pair<String>, Optional<BookResult>> bookResults = new HashMap<>();
        int bookPairsCount = (int) data.bookNamePairs().stream().filter(Pair::isPaired).count();
        int num = 0;
        
        if (data.bookNamePairs().size() == 0) {
            str.append(indent + "    - ").append(rb.getString("AppTaskBase.160")).append(BR);
            updateMessage(str.toString());
        }
        
        for (int i = 0; i < data.bookNamePairs().size(); i++) {
            Pair<String> bookNamePair = data.bookNamePairs().get(i);
            
            str.append(indent
                    + DirResult.formatBookNamesPair(dirId, Integer.toString(i + 1), bookNamePair));
            updateMessage(str.toString());
            
            if (bookNamePair.isPaired()) {
                
                BookOpenInfo srcInfo1 = null;
                BookOpenInfo srcInfo2 = null;
                BookOpenInfo dstInfo1 = null;
                BookOpenInfo dstInfo2 = null;
                BookResult bookResult = null;
                
                try {
                    srcInfo1 = new BookOpenInfo(
                            data.dirPair().a().path().resolve(bookNamePair.a()), null);
                    srcInfo2 = new BookOpenInfo(
                            data.dirPair().b().path().resolve(bookNamePair.b()), null);
                    dstInfo1 = new BookOpenInfo(
                            outputDirs.a().resolve("【A%s-%d】%s".formatted(dirId, i + 1, bookNamePair.a())), null);
                    dstInfo2 = new BookOpenInfo(
                            outputDirs.b().resolve("【B%s-%d】%s".formatted(dirId, i + 1, bookNamePair.b())), null);
                    
                    bookResult = compareBooks(
                            srcInfo1,
                            srcInfo2,
                            progressBefore + (progressAfter - progressBefore) * num / bookPairsCount,
                            progressBefore + (progressAfter - progressBefore) * (num + 1) / bookPairsCount);
                    bookResults.put(bookNamePair, Optional.of(bookResult));
                    
                } catch (Exception e) {
                    bookResults.putIfAbsent(bookNamePair, Optional.empty());
                    str.append("  -  ").append(rb.getString("AppTaskBase.150")).append(BR);
                    updateMessage(str.toString());
                    e.printStackTrace();
                    
                    try {
                        Files.copy(srcInfo1.bookPath(), dstInfo1.bookPath());
                        Files.copy(srcInfo2.bookPath(), dstInfo2.bookPath());
                    } catch (IOException e1) {
                        // nop
                    }
                    continue;
                }
                
                try {
                    BookPainter painter1 = factory.painter(settings, srcInfo1);
                    BookPainter painter2 = factory.painter(settings, srcInfo2);
                    painter1.paintAndSave(srcInfo1, dstInfo1, bookResult.getPiece(Side.A));
                    painter2.paintAndSave(srcInfo2, dstInfo2, bookResult.getPiece(Side.B));
                    
                    str.append("  -  ").append(bookResult.getDiffSimpleSummary()).append(BR);
                    updateMessage(str.toString());
                    
                    num++;
                    updateProgress(
                            progressBefore + (progressAfter - progressBefore) * num / bookPairsCount,
                            PROGRESS_MAX);
                    
                } catch (Exception e) {
                    bookResults.putIfAbsent(bookNamePair, Optional.empty());
                    str.append("  -  ").append(rb.getString("AppTaskBase.150")).append(BR);
                    updateMessage(str.toString());
                    e.printStackTrace();
                    continue;
                }
                
            } else {
                
                try {
                    Path src = bookNamePair.hasA()
                            ? data.dirPair().a().path().resolve(bookNamePair.a())
                            : data.dirPair().b().path().resolve(bookNamePair.b());
                    Path dst = bookNamePair.hasA()
                            ? outputDirs.a().resolve("【A%s-%d】%s".formatted(dirId, i + 1, bookNamePair.a()))
                            : outputDirs.b().resolve("【B%s-%d】%s".formatted(dirId, i + 1, bookNamePair.b()));
                    
                    Files.copy(src, dst);
                    dst.toFile().setReadable(true, false);
                    dst.toFile().setWritable(true, false);
                    
                    bookResults.put(bookNamePair, Optional.empty());
                    
                    str.append(BR);
                    updateMessage(str.toString());
                    
                } catch (Exception e) {
                    bookResults.putIfAbsent(bookNamePair, Optional.empty());
                    str.append("  -  ").append(rb.getString("AppTaskBase.150")).append(BR);
                    updateMessage(str.toString());
                    e.printStackTrace();
                    continue;
                }
            }
        }
        str.append(BR);
        updateMessage(str.toString());
        
        return new DirResult(
                data.dirPair(),
                data.bookNamePairs(),
                bookResults,
                dirId);
    }
    
    /**
     * 比較結果文字列をテキストファイルに保存するとともに、
     * 設定に応じてアプリケーション（メモ帳）を立ち上げて表示します。<br>
     * 
     * @param workDir 作業用フォルダ
     * @param resultText 比較結果文字列
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter 進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    protected void saveAndShowResultText(
            Path workDir,
            String resultText,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        assert workDir != null;
        assert resultText != null;
        assert 0 <= progressBefore;
        assert progressBefore <= progressAfter;
        assert progressAfter <= PROGRESS_MAX;
        
        Path textPath = null;
        try {
            textPath = workDir.resolve("result.txt");
            
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append("%s%n    - %s%n%n".formatted(rb.getString("AppTaskBase.030"), textPath));
            updateMessage(str.toString());
            
            try (BufferedWriter writer = Files.newBufferedWriter(textPath)) {
                writer.write(resultText);
            }
            if (settings.getOrDefault(SettingKeys.SHOW_RESULT_TEXT)) {
                str.append(rb.getString("AppTaskBase.040")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(textPath.toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.050")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s".formatted(rb.getString("AppTaskBase.050"), textPath),
                    e);
        }
    }
    
    /**
     * Excelブックの各シートに比較結果の色を付けて保存し、
     * 設定に応じてExcelを立ち上げて表示します。<br>
     * 
     * @param workDir 作業用フォルダ
     * @param bResult Excelブック比較結果
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter 進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    protected void paintSaveAndShowBook(
            Path workDir,
            BookResult bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        assert workDir != null;
        assert bResult != null;
        assert 0 <= progressBefore;
        assert progressBefore <= progressAfter;
        assert progressAfter <= PROGRESS_MAX;
        
        if (isSameBook()) {
            paintSaveAndShowBook1(workDir, bResult, progressBefore, progressAfter);
        } else {
            paintSaveAndShowBook2(workDir, bResult, progressBefore, progressAfter);
        }
    }
    
    private void paintSaveAndShowBook1(
            Path workDir,
            BookResult bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        BookOpenInfo dst = null;
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            str.append(rb.getString("AppTaskBase.060")).append(BR);
            updateMessage(str.toString());
            
            BookOpenInfo src = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1);
            dst = new BookOpenInfo(
                    workDir.resolve(src.bookPath().getFileName()),
                    src.readPassword());
            
            str.append("    - %s%n%n".formatted(dst));
            updateMessage(str.toString());
            
            BookPainter painter = factory.painter(settings, dst);
            Map<String, Optional<SheetResult.Piece>> result = new HashMap<>(bResult.getPiece(Side.A));
            result.putAll(bResult.getPiece(Side.B));
            painter.paintAndSave(src, dst, result);
            
            updateProgress(progressBefore + (progressAfter - progressBefore) * 4 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.070")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.070"), e);
        }
        
        try {
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("AppTaskBase.080")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dst.bookPath().toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.090")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.090"), e);
        }
    }
    
    private void paintSaveAndShowBook2(
            Path workDir,
            BookResult bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        BookOpenInfo dst1 = null;
        BookOpenInfo dst2 = null;
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("AppTaskBase.060")).append(BR);
            updateMessage(str.toString());
            
            BookOpenInfo src1 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1);
            dst1 = new BookOpenInfo(
                    workDir.resolve("【A】" + src1.bookPath().getFileName()),
                    src1.readPassword());
            
            str.append("    - %s%n".formatted(dst1));
            updateMessage(str.toString());
            
            BookPainter painter1 = factory.painter(settings, dst1);
            painter1.paintAndSave(src1, dst1, bResult.getPiece(Side.A));
            
            updateProgress(progressBefore + (progressAfter - progressBefore) * 2 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.100")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.100"), e);
        }
        
        try {
            BookOpenInfo src2 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2);
            dst2 = new BookOpenInfo(
                    workDir.resolve("【B】" + src2.bookPath().getFileName()),
                    src2.readPassword());
            
            str.append("    - %s%n%n".formatted(dst2));
            updateMessage(str.toString());
            
            BookPainter painter2 = factory.painter(settings, dst2);
            painter2.paintAndSave(src2, dst2, bResult.getPiece(Side.B));
            
            updateProgress(progressBefore + (progressAfter - progressBefore) * 4 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.110")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.110"), e);
        }
        
        try {
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("AppTaskBase.080")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dst1.bookPath().toFile());
                Desktop.getDesktop().open(dst2.bookPath().toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.090")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.090"), e);
        }
    }
    
    protected void createSaveAndShowResultBook(
            Path workDir,
            TreeResult tResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        Path resultBookPath = null;
        
        try {
            resultBookPath = workDir.resolve("result.xlsx");
            str.append("%s%n    - %s%n%n".formatted(rb.getString("CompareTreesTask.070"), resultBookPath));
            updateMessage(str.toString());
            
            TreeResultBookCreator creator = new TreeResultBookCreator();
            creator.createResultBook(resultBookPath, tResult);
            
        } catch (ExcelHandlingException e) {
            str.append(rb.getString("CompareTreesTask.080")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s".formatted(rb.getString("CompareTreesTask.080"), resultBookPath),
                    e);
        }
        
        try {
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("CompareTreesTask.090")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(resultBookPath.toFile());
            }
        } catch (IOException e) {
            str.append(rb.getString("CompareTreesTask.100")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s".formatted(rb.getString("CompareTreesTask.100"), resultBookPath),
                    e);
        }
        updateProgress(progressAfter, PROGRESS_MAX);
    }
    
    /**
     * 処理修了をアナウンスする。<br>
     */
    protected void announceEnd() {
        str.append(rb.getString("AppTaskBase.120"));
        updateMessage(str.toString());
        updateProgress(PROGRESS_MAX, PROGRESS_MAX);
    }
}
