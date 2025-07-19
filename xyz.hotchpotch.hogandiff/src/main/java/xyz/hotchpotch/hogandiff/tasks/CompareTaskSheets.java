package xyz.hotchpotch.hogandiff.tasks;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import xyz.hotchpotch.hogandiff.ApplicationException;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.logic.Factory;
import xyz.hotchpotch.hogandiff.logic.comparators.ComparatorOfSheets;
import xyz.hotchpotch.hogandiff.logic.cellsloader.CellsLoader;
import xyz.hotchpotch.hogandiff.logic.models.CellData;
import xyz.hotchpotch.hogandiff.logic.models.Result;
import xyz.hotchpotch.hogandiff.logic.models.ResultOfBooks;
import xyz.hotchpotch.hogandiff.logic.models.ResultOfSheets;
import xyz.hotchpotch.hogandiff.logic.models.BookInfo;
import xyz.hotchpotch.hogandiff.logic.models.PairingInfoBooks;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * Excelシート同士の比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @author nmby
 */
public final class CompareTaskSheets extends CompareTask {

    // [static members] ********************************************************

    // [instance members] ******************************************************

    /**
     * コンストラクタ
     * 
     * @param settings 設定セット
     */
    public CompareTaskSheets(Settings settings) {
        super(settings);
    }

    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 0);

            // 1. シート同士の比較
            ResultOfBooks bResult = compareSheets(0, 75);

            Exception failed = null;

            // 2. 差分箇所への着色と表示
            try {
                PairingInfoBooks pairingInfoBooks = settings.get(SettingKeys.CURR_SHEET_COMPARE_INFO);
                paintSaveAndShowBook(workDir, pairingInfoBooks.parentBookInfoPair()
                        .map(BookInfo::bookPath), bResult, 75, 95);
            } catch (Exception e) {
                failed = e;
            }

            // 3. 比較結果レポート（Excelブック）の保存と表示
            try {
                createSaveAndShowReportBook(workDir, bResult, 95, 98);
            } catch (Exception e) {
                if (failed == null) {
                    failed = e;
                } else {
                    failed.addSuppressed(e);
                }
            }

            // 4. 比較結果レポート（テキスト）の保存
            try {
                saveResultText(workDir, bResult.toString(), 98, 99);
            } catch (Exception e) {
                if (failed == null) {
                    failed = e;
                } else {
                    failed.addSuppressed(e);
                }
            }

            // 5. 処理終了のアナウンス
            announceEnd();

            if (failed != null) {
                throw failed;
            }

            return bResult;

        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareSheetsTask::call2");
        }
    }

    // ■ タスクステップ ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■

    // 0. 処理開始のアナウンス
    private void announceStart(
            int progressBefore,
            int progressAfter)
            throws ApplicationException {

        try {
            updateProgress(progressBefore, PROGRESS_MAX);

            PairingInfoBooks pairingInfoBooks = settings.get(SettingKeys.CURR_SHEET_COMPARE_INFO);
            Pair<Path> bookPathPair = pairingInfoBooks.parentBookInfoPair().map(BookInfo::bookPath);
            Pair<String> sheetNamePair = pairingInfoBooks.childSheetNamePairs().get(0);

            str.append(rb.getString("CompareSheetsTask.010")).append(BR);
            str.append(isSameBook()
                    ? "%s%n[A] %s%n[B] %s%n%n".formatted(
                            bookPathPair.a(), sheetNamePair.a(), sheetNamePair.b())
                    : "[A] %s - %s%n[B] %s - %s%n%n".formatted(
                            bookPathPair.a(), sheetNamePair.a(), bookPathPair.b(), sheetNamePair.b()));

            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);

        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareSheetsTask::announceStart");
        }
    }

    // 2. シート同士の比較
    private ResultOfBooks compareSheets(
            int progressBefore,
            int progressAfter)
            throws ApplicationException {

        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareSheetsTask.020")).append(BR);
            updateMessage(str.toString());

            PairingInfoBooks pairingInfoBooks = settings.get(SettingKeys.CURR_SHEET_COMPARE_INFO);
            Pair<BookInfo> bookInfoPair = pairingInfoBooks.parentBookInfoPair();
            Pair<String> sheetNamePair = pairingInfoBooks.childSheetNamePairs().get(0);

            Pair<CellsLoader> loaderPair = bookInfoPair.unsafeMap(
                    bookInfo -> Factory.cellsLoader(settings, bookInfo));

            str.append(ResultOfBooks.formatSheetNamesPair("1", sheetNamePair));
            updateMessage(str.toString());

            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
            Pair<Set<CellData>> cellsSetPair = Side.unsafeMap(
                    side -> loaderPair.get(side).loadCells(
                            bookInfoPair.get(side),
                            readPasswords.get(bookInfoPair.get(side).bookPath()),
                            sheetNamePair.get(side)));

            ComparatorOfSheets comparator = Factory.sheetComparator(settings);
            ResultOfSheets result = comparator.compare(cellsSetPair);

            str.append("  -  ").append(result.getDiffSummary()).append(BR).append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);

            return new ResultOfBooks(
                    pairingInfoBooks,
                    Map.of(sheetNamePair, Optional.of(result)));

        } catch (Exception e) {
            throw getApplicationException(e, "CompareSheetsTask.030", "");
        }
    }
}
