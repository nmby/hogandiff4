package xyz.hotchpotch.hogandiff.logic;

import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Excelブックの形式を表す列挙型です。<br>
 * 
 * @author nmby
 */
public enum BookType {

    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /** .xlsx 形式 */
    XLSX(".xlsx"),

    /** .xlsm 形式 */
    XLSM(".xlsm"),

    /** .xlsb 形式 */
    XLSB(".xlsb"),

    /** .xls 形式 */
    XLS(".xls");

    /**
     * 指定されたExcelブックの形式を返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @return Excelブックの形式
     * @throws NullPointerException     パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code bookPath} がどの形式にも該当しない場合
     */
    public static BookType of(Path bookPath) {
        Objects.requireNonNull(bookPath);

        return Stream.of(values())
                .filter(type -> bookPath.toString().endsWith(type.extension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown BookType: " + bookPath));
    }

    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++

    private final String extension;

    private BookType(String extension) {
        assert extension != null;

        this.extension = extension;
    }

    /**
     * このブック形式の拡張子（{@code ".xlsx"} など）を返します。<br>
     * 
     * @return このブック形式の拡張子
     */
    public String extension() {
        return extension;
    }
}
