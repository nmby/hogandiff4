package xyz.hotchpotch.hogandiff;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
            + SettingKeys.CONSIDER_ROW_GAPS.ifNotSetSupplier().get() + BR
            + "    --consider-column-gaps=[true|false]  : default value is "
            + SettingKeys.CONSIDER_COLUMN_GAPS.ifNotSetSupplier().get() + BR
            + "    --compare-on-formulas=[true|false]   : default value is "
            + SettingKeys.COMPARE_ON_FORMULA_STRING.ifNotSetSupplier().get() + BR
            + "    --enable-fuzzy-matching=[true|false]  : default value is "
            + SettingKeys.ENABLE_FUZZY_MATCHING.ifNotSetSupplier().get() + BR
            + "    --show-painted-sheets=[true|false]   : default value is "
            + SettingKeys.SHOW_PAINTED_SHEETS.ifNotSetSupplier().get() + BR
            + "    --show-result-text=[true|false]      : default value is "
            + SettingKeys.SHOW_RESULT_REPORT.ifNotSetSupplier().get() + BR
            + "    --exit-when-finished=[true|false]    : default value is "
            + SettingKeys.EXIT_WHEN_FINISHED.ifNotSetSupplier().get() + BR
            + "    --prioritize-speed=[true|false]      : default value is "
            + SettingKeys.PRIORITIZE_SPEED.ifNotSetSupplier().get() + BR
            + BR;
    
    private static final Map<String, Key<Boolean>> OPTIONS = Map.of(
            "--consider-row-gaps", SettingKeys.CONSIDER_ROW_GAPS,
            "--consider-column-gaps", SettingKeys.CONSIDER_COLUMN_GAPS,
            "--compare-on-formulas", SettingKeys.COMPARE_ON_FORMULA_STRING,
            "--enable-fuzzy-matching", SettingKeys.ENABLE_FUZZY_MATCHING,
            "--show-painted-sheets", SettingKeys.SHOW_PAINTED_SHEETS,
            "--show-result-text", SettingKeys.SHOW_RESULT_REPORT,
            "--exit-when-finished", SettingKeys.EXIT_WHEN_FINISHED,
            "--prioritize-speed", SettingKeys.PRIORITIZE_SPEED);
    
    /**
     * アプリケーション実行時引数を解析してアプリケーション設定に変換します。<br>
     * アプリケーション実行時引数の一部でも解析できない部分がある場合は、
     * 空の {@link Optional} を返します。<br>
     * 
     * @param args アプリケーション実行時引数
     * @return アプリケーション設定。解析できない場合は空の {@link Optional}
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static Optional<Settings> parseArgs(String[] args) {
        Objects.requireNonNull(args);
        
        if (args.length < 2) {
            return Optional.empty();
        }
        
        // まず、第一・第二引数の評価を行う。
        Path pathA = null;
        Path pathB = null;
        try {
            pathA = Path.of(args[0]);
            pathB = Path.of(args[1]);
        } catch (Exception e) {
            // パスの評価に失敗した場合は解析失敗とする。
            return Optional.empty();
        }
        if (Files.notExists(pathA) || Files.notExists(pathB)
                || Files.isRegularFile(pathA) != Files.isRegularFile(pathB)
                || Files.isDirectory(pathA) != Files.isDirectory(pathB)
                || !Files.isRegularFile(pathA) && !Files.isDirectory(pathA)) {
            return Optional.empty();
        }
        
        Settings.Builder builder = Settings.builder().set(
                SettingKeys.CURR_MENU,
                Files.isDirectory(pathA) ? AppMenu.COMPARE_DIRS : AppMenu.COMPARE_BOOKS);
        
        builder.set(SettingKeys.CURR_ARG_PATH1, pathA);
        builder.set(SettingKeys.CURR_ARG_PATH2, pathB);
        
        // 次に、第三以降の引数を解析する。
        try {
            Deque<String> remainingParams = new ArrayDeque<String>(List.of(args));
            // 第一・第二引数はパース済みのため読み飛ばす
            remainingParams.remove();
            remainingParams.remove();
            
            // 廃止した --save-memory オプションは無視する。
            remainingParams.removeIf(p -> p.startsWith("--save-memory="));
            
            Map<String, Key<Boolean>> remainingOptions = new HashMap<>(OPTIONS);
            
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
