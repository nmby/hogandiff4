package xyz.hotchpotch.hogandiff.task;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.ApplicationException;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookPainter;
import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.CellsLoader;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.Result;
import xyz.hotchpotch.hogandiff.excel.SheetComparator;
import xyz.hotchpotch.hogandiff.excel.SheetResult;
import xyz.hotchpotch.hogandiff.excel.TreeResult;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.BookReportCreator;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.TreeReportCreator;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較タスクの基底クラスです。<br>
 * 
 * @author nmby
 */
/* package */ abstract sealed class CompareTask extends Task<Void>
        permits CompareTaskSheets, CompareTaskBooks, CompareTaskDirs, CompareTaskTrees {

    // [static members] ********************************************************

    /** 改行文字 */
    protected static final String BR = System.lineSeparator();

    /** 進捗度の最大値 */
    protected static final int PROGRESS_MAX = 100;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("yyyy/M/d H:mm");

    // [instance members] ******************************************************

    /** 今回の実行における各種設定を保持する設定セット */
    protected final Settings settings;

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
     * @param factory  ファクトリ
     */
    protected CompareTask(Settings settings) {
        assert settings != null;

        this.settings = settings;
        this.workDir = settings.get(SettingKeys.WORK_DIR_BASE)
                .resolve(settings.get(SettingKeys.CURR_TIMESTAMP));
    }

    protected ApplicationException getApplicationException(Throwable e, String msgId, String appendMsg) {
        if (e instanceof ApplicationException ee) {
            return ee;

        } else {
            str.append(BR).append(rb.getString(msgId) + appendMsg).append(BR).append(BR);
            updateMessage(str.toString());
            return new ApplicationException(rb.getString(msgId) + appendMsg, e);
        }
    }

    @Override
    protected Void call() throws ApplicationException {
        try {
            call2();
            return null;

        } catch (OutOfMemoryError e) {
            throw getApplicationException(e, "AppTaskBase.170", "");

        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at AppTaskBase::call");
        }
    }

    /**
     * タスク本体。タスクを実行し結果を返します。<br>
     * 
     * @return タスクの結果
     * @throws ApplicationException 処理が失敗した場合
     */
    protected abstract Result call2() throws ApplicationException;

    // ■ タスクステップ ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■

    /**
     * 比較結果文字列をテキストファイルに保存します。<br>
     * 
     * @param workDir        作業用フォルダ
     * @param resultText     比較結果文字列
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter  進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    // CompareSheetsTask, CompareBooksTask, CompareDirsTask, CompareTreesTask
    protected void saveResultText(
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

            String timestamp = settings.get(SettingKeys.CURR_TIMESTAMP);
            LocalDateTime execDatetime = LocalDateTime.parse(timestamp, formatter);
            String execDatetimeStr = "%s%s%n".formatted(
                    rb.getString("AppTaskBase.200"),
                    execDatetime.format(formatter2));

            try (BufferedWriter writer = Files.newBufferedWriter(textPath)) {
                writer.write(execDatetimeStr + resultText);
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
     * @param workDir         作業用フォルダ
     * @param srcBookPathPair Excelブックのパス
     * @param bResult         Excelブック比較結果
     * @param progressBefore  進捗率（開始時）
     * @param progressAfter   進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    // CompareSheetsTask, CompareBooksTask
    protected void paintSaveAndShowBook(
            Path workDir,
            Pair<Path> srcBookPathPair,
            BookResult bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {

        try {
            if (isSameBook()) {
                paintSaveAndShowBook1(workDir, srcBookPathPair.a(), bResult, 80, 98);
            } else {
                paintSaveAndShowBook2(workDir, srcBookPathPair, bResult, 80, 98);
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
     * @param workDir        作業用フォルダ
     * @param bResult        Excelブック比較結果
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter  進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    private void paintSaveAndShowBook1(
            Path workDir,
            Path srcBookPath,
            BookResult bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {

        Path dstBookPath = null;

        try {
            updateProgress(progressBefore, PROGRESS_MAX);

            str.append(rb.getString("AppTaskBase.060")).append(BR);
            updateMessage(str.toString());

            dstBookPath = workDir.resolve(srcBookPath.getFileName());
            String readPassword = settings.get(SettingKeys.CURR_READ_PASSWORDS).get(srcBookPath);

            str.append("    - %s%n%n".formatted(dstBookPath));
            updateMessage(str.toString());

            BookPainter painter = Factory.painter(settings, dstBookPath, readPassword);
            Map<String, Optional<SheetResult.Piece>> result = new HashMap<>(bResult.getPiece(Side.A));
            result.putAll(bResult.getPiece(Side.B));
            painter.paintAndSave(srcBookPath, dstBookPath, readPassword, result);

            updateProgress(progressBefore + (progressAfter - progressBefore) * 4 / 5, PROGRESS_MAX);

        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.070", "");
        }

        try {
            if (settings.get(SettingKeys.SHOW_PAINTED_SHEETS)) {
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
     * @param workDir        作業用フォルダ
     * @param bResult        Excelブック比較結果
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter  進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    private void paintSaveAndShowBook2(
            Path workDir,
            Pair<Path> srcBookPathPair,
            BookResult bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {

        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("AppTaskBase.060")).append(BR);
            updateMessage(str.toString());

            Pair<Path> dstBookPathPair = Side.map(
                    side -> workDir.resolve("【%s】%s".formatted(side, srcBookPathPair.get(side).getFileName())));
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);

            for (Side side : Side.values()) {
                try {
                    str.append("    - %s%n".formatted(dstBookPathPair.get(side)));
                    updateMessage(str.toString());

                    Path srcBookPath = srcBookPathPair.get(side);
                    Path dstBookPath = dstBookPathPair.get(side);

                    BookPainter painter = Factory.painter(settings, dstBookPath, readPasswords.get(srcBookPath));
                    painter.paintAndSave(
                            srcBookPath,
                            dstBookPath,
                            readPasswords.get(srcBookPath),
                            bResult.getPiece(side));
                    updateProgress(
                            progressBefore + (progressAfter - progressBefore) * (side == Side.A ? 2 : 4) / 5,
                            PROGRESS_MAX);

                } catch (Exception e) {
                    throw getApplicationException(e, side == Side.A ? "AppTaskBase.100" : "AppTaskBase.110", "");
                }
            }

            if (settings.get(SettingKeys.SHOW_PAINTED_SHEETS)) {
                str.append(BR).append(rb.getString("AppTaskBase.080")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(dstBookPathPair.a().toFile());
                Desktop.getDesktop().open(dstBookPathPair.b().toFile());
            } else {
                str.append(BR);
            }

            updateProgress(progressAfter, PROGRESS_MAX);

        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.090", "");
        }
    }

    /**
     * フォルダツリー同士の比較結果Excelブックを作成し、保存し、表示します。<br>
     * 
     * @param workDir        作業用フォルダ
     * @param tResult        フォルダツリー比較結果
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter  進捗率（終了時）
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

            TreeReportCreator creator = new TreeReportCreator();
            creator.createResultBook(
                    resultBookPath,
                    tResult,
                    settings.get(SettingKeys.CURR_MENU) == AppMenu.COMPARE_TREES);
            updateProgress(progressBefore + (progressAfter - progressBefore) * 4 / 5, PROGRESS_MAX);

        } catch (Exception e) {
            throw getApplicationException(e, "CompareTreesTask.080", "");
        }

        try {
            if (settings.get(SettingKeys.SHOW_RESULT_REPORT)) {
                str.append(rb.getString("CompareTreesTask.090")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(resultBookPath.toFile());
            }
            updateProgress(progressAfter, PROGRESS_MAX);

        } catch (Exception e) {
            throw getApplicationException(e, "CompareTreesTask.100", "");
        }
    }

    /**
     * Excelブック同士の比較結果Excelブックを作成し、保存し、表示します。<br>
     * 
     * @param workDir        作業用フォルダ
     * @param bResult        Excelブック比較結果
     * @param progressBefore 進捗率（開始時）
     * @param progressAfter  進捗率（終了時）
     * @throws ApplicationException 処理に失敗した場合
     */
    // CompareSheetsTask, CompareBooksTask
    protected void createSaveAndShowReportBook(
            Path workDir,
            BookResult bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {

        Path resultBookPath = null;
        try {
            updateProgress(progressBefore, PROGRESS_MAX);

            resultBookPath = workDir.resolve("result.xlsx");
            str.append("%s%n    - %s%n%n".formatted(rb.getString("CompareBooksTask.060"), resultBookPath));
            updateMessage(str.toString());

            BookReportCreator creator = new BookReportCreator();
            creator.createResultBook(resultBookPath, bResult);
            updateProgress(progressBefore + (progressAfter - progressBefore) * 4 / 5, PROGRESS_MAX);

        } catch (Exception e) {
            throw getApplicationException(e, "CompareBooksTask.070", "");
        }

        try {
            if (settings.get(SettingKeys.SHOW_RESULT_REPORT)) {
                str.append(rb.getString("CompareBooksTask.080")).append(BR).append(BR);
                updateMessage(str.toString());
                Desktop.getDesktop().open(resultBookPath.toFile());
            }
            updateProgress(progressAfter, PROGRESS_MAX);

        } catch (Exception e) {
            throw getApplicationException(e, "CompareBooksTask.090", "");
        }
    }

    // ■ 要素処理 ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■

    /**
     * Excelブック同士の比較を行います。<br>
     * 
     * @param bookPathPair   比較対象ブックのパス
     * @param readPasswords  比較対象ブックの読み取りパスワード
     * @param progressBefore 処理開始時の進捗度
     * @param progressAfter  処理終了時の進捗度
     * @return 比較結果
     * @throws ExcelHandlingException Excel関連処理に失敗した場合
     */
    // CompareTask#compareDirs
    private BookResult compareBooks(
            PairingInfoBooks bookComparison,
            int progressBefore,
            int progressAfter)
            throws ExcelHandlingException {

        updateProgress(progressBefore, PROGRESS_MAX);

        Pair<CellsLoader> cellsLoaderPair = bookComparison.parentBookInfoPair().map(BookInfo::bookPath).unsafeMap(
                bookPath -> Factory.cellsLoader(settings, bookPath));
        SheetComparator sheetComparator = Factory.sheetComparator(settings);
        Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
        Map<Pair<String>, Optional<SheetResult>> results = new HashMap<>();

        for (int i = 0; i < bookComparison.childSheetNamePairs().size(); i++) {
            Pair<String> sheetNamePair = bookComparison.childSheetNamePairs().get(i);

            if (sheetNamePair.isPaired()) {

                Path bookPathA = bookComparison.parentBookInfoPair().a().bookPath();
                Path bookPathB = bookComparison.parentBookInfoPair().b().bookPath();
                Set<CellData> cellsSetA = cellsLoaderPair.a().loadCells(
                        bookPathA, readPasswords.get(bookPathA), sheetNamePair.a());
                Set<CellData> cellsSetB = cellsLoaderPair.b().loadCells(
                        bookPathB, readPasswords.get(bookPathB), sheetNamePair.b());

                SheetResult result = sheetComparator.compare(Pair.of(cellsSetA, cellsSetB));
                results.put(sheetNamePair, Optional.of(result));

            } else {
                results.put(sheetNamePair, Optional.empty());
            }

            updateProgress(
                    progressBefore
                            + (progressAfter - progressBefore) * (i + 1) / bookComparison.childSheetNamePairs().size(),
                    PROGRESS_MAX);
        }

        return new BookResult(bookComparison, results);
    }

    /**
     * このタスクの比較対象Excelブックが同一ブックかを返します。<br>
     * 
     * @return 同一ブックの場合は {@code true}
     * @throws IllegalStateException 今回の実行メニューがブック同士の比較でもシート同士の比較でもない場合
     */
    // AppTaskBase#paintSaveAndShowBooks, CompareSheetsTask
    protected boolean isSameBook() {
        AppMenu menu = settings.get(SettingKeys.CURR_MENU);

        return switch (menu) {
            case COMPARE_SHEETS -> settings
                    .get(SettingKeys.CURR_SHEET_COMPARE_INFO).parentBookInfoPair().isIdentical();
            case COMPARE_BOOKS -> settings
                    .get(SettingKeys.CURR_BOOK_COMPARE_INFO).parentBookInfoPair().isIdentical();

            default -> throw new IllegalStateException("not suitable for " + menu);
        };
    }

    /**
     * フォルダ同士の比較を行います。<br>
     * 
     * @param dirId          フォルダ識別子
     * @param indent         インデント
     * @param dirComparison  比較対象フォルダの情報
     * @param outputDirPair  出力先フォルダ
     * @param progressBefore 処理開始時の進捗度
     * @param progressAfter  処理終了時の進捗度
     * @return 比較結果
     */
    // CompareDirsTask, CompareTreesTask
    protected DirResult compareDirs(
            String dirId,
            String indent,
            PairingInfoDirs dirComparison,
            Pair<Path> outputDirPair,
            int progressBefore,
            int progressAfter) {

        Map<Pair<BookInfo>, Optional<BookResult>> bookResults = new HashMap<>();
        IntUnaryOperator getProgress = n -> progressBefore
                + (progressAfter - progressBefore) * n / dirComparison.childBookInfoPairs().size();

        if (dirComparison.childBookInfoPairs().size() == 0) {
            str.append(indent + "    - ").append(rb.getString("AppTaskBase.160")).append(BR);
            updateMessage(str.toString());
        }

        for (int i = 0; i < dirComparison.childBookInfoPairs().size(); i++) {
            int ii = i;

            Pair<BookInfo> bookInfoPair = dirComparison.childBookInfoPairs().get(i);

            str.append(indent
                    + DirResult.formatBookNamesPair(dirId, Integer.toString(i + 1), bookInfoPair));
            updateMessage(str.toString());

            if (bookInfoPair.isPaired()
                    && dirComparison.childBookComparisons().get(bookInfoPair).isPresent()) {

                Pair<Path> srcPathPair = bookInfoPair.map(BookInfo::bookPath);
                Pair<Path> dstPathPair = Side.map(side -> outputDirPair.get(side).resolve(
                        "【%s%s-%d】%s".formatted(side, dirId, ii + 1, bookInfoPair.get(side).bookName())));

                BookResult bookResult = compareBooks(
                        dirComparison.childBookComparisons().get(bookInfoPair).get(),
                        srcPathPair,
                        dstPathPair,
                        getProgress.applyAsInt(i),
                        getProgress.applyAsInt(i + 1));
                bookResults.put(bookInfoPair, Optional.ofNullable(bookResult));

                if (bookResult != null) {
                    paintBook(
                            srcPathPair,
                            dstPathPair,
                            bookResult,
                            getProgress.applyAsInt(i + 1));
                }

            } else {
                if (bookInfoPair.isPaired()) {
                    str.append("  -  ").append(rb.getString("AppTaskBase.150")).append(BR);
                    updateMessage(str.toString());
                } else {
                    str.append(BR);
                    updateMessage(str.toString());
                }
                if (bookInfoPair.hasA()) {
                    Path srcBookPath = bookInfoPair.a().bookPath();
                    Path dstBookPath = outputDirPair.a().resolve(
                            "【A%s-%d】%s".formatted(dirId, i + 1, bookInfoPair.a().bookName()));
                    skipUnpairedBook(Side.A, srcBookPath, dstBookPath);
                }
                if (bookInfoPair.hasB()) {
                    Path srcBookPath = bookInfoPair.b().bookPath();
                    Path dstBookPath = outputDirPair.b().resolve(
                            "【B%s-%d】%s".formatted(dirId, i + 1, bookInfoPair.b().bookName()));
                    skipUnpairedBook(Side.B, srcBookPath, dstBookPath);
                }
                bookResults.put(bookInfoPair, Optional.empty());
            }
        }
        str.append(BR);
        updateMessage(str.toString());

        return new DirResult(dirComparison, bookResults, dirId);
    }

    private BookResult compareBooks(
            PairingInfoBooks bookComparison,
            Pair<Path> srcPathPair,
            Pair<Path> dstPathPair,
            int progressBefore,
            int progressAfter) {

        try {
            return compareBooks(
                    bookComparison,
                    progressBefore,
                    progressAfter);

        } catch (Exception e) {
            str.append("  -  ").append(rb.getString("AppTaskBase.150")).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();

            Side.forEach(side -> {
                try {
                    Files.copy(srcPathPair.get(side), dstPathPair.get(side));
                } catch (IOException e1) {
                    // nop
                }
            });
            return null;
        }
    }

    private void paintBook(
            Pair<Path> srcPathPair,
            Pair<Path> dstPathPair,
            BookResult bookResult,
            int progressAfter) {

        try {
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);

            for (Side side : Side.values()) {
                Path srcPath = srcPathPair.get(side);
                Path dstPath = dstPathPair.get(side);
                BookPainter painter = Factory.painter(settings, srcPath, readPasswords.get(srcPath));

                painter.paintAndSave(
                        srcPath,
                        dstPath,
                        readPasswords.get(srcPath),
                        bookResult.getPiece(side));
            }

            str.append("  -  ").append(bookResult.getDiffSimpleSummary()).append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);

        } catch (Exception e) {
            str.append("  -  ").append(rb.getString("AppTaskBase.150")).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
        }
    }

    private void skipUnpairedBook(Side side, Path srcBookPath, Path dstBookPath) {
        try {
            Files.copy(srcBookPath, dstBookPath);
            dstBookPath.toFile().setReadable(true, false);
            dstBookPath.toFile().setWritable(true, false);

        } catch (Exception e) {
            str.append("  -  ").append(rb.getString("AppTaskBase.150")).append(BR);
            updateMessage(str.toString());
            e.printStackTrace();
        }
    }
}
