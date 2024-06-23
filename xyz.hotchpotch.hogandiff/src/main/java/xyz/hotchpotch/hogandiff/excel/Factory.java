package xyz.hotchpotch.hogandiff.excel;

import java.awt.Color;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.core.StringDiffUtil;
import xyz.hotchpotch.hogandiff.util.IntPair;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較処理に必要な一連の機能を提供するファクトリです。<br>
 *
 * @author nmby
 */
public class Factory {
    
    // [static members] ********************************************************
    
    /**
     * Excelブック情報を抽出するローダーを返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return Excelブックからシート名の一覧を抽出するローダー
     * @throws NullPointerException {@code bookPath} が {@code null} の場合
     * @throws UnsupportedOperationException {@code bookPath} がサポート対象外の形式の場合
     */
    public static BookLoader bookLoader(Path bookPath) {
        Objects.requireNonNull(bookPath);
        
        return BookLoader.of(bookPath);
    }
    
    /**
     * Excelシートからセルデータを抽出するローダーを返します。<br>
     * 
     * @param settings 設定
     * @param bookPath Excepブックのパス
     * @return Excelシートからセルデータを抽出するローダー
     * @throws NullPointerException {@code settings}, {@code bookPath} のいずれかが {@code null} の場合
     * @throws UnsupportedOperationException {@code bookPath} がサポート対象外の形式の場合
     */
    public static CellsLoader cellsLoader(Settings settings, Path bookPath) {
        Objects.requireNonNull(settings);
        Objects.requireNonNull(bookPath);
        
        // 設計メモ：
        // Settings を扱うのは Factory の層までとし、これ以下の各機能へは
        // Settings 丸ごとではなく、必要な個別のパラメータを渡すこととする。
        
        boolean useCachedValue = !settings.get(SettingKeys.COMPARE_ON_FORMULA_STRING);
        
        return CellsLoader.of(bookPath, useCachedValue);
    }
    
    /**
     * フォルダ情報を抽出するローダーを返します。<br>
     * 
     * @param settings 設定
     * @return フォルダ情報を抽出するローダー
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static DirLoader dirLoader(Settings settings) {
        Objects.requireNonNull(settings);
        
        boolean recursively = settings.get(SettingKeys.COMPARE_DIRS_RECURSIVELY);
        return DirLoader.of(recursively);
    }
    
    /**
     * 2つのExcelブックに含まれるシート名同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param settings 設定
     * @return シート名同士の対応関係を決めるマッチャー
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static Matcher<String> sheetNamesMatcher(Settings settings) {
        Objects.requireNonNull(settings);
        
        boolean matchNamesStrictly = settings.get(SettingKeys.MATCH_NAMES_STRICTLY);
        return matchNamesStrictly
                ? Matcher.identityMatcherOf()
                : Matcher.combinedMatcherOf(List.of(
                        Matcher.identityMatcherOf(),
                        Matcher.minimumCostFlowMatcherOf(
                                String::length,
                                (s1, s2) -> StringDiffUtil.levenshteinDistance(s1, s2) + 1)));
    }
    
    /**
     * 2つのフォルダに含まれるExcelブック情報同士の対応関係を決めるマッチャーを返します。<br>
     * Excelブックパスの末尾のファイル名に基づいて対応関係を求めます。<br>
     * 
     * @param settings 設定
     * @return Excelブックパス同士の対応関係を決めるマッチャー
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static Matcher<BookInfo> bookInfosMatcher(Settings settings) {
        Objects.requireNonNull(settings);
        
        boolean matchNamesStrictly = settings.get(SettingKeys.MATCH_NAMES_STRICTLY);
        return matchNamesStrictly
                ? Matcher.identityMatcherOf()
                : Matcher.combinedMatcherOf(List.of(
                        Matcher.identityMatcherOf(),
                        Matcher.minimumCostFlowMatcherOf(
                                bookInfo -> bookInfo.toString().length(),
                                (bookInfo1, bookInfo2) -> {
                                    String bookName1 = bookInfo1.toString();
                                    String bookName2 = bookInfo2.toString();
                                    return StringDiffUtil.levenshteinDistance(bookName1, bookName2) + 1;
                                })));
    }
    
    /**
     * 2つのフォルダツリーに含まれるフォルダ同士の対応関係を決めるマッチャーを返します。<br>
     * 
     * @param settings 設定
     * @return フォルダ同士の対応関係を決めるマッチャー
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static Matcher<DirInfo> dirInfosMatcher(Settings settings) {
        Objects.requireNonNull(settings);
        
        boolean matchNamesStrictly = settings.get(SettingKeys.MATCH_NAMES_STRICTLY);
        return matchNamesStrictly
                ? strictDirInfosMatcher
                : Matcher.combinedMatcherOf(List.of(
                        strictDirInfosMatcher,
                        fuzzyButSimpleDirInfosMatcher));
    }
    
    private static final Function<DirInfo, String> dirNameExtractor = d -> d.dirPath().getFileName().toString();
    
    private static final Matcher<DirInfo> strictDirInfosMatcher = Matcher.identityMatcherOf(dirNameExtractor);
    
    private static final Matcher<DirInfo> fuzzyButSimpleDirInfosMatcher = Matcher.minimumCostFlowMatcherOf(
            d -> d.childDirInfos().size() + d.childBookInfos().size(),
            (d1, d2) -> {
                List<String> childrenNames1 = d1.childDirInfos().stream().map(dirNameExtractor).toList();
                List<String> childrenNames2 = d2.childDirInfos().stream().map(dirNameExtractor).toList();
                
                int gapChildren = (int) Matcher.identityMatcherOf().makeIdxPairs(childrenNames1, childrenNames2)
                        .stream().filter(Predicate.not(IntPair::isPaired)).count();
                int gapBookNames = (int) Matcher.identityMatcherOf()
                        .makeIdxPairs(d1.childBookInfos(), d2.childBookInfos())
                        .stream().filter(Predicate.not(IntPair::isPaired)).count();
                
                return gapChildren + gapBookNames;
            });
    
    /**
     * 2つのExcelシートから抽出したセルセット同士を比較するコンパレータを返します。<br>
     * 
     * @param settings 設定
     * @return セルセット同士を比較するコンパレータ
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static SheetComparator sheetComparator(Settings settings) {
        Objects.requireNonNull(settings);
        
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
     * @param readPassword Excelブックの読み取りパスワード（{@code null} 許容）
     * @return Excelブックの差分個所に色を付けて保存するペインター
     * @throws NullPointerException {@code settings}, {@code bookPath} のいずれかが {@code null} の場合
     * @throws UnsupportedOperationException {@code bookPath} がサポート対象外の形式の場合
     */
    public static BookPainter painter(
            Settings settings,
            Path bookPath,
            String readPassword) {
        
        Objects.requireNonNull(settings);
        Objects.requireNonNull(bookPath);
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
    
    // [instance members] ******************************************************
    
    private Factory() {
    }
}
