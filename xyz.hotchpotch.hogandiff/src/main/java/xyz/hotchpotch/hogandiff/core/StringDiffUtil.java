package xyz.hotchpotch.hogandiff.core;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import xyz.hotchpotch.hogandiff.util.IntPair;

/**
 * 文字列同士のdiffに関する機能を提供するユーティリティクラスです。<br>
 * <br>
 * 注意：
 * 本クラスが提供する機能は {@link Matcher} の実装を利用しています。
 * {@link Matcher} およびその実装は様々な評価関数を利用し様々な要素型のリストを処理できるように
 * 汎用的に設計されています。
 * 必ずしも文字列同士の比較に特化したものではないため、低速です。
 * パフォーマンスが重視される場合は、文字列比較専用のライブラリを利用することをお勧めします。<br>
 * 
 * @author nmby
 */
public class StringDiffUtil {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private static final Matcher<Integer> codeMatcher = new MinimumEditDistanceMatcher<>(
            x -> 1,
            (x, y) -> x.equals(y) ? 0 : 3);
    
    /**
     * 2つの文字列間のレーベンシュタイン距離を返します。<br>
     * 一文字の挿入と削除はそれぞれ距離1と評価します。
     * 一文字の置換は削除＋挿入とみなし、従って距離2と評価します。<br>
     * 
     * @param str1 文字列1
     * @param str2 文字列2
     * @return 2つの文字列間のレーベンシュタイン距離
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static int levenshteinDistance(String str1, String str2) {
        Objects.requireNonNull(str1);
        Objects.requireNonNull(str2);
        
        // 特殊ケースのためのショートカットたち
        if (str1 == str2 || str1.equals(str2)) {
            return 0;
        }
        if (str1.length() == 0) {
            return str2.codePointCount(0, str2.length());
        }
        if (str2.length() == 0) {
            return str1.codePointCount(0, str1.length());
        }
        
        // 一般ケース
        // サロゲートペアの扱いはこれで良いはず
        List<Integer> codePoints1 = str1.codePoints().boxed().toList();
        List<Integer> codePoints2 = str2.codePoints().boxed().toList();
        List<IntPair> pairs = codeMatcher.makeIdxPairs(codePoints1, codePoints2);
        
        return (int) pairs.stream()
                .filter(Predicate.not(IntPair::isPaired))
                .count();
    }
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private StringDiffUtil() {
    }
}
