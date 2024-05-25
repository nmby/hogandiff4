package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
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
import xyz.hotchpotch.hogandiff.excel.Result;
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
/*package*/ abstract sealed class AppTaskBase extends Task<Report>
        permits CompareSheetsTask, CompareBooksTask, CompareDirsTask, CompareTreesTask {
    
    // [static members] ********************************************************
    
    /** 改行文字 */
    protected static final String BR = System.lineSeparator();
    
    /** 進捗度の最大値 */
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
    protected final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * コンストラクタ
     * 
     * @param settings 設定セット
     * @param factory ファクトリ
     */
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
    protected Report call() throws ApplicationException {
        Report report = null;
        
        try {
            Instant time1 = Instant.now();
            Result result = call2();
            Instant time2 = Instant.now();
            
            report = new Report.Succeeded(
                    settings,
                    Duration.between(time1, time2),
                    result);
            
        } catch (OutOfMemoryError e) {
            str.append(BR).append(BR).append(rb.getString("AppTaskBase.170")).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.170"), e);
        }
        
        writeReport(report);
        
        return report;
    }
    
    /**
     * 統計情報を report.json ファイルに出力します。<br>
     * 
     * @param report 統計情報
     */
    private void writeReport(Report report) {
        Path reportPath = workDir.resolve("report.json");
        try (BufferedWriter writer = Files.newBufferedWriter(reportPath)) {
            writer.write(report.toJsonString());
        } catch (Exception e) {
            e.printStackTrace();
            // nop
        }
    }
    
    /**
     * タスク本体。タスクを実行し結果を返します。<br>
     * 
     * @return タスクの結果
     * @throws ApplicationException 処理が失敗した場合
     */
    protected abstract Result call2() throws ApplicationException;
    
    //■ タスクステップ ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
    
    /**
     * 比較対象のフォルダもしくはフォルダツリーを抽出し、
     * トップフォルダの情報のペアを返します。<br>
     * 
     * @return 比較対象フォルダ・フォルダツリーのトップフォルダの情報のペア
     * @throws ApplicationException 処理に失敗した場合
     */
    protected Pair<DirInfo> extractDirs() throws ApplicationException {
        Pair<Path> dirPaths = null;
        try {
            dirPaths = SettingKeys.CURR_DIR_PATHS.map(settings::get);
            DirLoader dirLoader = factory.dirLoader(settings);
            return dirPaths.unsafeMap(dirLoader::loadDir);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.190")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s%n%s".formatted(rb.getString("AppTaskBase.190"), dirPaths.a(), dirPaths.b()), e);
        }
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
        
        try {
            if (isSameBook()) {
                paintSaveAndShowBook1(workDir, bResult, 80, 98);
            } else {
                paintSaveAndShowBook2(workDir, bResult, 80, 98);
            }
            
        } catch (ApplicationException e) {
            throw e;
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.180")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.180"), e);
        }
    }
    
    /**
     * フォルダツリー同士の比較結果Excelブックを作成し、保存し、表示します。<br>
     * 
     * @param workDir 作業用フォルダ
     * @param tResult フォルダツリー比較結果
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter 進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    protected void createSaveAndShowResultBook(
            Path workDir,
            TreeResult tResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        Path resultBookPath = null;
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            resultBookPath = workDir.resolve("result.xlsx");
            str.append("%s%n    - %s%n%n".formatted(rb.getString("CompareTreesTask.070"), resultBookPath));
            updateMessage(str.toString());
            
            TreeResultBookCreator creator = new TreeResultBookCreator();
            creator.createResultBook(resultBookPath, tResult);
            
        } catch (Exception e) {
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
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("CompareTreesTask.100")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(
                    "%s%n%s".formatted(rb.getString("CompareTreesTask.100"), resultBookPath),
                    e);
        }
    }
    
    /**
     * 処理修了をアナウンスする。<br>
     * 
     * @throws ApplicationException 処理に失敗した場合
     */
    protected void announceEnd() throws ApplicationException {
        try {
            str.append(rb.getString("AppTaskBase.120"));
            updateMessage(str.toString());
            updateProgress(PROGRESS_MAX, PROGRESS_MAX);
            
        } catch (Exception e) {
            str.append(rb.getString("AppTaskBase.180")).append(BR).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
            throw new ApplicationException(rb.getString("AppTaskBase.180"), e);
        }
    }
    
    //■ 要素処理 ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
    
    /**
     * 指定された2つのExcelブックに含まれるシート名をロードし、
     * 設定内容に基づいてシート名をペアリングして返します。<br>
     * 
     * @param bookOpenInfos Excelブック情報
     * @return シート名のペアのリスト
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    protected List<Pair<String>> getSheetNamePairs(
            Pair<BookOpenInfo> bookOpenInfos)
            throws ExcelHandlingException {
        
        assert bookOpenInfos != null;
        assert !Objects.equals(bookOpenInfos.a().bookPath(), bookOpenInfos.b().bookPath());
        
        Pair<SheetNamesLoader> sheetNamesLoaders = bookOpenInfos.unsafeMap(factory::sheetNamesLoader);
        Pair<BookInfo> bookInfos = Side.unsafeMap(
                side -> sheetNamesLoaders.get(side).loadSheetNames(bookOpenInfos.get(side)));
        
        SheetNamesMatcher matcher = factory.sheetNamesMatcher(settings);
        return matcher.pairingSheetNames(bookInfos);
    }
    
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
    
    /**
     * フォルダ同士の比較を行います。<br>
     * 
     * @param dirId フォルダ識別子
     * @param indent インデント
     * @param data 比較対象フォルダの情報
     * @param outputDirs 出力先フォルダ
     * @param progressBefore 処理開始時の進捗度
     * @param progressAfter 処理終了時の進捗度
     * @return 比較結果
     */
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
            int ii = i;
            
            Pair<String> bookNamePair = data.bookNamePairs().get(i);
            
            str.append(indent
                    + DirResult.formatBookNamesPair(dirId, Integer.toString(i + 1), bookNamePair));
            updateMessage(str.toString());
            
            if (bookNamePair.isPaired()) {
                
                Pair<BookOpenInfo> srcInfos = Side.map(side -> new BookOpenInfo(
                        data.dirPair().get(side).path().resolve(bookNamePair.get(side)), null));
                Pair<BookOpenInfo> dstInfos = Side.map(side -> new BookOpenInfo(
                        outputDirs.get(side).resolve("【A%s-%d】%s".formatted(dirId, ii + 1, bookNamePair.get(side))),
                        null));
                BookResult bookResult = null;
                
                try {
                    
                    bookResult = compareBooks(
                            srcInfos,
                            progressBefore + (progressAfter - progressBefore) * num / bookPairsCount,
                            progressBefore + (progressAfter - progressBefore) * (num + 1) / bookPairsCount);
                    bookResults.put(bookNamePair, Optional.of(bookResult));
                    
                } catch (Exception e) {
                    bookResults.putIfAbsent(bookNamePair, Optional.empty());
                    str.append("  -  ").append(rb.getString("AppTaskBase.150")).append(BR);
                    updateMessage(str.toString());
                    e.printStackTrace();
                    
                    Side.forEach(side -> {
                        try {
                            Files.copy(srcInfos.get(side).bookPath(), dstInfos.get(side).bookPath());
                        } catch (IOException e1) {
                            // nop
                        }
                    });
                    continue;
                }
                
                try {
                    Pair<BookPainter> painters = srcInfos.unsafeMap(info -> factory.painter(settings, info));
                    BookResult bookResult2 = bookResult;
                    
                    Side.unsafeForEach(
                            side -> painters.get(side).paintAndSave(
                                    srcInfos.get(side),
                                    dstInfos.get(side),
                                    bookResult2.getPiece(side)));
                    
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
     * Excelブック同士の比較を行います。<br>
     * 
     * @param bookOpenInfos 比較対象ブックの情報
     * @param progressBefore 処理開始時の進捗度
     * @param progressAfter 処理終了時の進捗度
     * @return 比較結果
     * @throws ExcelHandlingException Excel関連処理に失敗した場合
     */
    private BookResult compareBooks(
            Pair<BookOpenInfo> bookOpenInfos,
            int progressBefore,
            int progressAfter)
            throws ExcelHandlingException {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        
        List<Pair<String>> sheetNamePairs = getSheetNamePairs(bookOpenInfos);
        
        Pair<CellsLoader> loaders = bookOpenInfos.unsafeMap(info -> factory.cellsLoader(settings, info));
        
        SheetComparator comparator = factory.comparator(settings);
        Map<Pair<String>, Optional<SheetResult>> results = new HashMap<>();
        
        for (int i = 0; i < sheetNamePairs.size(); i++) {
            Pair<String> sheetNamePair = sheetNamePairs.get(i);
            
            if (sheetNamePair.isPaired()) {
                Pair<Set<CellData>> cellsSets = Side.unsafeMap(
                        side -> loaders.get(side).loadCells(bookOpenInfos.get(side), sheetNamePair.get(side)));
                
                SheetResult result = comparator.compare(cellsSets);
                results.put(sheetNamePair, Optional.of(result));
                
            } else {
                results.put(sheetNamePair, Optional.empty());
            }
            
            updateProgress(
                    progressBefore + (progressAfter - progressBefore) * (i + 1) / sheetNamePairs.size(),
                    PROGRESS_MAX);
        }
        
        return new BookResult(
                bookOpenInfos.map(BookOpenInfo::bookPath),
                sheetNamePairs,
                results);
    }
    
    /**
     * Excelブックの各シートに比較結果の色を付けて保存し、
     * 設定に応じてExcelを立ち上げて表示します。<br>
     * 比較対象シートが同一ブックに属する場合のためのメソッドです。<br>
     * 
     * @param workDir 作業用フォルダ
     * @param bResult Excelブック比較結果
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter 進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
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
    
    /**
     * Excelブックの各シートに比較結果の色を付けて保存し、
     * 設定に応じてExcelを立ち上げて表示します。<br>
     * 比較対象シートが別々のブックに属する場合のためのメソッドです。<br>
     * 
     * @param workDir 作業用フォルダ
     * @param bResult Excelブック比較結果
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter 進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
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
}
