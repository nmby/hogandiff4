package xyz.hotchpotch.hogandiff.task;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import xyz.hotchpotch.hogandiff.ApplicationException;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
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
public final class CompareTaskBooks extends CompareTask {

    // [static members] ********************************************************

    // [instance members] ******************************************************

    /**
     * コンストラクタ
     * 
     * @param settings 設定セット
     */
    public CompareTaskBooks(Settings settings) {
        super(settings);
    }

    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 3);

            // 1. シート同士の比較
            BookResult bResult = compareSheets(3, 75);

            Exception failed = null;

            // 2. 差分箇所への着色と表示
            try {
                PairingInfoBooks pairingInfoBooks = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO);
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
            throw getApplicationException(e, "AppTaskBase.180", " at CompareBooksTask::call2");
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

            PairingInfoBooks pairingInfoBooks = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO);
            Pair<Path> bookPathPair = pairingInfoBooks.parentBookInfoPair().map(BookInfo::bookPath);

            str.append("%s%n[A] %s%n[B] %s%n"
                    .formatted(rb.getString("CompareBooksTask.010"), bookPathPair.a(), bookPathPair.b()));

            for (int i = 0; i < pairingInfoBooks.childSheetNamePairs().size(); i++) {
                Pair<String> sheetNamePair = pairingInfoBooks.childSheetNamePairs().get(i);
                str.append(BookResult.formatSheetNamesPair(Integer.toString(i + 1), sheetNamePair)).append(BR);
            }

            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);

        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareBooksTask::announceStart");
        }
    }

    // 1. シート同士の比較
    private BookResult compareSheets(
            int progressBefore,
            int progressAfter)
            throws ApplicationException {

        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(rb.getString("CompareBooksTask.040")).append(BR);
            updateMessage(str.toString());

            PairingInfoBooks pairingInfoBooks = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO);
            Pair<BookInfo> bookInfoPair = pairingInfoBooks.parentBookInfoPair();
            Pair<CellsLoader> loaderPair = bookInfoPair
                    .map(BookInfo::bookPath)
                    .unsafeMap(bookPath -> Factory.cellsLoader(settings, bookPath));

            SheetComparator sheetComparator = Factory.sheetComparator(settings);
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
            Map<Pair<String>, Optional<SheetResult>> results = new HashMap<>();

            double progressDelta = (progressAfter - progressBefore)
                    / (double) pairingInfoBooks.childSheetNamePairs().size();

            for (int i = 0; i < pairingInfoBooks.childSheetNamePairs().size(); i++) {
                Pair<String> sheetNamePair = pairingInfoBooks.childSheetNamePairs().get(i);
                SheetResult result = null;

                try {
                    if (sheetNamePair.isPaired()) {
                        str.append(BookResult.formatSheetNamesPair(Integer.toString(i + 1), sheetNamePair));
                        updateMessage(str.toString());

                        Pair<Set<CellData>> cellsSetPair = Side.unsafeMap(
                                side -> loaderPair.get(side).loadCells(
                                        bookInfoPair.get(side).bookPath(),
                                        readPasswords.get(bookInfoPair.get(side).bookPath()),
                                        sheetNamePair.get(side)));

                        result = sheetComparator.compare(cellsSetPair);

                        str.append("  -  ").append(result.getDiffSummary()).append(BR);
                        updateMessage(str.toString());
                    }
                } catch (Exception e) {
                    str.append("  -  ").append(rb.getString("AppTaskBase.150")).append(BR);
                }

                results.put(sheetNamePair, Optional.ofNullable(result));
                updateProgress(progressBefore + progressDelta * (i + 1), PROGRESS_MAX);
            }

            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);

            return new BookResult(pairingInfoBooks, results);

        } catch (Exception e) {
            throw getApplicationException(e, "CompareBooksTask.050", "");
        }
    }
}
