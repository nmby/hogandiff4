package xyz.hotchpotch.hogandiff;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

public abstract sealed class Report
        permits Report.Succeeded, Report.Failed {
    
    // [static members] ********************************************************
    
    private static final String COMMA = ", ";
    
    /**
     * 比較処理が成功したことを表す {@link Report} の拡張です。<br>
     */
    public static final class Succeeded extends Report {
        
        // [static members] ----------------------------------------------------
        
        private static final BinaryOperator<IntPair> sumIntPairs = (p1, p2) -> IntPair
                .of(p1.a() + p2.a(), p1.b() + p2.b());
        
        // [instance members] --------------------------------------------------
        
        private final Result result;
        
        /**
         * コンストラクタ
         * 
         * @param settings 設定セット
         * @param start 実行開始日時
         * @param end 実行完了日時
         * @param result 比較結果
         */
        public Succeeded(
                Settings settings,
                Instant start,
                Instant end,
                Result result) {
            
            super(
                    Objects.requireNonNull(settings, "settings"),
                    Objects.requireNonNull(start, "start"),
                    Objects.requireNonNull(end, "end"));
            
            this.result = Objects.requireNonNull(result, "result");
        }
        
        @Override
        protected String toJsonString2() {
            StringBuilder str = new StringBuilder();
            
            switch (super.settings.getOrDefault(SettingKeys.CURR_MENU)) {
                case COMPARE_TREES:
                    IntPair dPairs = dirPairs(result);
                    str.append("\"dirPairs\": [ %d, %d ]".formatted(dPairs.a(), dPairs.b())).append(COMMA);
                    // fallthrough
                    
                case COMPARE_DIRS:
                    IntPair bPairs = bookPairs(result);
                    str.append("\"bookPairs\": [ %d, %d ]".formatted(bPairs.a(), bPairs.b())).append(COMMA);
                    // fallthrough
                    
                case COMPARE_BOOKS:
                    IntPair sPairs = sheetPairs(result);
                    str.append("\"sheetPairs\": [ %d, %d ]".formatted(sPairs.a(), sPairs.b())).append(COMMA);
                    // fallthrough
                    
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
            
            return str.toString();
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
    
    /**
     * 比較処理が失敗したことを表す {@link Report} の拡張です。<br>
     */
    public static final class Failed extends Report {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final Throwable thrown;
        
        /**
         * コンストラクタ
         * 
         * @param settings 設定セット
         * @param start 実行開始日時
         * @param end 実行修了日時
         * @param thrown スローされた例外
         */
        public Failed(
                Settings settings,
                Instant start,
                Instant end,
                Throwable thrown) {
            
            super(
                    Objects.requireNonNull(settings, "settings"),
                    Objects.requireNonNull(start, "start"),
                    Objects.requireNonNull(end, "end"));
            
            this.thrown = Objects.requireNonNull(thrown, "thrown");
        }
        
        @Override
        protected String toJsonString2() {
            String chain = getChain().stream()
                    .map(Throwable::getClass)
                    .map(Class::getName)
                    .map("\"%s\""::formatted)
                    .collect(Collectors.joining(", "));
            
            return "\"thrown\": [ %s ], \"errorMessage\": \"%s\""
                    .formatted(chain, thrown.getMessage());
        }
        
        private List<Throwable> getChain() {
            List<Throwable> exceptions = new ArrayList<>();
            Throwable e = thrown;
            
            do {
                exceptions.add(e);
                e = e.getCause();
            } while (e != null);
            
            return exceptions;
        }
    }
    
    // [instance members] ******************************************************
    
    private final Settings settings;
    private final Instant start;
    private final Instant end;
    
    private Report(
            Settings settings,
            Instant start,
            Instant end) {
        
        assert settings != null;
        assert start != null;
        assert end != null;
        
        this.settings = settings;
        this.start = start;
        this.end = end;
    }
    
    /**
     * この統計情報のJSON形式の文字列表現を返します。<br>
     * 
     * @return この統計情報のJSON形式の文字列表現
     */
    public String toJsonString() {
        StringBuilder str = new StringBuilder();
        
        str.append("{ ");
        {
            str.append(stringProperty("uuid", SettingKeys.CLIENT_UUID)).append(COMMA);
            
            str.append("\"settings\": ").append(settingsToJson()).append(COMMA);
            
            str.append(stringProperty("menu", SettingKeys.CURR_MENU)).append(COMMA);
            str.append("\"executedAt\": ").append("\"%s\"".formatted(start.toString())).append(COMMA);
            str.append("\"elapsedMillis\": ").append(Duration.between(start, end).toMillis()).append(COMMA);
            
            str.append(toJsonString2());
        }
        str.append(" }");
        
        return str.toString();
    }
    
    private String settingsToJson() {
        StringBuilder str = new StringBuilder();
        
        str.append("{ ");
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
        str.append(" }");
        
        return str.toString();
    }
    
    private String stringProperty(String jsonKey, Key<?> settingKey) {
        return "\"%s\": \"%s\"".formatted(jsonKey, settings.getOrDefault(settingKey));
    }
    
    private String booleanProperty(String jsonKey, Key<Boolean> settingKey) {
        return "\"%s\": %b".formatted(jsonKey, settings.getOrDefault(settingKey));
    }
    
    /**
     * サブクラス独自内容のJSON形式の文字列表現を返します。<br>
     * 
     * @return サブクラス独自内容のJSON形式の文字列表現
     */
    protected abstract String toJsonString2();
}
