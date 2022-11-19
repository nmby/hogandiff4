package xyz.hotchpotch.hogandiff;

import java.awt.Color;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.IndexedColors;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * このアプリケーションの設定項目を集めたクラスです。<br>
 *
 * @author nmby
 */
public class SettingKeys {
    
    // [static members] ********************************************************
    
    /** 作業用フォルダの作成場所のパス */
    public static final Key<Locale> APP_LOCALE = new Key<>(
            "application.appLocale",
            () -> Locale.JAPANESE,
            Locale::toLanguageTag,
            Locale::forLanguageTag,
            true);
    
    /** 作業用フォルダの作成場所のパス */
    public static final Key<Path> WORK_DIR_BASE = new Key<Path>(
            "application.workDirBase",
            () -> Path.of(
                    System.getProperty("user.home"),
                    AppMain.APP_DOMAIN),
            Path::toString,
            Path::of,
            true);
    
    /** 設定エリアを表示するか */
    public static final Key<Boolean> SHOW_SETTINGS = new Key<Boolean>(
            "application.showSettings",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** メインステージの縦幅 */
    public static final Key<Double> STAGE_HEIGHT = new Key<Double>(
            "application.height",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            String::valueOf,
            Double::valueOf,
            true);
    
    /** メインステージの横幅 */
    public static final Key<Double> STAGE_WIDTH = new Key<Double>(
            "application.width",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            String::valueOf,
            Double::valueOf,
            true);
    
    /** 今回の実行を識別するためのタイムスタンプタグ */
    public static final Key<String> CURR_TIMESTAMP = new Key<String>(
            "current.timestamp",
            () -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")),
            Function.identity(),
            Function.identity(),
            false);
    
    /** 今回の実行における比較メニュー */
    public static final Key<AppMenu> CURR_MENU = new Key<AppMenu>(
            "current.menu",
            () -> AppMenu.COMPARE_BOOKS,
            AppMenu::toString,
            AppMenu::valueOf,
            false);
    
    /** 今回の実行における比較対象Excelブック1の情報 */
    public static final Key<BookInfo> CURR_BOOK_INFO1 = new Key<BookInfo>(
            "current.bookInfo1",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            BookInfo::toString,
            null,
            false);
    
    /** 今回の実行における比較対象Excelブック2の情報 */
    public static final Key<BookInfo> CURR_BOOK_INFO2 = new Key<BookInfo>(
            "current.bookInfo2",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            BookInfo::toString,
            null,
            false);
    
    /** 今回の実行における比較対象Excelシート1の名前 */
    public static final Key<String> CURR_SHEET_NAME1 = new Key<String>(
            "current.sheetName1",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Function.identity(),
            Function.identity(),
            false);
    
    /** 今回の実行における比較対象Excelシート2の名前 */
    public static final Key<String> CURR_SHEET_NAME2 = new Key<String>(
            "current.sheetName2",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Function.identity(),
            Function.identity(),
            false);
    
    /**
     * Excelシート同士の比較において、
     * 行の挿入／削除を考慮する（{@code true}）か考慮しない（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> CONSIDER_ROW_GAPS = new Key<Boolean>(
            "compare.considerRowGaps",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * Excelシート同士の比較において、
     * 列の挿入／削除を考慮する（{@code true}）か考慮しない（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> CONSIDER_COLUMN_GAPS = new Key<Boolean>(
            "compare.considerColumnGaps",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * Excelセル内容の比較において、セルの内容が数式の場合に
     * 数式文字列を比較する（{@code true}）か
     * Excelファイルにキャッシュされている計算結果の値を比較する（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> COMPARE_ON_FORMULA_STRING = new Key<Boolean>(
            "compare.compareOnFormulaString",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * シート名同士の対応付けにおいて完全一致（{@code true}）でマッチングするか
     * ある程度の揺らぎを許容する（{@code flase}）かを表します。<br>
     */
    public static final Key<Boolean> MATCH_NAMES_STRICTLY = new Key<Boolean>(
            "compare.matchNamesStrictly",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            false);
    
    /**
     * 比較結果のレポートにおいて、余剰行・余剰列に着ける色のインデックス値を表します。<br>
     */
    public static final Key<Short> REDUNDANT_COLOR = new Key<Short>(
            "report.redundantColor",
            () -> IndexedColors.CORAL.getIndex(),
            String::valueOf,
            Short::valueOf,
            false);
    
    /**
     * 比較結果のレポートにおいて、差分セルに着ける色のインデックス値を表します。<br>
     */
    public static final Key<Short> DIFF_COLOR = new Key<Short>(
            "report.diffColor",
            () -> IndexedColors.YELLOW.getIndex(),
            String::valueOf,
            Short::valueOf,
            false);
    
    /**
     * 比較結果のレポートにおいて、余剰セルコメントに着ける色を表します。<br>
     */
    public static final Key<Color> REDUNDANT_COMMENT_COLOR = new Key<Color>(
            "report.redundantCommentColor",
            () -> new Color(255, 128, 128),
            color -> "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /**
     * 比較結果のレポートにおいて、差分セルコメントに着ける色を表します。<br>
     */
    public static final Key<Color> DIFF_COMMENT_COLOR = new Key<Color>(
            "report.diffCommentColor",
            () -> Color.YELLOW,
            color -> "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /**
     * 比較結果のレポートにおいて、余剰シートの見出しに着ける色を表します。<br>
     */
    public static final Key<Color> REDUNDANT_SHEET_COLOR = new Key<Color>(
            "report.redundantSheetColor",
            () -> Color.RED,
            color -> "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /**
     * 比較結果のレポートにおいて、差分シートの見出しに着ける色を表します。<br>
     */
    public static final Key<Color> DIFF_SHEET_COLOR = new Key<Color>(
            "report.diffSheetColor",
            () -> Color.YELLOW,
            color -> "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /**
     * 比較結果のレポートにおいて、差分無しシートの見出しに着ける色を表します。<br>
     */
    public static final Key<Color> SAME_SHEET_COLOR = new Key<Color>(
            "report.sameSheetColor",
            () -> Color.CYAN,
            color -> "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /** レポートオプション：差分個所に色を付けたシートを表示するか */
    public static final Key<Boolean> SHOW_PAINTED_SHEETS = new Key<Boolean>(
            "report.showPaintedSheets",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** レポートオプション：比較結果が記載されたテキストを表示するか */
    public static final Key<Boolean> SHOW_RESULT_TEXT = new Key<Boolean>(
            "report.showResultText",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** 実行オプション：比較完了時にこのアプリを終了するか */
    public static final Key<Boolean> EXIT_WHEN_FINISHED = new Key<Boolean>(
            "execution.exitWhenFinished",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** 実行オプション：省メモリモードで比較するか */
    public static final Key<Boolean> SAVE_MEMORY = new Key<Boolean>(
            "execution.saveMemory",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** 全ての定義済み設定項目を含むセット */
    // Collectors#toSet は現在の実装では immutable set を返すが
    // 保証されないということなので、一応 Set#copyOf でラップしておく。
    public static final Set<Key<?>> keys = Set.copyOf(
            Stream.of(SettingKeys.class.getFields())
                    .filter(f -> f.getType() == Key.class && Modifier.isPublic(f.getModifiers()))
                    .map(f -> {
                        try {
                            return (Key<?>) f.get(null);
                        } catch (IllegalAccessException e) {
                            throw new AssertionError(e);
                        }
                    })
                    .collect(Collectors.toSet()));
    
    /** プロパティファイルに保存可能な設定項目を含むセット */
    public static final Set<Key<?>> storableKeys = Set.copyOf(keys.stream()
            .filter(Key::storable)
            .collect(Collectors.toSet()));
    
    // [instance members] ******************************************************
    
    private SettingKeys() {
    }
}
