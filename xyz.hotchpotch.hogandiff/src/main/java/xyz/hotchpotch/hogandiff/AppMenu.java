package xyz.hotchpotch.hogandiff;

import java.util.List;
import java.util.Objects;

import javafx.concurrent.Task;
import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
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
            
            BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
            BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
            
            return !Objects.equals(bookInfo1.bookPath(), bookInfo2.bookPath());
        }
        
        @Override
        public List<Pair<String>> getSheetNamePairs(Settings settings, Factory factory)
                throws ExcelHandlingException {
            
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(factory, "factory");
            
            BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
            BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
            BookLoader bookLoader1 = factory.bookLoader(bookInfo1.bookPath());
            BookLoader bookLoader2 = factory.bookLoader(bookInfo2.bookPath());
            List<String> sheetNames1 = bookLoader1.loadSheetNames(bookInfo1.bookPath());
            List<String> sheetNames2 = bookLoader2.loadSheetNames(bookInfo2.bookPath());
            
            Matcher<String> matcher = factory.sheetNameMatcher(settings);
            List<IntPair> pairs = matcher.makePairs(sheetNames1, sheetNames2);
            
            return pairs.stream()
                    .map(p -> Pair.ofNullable(
                            p.hasA() ? sheetNames1.get(p.a()) : null,
                            p.hasB() ? sheetNames2.get(p.b()) : null))
                    .toList();
        }
    },
    
    /**
     * 特定のExcelシート同士を比較します。
     */
    COMPARE_SHEETS {
        
        @Override
        public boolean isValidTargets(Settings settings) {
            Objects.requireNonNull(settings, "settings");
            
            BookInfo bookInfo1 = settings.get(SettingKeys.CURR_BOOK_INFO1);
            BookInfo bookInfo2 = settings.get(SettingKeys.CURR_BOOK_INFO2);
            String sheetName1 = settings.get(SettingKeys.CURR_SHEET_NAME1);
            String sheetName2 = settings.get(SettingKeys.CURR_SHEET_NAME2);
            
            return !Objects.equals(bookInfo1.bookPath(), bookInfo2.bookPath())
                    || !Objects.equals(sheetName1, sheetName2);
        }
        
        @Override
        public List<Pair<String>> getSheetNamePairs(Settings settings, Factory factory)
                throws ExcelHandlingException {
            
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(factory, "factory");
            
            return List.of(Pair.of(
                    settings.get(SettingKeys.CURR_SHEET_NAME1),
                    settings.get(SettingKeys.CURR_SHEET_NAME2)));
        }
    };
    
    // [instance members] ******************************************************
    
    /**
     * 処理対象のExcelブック／シートの指定が妥当なものかを確認します。<br>
     * 具体的には、2つの比較対象が同じものの場合は {@code false} を、
     * それ以外の場合は {@code true} を返します。<br>
     * 
     * @param settings 設定
     * @return 比較対象の指定が妥当な場合は {@code true}
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public abstract boolean isValidTargets(Settings settings);
    
    /**
     * 比較対象のシートの組み合わせを決定し、シート名のペアのリストとして返します。<br>
     * 
     * @param settings 設定
     * @param factory ファクトリ
     * @return シート名のペアのリスト
     * @throws NullPointerException
     *              {@code settings}, {@code factory} のいずれかが {@code null} の場合
     * @throws ExcelHandlingException
     *              Excelファイルに対する処理に失敗した場合
     */
    public abstract List<Pair<String>> getSheetNamePairs(Settings settings, Factory factory)
            throws ExcelHandlingException;
    
    /**
     * このメニューを実行するためのタスクを生成して返します。<br>
     * 
     * @param settings 設定
     * @param factory ファクトリ
     * @return 新しいタスク
     * @throws NullPointerException {@code settings}, {@code factory} のいずれかが {@code null} の場合
     */
    public Task<Void> getTask(
            Settings settings,
            Factory factory) {
        
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(factory, "factory");
        
        return new AppTask(settings, factory);
    }
}
