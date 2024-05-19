package xyz.hotchpotch.hogandiff;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.excel.BookResult;
import xyz.hotchpotch.hogandiff.excel.DirResult;
import xyz.hotchpotch.hogandiff.excel.Result;
import xyz.hotchpotch.hogandiff.excel.SheetResult;
import xyz.hotchpotch.hogandiff.excel.SheetResult.Stats;
import xyz.hotchpotch.hogandiff.excel.TreeResult;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

public record Report(
        Settings settings,
        Result result,
        Duration elapsedTime) {
    
    // [static members] ********************************************************
    
    private static final String COMMA = ", ";
    private static final BinaryOperator<IntPair> sumIntPairs = (p1, p2) -> IntPair.of(p1.a() + p2.a(), p1.b() + p2.b());
    
    // [instance members] ******************************************************
    
    public String toJsonString() {
        // 大したことする訳じゃないので、Gsonなどは使わず頑張る！
        // 配布物のサイズを小さくすることの方が重要！
        // と思ったけど流石に酷過ぎるので
        // TODO: 実装改善する
        
        StringBuilder str = new StringBuilder();
        
        str.append("{ ");
        {
            str.append(stringProperty("uuid", SettingKeys.CLIENT_UUID)).append(COMMA);
            
            str.append("\"settings\": { ");
            {
                str.append(stringProperty("appVersion", SettingKeys.APP_VERSION)).append(COMMA);
                str.append(stringProperty("appLocale", SettingKeys.APP_LOCALE)).append(COMMA);
                str.append(booleanProperty("considerRowGaps", SettingKeys.CONSIDER_ROW_GAPS)).append(COMMA);
                str.append(booleanProperty("considerColumnGaps", SettingKeys.CONSIDER_COLUMN_GAPS)).append(COMMA);
                str.append(booleanProperty("compareOnFormula", SettingKeys.COMPARE_ON_FORMULA_STRING)).append(COMMA);
                str.append(booleanProperty("showPaintedSheets", SettingKeys.SHOW_PAINTED_SHEETS)).append(COMMA);
                str.append(booleanProperty("showResultText", SettingKeys.SHOW_RESULT_TEXT)).append(COMMA);
                str.append(booleanProperty("exitWhenFinished", SettingKeys.EXIT_WHEN_FINISHED)).append(COMMA);
                str.append(booleanProperty("prioritizeSpeed", SettingKeys.PRIORITIZE_SPEED));
            }
            str.append(" }").append(COMMA);
            
            str.append(stringProperty("menu", SettingKeys.CURR_MENU)).append(COMMA);
            str.append("\"elapsedMillis\": ").append(elapsedTime.toMillis()).append(COMMA);
            
            switch (settings.getOrDefault(SettingKeys.CURR_MENU)) {
                case COMPARE_TREES:
                    IntPair dPairs = dirPairs(result);
                    str.append("\"dirPairs\": [ %d, %d ]".formatted(dPairs.a(), dPairs.b())).append(COMMA);
                    
                case COMPARE_DIRS:
                    IntPair bPairs = bookPairs(result);
                    str.append("\"bookPairs\": [ %d, %d ]".formatted(bPairs.a(), bPairs.b())).append(COMMA);
                    
                case COMPARE_BOOKS:
                    IntPair sPairs = sheetPairs(result);
                    str.append("\"sheetPairs\": [ %d, %d ]".formatted(sPairs.a(), sPairs.b())).append(COMMA);
                    
                case COMPARE_SHEETS:
                    // nop
            }
            
            str.append("\"stats\": [ ");
            {
                str.append(
                        result.getSheetStats().stream()
                                .map(this::statsToJson)
                                .collect(Collectors.joining(", ")));
            }
            str.append(" ]");
        }
        str.append(" }");
        
        return str.toString();
    }
    
    private String stringProperty(String jsonKey, Key<?> settingKey) {
        return "\"%s\": \"%s\"".formatted(jsonKey, settings.getOrDefault(settingKey));
    }
    
    private String booleanProperty(String jsonKey, Key<Boolean> settingKey) {
        return "\"%s\": %b".formatted(jsonKey, settings.getOrDefault(settingKey));
    }
    
    private String statsToJson(Stats stats) {
        StringBuilder str = new StringBuilder();
        
        str.append("{ ");
        {
            str.append("\"rows\": [ %d, %d ]".formatted(stats.rows().a(), stats.rows().b())).append(COMMA);
            str.append("\"columns\": [ %d, %d ]".formatted(stats.columns().a(), stats.columns().b())).append(COMMA);
            str.append("\"cells\": [ %d, %d ]".formatted(stats.cells().a(), stats.cells().b())).append(COMMA);
            str.append("\"redundantRows\": [ %d, %d ]"
                    .formatted(stats.redundantRows().a(), stats.redundantRows().b()))
                    .append(COMMA);
            str.append("\"redundantColumns\": [ %d, %d ]"
                    .formatted(stats.redundantColumns().a(), stats.redundantColumns().b()))
                    .append(COMMA);
            str.append("\"diffCells\": %d".formatted(stats.diffCells()));
        }
        str.append(" }");
        
        return str.toString();
    }
    
    private IntPair sheetPairs(Result result) {
        return switch (result) {
            case SheetResult sResult -> throw new AssertionError();
            case BookResult bResult -> {
                int paired = (int) bResult.sheetPairs().stream().filter(Pair::isPaired).count();
                yield IntPair.of(paired, bResult.sheetPairs().size() - paired);
            }
            case DirResult dResult -> dResult.bookResults().values().stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(this::sheetPairs)
                    .reduce(IntPair.of(0, 0), sumIntPairs);
            case TreeResult tResult -> tResult.dirResults().values().stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(this::sheetPairs)
                    .reduce(IntPair.of(0, 0), sumIntPairs);
        };
    }
    
    private IntPair bookPairs(Result result) {
        return switch (result) {
            case SheetResult sResult -> throw new AssertionError();
            case BookResult bResult -> throw new AssertionError();
            case DirResult dResult -> {
                int paired = (int) dResult.bookNamePairs().stream().filter(Pair::isPaired).count();
                yield IntPair.of(paired, dResult.bookNamePairs().size() - paired);
            }
            case TreeResult tResult -> tResult.dirResults().values().stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(this::bookPairs)
                    .reduce(IntPair.of(0, 0), sumIntPairs);
        };
    }
    
    private IntPair dirPairs(Result result) {
        return switch (result) {
            case SheetResult sResult -> throw new AssertionError();
            case BookResult bResult -> throw new AssertionError();
            case DirResult dResult -> throw new AssertionError();
            case TreeResult tResult -> {
                int paired = (int) tResult.dirResults().values().stream().filter(Optional::isPresent).count();
                yield IntPair.of(paired, tResult.dirResults().size() - paired);
            }
        };
    }
}
