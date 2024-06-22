package xyz.hotchpotch.hogandiff;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.excel.BookInfoComparison;
import xyz.hotchpotch.hogandiff.excel.DirCompareInfo;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * このアプリケーションの比較メニューです。<br>
 *
 * @author nmby
 */
public enum AppMenu {
    
    // [static members] ********************************************************
    
    /**
     * Excelブックに含まれる全シートを比較します。
     * 具体的には、2つのExcelブックに含まれる名前の似ているシート同士をマッチングし、
     * それらのペアごとに比較を行います。<br>
     */
    COMPARE_BOOKS(
            CompareBooksTask::new,
            settings -> {
                BookInfoComparison bookInfoComparison = settings.get(SettingKeys.CURR_BOOK_COMPARE_INFO);
                Objects.requireNonNull(bookInfoComparison);
                
                return !bookInfoComparison.parentBookInfoPair().isIdentical();
            }),
    
    /**
     * 特定のExcelシート同士を比較します。
     */
    COMPARE_SHEETS(
            CompareSheetsTask::new,
            settings -> {
                BookInfoComparison bookInfoComparison = settings.get(SettingKeys.CURR_SHEET_COMPARE_INFO);
                Objects.requireNonNull(bookInfoComparison);
                
                return !bookInfoComparison.parentBookInfoPair().isIdentical()
                        || !bookInfoComparison.childSheetNamePairs().get(0).isIdentical();
            }),
    
    /**
     * 指定されたフォルダに含まれる全Excelブックを比較します。
     * 具体的には、2つのフォルダに含まれる名前の似ているExcelブック同士をマッチングし、
     * それらのペアごとに比較を行います。<br>
     */
    COMPARE_DIRS(
            CompareDirsTask::new,
            settings -> {
                DirCompareInfo dirCompareInfo = settings.get(SettingKeys.CURR_DIR_COMPARE_INFO);
                Objects.requireNonNull(dirCompareInfo);
                
                return !dirCompareInfo.parentDirInfoPair().isIdentical();
            }),
    
    /**
     * 指定されたフォルダ配下のフォルダツリーを比較します。
     * 具体的には、2つのフォルダツリーに含まれるフォルダ同士をマッチングし、
     * それらのペアごとに比較を行います。<br>
     */
    COMPARE_TREES(
            CompareTreesTask::new,
            settings -> {
                DirCompareInfo dirCompareInfo = settings.get(SettingKeys.CURR_TREE_COMPARE_INFO);
                Objects.requireNonNull(dirCompareInfo);
                
                return !dirCompareInfo.parentDirInfoPair().isIdentical();
            });
    
    // [instance members] ******************************************************
    
    private final Function<Settings, Task<Report>> taskFactory;
    private final Predicate<Settings> targetValidator;
    
    private AppMenu(
            Function<Settings, Task<Report>> taskFactory,
            Predicate<Settings> targetValidator) {
        
        assert taskFactory != null;
        assert targetValidator != null;
        
        this.taskFactory = taskFactory;
        this.targetValidator = targetValidator;
    }
    
    /**
     * 処理対象のフォルダ／Excelブック／シートの指定が妥当なものかを確認します。<br>
     * 具体的には、2つの比較対象が同じものの場合は {@code false} を、
     * それ以外の場合は {@code true} を返します。<br>
     * 
     * @param settings 設定
     * @return 比較対象の指定が妥当な場合は {@code true}
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public boolean isValidTargets(Settings settings) {
        Objects.requireNonNull(settings);
        
        return targetValidator.test(settings);
    }
    
    /**
     * このメニューを実行するためのタスクを生成して返します。<br>
     * 
     * @param settings 設定
     * @return 新しいタスク
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public Task<Report> getTask(Settings settings) {
        Objects.requireNonNull(settings);
        
        return taskFactory.apply(settings);
    }
}
