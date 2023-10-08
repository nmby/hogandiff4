package xyz.hotchpotch.hogandiff;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.excel.Factory;
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
            settings -> !Objects.equals(
                    settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1).bookPath(),
                    settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2).bookPath())),
    
    /**
     * 特定のExcelシート同士を比較します。
     */
    COMPARE_SHEETS(
            CompareSheetsTask::new,
            settings -> !Objects.equals(
                    settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1).bookPath(),
                    settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2).bookPath())
                    || !Objects.equals(
                            settings.get(SettingKeys.CURR_SHEET_NAME1),
                            settings.get(SettingKeys.CURR_SHEET_NAME2))),
    
    /**
     * 指定されたフォルダに含まれる全Excelブックを比較します。
     * 具体的には、2つのフォルダに含まれる名前の似ているExcelブック同士をマッチングし、
     * それらのペアごとに比較を行います。<br>
     */
    COMPARE_DIRS(
            CompareDirsTask::new,
            settings -> !Objects.equals(
                    settings.get(SettingKeys.CURR_DIR_PATH1),
                    settings.get(SettingKeys.CURR_DIR_PATH2))),
    
    /**
     * 指定されたフォルダ配下のフォルダツリーを比較します。
     * 具体的には、2つのフォルダツリーに含まれるフォルダ同士をマッチングし、
     * それらのペアごとに比較を行います。<br>
     */
    COMPARE_TREES(
            CompareTreesTask::new,
            settings -> !Objects.equals(
                    settings.get(SettingKeys.CURR_DIR_PATH1),
                    settings.get(SettingKeys.CURR_DIR_PATH2)));
    
    // [instance members] ******************************************************
    
    private final BiFunction<Settings, Factory, Task<Void>> taskFactory;
    private final Predicate<Settings> targetValidator;
    
    private AppMenu(
            BiFunction<Settings, Factory, Task<Void>> taskFactory,
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
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public boolean isValidTargets(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        return targetValidator.test(settings);
    }
    
    /**
     * このメニューを実行するためのタスクを生成して返します。<br>
     * 
     * @param settings 設定
     * @param factory ファクトリ
     * @return 新しいタスク
     * @throws NullPointerException {@code settings}, {@code factory} のいずれかが {@code null} の場合
     */
    public Task<Void> getTask(Settings settings, Factory factory) {
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(factory, "factory");
        
        return taskFactory.apply(settings, factory);
    }
}
