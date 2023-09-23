package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.Objects;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
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
    COMPARE_BOOKS {
        
        @Override
        public boolean isValidTargets(Settings settings) {
            Objects.requireNonNull(settings, "settings");
            
            BookOpenInfo bookOpenInfo1 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1);
            BookOpenInfo bookOpenInfo2 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2);
            
            return !Objects.equals(bookOpenInfo1.bookPath(), bookOpenInfo2.bookPath());
        }
        
        @Override
        public Task<Void> getTask(
                Settings settings,
                Factory factory) {
            
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(factory, "factory");
            
            return new CompareBooksTask(settings, factory);
        }
    },
    
    /**
     * 特定のExcelシート同士を比較します。
     */
    COMPARE_SHEETS {
        
        @Override
        public boolean isValidTargets(Settings settings) {
            Objects.requireNonNull(settings, "settings");
            
            BookOpenInfo bookOpenInfo1 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO1);
            BookOpenInfo bookOpenInfo2 = settings.get(SettingKeys.CURR_BOOK_OPEN_INFO2);
            String sheetName1 = settings.get(SettingKeys.CURR_SHEET_NAME1);
            String sheetName2 = settings.get(SettingKeys.CURR_SHEET_NAME2);
            
            return !Objects.equals(bookOpenInfo1.bookPath(), bookOpenInfo2.bookPath())
                    || !Objects.equals(sheetName1, sheetName2);
        }
        
        @Override
        public Task<Void> getTask(
                Settings settings,
                Factory factory) {
            
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(factory, "factory");
            
            return new CompareSheetsTask(settings, factory);
        }
    },
    
    /**
     * 指定されたフォルダに含まれる全Excelブックを比較します。
     * 具体的には、2つのフォルダに含まれる名前の似ているExcelブック同士をマッチングし、
     * それらのペアごとに比較を行います。<br>
     */
    COMPARE_DIRS {
        
        @Override
        public boolean isValidTargets(Settings settings) {
            Objects.requireNonNull(settings, "settings");
            
            Path dirPath1 = settings.get(SettingKeys.CURR_DIR_PATH1);
            Path dirPath2 = settings.get(SettingKeys.CURR_DIR_PATH2);
            
            return !Objects.equals(dirPath1, dirPath2);
        }
        
        @Override
        public Task<Void> getTask(
                Settings settings,
                Factory factory) {
            
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(factory, "factory");
            
            return new CompareDirsTask(settings, factory);
        }
    },
    
    /**
     * 指定されたフォルダ配下のフォルダツリーを比較します。
     * 具体的には、2つのフォルダツリーに含まれるフォルダ同士をマッチングし、
     * それらのペアごとに比較を行います。<br>
     */
    COMPARE_TREES {
        
        @Override
        public boolean isValidTargets(Settings settings) {
            Objects.requireNonNull(settings, "settings");
            
            Path dirPath1 = settings.get(SettingKeys.CURR_DIR_PATH1);
            Path dirPath2 = settings.get(SettingKeys.CURR_DIR_PATH2);
            
            return !Objects.equals(dirPath1, dirPath2);
        }
        
        @Override
        public Task<Void> getTask(
                Settings settings,
                Factory factory) {
            
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(factory, "factory");
            
            // TODO: coding
            throw new UnsupportedOperationException("★要実装");
        }
    };
    
    // [instance members] ******************************************************
    
    /**
     * 処理対象のフォルダ／Excelブック／シートの指定が妥当なものかを確認します。<br>
     * 具体的には、2つの比較対象が同じものの場合は {@code false} を、
     * それ以外の場合は {@code true} を返します。<br>
     * 
     * @param settings 設定
     * @return 比較対象の指定が妥当な場合は {@code true}
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public abstract boolean isValidTargets(Settings settings);
    
    /**
     * このメニューを実行するためのタスクを生成して返します。<br>
     * 
     * @param settings 設定
     * @param factory ファクトリ
     * @return 新しいタスク
     * @throws NullPointerException {@code settings}, {@code factory} のいずれかが {@code null} の場合
     */
    public abstract Task<Void> getTask(Settings settings, Factory factory);
}
