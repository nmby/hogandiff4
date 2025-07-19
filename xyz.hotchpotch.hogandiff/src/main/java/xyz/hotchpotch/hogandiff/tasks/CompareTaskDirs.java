package xyz.hotchpotch.hogandiff.tasks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.ApplicationException;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.logic.models.Result;
import xyz.hotchpotch.hogandiff.logic.models.ResultOfTrees;
import xyz.hotchpotch.hogandiff.logic.models.ResultOtDirs;
import xyz.hotchpotch.hogandiff.logic.models.BookInfo;
import xyz.hotchpotch.hogandiff.logic.models.DirInfo;
import xyz.hotchpotch.hogandiff.logic.models.PairingInfoDirs;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * フォルダ同士の比較処理を実行するためのタスクです。<br>
 * <br>
 * <strong>注意：</strong><br>
 * このタスクは、いわゆるワンショットです。
 * 同一インスタンスのタスクを複数回実行しないでください。<br>
 * 
 * @author nmby
 */
public final class CompareTaskDirs extends CompareTask {

    // [static members] ********************************************************

    // [instance members] ******************************************************

    /**
     * コンストラクタ
     * 
     * @param settings 設定セット
     */
    public CompareTaskDirs(Settings settings) {
        super(settings);
    }

    @Override
    protected Result call2() throws ApplicationException {
        try {
            // 0. 処理開始のアナウンス
            announceStart(0, 5);

            // 1. 出力用ディレクトリの作成
            Pair<Path> outputDirPair = createOutputDirs(workDir);

            // 2. フォルダ同士の比較
            ResultOtDirs dResult = compareDirs(outputDirPair, 5, 93);

            PairingInfoDirs pairingInfoDirs = settings.get(SettingKeys.CURR_DIR_COMPARE_INFO);
            Pair<DirInfo> dirInfoPair = pairingInfoDirs.parentDirInfoPair();
            ResultOfTrees tResult = new ResultOfTrees(
                    pairingInfoDirs.flatten(),
                    Map.of(dirInfoPair, Optional.of(dResult)));

            Exception failed = null;

            // 3. 比較結果レポート（Excelブック）の保存と表示
            try {
                createSaveAndShowResultBook(workDir, tResult, 93, 97);
            } catch (Exception e) {
                failed = e;
            }

            // 4. 比較結果レポート（テキスト）の保存
            try {
                saveResultText(workDir, dResult.toString(), 97, 99);
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

            return tResult;

        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareDirsTask::call2");
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

            PairingInfoDirs pairingInfoDirs = settings.get(SettingKeys.CURR_DIR_COMPARE_INFO);
            Pair<DirInfo> dirInfoPair = pairingInfoDirs.parentDirInfoPair();
            List<Pair<BookInfo>> bookInfoPairs = pairingInfoDirs.childBookInfoPairs();

            str.append("%s%n[A] %s%n[B] %s%n".formatted(
                    rb.getString("CompareDirsTask.010"),
                    dirInfoPair.a().dirPath(),
                    dirInfoPair.b().dirPath()));

            if (bookInfoPairs.size() == 0) {
                str.append("    - ").append(rb.getString("CompareDirsTask.070")).append(BR);
            }
            for (int i = 0; i < bookInfoPairs.size(); i++) {
                Pair<BookInfo> bookInfoPair = bookInfoPairs.get(i);
                str.append(ResultOtDirs.formatBookNamesPair("", Integer.toString(i + 1), bookInfoPair)).append(BR);
            }

            updateMessage(str.toString());
            updateProgress(progressAfter, PROGRESS_MAX);

        } catch (Exception e) {
            throw getApplicationException(e, "AppTaskBase.180", " at CompareDirsTask::announceStart");
        }
    }

    // 3. 出力用ディレクトリの作成
    private Pair<Path> createOutputDirs(Path workDir)
            throws ApplicationException {

        PairingInfoDirs pairingInfoDirs = settings.get(SettingKeys.CURR_DIR_COMPARE_INFO);
        Pair<DirInfo> dirInfoPair = pairingInfoDirs.parentDirInfoPair();
        Pair<Path> outputDirPair = null;

        try {
            outputDirPair = Side.map(
                    side -> workDir.resolve("【%s】%s".formatted(side, dirInfoPair.get(side).dirPath().getFileName())));

            return outputDirPair.unsafeMap(Files::createDirectory);

        } catch (Exception e) {
            throw getApplicationException(e, "CompareDirsTask.020", "");
        }
    }

    // 5. フォルダ同士の比較
    private ResultOtDirs compareDirs(
            Pair<Path> outputDirPair,
            int progressBefore,
            int progressAfter)
            throws ApplicationException {

        try {
            updateProgress(progressBefore, PROGRESS_MAX);

            PairingInfoDirs pairingInfoDirs = settings.get(SettingKeys.CURR_DIR_COMPARE_INFO);

            if (0 < pairingInfoDirs.childBookInfoPairs().size()) {
                str.append(BR).append(rb.getString("CompareDirsTask.050")).append(BR);
                updateMessage(str.toString());
                return compareDirs(
                        "",
                        "",
                        pairingInfoDirs,
                        outputDirPair,
                        progressBefore,
                        progressAfter);

            } else {
                return new ResultOtDirs(
                        pairingInfoDirs,
                        pairingInfoDirs.childBookInfoPairs().stream().collect(Collectors.toMap(
                                Function.identity(),
                                name -> Optional.empty())),
                        "");
            }

        } catch (Exception e) {
            throw getApplicationException(e, "CompareDirsTask.080", "");
        }
    }
}
