package xyz.hotchpotch.hogandiff;

import java.awt.Color;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.IndexedColors;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookInfoComparison;
import xyz.hotchpotch.hogandiff.excel.DirInfoComparison;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * このアプリケーションの設定項目を集めたクラスです。<br>
 *
 * @author nmby
 */
public class SettingKeys {
    
    // [static members] ********************************************************
    
    private static <T> Function<T, String> encodeNotSupported(String msg) {
        return obj -> {
            throw new UnsupportedOperationException(msg);
        };
    }
    
    private static <T> Function<String, T> decodeNotSupported(String msg) {
        return str -> {
            throw new UnsupportedOperationException(msg);
        };
    }
    
    /** クライアント上で生成されたUUID */
    public static final Key<UUID> CLIENT_UUID = new Key<>(
            "client.uuid",
            () -> null,
            UUID::toString,
            UUID::fromString,
            true);
    
    /** このアプリケーションの実行したことのあるバージョン */
    public static final Key<String> APP_VERSION = new Key<>(
            "application.appVersion",
            () -> null,
            Function.identity(),
            Function.identity(),
            true);
    
    /** このアプリケーションのロケール（表示言語） */
    public static final Key<Locale> APP_LOCALE = new Key<>(
            "application.appLocale",
            () -> Locale.JAPANESE,
            Locale::toLanguageTag,
            Locale::forLanguageTag,
            true);
    
    /** 作業用フォルダの作成場所のパス */
    public static final Key<Path> WORK_DIR_BASE = new Key<>(
            "application.workDirBase",
            () -> Path.of(System.getProperty("user.home"), AppMain.APP_DOMAIN),
            Path::toString,
            Path::of,
            true);
    
