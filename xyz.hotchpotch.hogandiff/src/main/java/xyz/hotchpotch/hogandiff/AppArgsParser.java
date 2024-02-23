package xyz.hotchpotch.hogandiff;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * アプリケーション実行時引数を解析してアプリケーション設定に変換するパーサーです。<br>
 *
 * @author nmby
 */
public class AppArgsParser {
    
    // [static members] ********************************************************
    
    private static final String BR = System.lineSeparator();
    
    /** このアプリケーションのコマンドライン起動時の使い方 */
    public static final String USAGE = ""
            + "方眼Diff.exe bookPath1 bookPath2 <OPTIONS>" + BR
            + BR
            + "<OPTIONS>" + BR
            + "    --consider-row-gaps=[true|false]     : default value is "
            + SettingKeys.CONSIDER_ROW_GAPS.defaultValueSupplier().get() + BR
            + "    --consider-column-gaps=[true|false]  : default value is "
            + SettingKeys.CONSIDER_COLUMN_GAPS.defaultValueSupplier().get() + BR
            + "    --compare-on-formulas=[true|false]   : default value is "
            + SettingKeys.COMPARE_ON_FORMULA_STRING.defaultValueSupplier().get() + BR
            + "    --show-painted-sheets=[true|false]   : default value is "
            + SettingKeys.SHOW_PAINTED_SHEETS.defaultValueSupplier().get() + BR
            + "    --show-result-text=[true|false]      : default value is "
            + SettingKeys.SHOW_RESULT_TEXT.defaultValueSupplier().get() + BR
            + "    --exit-when-finished=[true|false]    : default value is "
            + SettingKeys.EXIT_WHEN_FINISHED.defaultValueSupplier().get() + BR
            + BR;
    
    private static final Map<String, Key<Boolean>> OPTIONS = Map.of(
            "--consider-row-gaps", SettingKeys.CONSIDER_ROW_GAPS,
            "--consider-column-gaps", SettingKeys.CONSIDER_COLUMN_GAPS,
            "--compare-on-formulas", SettingKeys.COMPARE_ON_FORMULA_STRING,
            "--show-painted-sheets", SettingKeys.SHOW_PAINTED_SHEETS,
            "--show-result-text", SettingKeys.SHOW_RESULT_TEXT,
            "--exit-when-finished", SettingKeys.EXIT_WHEN_FINISHED);
    
    /**
     * アプリケーション実行時引数を解析してアプリケーション設定に変換します。<br>
     * アプリケーション実行時引数の一部でも解析できない部分がある場合は、
     * 空の {@link Optional} を返します。<br>
     * 
     * @param args アプリケーション実行時引数
     * @return アプリケーション設定。解析できない場合は空の {@link Optional}
     * @throws NullPointerException {@code args} が {@code null} の場合
     */
    public static Optional<Settings> parseArgs(String[] args) {
        Objects.requireNonNull(args, "args");
        
        if (args.length < 2) {
            return Optional.empty();
        }
        
        try {
            // 比較メニューと比較対象Excelブックパスのパース
            Settings.Builder builder = Settings.builder()
                    .set(SettingKeys.CURR_MENU, AppMenu.COMPARE_BOOKS)
                    .set(SettingKeys.CURR_BOOK_OPEN_INFO1, new BookOpenInfo(Path.of(args[0]), null))
                    .set(SettingKeys.CURR_BOOK_OPEN_INFO2, new BookOpenInfo(Path.of(args[1]), null));
            
            Deque<String> remainingParams = new ArrayDeque<String>(List.of(args));
            remainingParams.remove();
            remainingParams.remove();
            
            // 廃止した --save-memory オプションは無視する。
            remainingParams.removeIf(p -> p.startsWith("--save-memory="));
            
            Map<String, Key<Boolean>> remainingOptions = new HashMap<>(OPTIONS);
            
            // オプションのパース
            while (!remainingParams.isEmpty() && !remainingOptions.isEmpty()) {
                String[] keyValue = remainingParams.removeFirst().split("=", 2);
                if (!remainingOptions.containsKey(keyValue[0])) {
                    return Optional.empty();
                }
                if (!"true".equals(keyValue[1]) && !"false".equals(keyValue[1])) {
                    return Optional.empty();
                }
                builder.set(remainingOptions.remove(keyValue[0]), Boolean.valueOf(keyValue[1]));
            }
            
            // 解析不能なパラメータが残っている場合はエラー（解析失敗）とする。
            if (!remainingParams.isEmpty()) {
                return Optional.empty();
            }
            
            return Optional.of(builder.build());
            
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
    
    // [instance members] ******************************************************
    
    private AppArgsParser() {
    }
}
