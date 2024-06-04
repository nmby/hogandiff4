package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    
    protected ApplicationException getApplicationException(Throwable e, String msgId, String appendMsg) {
        if (e instanceof ApplicationException ee) {
            return ee;
            
        } else {
            str.append(BR).append(BR).append(rb.getString(msgId) + appendMsg).append(BR).append(BR);
            updateMessage(str.toString());
            return new ApplicationException(rb.getString(msgId) + appendMsg, e);
        }
    }
    
    @Override
    protected Report call() throws ApplicationException {
        Instant start = Instant.now();
        Report report = null;
        
        try {
            Result result = call2();
            
            report = new Report.Succeeded(settings, start, Instant.now(), result);
            
            return report;
            
        } catch (OutOfMemoryError e) {
            report = new Report.Failed(settings, start, Instant.now(), e);
            throw getApplicationException(e, "AppTaskBase.170", "");
            
        } catch (Exception e) {
            report = new Report.Failed(settings, start, Instant.now(), e);
            throw getApplicationException(e, "AppTaskBase.180", " at AppTaskBase::call");
            
        } finally {
            writeReport(report);
        }
    }
    
    /**
     * 統計情報を report.json ファイルに出力します。<br>
     * 
     * @param report 統計情報
     */
    private void writeReport(Report report) {
        try {
            Path reportPath = workDir.resolve("report.json");
            try (BufferedWriter writer = Files.newBufferedWriter(reportPath)) {
                writer.write(report.toJsonString());
            }
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
     * 比較結果文字列をテキストファイルに保存するとともに、
     * 設定に応じてアプリケーション（メモ帳）を立ち上げて表示します。<br>
     * 
     * @param workDir 作業用フォルダ
     * @param resultText 比較結果文字列
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter 進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    // CompareSheetsTask, CompareBooksTask, CompareDirsTask, CompareTreesTask
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
        
        try {
            Path textPath = workDir.resolve("result.txt");
            
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
            throw getApplicationException(e, "AppTaskBase.050", "");
        }
    }
    
    /**
     * 処理修了をアナウンスする。<br>
     * 
     * @throws ApplicationException 処理に失敗した場合
     */
    // CompareSheetsTask, CompareBooksTask, CompareDirsTask, CompareTreesTask
    protected void announceEnd() throws ApplicationException {
        try {
            str.append(rb.getString("AppTaskBase.120"));
            updateMessage(str.toString());
            updateProgress(PROGRESS_MAX, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at AppTaskBase::announceEnd");
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
    // CompareSheetsTask, CompareBooksTask
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
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at AppTaskBase::paintSaveAndShowBook");
        }
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
        
        Path dstBookPath = null;
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            str.append(rb.getString("AppTaskBase.060")).append(BR);
            updateMessage(str.toString());
            
            Path srcBookPath = settings.get(SettingKeys.CURR_BOOK_PATH1);
            dstBookPath = workDir.resolve(srcBookPath.getFileName());
            String readPassword = settings.get(SettingKeys.CURR_READ_PASSWORDS).get(srcBookPath);
            
            str.append("    - %s%n%n".formatted(dstBookPath));
            updateMessage(str.toString());
            
            BookPainter painter = factory.painter(settings, dstBookPath, readPassword);
            Map<String, Optional<SheetResult.Piece>> result = new HashMap<>(bResult.getPiece(Side.A));
            result.putAll(bResult.getPiece(Side.B));
            painter.paintAndSave(srcBookPath, dstBookPath, readPassword, result);
            
            updateProgress(progressBefore + (progressAfter - progressBefore) * 4 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.070", "");
        }
        
        try {
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("AppTaskBase.080")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dstBookPath.toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.090", "");
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
        
        Path dstBookPath1 = null;
        Path dstBookPath2 = null;
        Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("AppTaskBase.060")).append(BR);
            updateMessage(str.toString());
            
            Path srcBookPath1 = settings.get(SettingKeys.CURR_BOOK_PATH1);
            dstBookPath1 = workDir.resolve("【A】" + srcBookPath1.getFileName());
            String readPassword1 = readPasswords.get(srcBookPath1);
            
            str.append("    - %s%n".formatted(dstBookPath1));
            updateMessage(str.toString());
            
            BookPainter painter1 = factory.painter(settings, dstBookPath1, readPassword1);
            painter1.paintAndSave(srcBookPath1, dstBookPath1, readPassword1, bResult.getPiece(Side.A));
            
            updateProgress(progressBefore + (progressAfter - progressBefore) * 2 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.100", "");
        }
        
        try {
            Path srcBookPath2 = settings.get(SettingKeys.CURR_BOOK_PATH2);
            dstBookPath2 = workDir.resolve("【B】" + srcBookPath2.getFileName());
            String readPassword2 = readPasswords.get(srcBookPath2);
            
            str.append("    - %s%n%n".formatted(dstBookPath2));
            updateMessage(str.toString());
            
            BookPainter painter2 = factory.painter(settings, dstBookPath2, readPassword2);
            painter2.paintAndSave(srcBookPath2, dstBookPath2, readPassword2, bResult.getPiece(Side.B));
            
            updateProgress(progressBefore + (progressAfter - progressBefore) * 4 / 5, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.110", "");
        }
        
        try {
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("AppTaskBase.080")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dstBookPath1.toFile());
                Desktop.getDesktop().open(dstBookPath2.toFile());
            }
            
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.090", "");
        }
    }
    
    /**
     * 比較対象のフォルダもしくはフォルダツリーを抽出し、
     * トップフォルダの情報のペアを返します。<br>
     * 
     * @return 比較対象フォルダ・フォルダツリーのトップフォルダの情報のペア
     * @throws ApplicationException 処理に失敗した場合
     */
    // CompareDirsTask, CompareTreesTask
    protected Pair<DirInfo> extractDirs() throws ApplicationException {
        try {
            Pair<Path> dirPaths = SettingKeys.CURR_DIR_PATHS.map(settings::get);
            DirLoader dirLoader = factory.dirLoader(settings);
            return dirPaths.unsafeMap(dirLoader::loadDir);
            
        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.190", "");
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
    // CompareDirsTask, CompareTreesTask
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
            throw getApplicationException(e, "CompareTreesTask.080", "");
        }
        
        try {
            if (settings.getOrDefault(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(rb.getString("CompareTreesTask.090")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(resultBookPath.toFile());
            }
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, "CompareTreesTask.100", "");
        }
    }
    
    //■ 要素処理 ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
    
    /**
     * Excelブック同士の比較を行います。<br>
     * 
     * @param bookPaths 比較対象ブックのパス
     * @param readPasswords 比較対象ブックの読み取りパスワード
     * @param progressBefore 処理開始時の進捗度
     * @param progressAfter 処理終了時の進捗度
     * @return 比較結果
     * @throws ExcelHandlingException Excel関連処理に失敗した場合
     */
    // AppTaskBase#compareDirs
    private BookResult compareBooks(
            Pair<Path> bookPaths,
            Map<Path, String> readPasswords,
            int progressBefore,
            int progressAfter)
            throws ExcelHandlingException {
        
        updateProgress(progressBefore, PROGRESS_MAX);
        
        List<Pair<String>> sheetNamePairs = getSheetNamePairs(bookPaths, readPasswords);
        
        Pair<CellsLoader> loaders = bookPaths.unsafeMap(
                bookPath -> factory.cellsLoader(settings, bookPath, readPasswords.get(bookPath)));
        
        SheetComparator comparator = factory.comparator(settings);
        Map<Pair<String>, Optional<SheetResult>> results = new HashMap<>();
        
        for (int i = 0; i < sheetNamePairs.size(); i++) {
            Pair<String> sheetNamePair = sheetNamePairs.get(i);
            
            if (sheetNamePair.isPaired()) {
                Pair<Set<CellData>> cellsSets = Side.unsafeMap(side -> loaders.get(side).loadCells(
                        bookPaths.get(side),
                        readPasswords.get(bookPaths.get(side)),
                        sheetNamePair.get(side)));
                
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
                bookPaths,
                sheetNamePairs,
                results);
    }
    
    /**
     * このタスクの比較対象Excelブックが同一ブックかを返します。<br>
     * 
     * @return 同一ブックの場合は {@code true}
     * @throws IllegalStateException 今回の実行メニューがブック同士の比較でもシート同士の比較でもない場合
     */
    // AppTaskBase#paintSaveAndShowBooks, CompareSheetsTask
    protected boolean isSameBook() {
        AppMenu menu = settings.getOrDefault(SettingKeys.CURR_MENU);
        
        return switch (menu) {
            case COMPARE_BOOKS, COMPARE_SHEETS -> Objects.equals(
                    settings.get(SettingKeys.CURR_BOOK_PATH1),
                    settings.get(SettingKeys.CURR_BOOK_PATH2));
        
            default -> throw new IllegalStateException("not suitable for " + menu);
        };
    }
    
    /**
     * 指定された2つのExcelブックに含まれるシート名をロードし、
     * 設定内容に基づいてシート名をペアリングして返します。<br>
     * 
     * @param bookPaths Excelブックのパス
     * @param readPasswords Excelブックの読み取りパスワード
     * @return シート名のペアのリスト
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    // AppTaskBase#compareBooks, CompareBooksTask
    protected List<Pair<String>> getSheetNamePairs(
            Pair<Path> bookPaths,
            Map<Path, String> readPasswords)
            throws ExcelHandlingException {
        
        assert bookPaths != null;
        assert readPasswords != null;
        assert !Objects.equals(bookPaths.a(), bookPaths.b());
        
        Pair<BookInfo> bookInfos = bookPaths.unsafeMap(bookPath -> {
            String readPassword = readPasswords.get(bookPath);
            SheetNamesLoader sheetNamesLoader = factory.sheetNamesLoader(bookPath, readPassword);
            return sheetNamesLoader.loadSheetNames(bookPath, readPassword);
        });
        
        SheetNamesMatcher matcher = factory.sheetNamesMatcher(settings);
        return matcher.pairingSheetNames(bookInfos);
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
    // CompareDirsTask, CompareTreesTask
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
                
                Pair<Path> srcPaths = Side.map(side -> data.dirPair().get(side).path().resolve(bookNamePair.get(side)));
                Pair<Path> dstPaths = Side.map(side -> outputDirs.get(side)
                        .resolve("【A%s-%d】%s".formatted(dirId, ii + 1, bookNamePair.get(side))));
                BookResult bookResult = null;
                // TODO: メソッド引数化する
                Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
                
                try {
                    
                    bookResult = compareBooks(
                            srcPaths,
                            readPasswords,
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
                            Files.copy(srcPaths.get(side), dstPaths.get(side));
                        } catch (IOException e1) {
                            // nop
                        }
                    });
                    continue;
                }
                
                try {
                    Pair<BookPainter> painters = srcPaths.unsafeMap(
                            bookPath -> factory.painter(settings, bookPath, readPasswords.get(bookPath)));
                    BookResult bookResult2 = bookResult;
                    
                    Side.unsafeForEach(side -> {
                        Path srcPath = srcPaths.get(side);
                        Path dstPath = dstPaths.get(side);
                        
                        painters.get(side).paintAndSave(
                                srcPath,
                                dstPath,
                                readPasswords.get(srcPath),
                                bookResult2.getPiece(side));
                    });
                    
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
}
