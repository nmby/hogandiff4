package xyz.hotchpotch.hogandiff;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                        result.sheetStats().stream()
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
        
        @Override
        protected String toUrlParamString2() {
            StringBuilder str = new StringBuilder();
            
            switch (super.settings.get(SettingKeys.CURR_MENU)) {
                case COMPARE_TREES:
                    // pd: dirPairs(paired-unpaired): "9-9"
                    IntPair dPair = dirPairs(result);
                    str.append("&pd=").append("%d-%d".formatted(dPair.a(), dPair.b()));
                    
                    // fallthrough
                    
                case COMPARE_DIRS:
                    // pb: bookPairs(paired-unpaired): "9-9"
                    IntPair bPair = bookPairs(result);
                    str.append("&pb=").append("%d-%d".formatted(bPair.a(), bPair.b()));
                    
                    // fallthrough
                    
                case COMPARE_BOOKS:
                    // ps: sheetPairs(paired-unpaired): "9-9"
                    IntPair sPair = sheetPairs(result);
                    str.append("&ps=").append("%d-%d".formatted(sPair.a(), sPair.b()));
                    
                    // fallthrough
                    
                case COMPARE_SHEETS:
                    // nop
            }
            
            IntPair sumRows = IntPair.of(0, 0);
            IntPair sumCols = IntPair.of(0, 0);
            IntPair sumCells = IntPair.of(0, 0);
            IntPair sumRRows = IntPair.of(0, 0);
            IntPair sumRCols = IntPair.of(0, 0);
            int sumDCells = 0;
            int maxRows = 0;
            int maxCols = 0;
            int maxCells = 0;
            int maxRRows = 0;
            int maxRCols = 0;
            int maxDCells = 0;
            
            for (SheetStats stats : result.sheetStats()) {
                sumRows = sumIntPairs.apply(sumRows, stats.rows());
                sumCols = sumIntPairs.apply(sumCols, stats.columns());
                sumCells = sumIntPairs.apply(sumCells, stats.cells());
                sumRRows = sumIntPairs.apply(sumRRows, stats.redundantRows());
                sumRCols = sumIntPairs.apply(sumRCols, stats.redundantColumns());
                sumDCells += stats.diffCells();
                maxRows = Math.max(maxRows, Math.max(stats.rows().a(), stats.rows().b()));
                maxCols = Math.max(maxCols, Math.max(stats.columns().a(), stats.columns().b()));
                maxCells = Math.max(maxCells, Math.max(stats.cells().a(), stats.cells().b()));
                maxRRows = Math.max(maxRRows, Math.max(stats.redundantRows().a(), stats.redundantRows().b()));
                maxRCols = Math.max(maxRCols, Math.max(stats.redundantColumns().a(), stats.redundantColumns().b()));
                maxDCells = Math.max(maxDCells, stats.diffCells());
            }
            
            // num: result.sheetStats().size(): "99"
            str.append("&num=").append(result.sheetStats().size());
            
            // sr: sumRows(A-B): "9-9"
            str.append("&sr=").append("%d-%d".formatted(sumRows.a(), sumRows.b()));
            
            // sc: sumCols(A-B): "9-9"
            str.append("&sc=").append("%d-%d".formatted(sumCols.a(), sumCols.b()));
            
            // se: sumCells(A-B): "9-9"
            str.append("&se=").append("%d-%d".formatted(sumCells.a(), sumCells.b()));
            
            // srr: sumRRows(A-B): "9-9"
            str.append("&srr=").append("%d-%d".formatted(sumRRows.a(), sumRRows.b()));
            
            // src: sumRCols(A-B): "9-9"
            str.append("&src=").append("%d-%d".formatted(sumRCols.a(), sumRCols.b()));
            
            // sde: sumDCells(A-B): "9"
            str.append("&sde=").append(sumDCells);
            
            // mr: maxRows: "9"
            str.append("&mr=").append(maxRows);
            
            // mc: maxCols: "9"
            str.append("&mc=").append(maxCols);
            
            // me: maxCells: "9"
            str.append("&me=").append(maxCells);
            
            // mrr: maxRRows: "9"
            str.append("&mrr=").append(maxRRows);
            
            // mrc: maxRCols: "9"
            str.append("&mrc=").append(maxRCols);
            
            // mde: maxDCells: "9"
            str.append("&mde=").append(maxDCells);
            
            return str.toString();
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
        
        @Override
        protected String toUrlParamString2() {
            String chain = getChain().stream()
                    .map(Throwable::getClass)
                    .map(Class::getName)
                    .collect(Collectors.joining("-"));
            
            return "&thr=%s&msg=%s".formatted(
                    chain,
                    URLEncoder.encode(thrown.getMessage(), StandardCharsets.UTF_8));
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
            str.append(booleanProperty("matchNamesLoosely", SettingKeys.MATCH_NAMES_LOOSELY)).append(COMMA);
            str.append(booleanProperty("showPaintedSheets", SettingKeys.SHOW_PAINTED_SHEETS)).append(COMMA);
            str.append(booleanProperty("showResultText", SettingKeys.SHOW_RESULT_REPORT)).append(COMMA);
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
    
    /**
     * この統計情報のURLクエリパラメータ形式の文字列表現を返します。<br>
     * 
     * @return この統計情報のURLクエリパラメータ形式の文字列表現
     */
    public String toUrlParamString() {
        StringBuilder str = new StringBuilder();
        
        // p00: CLIENT_UUID: "01234567-89ab-cde0-1234-56789abcde01"
        str.append("p00=").append(settings.get(SettingKeys.CLIENT_UUID));
        
        // p01: APP_VERSION: "v0.22.1"
        str.append("&p01=").append(settings.get(SettingKeys.APP_VERSION));
        
        // p02: APP_LOCALE: "ja"/"en"/"cn"
        str.append("&p02=").append(settings.get(SettingKeys.APP_LOCALE));
        
        // p11: CONSIDER_ROW_GAPS: "y"/"n"
        str.append("&p11=").append(settings.get(SettingKeys.CONSIDER_ROW_GAPS) ? "y" : "n");
        
        // p12: CONSIDER_COLUMN_GAPS: "y"/"n"
        str.append("&p12=").append(settings.get(SettingKeys.CONSIDER_COLUMN_GAPS) ? "y" : "n");
        
        // p13: COMPARE_ON_FORMULA_STRING: "y"/"n"
        str.append("&p13=").append(settings.get(SettingKeys.COMPARE_ON_FORMULA_STRING) ? "y" : "n");
        
        // p18: MATCH_NAMES_LOOSELY: "y"/"n"
        str.append("&p18=").append(settings.get(SettingKeys.MATCH_NAMES_LOOSELY) ? "y" : "n");
        
        // p14: SHOW_PAINTED_SHEETS: "y"/"n"
        str.append("&p14=").append(settings.get(SettingKeys.SHOW_PAINTED_SHEETS) ? "y" : "n");
        
        // p15: SHOW_RESULT_REPORT: "y"/"n"
        str.append("&p15=").append(settings.get(SettingKeys.SHOW_RESULT_REPORT) ? "y" : "n");
        
        // p16: EXIT_WHEN_FINISHED: "y"/"n"
        str.append("&p16=").append(settings.get(SettingKeys.EXIT_WHEN_FINISHED) ? "y" : "n");
        
        // p17: PRIORITIZE_SPEED: "y"/"n"
        str.append("&p17=").append(settings.get(SettingKeys.PRIORITIZE_SPEED) ? "y" : "n");
        
        // p21: CURR_MENU: "S"/"B"/"D"/"T"
        str.append("&p21=").append(switch (settings.get(SettingKeys.CURR_MENU)) {
            case COMPARE_SHEETS -> "S";
            case COMPARE_BOOKS -> "B";
            case COMPARE_DIRS -> "D";
            case COMPARE_TREES -> "T";
        });
        
        // p22: executedAt: "2024-08-11T09%3A18%3A22.556913700Z"
        str.append("&p22=").append(start.toString().replace(":", "%3A"));
        
        // p23: elapsedMillis: "9999"
        str.append("&p23=").append(Duration.between(start, end).toMillis());
        
        str.append(toUrlParamString2());
        
        return str.toString();
    }
    
    /**
     * サブクラス独自内容のURLクエリパラメータ形式の文字列表現を返します。<br>
     * 
     * @return サブクラス独自内容のURLクエリパラメータ形式の文字列表現
     */
    protected abstract String toUrlParamString2();
}