    /** 設定エリアを表示するか */
    public static final Key<Boolean> SHOW_SETTINGS = new Key<>(
            "application.showSettings",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** メインステージの縦幅 */
    public static final Key<Double> STAGE_HEIGHT = new Key<>(
            "application.height",
            () -> null,
            String::valueOf,
            Double::valueOf,
            true);
    
    /** メインステージの横幅 */
    public static final Key<Double> STAGE_WIDTH = new Key<>(
            "application.width",
            () -> null,
            String::valueOf,
            Double::valueOf,
            true);
    
    /** ウィンドウ最大表示化 */
    public static final Key<Boolean> STAGE_MAXIMIZED = new Key<>(
            "application.maximized",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** 今回の実行を識別するためのタイムスタンプタグ */
    public static final Key<String> CURR_TIMESTAMP = new Key<>(
            "current.timestamp",
            () -> null,
            Function.identity(),
            Function.identity(),
            false);
    
    /** 今回の実行における比較メニュー */
    public static final Key<AppMenu> CURR_MENU = new Key<>(
            "current.menu",
            () -> AppMenu.COMPARE_BOOKS,
            AppMenu::toString,
            AppMenu::valueOf,
            false);
    
    /** 比較対象Excelブックたちの読み取りパスワード */
    public static final Key<Map<Path, String>> CURR_READ_PASSWORDS = new Key<>(
            "current.readPasswords",
            () -> Map.of(),
            encodeNotSupported("cannot encode."),
            decodeNotSupported("cannot decode."),
            false);
    
    /** 今回の実行における比較対象Excelブック1の情報 */
    public static final Key<BookInfo> CURR_BOOK_INFO1 = new Key<>(
            "current.bookInfo1",
            () -> null,
            encodeNotSupported("cannot encode."),
            decodeNotSupported("cannot decode."),
            false);
    
    /** 今回の実行における比較対象Excelブック2の情報 */
    public static final Key<BookInfo> CURR_BOOK_INFO2 = new Key<>(
            "current.bookInfo2",
            () -> null,
            encodeNotSupported("cannot encode."),
            decodeNotSupported("cannot decode."),
            false);
    
    /** {@link #CURR_BOOK_INFO1}, {@link #CURR_BOOK_INFO2} のペア */
    public static final Pair<Key<BookInfo>> CURR_BOOK_INFOS = Pair.of(
            CURR_BOOK_INFO1,
            CURR_BOOK_INFO2);
    
    /** 今回の実行におけるシート比較情報 */
    public static final Key<BookInfoComparison> CURR_SHEET_COMPARE_INFO = new Key<>(
            "current.sheetComparison",
            () -> null,
            encodeNotSupported("cannot encode."),
            decodeNotSupported("cannnot decode."),
            false);
    
    /** 今回の実行におけるExcelブック比較情報 */
    public static final Key<BookInfoComparison> CURR_BOOK_COMPARE_INFO = new Key<>(
            "current.bookComparison",
            () -> null,
            encodeNotSupported("cannot encode."),
            decodeNotSupported("cannnot decode."),
            false);
    
    /** 今回の実行におけるフォルダ比較情報 */
    public static final Key<DirInfoComparison> CURR_DIR_COMPARE_INFO = new Key<>(
            "current.dirComparison",
            () -> null,
            encodeNotSupported("cannot encode."),
            decodeNotSupported("cannnot decode."),
            false);
    
    /** 今回の実行におけるフォルダツリー比較情報 */
    public static final Key<DirInfoComparison> CURR_TREE_COMPARE_INFO = new Key<>(
            "current.treeComparison",
            () -> null,
            encodeNotSupported("cannot encode."),
            decodeNotSupported("cannnot decode."),
            false);
    
    /** 行の挿入／削除を考慮するか */
    public static final Key<Boolean> CONSIDER_ROW_GAPS = new Key<>(
            "compare.considerRowGaps",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** 列の挿入／削除を考慮するか */
    public static final Key<Boolean> CONSIDER_COLUMN_GAPS = new Key<>(
            "compare.considerColumnGaps",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * セルの内容が数式の場合に数式文字列を比較する（{@code true}）か、
     * Excelファイルにキャッシュされている計算結果の値を比較する（{@code false}）か
     */
    public static final Key<Boolean> COMPARE_ON_FORMULA_STRING = new Key<>(
            "compare.compareOnFormulaString",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * シート名同士の対応付けにおいて完全一致でマッチングする（{@code true}）か、
     * ある程度の揺らぎを許容する（{@code flase}）か
     */
    public static final Key<Boolean> MATCH_NAMES_STRICTLY = new Key<>(
            "compare.matchNamesStrictly",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            false);
    
    /** 子フォルダも再帰的に比較するか */
    public static final Key<Boolean> COMPARE_DIRS_RECURSIVELY = new Key<>(
            "compare.compareDirsRecursively",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            false);
    
    /** 比較結果レポートにおける、余剰行・余剰列に着ける色のインデックス値 */
    public static final Key<Short> REDUNDANT_COLOR = new Key<>(
            "report.redundantColor",
            () -> IndexedColors.CORAL.getIndex(),
            String::valueOf,
            Short::valueOf,
            false);
    
    /** 比較結果レポートにおける、差分セルに着ける色のインデックス値 */
    public static final Key<Short> DIFF_COLOR = new Key<>(
            "report.diffColor",
            () -> IndexedColors.YELLOW.getIndex(),
            String::valueOf,
            Short::valueOf,
            false);
    
    /** 比較結果レポートにおける、余剰セルコメントに着ける色 */
    public static final Key<Color> REDUNDANT_COMMENT_COLOR = new Key<>(
            "report.redundantCommentColor",
            () -> new Color(255, 128, 128),
            color -> "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /** 比較結果レポートにおける、差分セルコメントに着ける色 */
    public static final Key<Color> DIFF_COMMENT_COLOR = new Key<>(
            "report.diffCommentColor",
            () -> Color.YELLOW,
            color -> "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /** 比較結果レポートにおける、余剰シートの見出しに着ける色 */
    public static final Key<Color> REDUNDANT_SHEET_COLOR = new Key<>(
            "report.redundantSheetColor",
            () -> Color.RED,
            color -> "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /** 比較結果レポートにおける、差分シートの見出しに着ける色 */
    public static final Key<Color> DIFF_SHEET_COLOR = new Key<>(
            "report.diffSheetColor",
            () -> Color.YELLOW,
            color -> "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /** 比較結果レポートにおける、差分無しシートの見出しに着ける色 */
    public static final Key<Color> SAME_SHEET_COLOR = new Key<>(
            "report.sameSheetColor",
            () -> Color.CYAN,
            color -> "%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /** レポートオプション：差分個所に色を付けたシートを表示するか */
    public static final Key<Boolean> SHOW_PAINTED_SHEETS = new Key<>(
            "report.showPaintedSheets",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** レポートオプション：比較結果が記載されたテキストを表示するか */
    public static final Key<Boolean> SHOW_RESULT_TEXT = new Key<>(
            "report.showResultText",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** 実行オプション：比較完了時にこのアプリを終了するか */
    public static final Key<Boolean> EXIT_WHEN_FINISHED = new Key<>(
            "execution.exitWhenFinished",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** 実行オプション：早さ優先か精度優先か */
    // TODO: ユーザー指定可能オプションで任意の列挙型を取れるようにする
    public static final Key<Boolean> PRIORITIZE_SPEED = new Key<>(
            "execution.prioritizeSpeed",
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
