package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
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
            
            Path bookPath1 = settings.get(SettingKeys.CURR_BOOK_PATH1);
            Path bookPath2 = settings.get(SettingKeys.CURR_BOOK_PATH2);
            
            return !Objects.equals(bookPath1, bookPath2);
        }
        
        @Override
        public List<Pair<String>> getSheetNamePairs(Settings settings, Factory factory)
                throws ExcelHandlingException {
            
            Objects.requireNonNull(settings, "settings");
            Objects.requireNonNull(factory, "factory");
            
            Path bookPath1 = settings.get(SettingKeys.CURR_BOOK_PATH1);
            Path bookPath2 = settings.get(SettingKeys.CURR_BOOK_PATH2);
            BookLoader bookLoader1 = factory.bookLoader(bookPath1);
            BookLoader bookLoader2 = factory.bookLoader(bookPath2);
            List<String> sheetNames1 = bookLoader1.loadSheetNames(bookPath1);
            List<String> sheetNames2 = bookLoader2.loadSheetNames(bookPath2);
            
            Matcher<String> matcher = Matcher.nerutonMatcherOf(
                    String::length,
                    StringDiffUtil::levenshteinDistance);
            
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
            
            Path bookPath1 = settings.get(SettingKeys.CURR_BOOK_PATH1);
            Path bookPath2 = settings.get(SettingKeys.CURR_BOOK_PATH2);
            String sheetName1 = settings.get(SettingKeys.CURR_SHEET_NAME1);
            String sheetName2 = settings.get(SettingKeys.CURR_SHEET_NAME2);
            
            return !Objects.equals(bookPath1, bookPath2)
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
    
    private AppMenu() {
    }
    
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
}
