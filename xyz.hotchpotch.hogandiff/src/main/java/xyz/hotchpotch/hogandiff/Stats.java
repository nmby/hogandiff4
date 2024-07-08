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
import xyz.hotchpotch.hogandiff.excel.SheetResult.SheetStats;
import xyz.hotchpotch.hogandiff.excel.TreeResult;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * 比較処理結果の統計情報を表す不変クラスです。<br>
 * 
 * @author nmby
 */
public abstract sealed class Stats
        permits Stats.Succeeded, Stats.Failed {
    
    // [static members] ********************************************************
    
    private static final String COMMA = ", ";
    
    /**
     * 比較処理が成功したことを表す {@link Stats} の拡張です。<br>
     */
    public static final class Succeeded extends Stats {
        
        // [static members] ----------------------------------------------------
        
        private static final BinaryOperator<IntPair> sumIntPairs = (p1, p2) -> IntPair
                .of(p1.a() + p2.a(), p1.b() + p2.b());
        
        // [instance members] --------------------------------------------------
        
        private final Result result;
        
        /**
         * コンストラクタ
         * 
         * @param settings 設定セット
         * @param start 実行開始時刻
         * @param end 実行終了時刻
         * @param result 比較結果
         * @throws NullPointerException パラメータが {@code null} の場合
         */
        public Succeeded(
                Settings settings,
                Instant start,
                Instant end,
                Result result) {
            
            super(
                    Objects.requireNonNull(settings),
                    Objects.requireNonNull(start),
                    Objects.requireNonNull(end));
            
            this.result = Objects.requireNonNull(result);
        }
        
        /**
         * {@code
         *      dirPairs: [ %d, %d ],
         *      bookPairs: [ %d, %d ],
         *      sheetPairs: [ %d, %d ],
         *      stats: [ %s ]
         * }
         */
        @Override
        protected String toJsonString2() {
            StringBuilder str = new StringBuilder();
            
            switch (super.settings.get(SettingKeys.CURR_MENU)) {
                case COMPARE_TREES:
                    IntPair dPair = dirPairs(result);
                    str.append("\"dirPairs\": [ %d, %d ]".formatted(dPair.a(), dPair.b())).append(COMMA);
                    // fallthrough
                    
                case COMPARE_DIRS:
                    IntPair bPair = bookPairs(result);
                    str.append("\"bookPairs\": [ %d, %d ]".formatted(bPair.a(), bPair.b())).append(COMMA);
                    // fallthrough
                    
                case COMPARE_BOOKS:
                    IntPair sPair = sheetPairs(result);
                    str.append("\"sheetPairs\": [ %d, %d ]".formatted(sPair.a(), sPair.b())).append(COMMA);
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
        
        /**
         * {@code
         *      {
         *          rows: [ %d, %d ],
         *          columns: [ %d, %d ],
         *          cells: [ %d, %d ],
         *          redundantRows: [ %d, %d ],
         *          redundantColumns: [ %d, %d ],
         *          diffCells: %d
         *      }
         * }
         * 
         * @param sheetStats 統計情報
         * @return 統計情報のJSON形式の文字列表現
         */
        private String statsToJson(SheetStats sheetStats) {
            StringBuilder str = new StringBuilder();
            
            str.append("{ ");
            {
                str.append("\"rows\": [ %d, %d ]".formatted(sheetStats.rows().a(), sheetStats.rows().b()))
                        .append(COMMA);
                str.append("\"columns\": [ %d, %d ]".formatted(sheetStats.columns().a(), sheetStats.columns().b()))
                        .append(COMMA);
                str.append("\"cells\": [ %d, %d ]".formatted(sheetStats.cells().a(), sheetStats.cells().b()))
                        .append(COMMA);
                str.append("\"redundantRows\": [ %d, %d ]"
                        .formatted(sheetStats.redundantRows().a(), sheetStats.redundantRows().b()))
                        .append(COMMA);
                str.append("\"redundantColumns\": [ %d, %d ]"
                        .formatted(sheetStats.redundantColumns().a(), sheetStats.redundantColumns().b()))
                        .append(COMMA);
                str.append("\"diffCells\": %d".formatted(sheetStats.diffCells()));
            }
            str.append(" }");
            
            return str.toString();
        }
        
        private IntPair sheetPairs(Result result) {
            return switch (result) {
                case SheetResult sResult -> throw new AssertionError();
                case BookResult bResult -> {
                    int paired = (int) bResult.bookComparison().childSheetNamePairs().stream()
                            .filter(Pair::isPaired)
                            .count();
                    yield IntPair.of(paired, bResult.bookComparison().childSheetNamePairs().size() - paired);
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
                    int paired = (int) dResult.dirComparison().childBookInfoPairs().stream()
                            .filter(Pair::isPaired)
                            .count();
                    yield IntPair.of(paired, dResult.dirComparison().childBookInfoPairs().size() - paired);
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
     * 比較処理が失敗したことを表す {@link Stats} の拡張です。<br>
     */
    public static final class Failed extends Stats {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final Throwable thrown;
        
        /**
         * コンストラクタ
         * 
         * @param settings 設定セット
         * @param start 実行開始時刻
         * @param end 実行終了時刻
         * @param thrown スローされた例外
         * @throws NullPointerException パラメータが {@code null} の場合
         */
        public Failed(
                Settings settings,
                Instant start,
                Instant end,
                Throwable thrown) {
            
            super(
                    Objects.requireNonNull(settings),
                    Objects.requireNonNull(start),
                    Objects.requireNonNull(end));
            
            this.thrown = Objects.requireNonNull(thrown);
        }
        
        /**
         * {@code
         *      thrown: [ %s ],
         *      errorMessage: %s
         * }
         */
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
    
    private Stats(
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
     * {@code
     *      {
     *          uuid: %s,
     *          settings: %s,
     *          menu: %s,
     *          executedAt: %s,
     *          elapsedMillis: %d,
     *          %s
     *      }
     * }
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
    
    /**
     * {@code
     *      appVersion: %s,
     *      appLocale: %s,
     *      considerRowGaps: %b,
     *      considerColumnGaps: %b,
     *      compareOnFormula: %b,
     *      showPaintedSheets: %b,
     *      showResultText: %b,
     *      exitWhenFinished: %b,
     *      prioritizeSpeed: %b
     * }
     * 
     * @return
     */
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
        return "\"%s\": \"%s\"".formatted(jsonKey, settings.get(settingKey));
    }
    
    private String booleanProperty(String jsonKey, Key<Boolean> settingKey) {
        return "\"%s\": %b".formatted(jsonKey, settings.get(settingKey));
    }
    
    /**
     * サブクラス独自内容のJSON形式の文字列表現を返します。<br>
     * 
     * @return サブクラス独自内容のJSON形式の文字列表現
     */
    protected abstract String toJsonString2();
}
