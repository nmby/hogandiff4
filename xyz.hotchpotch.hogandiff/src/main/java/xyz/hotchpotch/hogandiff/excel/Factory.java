package xyz.hotchpotch.hogandiff.excel;

import java.awt.Color;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;

import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.excel.common.CombinedBookPainter;
import xyz.hotchpotch.hogandiff.excel.common.CombinedCellsLoader;
import xyz.hotchpotch.hogandiff.excel.common.CombinedSheetNamesLoader;
import xyz.hotchpotch.hogandiff.excel.common.DirLoaderImpl;
import xyz.hotchpotch.hogandiff.excel.common.SheetComparatorImpl;
import xyz.hotchpotch.hogandiff.excel.poi.eventmodel.HSSFCellsLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.excel.poi.eventmodel.HSSFSheetNamesLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.BookPainterWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.CellsLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.PoiUtil;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.SheetNamesLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.sax.XSSFCellsLoaderWithSax;
import xyz.hotchpotch.hogandiff.excel.sax.XSSFSheetNamesLoaderWithSax;
import xyz.hotchpotch.hogandiff.excel.stax.XSSFBookPainterWithStax;
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
        
        Set<SheetType> targetSheetTypes = EnumSet.of(SheetType.WORKSHEET);
        
        switch (bookOpenInfo.bookType()) {
            case XLS:
                return CombinedSheetNamesLoader.of(List.of(
                        () -> HSSFSheetNamesLoaderWithPoiEventApi.of(targetSheetTypes),
                        () -> SheetNamesLoaderWithPoiUserApi.of(targetSheetTypes)));
            
            case XLSX:
            case XLSM:
                return CombinedSheetNamesLoader.of(List.of(
                        () -> XSSFSheetNamesLoaderWithSax.of(targetSheetTypes),
                        () -> SheetNamesLoaderWithPoiUserApi.of(targetSheetTypes)));
            
            case XLSB:
                // FIXME: [No.2 .xlsbのサポート]
                //throw new UnsupportedOperationException(rb.getString("excel.Factory.010"));
                throw new UnsupportedOperationException("unsupported book type: " + bookOpenInfo.bookType());
            
            default:
                throw new AssertionError("unknown book type: " + bookOpenInfo.bookType());
        }
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
        
        Function<Cell, CellData> converter = cell -> {
            String content = PoiUtil.getCellContentAsString(cell, useCachedValue);
            return "".equals(content)
                    ? null
                    : CellData.of(
                            cell.getRowIndex(),
                            cell.getColumnIndex(),
                            content,
                            saveMemory);
        };
        
        switch (bookOpenInfo.bookType()) {
            case XLS:
                return useCachedValue
                        ? CombinedCellsLoader.of(List.of(
                                () -> HSSFCellsLoaderWithPoiEventApi.of(
                                        useCachedValue,
                                        saveMemory),
                                () -> CellsLoaderWithPoiUserApi.of(
                                        saveMemory,
                                        converter)))
                        : CellsLoaderWithPoiUserApi.of(
                                saveMemory,
                                converter);
            
            case XLSX:
            case XLSM:
                return useCachedValue
                        ? CombinedCellsLoader.of(List.of(
                                () -> XSSFCellsLoaderWithSax.of(
                                        useCachedValue,
                                        saveMemory,
                                        bookOpenInfo),
                                () -> CellsLoaderWithPoiUserApi.of(
                                        saveMemory,
                                        converter)))
                        : CellsLoaderWithPoiUserApi.of(
                                saveMemory,
                                converter);
            
            case XLSB:
                // FIXME: [No.2 .xlsbのサポート]
                throw new UnsupportedOperationException("unsupported book type: " + bookOpenInfo.bookType());
            
            default:
                throw new AssertionError("unknown book type: " + bookOpenInfo.bookType());
        }
    }
    
    /**
     * フォルダ情報を抽出するローダーを返します。<br>
     * 
     * @return フォルダ情報を抽出するローダー
     */
    public DirLoader dirLoader() {
        return DirLoaderImpl.of();
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
     * 2つのフォルダに含まれるExcelブック名の対応付けを行うマッチャーを返します。<br>
     * 
     * @param settings 設定
     * @return Excelブック名の対応付けを行うマッチャー
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public Matcher<String> bookNamesMatcher(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        //TODO: Excelブック名だけでなく内包するシートも加味したマッチャーに改善可能
        
        return settings.getOrDefault(SettingKeys.MATCH_NAMES_STRICTLY)
                ? Matcher.identityMatcher()
                : Matcher.nerutonMatcherOf(
                        String::length,
                        StringDiffUtil::levenshteinDistance);
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
        
        return SheetComparatorImpl.of(
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
        
        switch (bookOpenInfo.bookType()) {
            case XLS:
                return CombinedBookPainter.of(List.of(
                        // FIXME: [No.3 着色関連] 形式特化型ペインターも実装して追加する
                        () -> BookPainterWithPoiUserApi.of(
                                redundantColor,
                                diffColor,
                                redundantCommentColor,
                                diffCommentColor,
                                redundantSheetColor,
                                diffSheetColor,
                                sameSheetColor)));
            
            case XLSX:
            case XLSM:
                return CombinedBookPainter.of(List.of(
                        () -> XSSFBookPainterWithStax.of(
                                redundantColor,
                                diffColor,
                                redundantCommentHex,
                                diffCommentHex,
                                redundantSheetColor,
                                diffSheetColor,
                                sameSheetColor),
                        () -> BookPainterWithPoiUserApi.of(
                                redundantColor,
                                diffColor,
                                redundantCommentColor,
                                diffCommentColor,
                                redundantSheetColor,
                                diffSheetColor,
                                sameSheetColor)));
            
            case XLSB:
                // FIXME: [No.2 .xlsbのサポート]
                throw new UnsupportedOperationException("unsupported book type: " + bookOpenInfo.bookType());
            
            default:
                throw new AssertionError("unknown book type: " + bookOpenInfo.bookType());
        }
    }
}
