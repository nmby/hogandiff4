package xyz.hotchpotch.hogandiff.excel;

import java.awt.Color;
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
     * @param bookOpenInfo Excelブックの情報
     * @return Excelブックからシート名の一覧を抽出するローダー
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code bookOpenInfo} が {@code null} の場合
     * @throws UnsupportedOperationException
     *              {@code bookOpenInfo} がサポート対象外の形式の場合
     */
    public SheetNamesLoader sheetNamesLoader(
            BookOpenInfo bookOpenInfo)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        
        return SheetNamesLoader.of(bookOpenInfo);
    }
    
    /**
     * Excelシートからセルデータを抽出するローダーを返します。<br>
     * 
     * @param settings 設定
     * @param bookOpenInfo Excelブックの情報
     * @return Excelシートからセルデータを抽出するローダー
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code settings}, {@code bookOpenInfo} のいずれかが {@code null} の場合
     * @throws UnsupportedOperationException
     *              {@code bookOpenInfo} がサポート対象外の形式の場合
     */
    public CellsLoader cellsLoader(
            Settings settings,
            BookOpenInfo bookOpenInfo)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        
        // 設計メモ：
        // Settings を扱うのは Factory の層までとし、これ以下の各機能へは
        // Settings 丸ごとではなく、必要な個別のパラメータを渡すこととする。
        
        boolean useCachedValue = !settings.getOrDefault(SettingKeys.COMPARE_ON_FORMULA_STRING);
        boolean saveMemory = settings.getOrDefault(SettingKeys.SAVE_MEMORY);
        
        return CellsLoader.of(
                bookOpenInfo,
                useCachedValue,
                saveMemory);
    }
    
    /**
     * フォルダ情報を抽出するローダーを返します。<br>
     * 
     * @return フォルダ情報を抽出するローダー
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public DirLoader dirLoader(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        boolean recursively = settings.getOrDefault(SettingKeys.COMPARE_DIRS_RECURSIVELY);
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
        
        boolean matchNamesStrictly = settings.getOrDefault(SettingKeys.MATCH_NAMES_STRICTLY);
        return SheetNamesMatcher.of(matchNamesStrictly);
    }
    
    /**
     * 2つのフォルダに含まれるExcelブック名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param settings 設定
     * @return Excelブック名同士の対応関係を決めるマッチャー
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public BookNamesMatcher bookNamesMatcher(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        boolean matchNamesStrictly = settings.getOrDefault(SettingKeys.MATCH_NAMES_STRICTLY);
        return BookNamesMatcher.of(matchNamesStrictly);
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
        
        boolean matchNamesStrictly = settings.getOrDefault(SettingKeys.MATCH_NAMES_STRICTLY);
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
        
        boolean considerRowGaps = settings.getOrDefault(SettingKeys.CONSIDER_ROW_GAPS);
        boolean considerColumnGaps = settings.getOrDefault(SettingKeys.CONSIDER_COLUMN_GAPS);
        boolean saveMemory = settings.getOrDefault(SettingKeys.SAVE_MEMORY);
        
        return SheetComparator.of(
                considerRowGaps,
                considerColumnGaps,
                saveMemory);
    }
    
    /**
     * Excelブックの差分個所に色を付けて新しいファイルとして保存する
     * ペインターを返します。<br>
     * 
     * @param settings 設定
     * @param bookOpenInfo Excelブックの情報
     * @return Excelブックの差分個所に色を付けて保存するペインター
     * @throws ExcelHandlingException 処理に失敗した場合
     * @throws NullPointerException
     *              {@code settings}, {@code bookOpenInfo} のいずれかが {@code null} の場合
     * @throws UnsupportedOperationException
     *              {@code bookOpenInfo} がサポート対象外の形式の場合
     */
    public BookPainter painter(
            Settings settings,
            BookOpenInfo bookOpenInfo)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        
        short redundantColor = settings.getOrDefault(SettingKeys.REDUNDANT_COLOR);
        short diffColor = settings.getOrDefault(SettingKeys.DIFF_COLOR);
        Color redundantCommentColor = settings.getOrDefault(SettingKeys.REDUNDANT_COMMENT_COLOR);
        Color diffCommentColor = settings.getOrDefault(SettingKeys.DIFF_COMMENT_COLOR);
        // もうなんか滅茶苦茶や・・・
        String redundantCommentHex = "#" + SettingKeys.REDUNDANT_COMMENT_COLOR.encoder().apply(redundantCommentColor);
        String diffCommentHex = "#" + SettingKeys.DIFF_COMMENT_COLOR.encoder().apply(diffCommentColor);
        Color redundantSheetColor = settings.getOrDefault(SettingKeys.REDUNDANT_SHEET_COLOR);
        Color diffSheetColor = settings.getOrDefault(SettingKeys.DIFF_SHEET_COLOR);
        Color sameSheetColor = settings.getOrDefault(SettingKeys.SAME_SHEET_COLOR);
        
        return BookPainter.of(
                bookOpenInfo,
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
