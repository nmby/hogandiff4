package xyz.hotchpotch.hogandiff;

import java.util.Objects;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.logic.PairingInfoBooks;
import xyz.hotchpotch.hogandiff.tasks.CompareTaskBooks;
import xyz.hotchpotch.hogandiff.tasks.CompareTaskDirs;
import xyz.hotchpotch.hogandiff.tasks.CompareTaskSheets;
import xyz.hotchpotch.hogandiff.tasks.CompareTaskTrees;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Triple.Side3;

/**
 * 比較メニューの選択状況を表す不変クラスです。<br>
 * 
 * @author nmby
 * @param compareObject
 *            比較の対象
 * @param compareWay
 *            比較の方法
 */
public record CompareMenu(
        CompareObject compareObject,
        CompareWay compareWay) {
    
    // [static members] ********************************************************
    
    /**
     * 比較対象を表します。<br>
     *
     * @author nmby
     */
    public static enum CompareObject {
        
        // [static members] ----------------------------------------------------
        
        /**
         * Excelブックに含まれる全シートを比較します。
         * 具体的には、2つのExcelブックに含まれる名前の似ているシート同士をマッチングし、
         * それらのペアごとに比較を行います。<br>
         */
        COMPARE_BOOKS,
        
        /**
         * 特定のExcelシート同士を比較します。
         */
        COMPARE_SHEETS,
        
        /**
         * 指定されたフォルダに含まれる全Excelブックを比較します。
         * 具体的には、2つのフォルダに含まれる名前の似ているExcelブック同士をマッチングし、
         * それらのペアごとに比較を行います。<br>
         */
        COMPARE_DIRS,
        
        /**
         * 指定されたフォルダ配下のフォルダツリーを比較します。
         * 具体的には、2つのフォルダツリーに含まれるフォルダ同士をマッチングし、
         * それらのペアごとに比較を行います。<br>
         */
        COMPARE_TREES;
        
        // [instance members] --------------------------------------------------
    }
    
    /**
     * 比較の方法を表します。<br>
     */
    public enum CompareWay {
        
        // [static members] ----------------------------------------------------
        
        /** 2つの対象物を比較します。 */
        TWO_WAY,
        
        /** 3つの対象物を比較します。 */
        THREE_WAY;
        
        // [instance members] --------------------------------------------------
    }
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ。<br>
     * 
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public CompareMenu {
        Objects.requireNonNull(compareObject);
        Objects.requireNonNull(compareWay);
    }
    
    /**
     * 処理対象のシート／ブック／フォルダの指定が妥当なものかを確認します。<br>
     * 具体的には、比較対象が同一物の場合は {@code false} を、
     * それ以外の場合は {@code true} を返します。<br>
     * 
     * @param settings
     *            設定
     * @return 比較対象の指定が妥当な場合は {@code true}
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public boolean isValidTargets(Settings settings) {
        Objects.requireNonNull(settings);
        
        switch (compareWay) {
        case TWO_WAY:
            return isIdentical(settings, Side3.O);
        
        case THREE_WAY:
            return isIdentical(settings, Side3.O) && isIdentical(settings, Side3.A) && isIdentical(settings, Side3.B);
        
        default:
            throw new AssertionError("Unreachable code: " + compareWay);
        }
    }
    
    private boolean isIdentical(Settings settings, Side3 side3) {
        return switch (compareObject) {
        case COMPARE_SHEETS -> {
            PairingInfoBooks bookComparison = settings.get(SettingKeys.CURR_SHEET_COMPARE_INFOS.get(side3));
            yield !bookComparison.parentBookInfoPair().isIdentical()
                    || !bookComparison.childSheetNamePairs().get(0).isIdentical();
        }
        case COMPARE_BOOKS -> !settings.get(SettingKeys.CURR_BOOK_COMPARE_INFOS.get(side3))
                .parentBookInfoPair().isIdentical();
        case COMPARE_DIRS -> !settings.get(SettingKeys.CURR_DIR_COMPARE_INFOS.get(side3))
                .parentDirInfoPair().isIdentical();
        case COMPARE_TREES -> !settings.get(SettingKeys.CURR_TREE_COMPARE_INFOS.get(side3))
                .parentDirInfoPair().isIdentical();
        default -> throw new AssertionError("Unreachable code: " + compareObject);
        };
    }
    
    /**
     * このメニューを実行するためのタスクを生成して返します。<br>
     * 
     * @param settings
     *            設定
     * @return 新しいタスク
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public Task<Void> getTask(Settings settings) {
        Objects.requireNonNull(settings);
        
        switch (compareWay) {
        case TWO_WAY:
            return switch (compareObject) {
            case COMPARE_SHEETS -> new CompareTaskSheets(settings);
            case COMPARE_BOOKS -> new CompareTaskBooks(settings);
            case COMPARE_DIRS -> new CompareTaskDirs(settings);
            case COMPARE_TREES -> new CompareTaskTrees(settings);
            };
        
        case THREE_WAY:
            throw new UnsupportedOperationException("Three-way comparison is not supported yet.");
        
        default:
            throw new AssertionError("Unreachable code: " + compareWay);
        }
    }
}
