package xyz.hotchpotch.hogandiff;

import java.awt.Color;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.IndexedColors;

import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * このアプリケーションの設定項目を集めたクラスです。<br>
 *
 * @author nmby
 */
public class SettingKeys {
    
    // [static members] ********************************************************
    
    /** 作業用フォルダの作成場所のパス */
    public static final Key<Path> WORK_DIR_BASE = new Key<>(
            "application.system.workDirBase",
            () -> Path.of(
                    System.getProperty("user.home"),
                    AppMain.APP_DOMAIN),
            Path::toString,
            Path::of,
            true);
    
    /** 今回の実行を識別するためのタイムスタンプタグ */
    public static final Key<String> CURR_TIMESTAMP = new Key<>(
            "application.current.timestamp",
            () -> LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")),
            Function.identity(),
            Function.identity(),
            false);
    
    /** 今回の実行における比較メニュー */
    public static final Key<AppMenu> CURR_MENU = new Key<>(
            "application.current.menu",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            AppMenu::toString,
            AppMenu::valueOf,
            false);
    
    /** 今回の実行における比較対象Excelブック1のパス */
    public static final Key<Path> CURR_BOOK_PATH1 = new Key<>(
            "application.current.bookPath1",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Path::toString,
            Path::of,
            false);
    
    /** 今回の実行における比較対象Excelブック2のパス */
    public static final Key<Path> CURR_BOOK_PATH2 = new Key<>(
            "application.current.bookPath2",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Path::toString,
            Path::of,
            false);
    
    /** 今回の実行における比較対象Excelシート1の名前 */
    public static final Key<String> CURR_SHEET_NAME1 = new Key<>(
            "application.current.sheetName1",
            () -> {
                throw new UnsupportedOperationException("the key has no default value.");
            },
            Function.identity(),
            Function.identity(),
            false);
    
    /** 今回の実行における比較対象Excelシート2の名前 */
    public static final Key<String> CURR_SHEET_NAME2 = new Key<>(
            "application.current.sheetName2",
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
    public static final Key<Boolean> CONSIDER_ROW_GAPS = new Key<>(
            "compare.considerRowGaps",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * Excelシート同士の比較において、
     * 列の挿入／削除を考慮する（{@code true}）か考慮しない（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> CONSIDER_COLUMN_GAPS = new Key<>(
            "compare.considerColumnGaps",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * Excelセルの比較において、セル内容を比較するかを表します。<br>
     */
    public static final Key<Boolean> COMPARE_CELL_CONTENTS = new Key<>(
            "compare.compareCellContents",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * Excelセルの比較において、セルコメントを比較するかを表します。<br>
     */
    public static final Key<Boolean> COMPARE_CELL_COMMENTS = new Key<>(
            "compare.compareCellComments",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * Excelセル内容の比較において、セルの内容が数式の場合に
     * 数式文字列を比較する（{@code true}）か
     * Excelファイルにキャッシュされている計算結果の値を比較する（{@code false}）かを表します。<br>
     */
    public static final Key<Boolean> COMPARE_ON_FORMULA_STRING = new Key<>(
            "compare.compareOnFormulaString",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /**
     * 比較結果のレポートにおいて、余剰行・余剰列に着ける色のインデックス値を表します。<br>
     */
    public static final Key<Short> REDUNDANT_COLOR = new Key<>(
            "report.redundantColor",
            () -> IndexedColors.CORAL.getIndex(),
            String::valueOf,
            Short::valueOf,
            false);
    
    /**
     * 比較結果のレポートにおいて、差分セルに着ける色のインデックス値を表します。<br>
     */
    public static final Key<Short> DIFF_COLOR = new Key<>(
            "report.diffColor",
            () -> IndexedColors.YELLOW.getIndex(),
            String::valueOf,
            Short::valueOf,
            false);
    
    /**
     * 比較結果のレポートにおいて、余剰セルコメントに着ける色を表します。<br>
     */
    public static final Key<Color> REDUNDANT_COMMENT_COLOR = new Key<>(
            "report.redundantCommentColor",
            () -> new Color(255, 128, 128),
            color -> String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /**
     * 比較結果のレポートにおいて、差分セルコメントに着ける色を表します。<br>
     */
    public static final Key<Color> DIFF_COMMENT_COLOR = new Key<>(
            "report.diffCommentColor",
            () -> Color.YELLOW,
            color -> String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /**
     * 比較結果のレポートにおいて、余剰シートの見出しに着ける色を表します。<br>
     */
    public static final Key<Color> REDUNDANT_SHEET_COLOR = new Key<>(
            "report.redundantSheetColor",
            () -> Color.RED,
            color -> String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /**
     * 比較結果のレポートにおいて、差分シートの見出しに着ける色を表します。<br>
     */
    public static final Key<Color> DIFF_SHEET_COLOR = new Key<>(
            "report.diffSheetColor",
            () -> Color.YELLOW,
            color -> String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /**
     * 比較結果のレポートにおいて、差分無しシートの見出しに着ける色を表します。<br>
     */
    public static final Key<Color> SAME_SHEET_COLOR = new Key<>(
            "report.sameSheetColor",
            () -> Color.CYAN,
            color -> String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue()),
            Color::decode,
            false);
    
    /** レポートオプション：差分個所に色を付けたシートを表示するか */
    public static final Key<Boolean> SHOW_PAINTED_SHEETS = new Key<>(
            "application.report.showPaintedSheets",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** レポートオプション：比較結果が記載されたテキストを表示するか */
    public static final Key<Boolean> SHOW_RESULT_TEXT = new Key<>(
            "application.report.showResultText",
            () -> true,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** 実行オプション：比較完了時にこのアプリを終了するか */
    public static final Key<Boolean> EXIT_WHEN_FINISHED = new Key<>(
            "application.execution.exitWhenFinished",
            () -> false,
            String::valueOf,
            Boolean::valueOf,
            true);
    
    /** 実行オプション：省メモリモードで比較するか */
    public static final Key<Boolean> SAVE_MEMORY = new Key<>(
            "application.execution.saveMemory",
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
