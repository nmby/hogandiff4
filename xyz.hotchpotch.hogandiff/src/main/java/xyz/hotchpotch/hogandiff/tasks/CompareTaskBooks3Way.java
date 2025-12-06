package xyz.hotchpotch.hogandiff.tasks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.ApplicationException;
import xyz.hotchpotch.hogandiff.ErrorReporter;
import xyz.hotchpotch.hogandiff.Msg;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.logic.BookInfo;
import xyz.hotchpotch.hogandiff.logic.CellData;
import xyz.hotchpotch.hogandiff.logic.CellsLoader;
import xyz.hotchpotch.hogandiff.logic.ComparatorOfSheets;
import xyz.hotchpotch.hogandiff.logic.Factory;
import xyz.hotchpotch.hogandiff.logic.PairingInfoBooks;
import xyz.hotchpotch.hogandiff.logic.Result;
import xyz.hotchpotch.hogandiff.logic.ResultOfBooks;
import xyz.hotchpotch.hogandiff.logic.ResultOfSheets;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Triple;
import xyz.hotchpotch.hogandiff.util.Triple.Side3;

/**
 * Excelブック同士の3-way比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @author nmby
 */
public final class CompareTaskBooks3Way extends CompareTask {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private List<Triple<String>> sheetNameTriples;
    
    /**
     * コンストラクタ
     * 
     * @param settings
     *            設定セット
     */
    public CompareTaskBooks3Way(Settings settings) {
        super(settings);
    }
    
    @Override
    protected Result call2() throws ApplicationException {
        try {
            sheetNameTriples = arrangeSheetNameTriples();
            
            // 0. 処理開始のアナウンス
            announceStart(0, 2);
            
            // 1. シート同士の比較
            ResultOfBooks bResultOA = compareSheets(2, 37, Side3.A);
            ResultOfBooks bResultOB = compareSheets(2, 72, Side3.B);
            
            Exception failed = null;
            
//            // 2. 差分箇所への着色と表示
//            try {
//                PairingInfoBooks pairingInfoBooks = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO_AB);
//                paintSaveAndShowBook(workDir, pairingInfoBooks.parentBookInfoPair(), bResult, 75, 95);
//            } catch (Exception e) {
//                failed = e;
//            }
//            
//            // 3. 比較結果レポート（Excelブック）の保存と表示
//            try {
//                createSaveAndShowReportBook(workDir, bResult, 95, 98);
//            } catch (Exception e) {
//                if (failed == null) {
//                    failed = e;
//                } else {
//                    failed.addSuppressed(e);
//                }
//            }
//            
//            // 4. 比較結果レポート（テキスト）の保存
//            try {
//                saveAndShowResultText(workDir, bResult.toString(), 98, 99);
//            } catch (Exception e) {
//                if (failed == null) {
//                    failed = e;
//                } else {
//                    failed.addSuppressed(e);
//                }
//            }
//            
//            // 5. 処理終了のアナウンス
//            announceEnd();
//            
//            if (failed != null) {
//                throw failed;
//            }
//            
//            return bResult;
            return null;
            
        } catch (Exception e) {
            throw getApplicationException(e, Msg.APP_0150.get() + " at CompareBooksTask::call2");
        }
    }
    
    // ■ タスクステップ ■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■■
    
