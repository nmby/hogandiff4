package xyz.hotchpotch.hogandiff.tasks;

import java.awt.Color;
import java.awt.Desktop;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import xyz.hotchpotch.hogandiff.ApplicationException;
import xyz.hotchpotch.hogandiff.ErrorReporter;
import xyz.hotchpotch.hogandiff.Msg;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.logic.BookInfo;
import xyz.hotchpotch.hogandiff.logic.CellData;
import xyz.hotchpotch.hogandiff.logic.CellsLoader;
import xyz.hotchpotch.hogandiff.logic.ComparatorOfSheets;
import xyz.hotchpotch.hogandiff.logic.Factory;
import xyz.hotchpotch.hogandiff.logic.PairingInfoBooksTriple;
import xyz.hotchpotch.hogandiff.logic.Painter;
import xyz.hotchpotch.hogandiff.logic.Result;
import xyz.hotchpotch.hogandiff.logic.ResultOfBooksTriple;
import xyz.hotchpotch.hogandiff.logic.ResultOfSheets;
import xyz.hotchpotch.hogandiff.logic.ResultOfSheetsTriple;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Triple;
import xyz.hotchpotch.hogandiff.util.Triple.Side;

/**
 * Excelブックの 3-way 比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 *
 * @author nmby
 */
public final class CompareTaskBooksTriple extends CompareTask {

    // [static members] ********************************************************

    // [instance members] ******************************************************

