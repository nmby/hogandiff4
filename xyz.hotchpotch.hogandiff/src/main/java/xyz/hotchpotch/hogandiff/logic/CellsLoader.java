package xyz.hotchpotch.hogandiff.logic;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;

import xyz.hotchpotch.hogandiff.logic.plain.CellsLoaderCombined;
import xyz.hotchpotch.hogandiff.logic.poi.CellsLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.logic.poi.CellsLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.logic.poi.PoiUtil;
import xyz.hotchpotch.hogandiff.logic.sax.CellsLoaderWithSax;

/**
 * Excelシートからセルデータを抽出するローダーを表します。<br>
 * これは、{@link #loadCells(Path, String, String)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface CellsLoader {

        // [static members] ********************************************************

        /**
         * Excelシートからセルデータを抽出するローダーを返します。<br>
         * 
         * @param bookInfo       Excelブック情報
         * @param useCachedValue 数式ではなく値で比較する場合は {@code true}
         * @return Excelシートからセルデータを抽出するローダー
         * @throws NullPointerException          {@code bookInfo} が {@code null} の場合
         * @throws UnsupportedOperationException {@code bookInfo} がサポート対象外の形式の場合
         */
        public static CellsLoader of(BookInfo bookInfo, boolean useCachedValue) {
                Objects.requireNonNull(bookInfo);

                Function<Cell, CellData> converter = cell -> {
                        String content = PoiUtil.getCellContentAsString(cell, useCachedValue);
                        return "".equals(content)
                                        ? null
                                        : new CellData(
                                                        cell.getRowIndex(),
                                                        cell.getColumnIndex(),
                                                        content,
                                                        null);
                };

                return switch (BookType.of(bookInfo.bookPath())) {
                        case XLS -> useCachedValue
                                        ? CellsLoaderCombined.of(List.of(
                                                        () -> new CellsLoaderWithPoiEventApi(useCachedValue),
                                                        () -> new CellsLoaderWithPoiUserApi(converter)))
                                        : new CellsLoaderWithPoiUserApi(converter);

                        case XLSX, XLSM -> useCachedValue
                                        ? CellsLoaderCombined.of(List.of(
                                                        () -> new CellsLoaderWithSax(useCachedValue),
                                                        () -> new CellsLoaderWithPoiUserApi(converter)))
                                        : new CellsLoaderWithPoiUserApi(converter);

                        // FIXME: [No.02 .xlsbのサポート]
                        case XLSB -> throw new UnsupportedOperationException("unsupported book type: " + BookType.XLSB);
                        default -> throw new AssertionError("unknown book type: " + BookType.of(bookInfo.bookPath()));
                };
        }

        // [instance members] ******************************************************

        /**
         * 指定されたExcelシートに含まれるセルのセットを返します。<br>
         * 
         * @param bookInfo     Excelブック情報
         * @param readPassword Excelブックの読み取りパスワード
         * @param sheetName    シート名
         * @return 指定されたExcelシートに含まれるセルのセット
         * @throws ExcelHandlingException 処理に失敗した場合
         */
        Set<CellData> loadCells(
                        BookInfo bookInfo,
                        String readPassword,
                        String sheetName)
                        throws ExcelHandlingException;
}
