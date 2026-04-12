package xyz.hotchpotch.hogandiff.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Triple;
import xyz.hotchpotch.hogandiff.util.Triple.Side;

/**
 * Excelブック3-way比較情報を表す不変クラスです。<br>
 * 起源（O）に対するA, Bの差分比較のための情報を保持します。<br>
 *
 * @param parentBookInfoTriple 親Excelブック情報（O, A, B）
 * @param childSheetNameTriples 子シート名の組み合わせ（O, A, Bのトリプル）
 * @author nmby
 */
public final record PairingInfoBooksTriple(
        Triple<BookInfo> parentBookInfoTriple,
        List<Triple<String>> childSheetNameTriples)
        implements PairingInfo {

    // [static members] ********************************************************

    /**
     * 与えられたマッチャーを使用して新たな {@link PairingInfoBooksTriple} インスタンスを生成します。<br>
     * <p>
     * シートのマッチングは O を軸として行います。
     * O vs A、O vs B をそれぞれマッチングし、O のシート名をキーに結合します。<br>
     *
     * @param parentBookInfoTriple 比較対象Excelブックの情報（O, A, B）
     * @param sheetNamesMatcher シート名の組み合わせを決めるマッチャー
     * @return 新たなインスタンス
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static PairingInfoBooksTriple calculate(
            Triple<BookInfo> parentBookInfoTriple,
            Matcher<String> sheetNamesMatcher) {

        Objects.requireNonNull(parentBookInfoTriple);
        Objects.requireNonNull(sheetNamesMatcher);

        List<String> oSheets = parentBookInfoTriple.hasO()
                ? parentBookInfoTriple.o().sheetNames()
                : List.of();
        List<String> aSheets = parentBookInfoTriple.hasA()
                ? parentBookInfoTriple.a().sheetNames()
                : List.of();
        List<String> bSheets = parentBookInfoTriple.hasB()
                ? parentBookInfoTriple.b().sheetNames()
                : List.of();

        // O vs A のマッチング（pair.a() = Oシート名, pair.b() = Aシート名）
        List<Pair<String>> oaPairs = sheetNamesMatcher.makeItemPairs(oSheets, aSheets);

        // O vs B のマッチング（pair.a() = Oシート名, pair.b() = Bシート名）
        List<Pair<String>> obPairs = sheetNamesMatcher.makeItemPairs(oSheets, bSheets);

        // Oシート名 → Bシート名 のマップを構築
        Map<String, String> oToB = new HashMap<>();
        List<String> bOnlySheets = new ArrayList<>();
        for (Pair<String> obPair : obPairs) {
            if (obPair.isPaired()) {
                oToB.put(obPair.a(), obPair.b());
            } else if (obPair.isOnlyB()) {
                bOnlySheets.add(obPair.b());
            }
            // isOnlyA（OのみでBに対応なし）は oaPairs 側で処理される
        }

        // OAペアを基にトリプルを構築
        List<Triple<String>> triples = new ArrayList<>();
        for (Pair<String> oaPair : oaPairs) {
            String oName = oaPair.a();  // Oシート名（A専有の場合はnull）
            String aName = oaPair.b();  // Aシート名（O専有の場合はnull）
            String bName = (oName != null) ? oToB.get(oName) : null;  // Bの対応シート名
            triples.add(Triple.of(oName, aName, bName));
        }

        // B専有シートを追加
        for (String bName : bOnlySheets) {
            triples.add(Triple.ofOnly(Side.B, bName));
        }

        return new PairingInfoBooksTriple(parentBookInfoTriple, triples);
    }

    // [instance members] ******************************************************

    /**
     * コンストラクタ
     *
     * @param parentBookInfoTriple Excelブック情報（O, A, B）
     * @param childSheetNameTriples シート名の組み合わせ
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public PairingInfoBooksTriple {
        Objects.requireNonNull(parentBookInfoTriple);
        Objects.requireNonNull(childSheetNameTriples);

        childSheetNameTriples = List.copyOf(childSheetNameTriples);
    }
}
