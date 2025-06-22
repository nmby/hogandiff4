package xyz.hotchpotch.hogandiff.main.misc.excel.common;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import xyz.hotchpotch.hogandiff.main.models.BookType;
import xyz.hotchpotch.hogandiff.main.models.SheetType;

/**
 * 汎用機能を提供するユーティリティクラスです。<br>
 *
 * @author nmby
 */
public class CommonUtil {

    // [static members] ********************************************************

    /**
     * 指定されたクラスが指定されたExcelブックの形式を扱えるかを返します。<br>
     * 
     * @param clazz    検査対象のクラス
     * @param bookType 処理対象のExcelブックの形式
     * @return 指定されたクラスが指定されたExcelブックの形式を扱える場合は {@code true}
     * @throws NullPointerException     パラメータが {@code null} の場合
     * @throws IllegalArgumentException
     *                                  指定されたクラスに {@link BookHandler}
     *                                  アノテーションが付与されていない場合
     */
    public static boolean isSupportedBookType(
            Class<?> clazz,
            BookType bookType) {

        Objects.requireNonNull(clazz);
        Objects.requireNonNull(bookType);

        BookHandler bookHandler = clazz.getAnnotation(BookHandler.class);
        if (bookHandler == null) {
            throw new IllegalArgumentException(
                    "the class [%s] does not have the annotation [%s]".formatted(
                            clazz.getSimpleName(),
                            BookHandler.class.getSimpleName()));
        }

        // お行儀の悪い実装による重複にも耐えられるように Set ではなく List にしている。
        List<BookType> targetTypes = List.of(bookHandler.targetTypes());
        return targetTypes.contains(bookType);
    }

    /**
     * 指定されたクラスが指定されたExcelブックの形式を扱えない場合に例外をスローします。<br>
     * 
     * @param clazz    検査対象のクラス
     * @param bookType 処理対象のExcelブックの形式
     * @throws NullPointerException
     *                                  {@code clazz}, {@code bookType} のいずれかが
     *                                  {@code null} の場合
     * @throws IllegalArgumentException
     *                                  指定されたクラスに {@link BookHandler}
     *                                  アノテーションが付与されていない場合
     * @throws IllegalArgumentException
     *                                  {@code bookPath} がサポート対象外の形式の場合
     */
    public static void ifNotSupportedBookTypeThenThrow(
            Class<?> clazz,
            BookType bookType) {

        if (!isSupportedBookType(clazz, bookType)) {
            throw new IllegalArgumentException("unsupported book type : " + bookType);
        }
    }

    /**
     * 指定されたクラスが指定されたExcelシートの種類を扱えそうかを返します。<br>
     * 
     * @param clazz         検査対象のクラス
     * @param possibleTypes 処理対象のExcelシートのありうる種類
     * @return 指定されたクラスが指定されたExcelシートの種類を扱えそうな場合は {@code true}
     * @throws NullPointerException     パラメータが {@code null} の場合
     * @throws IllegalArgumentException
     *                                  指定されたクラスに {@link SheetHandler}
     *                                  アノテーションが付与されていない場合
     */
    // FIXME: [No.01 シート識別不正] 「ありうる種類」とか「扱えそう」とかの部分をどうにかしたい orz...
    public static boolean isSupportedSheetType(
            Class<?> clazz,
            Set<SheetType> possibleTypes) {

        Objects.requireNonNull(clazz);
        Objects.requireNonNull(possibleTypes);

        SheetHandler sheetHandler = clazz.getAnnotation(SheetHandler.class);
        if (sheetHandler == null) {
            throw new IllegalArgumentException(
                    "the class [%s] does not have the annotation [%s]".formatted(
                            clazz.getSimpleName(),
                            SheetHandler.class.getSimpleName()));
        }

        // お行儀の悪い実装による重複に耐えうるように Set ではなく List にしている。
        List<SheetType> targetTypes = List.of(sheetHandler.targetTypes());
        return possibleTypes.stream().anyMatch(targetTypes::contains);
    }

    /**
     * 指定されたクラスが指定されたExcelシートの種類を扱えない場合に例外をスローします。<br>
     * 
     * @param clazz         検査対象のクラス
     * @param possibleTypes 処理対象のExcelシートのありうる種類
     * @throws NullPointerException
     *                                  {@code clazz}, {@code possibleTypes} のいずれかが
     *                                  {@code null} の場合
     * @throws IllegalArgumentException
     *                                  指定されたクラスに {@link SheetHandler}
     *                                  アノテーションが付与されていない場合
     * @throws IllegalArgumentException
     *                                  {@code possibleTypes} のいずれもがサポートされない種類の場合
     */
    public static void ifNotSupportedSheetTypeThenThrow(
            Class<?> clazz,
            Set<SheetType> possibleTypes) {

        if (!isSupportedSheetType(clazz, possibleTypes)) {
            throw new IllegalArgumentException("unsupported sheet type : "
                    + possibleTypes.stream()
                            .map(SheetType::description)
                            .collect(Collectors.joining(", ")));
        }
    }

    // [instance members] ******************************************************

    private CommonUtil() {
    }
}