    /**
     * コンストラクタ
     *
     * @param settings 設定セット
     */
    public CompareTaskBooksTriple(Settings settings) {
        super(settings);
    }

    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 3);

            // 1. シート同士の比較
            ResultOfBooksTriple bResult = compareSheets(3, 75);

            Exception failed = null;

            // 2. 差分箇所への着色と表示
            try {
                paintSaveAndShowBooks(bResult, 75, 95);
            } catch (Exception e) {
                failed = e;
            }

            // 3. 比較結果レポート（テキスト）の保存
            try {
                saveResultText(workDir, bResult.toString(), 95, 98);
            } catch (Exception e) {
                if (failed == null) {
                    failed = e;
                } else {
                    failed.addSuppressed(e);
                }
            }

            // 4. 処理終了のアナウンス
            announceEnd();

            if (failed != null) {
                throw failed;
            }

            return bResult;

        } catch (Exception e) {
            throw getApplicationException(e, Msg.APP_0150.get() + " at CompareTaskBooksTriple::call2");
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

            PairingInfoBooksTriple pairingInfo = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO_TRIPLE);
            Triple<String> dispPathTriple = pairingInfo.parentBookInfoTriple().map(BookInfo::dispPathInfo);

            str.append("%s%n[O] %s%n[A] %s%n[B] %s%n"
                    .formatted(Msg.APP_0170.get(),
                            dispPathTriple.o(), dispPathTriple.a(), dispPathTriple.b()));

            for (int i = 0; i < pairingInfo.childSheetNameTriples().size(); i++) {
                Triple<String> sheetNameTriple = pairingInfo.childSheetNameTriples().get(i);
                str.append(ResultOfBooksTriple.formatSheetNamesTriple(
                        Integer.toString(i + 1), sheetNameTriple)).append(BR);
            }

            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);

        } catch (Exception e) {
            throw getApplicationException(e, Msg.APP_0150.get() + " at CompareTaskBooksTriple::announceStart");
        }
    }

    // 1. シート同士の比較
    private ResultOfBooksTriple compareSheets(
            int progressBefore,
            int progressAfter)
            throws ApplicationException {

        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(Msg.APP_0180.get()).append(BR);
            updateMessage(str.toString());

            PairingInfoBooksTriple pairingInfo = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO_TRIPLE);
            Triple<BookInfo> bookInfoTriple = pairingInfo.parentBookInfoTriple();

            Triple<CellsLoader> loaderTriple = bookInfoTriple
                    .unsafeMap(bookInfo -> Factory.cellsLoader(settings, bookInfo));

            ComparatorOfSheets sheetComparator = Factory.sheetComparator(settings);
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
            Map<Triple<String>, Optional<ResultOfSheetsTriple>> results = new HashMap<>();

            double progressDelta = (progressAfter - progressBefore)
                    / (double) pairingInfo.childSheetNameTriples().size();

            for (int i = 0; i < pairingInfo.childSheetNameTriples().size(); i++) {
                Triple<String> sheetNameTriple = pairingInfo.childSheetNameTriples().get(i);
                ResultOfSheetsTriple result = null;

                try {
                    if (sheetNameTriple.isPaired()) {
                        str.append(ResultOfBooksTriple.formatSheetNamesTriple(
                                Integer.toString(i + 1), sheetNameTriple));
                        updateMessage(str.toString());

                        // O vs A の比較
                        BookInfo bookInfoO = bookInfoTriple.o();
                        BookInfo bookInfoA = bookInfoTriple.a();
                        BookInfo bookInfoB = bookInfoTriple.b();

                        Set<CellData> cellsO_forA = loaderTriple.o().loadCells(
                                bookInfoO,
                                readPasswords.get(bookInfoO.bookPath()),
                                sheetNameTriple.o());
                        Set<CellData> cellsA = loaderTriple.a().loadCells(
                                bookInfoA,
                                readPasswords.get(bookInfoA.bookPath()),
                                sheetNameTriple.a());
                        ResultOfSheets resultOA = sheetComparator.compare(Pair.of(cellsO_forA, cellsA));

                        // O vs B の比較
                        Set<CellData> cellsO_forB = loaderTriple.o().loadCells(
                                bookInfoO,
                                readPasswords.get(bookInfoO.bookPath()),
                                sheetNameTriple.o());
                        Set<CellData> cellsB = loaderTriple.b().loadCells(
                                bookInfoB,
                                readPasswords.get(bookInfoB.bookPath()),
                                sheetNameTriple.b());
                        ResultOfSheets resultOB = sheetComparator.compare(Pair.of(cellsO_forB, cellsB));

                        result = new ResultOfSheetsTriple(resultOA, resultOB);

                        str.append("  -  ").append(result.getDiffSummary()).append(BR);
                        updateMessage(str.toString());
                    }
                } catch (Exception e) {
                    str.append("  -  ").append(Msg.APP_0120.get()).append(BR);
                    ErrorReporter.reportIfEnabled(e, "CompareTaskBooksTriple::compareSheets-1");
                }

                results.put(sheetNameTriple, Optional.ofNullable(result));
                updateProgress(progressBefore + progressDelta * (i + 1), PROGRESS_MAX);
            }

            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);

            return new ResultOfBooksTriple(pairingInfo, results);

        } catch (Exception e) {
            throw getApplicationException(e, Msg.APP_0190.get());
        }
    }

    // 2. 差分箇所への着色と表示
    private void paintSaveAndShowBooks(
            ResultOfBooksTriple bResult,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {

        ApplicationException thrown = null;

        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(Msg.APP_0050.get()).append(BR);
            updateMessage(str.toString());

            PairingInfoBooksTriple pairingInfo = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO_TRIPLE);
            Triple<BookInfo> bookInfoTriple = pairingInfo.parentBookInfoTriple();
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);

            // 出力ファイルパス
            Path dstA = workDir.resolve(
                    "【A】" + bookInfoTriple.a().bookNameWithExtension());
            Path dstB = workDir.resolve(
                    "【B】" + bookInfoTriple.b().bookNameWithExtension());
            Path dstO = workDir.resolve(
                    "【O】" + bookInfoTriple.o().bookNameWithExtension());
            Path dstOTmp1 = workDir.resolve(
                    "【O_tmp1】" + bookInfoTriple.o().bookNameWithExtension());
            Path dstOTmp2 = workDir.resolve(
                    "【O_tmp2】" + bookInfoTriple.o().bookNameWithExtension());

            Path srcA = bookInfoTriple.a().bookPath();
            Path srcB = bookInfoTriple.b().bookPath();
            Path srcO = bookInfoTriple.o().bookPath();
            String pwdA = readPasswords.get(srcA);
            String pwdB = readPasswords.get(srcB);
            String pwdO = readPasswords.get(srcO);

            // A ファイル着色（既存の DIFF_COLOR を使用）
            try {
                str.append("    - %s%n".formatted(dstA));
                updateMessage(str.toString());

                Painter painterA = Factory.painter(settings, dstA, pwdA);
                painterA.paintAndSave(srcA, dstA, pwdA, bResult.getPieceForA());
                updateProgress(
                        progressBefore + (progressAfter - progressBefore) * 1 / 5,
                        PROGRESS_MAX);
            } catch (Exception e) {
                ApplicationException ee = getApplicationException(e,
                        Msg.APP_0090.get().formatted("A"));
                if (thrown == null) {
                    thrown = ee;
                } else {
                    thrown.addSuppressed(ee);
                }
            }

            // B ファイル着色（既存の DIFF_COLOR を使用）
            try {
                str.append("    - %s%n".formatted(dstB));
                updateMessage(str.toString());

                Painter painterB = Factory.painter(settings, dstB, pwdB);
                painterB.paintAndSave(srcB, dstB, pwdB, bResult.getPieceForB());
                updateProgress(
                        progressBefore + (progressAfter - progressBefore) * 2 / 5,
                        PROGRESS_MAX);
            } catch (Exception e) {
                ApplicationException ee = getApplicationException(e,
                        Msg.APP_0090.get().formatted("B"));
                if (thrown == null) {
                    thrown = ee;
                } else {
                    thrown.addSuppressed(ee);
                }
            }

            // O ファイル着色（3 パス）
            try {
                str.append("    - %s%n".formatted(dstO));
                updateMessage(str.toString());

                Color redundantCommentColor = settings.get(SettingKeys.REDUNDANT_COMMENT_COLOR);
                Color diffCommentColor = settings.get(SettingKeys.DIFF_COMMENT_COLOR);
                String redundantCommentHex = SettingKeys.REDUNDANT_COMMENT_COLOR.encoder()
                        .apply(redundantCommentColor);
                String diffCommentHex = SettingKeys.DIFF_COMMENT_COLOR.encoder()
                        .apply(diffCommentColor);
                Color redundantSheetColor = settings.get(SettingKeys.REDUNDANT_SHEET_COLOR);
                Color diffSheetColor = settings.get(SettingKeys.DIFF_SHEET_COLOR);
                Color sameSheetColor = settings.get(SettingKeys.SAME_SHEET_COLOR);

                // パス1: A-only 差分を赤系（THREE_WAY_DIFF_COLOR_A）で着色
                short colorA = settings.get(SettingKeys.THREE_WAY_DIFF_COLOR_A);
                Painter painterOA = Painter.of(
                        dstOTmp1, pwdO,
                        colorA, colorA,
                        redundantCommentColor, diffCommentColor,
                        redundantCommentHex, diffCommentHex,
                        redundantSheetColor, diffSheetColor, sameSheetColor);
                painterOA.paintAndSave(srcO, dstOTmp1, pwdO, bResult.getPieceForOriginAOnly());

                updateProgress(
                        progressBefore + (progressAfter - progressBefore) * 3 / 5,
                        PROGRESS_MAX);

                // パス2: B-only 差分を青系（THREE_WAY_DIFF_COLOR_B）で着色
                short colorB = settings.get(SettingKeys.THREE_WAY_DIFF_COLOR_B);
                Painter painterOB = Painter.of(
                        dstOTmp2, pwdO,
                        colorB, colorB,
                        redundantCommentColor, diffCommentColor,
                        redundantCommentHex, diffCommentHex,
                        redundantSheetColor, diffSheetColor, sameSheetColor);
                painterOB.paintAndSave(dstOTmp1, dstOTmp2, pwdO, bResult.getPieceForOriginBOnly());

                updateProgress(
                        progressBefore + (progressAfter - progressBefore) * 4 / 5,
                        PROGRESS_MAX);

                // パス3: 競合差分を紫系（THREE_WAY_DIFF_COLOR_CONFLICT）で着色
                short colorConflict = settings.get(SettingKeys.THREE_WAY_DIFF_COLOR_CONFLICT);
                Painter painterOConflict = Painter.of(
                        dstO, pwdO,
                        colorConflict, colorConflict,
                        redundantCommentColor, diffCommentColor,
                        redundantCommentHex, diffCommentHex,
                        redundantSheetColor, diffSheetColor, sameSheetColor);
                painterOConflict.paintAndSave(dstOTmp2, dstO, pwdO, bResult.getPieceForOriginConflict());

                // 中間ファイルを削除
                try {
                    Files.deleteIfExists(dstOTmp1);
                } catch (Exception e) {
                    ErrorReporter.reportIfEnabled(e, "CompareTaskBooksTriple::paintSaveAndShowBooks-tmp1");
                }
                try {
                    Files.deleteIfExists(dstOTmp2);
                } catch (Exception e) {
                    ErrorReporter.reportIfEnabled(e, "CompareTaskBooksTriple::paintSaveAndShowBooks-tmp2");
                }

            } catch (Exception e) {
                ApplicationException ee = getApplicationException(e,
                        Msg.APP_0090.get().formatted("O"));
                if (thrown == null) {
                    thrown = ee;
                } else {
                    thrown.addSuppressed(ee);
                }
            }

            // 着色済みファイルを表示
            try {
                if (settings.get(SettingKeys.SHOW_PAINTED_SHEETS)) {
                    str.append(BR).append(Msg.APP_0070.get()).append(BR).append(BR);
                    updateMessage(str.toString());
                    Desktop.getDesktop().open(dstA.toFile());
                    Desktop.getDesktop().open(dstB.toFile());
                    Desktop.getDesktop().open(dstO.toFile());
                } else {
                    str.append(BR);
                }
            } catch (Exception e) {
                ApplicationException ee = getApplicationException(e, Msg.APP_0080.get());
                if (thrown == null) {
                    thrown = ee;
                } else {
                    thrown.addSuppressed(ee);
                }
            }

            updateProgress(progressAfter, PROGRESS_MAX);

        } catch (Exception e) {
            ApplicationException ee = getApplicationException(e, Msg.APP_0060.get());
            if (thrown == null) {
                thrown = ee;
            } else {
                thrown.addSuppressed(ee);
            }
        }

        if (thrown != null) {
            throw thrown;
        }
    }
}
