package xyz.hotchpotch.hogandiff.excel;

import java.awt.Color;
import java.nio.file.Path;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較処理に必要な一連の機能を提供するファクトリです。<br>
 *
 * @author nmby
 */
public class Factory {
    
    // [static members] ********************************************************
    
    /**
     * 新しいファクトリを返します。<br>
     * 
     * @return 新しいファクトリ
     */
    public static Factory of() {
        return new Factory();
    }
    
    // [instance members] ******************************************************
    
    private Factory() {
    }
    
    /**
     * Excelブックからシート名の一覧を抽出するローダーを返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @return Excelブックからシート名の一覧を抽出するローダー
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code bookPath} が {@code null} の場合
     * @throws UnsupportedOperationException
     *              {@code bookPath} がサポート対象外の形式の場合
     */
    public SheetNamesLoader sheetNamesLoader(
            Path bookPath,
            String readPassword)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookPath, "bookPath");
        // readPassword may be null.
        
        return SheetNamesLoader.of(bookPath, readPassword);
    }
    
    /**
     * Excelシートからセルデータを抽出するローダーを返します。<br>
     * 
     * @param settings 設定
     * @param bookPath Excepブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @return Excelシートからセルデータを抽出するローダー
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code settings}, {@code bookPath} のいずれかが {@code null} の場合
     * @throws UnsupportedOperationException {@code bookPath} がサポート対象外の形式の場合
     */
    public CellsLoader cellsLoader(
            Settings settings,
            Path bookPath,
            String readPassword)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(bookPath, "bookPath");
        // readPassword may be null.
        
        // 設計メモ：
        // Settings を扱うのは Factory の層までとし、これ以下の各機能へは
        // Settings 丸ごとではなく、必要な個別のパラメータを渡すこととする。
        
        boolean useCachedValue = !settings.get(SettingKeys.COMPARE_ON_FORMULA_STRING);
        
        return CellsLoader.of(bookPath, readPassword, useCachedValue);
    }
    
    /**
     * フォルダ情報を抽出するローダーを返します。<br>
     * 
     * @param settings 設定
     * @return フォルダ情報を抽出するローダー
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public DirLoader dirLoader(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        boolean recursively = settings.get(SettingKeys.COMPARE_DIRS_RECURSIVELY);
        return DirLoader.of(recursively);
    }
    
    /**
     * 2つのExcelブックに含まれるシート名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param settings 設定
     * @return シート名同士の対応関係を決めるマッチャー
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public SheetNamesMatcher sheetNamesMatcher(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        boolean matchNamesStrictly = settings.get(SettingKeys.MATCH_NAMES_STRICTLY);
        return SheetNamesMatcher.of(matchNamesStrictly);
    }
    
    /**
     * 2つのフォルダに含まれるExcelブック名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param settings 設定
     * @return Excelブック名同士の対応関係を決めるマッチャー
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public BooksMatcher bookNamesMatcher(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        boolean matchNamesStrictly = settings.get(SettingKeys.MATCH_NAMES_STRICTLY);
        return BooksMatcher.of(matchNamesStrictly);
    }
    
    /**
     * 2つのフォルダツリーに含まれるフォルダ同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param settings 設定
     * @return フォルダ同士の対応関係を決めるマッチャー
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public DirsMatcher dirsMatcher(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        boolean matchNamesStrictly = settings.get(SettingKeys.MATCH_NAMES_STRICTLY);
        return DirsMatcher.of(matchNamesStrictly);
    }
    
    /**
     * 2つのExcelシートから抽出したセルセット同士を比較するコンパレータを返します。<br>
     * 
     * @param settings 設定
     * @return セルセット同士を比較するコンパレータ
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public SheetComparator comparator(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        boolean considerRowGaps = settings.get(SettingKeys.CONSIDER_ROW_GAPS);
        boolean considerColumnGaps = settings.get(SettingKeys.CONSIDER_COLUMN_GAPS);
        boolean prioritizeSpeed = settings.get(SettingKeys.PRIORITIZE_SPEED);
        
        return SheetComparator.of(considerRowGaps, considerColumnGaps, prioritizeSpeed);
    }
    
    /**
     * Excelブックの差分個所に色を付けて新しいファイルとして保存する
     * ペインターを返します。<br>
     * 
     * @param settings 設定
     * @param bookPath Excepブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @return Excelブックの差分個所に色を付けて保存するペインター
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code settings}, {@code bookPath} のいずれかが {@code null} の場合
     * @throws UnsupportedOperationException {@code bookPath} がサポート対象外の形式の場合
     */
    public BookPainter painter(
            Settings settings,
            Path bookPath,
            String readPassword)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(bookPath, "bookPath");
        // readPassword may be null.
        
        short redundantColor = settings.get(SettingKeys.REDUNDANT_COLOR);
        short diffColor = settings.get(SettingKeys.DIFF_COLOR);
        Color redundantCommentColor = settings.get(SettingKeys.REDUNDANT_COMMENT_COLOR);
        Color diffCommentColor = settings.get(SettingKeys.DIFF_COMMENT_COLOR);
        // もうなんか滅茶苦茶や・・・
        String redundantCommentHex = "#" + SettingKeys.REDUNDANT_COMMENT_COLOR.encoder().apply(redundantCommentColor);
        String diffCommentHex = "#" + SettingKeys.DIFF_COMMENT_COLOR.encoder().apply(diffCommentColor);
        Color redundantSheetColor = settings.get(SettingKeys.REDUNDANT_SHEET_COLOR);
        Color diffSheetColor = settings.get(SettingKeys.DIFF_SHEET_COLOR);
        Color sameSheetColor = settings.get(SettingKeys.SAME_SHEET_COLOR);
        
        return BookPainter.of(
                bookPath,
                readPassword,
                redundantColor,
                diffColor,
                redundantCommentColor,
                diffCommentColor,
                redundantCommentHex,
                diffCommentHex,
                redundantSheetColor,
                diffSheetColor,
                sameSheetColor);
    }
}