    private List<Triple<String>> arrangeSheetNameTriples() {
        PairingInfoBooks pairingInfoBooksOA = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO_OA);
        PairingInfoBooks pairingInfoBooksOB = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO_OB);
        
        Map<String, String> sheetNameOtoB = new HashMap<>(
                pairingInfoBooksOB.childSheetNamePairs().stream()
                        .filter(Pair::isPaired)
                        .collect(Collectors.toMap(Pair::a, Pair::b)));
        
        Set<String> sheetNameOnlyB = new HashSet<>(pairingInfoBooksOB.childSheetNamePairs().stream()
                .filter(pair -> !pair.hasA())
                .map(Pair::b)
                .collect(Collectors.toSet()));
        
        List<Triple<String>> sheetNamesTriples = new ArrayList<>();
        
        pairingInfoBooksOA.childSheetNamePairs().forEach(sheetNamePairOA -> {
            if (sheetNamePairOA.hasA()) {
                String sheetNameO = sheetNamePairOA.a();
                String sheetNameA = sheetNamePairOA.b();
                String sheetNameB = sheetNameOtoB.remove(sheetNameO);
                sheetNamesTriples.add(new Triple<>(sheetNameO, sheetNameA, sheetNameB));
            } else {
                String sheetNameA = sheetNamePairOA.b();
                String sheetNameB = sheetNameOnlyB.remove(sheetNameA) ? sheetNameA : null;
                sheetNamesTriples.add(new Triple<>(null, sheetNameA, sheetNameB));
            }
        });
        sheetNameOtoB.forEach((sheetNameO, sheetNameB) -> {
            sheetNamesTriples.add(new Triple<>(sheetNameO, null, sheetNameB));
        });
        sheetNameOnlyB.forEach(sheetNameB -> {
            sheetNamesTriples.add(new Triple<>(null, null, sheetNameB));
        });
        
        sheetNamesTriples.sort((t1, t2) -> {
            if (t1.has(Side3.O) == t2.has(Side3.O)
                    && t1.has(Side3.A) == t2.has(Side3.A)
                    && t1.has(Side3.B) == t2.has(Side3.B)) {
                return 0;
            }
            if (t1.hasAll() != t2.hasAll()) {
                return t1.hasAll() ? -1 : 1;
            }
            if (t1.has(Side3.O) != t2.has(Side3.O)) {
                return t1.has(Side3.O) ? -1 : 1;
            }
            int num1 = (t1.has(Side3.O) ? 1 : 0) + (t1.has(Side3.A) ? 1 : 0) + (t1.has(Side3.B) ? 1 : 0);
            int num2 = (t2.has(Side3.O) ? 1 : 0) + (t2.has(Side3.A) ? 1 : 0) + (t2.has(Side3.B) ? 1 : 0);
            if (num1 != num2) {
                return Integer.compare(num2, num1);
            }
            if (t1.has(Side3.A) != t2.has(Side3.A)) {
                return t1.has(Side3.A) ? -1 : 1;
            }
            return 0;
        });
        
        return sheetNamesTriples;
    }
    
    // 0. 処理開始のアナウンス
    private void announceStart(
            int progressBefore,
            int progressAfter)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            
            PairingInfoBooks pairingInfoBooksOA = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO_OA);
            PairingInfoBooks pairingInfoBooksOB = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO_OB);
            
            str.append("%s%n[O] %s%n[A] %s%n[B] %s%n".formatted(
                    Msg.APP_0170.get(),
                    pairingInfoBooksOA.parentBookInfoPair().a(),
                    pairingInfoBooksOA.parentBookInfoPair().b(),
                    pairingInfoBooksOB.parentBookInfoPair().a()));
            
            for (int i = 0; i < sheetNameTriples.size(); i++) {
                Triple<String> sheetNameTriple = sheetNameTriples.get(i);
                str.append("    %d) %s  vs  %s  vs  %s".formatted(
                        i + 1,
                        sheetNameTriple.has(Side3.A) ? "A[ " + sheetNameTriple.a() + " ]" : Msg.APP_0400.get(),
                        sheetNameTriple.has(Side3.O) ? "O[ " + sheetNameTriple.o() + " ]" : Msg.APP_0400.get(),
                        sheetNameTriple.has(Side3.B) ? "B[ " + sheetNameTriple.b() + " ]" : Msg.APP_0400.get()))
                        .append(BR);
            }
            
            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
        } catch (Exception e) {
            throw getApplicationException(e, Msg.APP_0150.get() + " at CompareTaskBooks3Way::announceStart");
        }
    }
    
    // 1. シート同士の比較
    private ResultOfBooks compareSheets(
            int progressBefore,
            int progressAfter,
            Side3 side3)
            throws ApplicationException {
        
        try {
            updateProgress(progressBefore, PROGRESS_MAX);
            str.append(Msg.APP_0181.get().formatted(side3)).append(BR);
            updateMessage(str.toString());
            
            PairingInfoBooks pairingInfoBooks = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFOS.get(side3));
            Pair<BookInfo> bookInfoPair = pairingInfoBooks.parentBookInfoPair();
            Pair<CellsLoader> loaderPair = bookInfoPair
                    .unsafeMap(bookInfo -> Factory.cellsLoader(settings, bookInfo));
            
            ComparatorOfSheets sheetComparator = Factory.sheetComparator(settings);
            Map<Path, String> readPasswords = settings.get(SettingKeys.CURR_READ_PASSWORDS);
            Map<Pair<String>, Optional<ResultOfSheets>> results = new HashMap<>();
            
            double progressDelta = (progressAfter - progressBefore)
                    / (double) pairingInfoBooks.childSheetNamePairs().size();
            
            for (int i = 0; i < pairingInfoBooks.childSheetNamePairs().size(); i++) {
                Pair<String> sheetNamePair = pairingInfoBooks.childSheetNamePairs().get(i);
                ResultOfSheets result = null;
                
                try {
                    if (sheetNamePair.isPaired()) {
                        str.append(ResultOfBooks.formatSheetNamesPair(Integer.toString(i + 1), sheetNamePair));
                        updateMessage(str.toString());
                        
                        Pair<Set<CellData>> cellsSetPair = Side.unsafeMap(
                                side -> loaderPair.get(side).loadCells(
                                        bookInfoPair.get(side),
                                        readPasswords.get(bookInfoPair.get(side).bookPath()),
                                        sheetNamePair.get(side)));
                        
                        result = sheetComparator.compare(cellsSetPair);
                        
                        str.append("  -  ").append(result.getDiffSummary()).append(BR);
                        updateMessage(str.toString());
                    }
                } catch (Exception e) {
                    str.append("  -  ").append(Msg.APP_0120.get()).append(BR);
                    ErrorReporter.reportIfEnabled(e, "CompareTaskBooks3Way::compareSheets-1");
                }
                
                results.put(sheetNamePair, Optional.ofNullable(result));
                updateProgress(progressBefore + progressDelta * (i + 1), PROGRESS_MAX);
            }
            
            str.append(BR);
            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);
            
            return new ResultOfBooks(pairingInfoBooks, results);
            
        } catch (Exception e) {
            throw getApplicationException(e, Msg.APP_0190.get());
        }
    }
}
