package xyz.hotchpotch.hogandiff.logic.cellloaders;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;

import xyz.hotchpotch.hogandiff.logic.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.logic.PoiUtil;
import xyz.hotchpotch.hogandiff.logic.models.BookType;
import xyz.hotchpotch.hogandiff.logic.models.CellData;

/**
 * Excelシートからセルデータを抽出するローダーを表します。<br>
 * これは、{@link #loadCells(Path, String, String)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface LoaderForCells {

        // [static members] ********************************************************

        /**
         * Excelシートからセルデータを抽出するローダーを返します。<br>
         * 
         * @param bookPath       Excepブックのパス
         * @param useCachedValue 数式ではなく値で比較する場合は {@code true}
         * @return Excelシートからセルデータを抽出するローダー
         * @throws NullPointerException          {@code bookPath} が {@code null} の場合
         * @throws UnsupportedOperationException {@code bookPath} がサポート対象外の形式の場合
         */
        public static LoaderForCells of(Path bookPath, boolean useCachedValue) {
                Objects.requireNonNull(bookPath);

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

                return switch (BookType.of(bookPath)) {
                        case XLS -> useCachedValue
                                        ? LoaderForCellsCombined.of(List.of(
                                                        () -> new LoaderForCellsWithPoiEventApi(useCachedValue),
                                                        () -> new LoaderForCellsWithPoiUserApi(converter)))
                                        : new LoaderForCellsWithPoiUserApi(converter);

                        case XLSX, XLSM -> useCachedValue
                                        ? LoaderForCellsCombined.of(List.of(
                                                        () -> new LoaderForCellsWithSax(useCachedValue, bookPath),
                                                        () -> new LoaderForCellsWithPoiUserApi(converter)))
                                        : new LoaderForCellsWithPoiUserApi(converter);

                        // FIXME: [No.02 .xlsbのサポート]
                        case XLSB -> throw new UnsupportedOperationException("unsupported book type: " + BookType.XLSB);
                        default -> throw new AssertionError("unknown book type: " + BookType.of(bookPath));
                };
        }

        // [instance members] ******************************************************

        /**
         * 指定されたExcelシートに含まれるセルのセットを返します。<br>
         * 
         * @param bookPath     Excepブックのパス
         * @param readPassword Excelブックの読み取りパスワード
         * @param sheetName    シート名
         * @return 指定されたExcelシートに含まれるセルのセット
         * @throws ExcelHandlingException 処理に失敗した場合
         */
        Set<CellData> loadCells(
                        Path bookPath,
                        String readPassword,
                        String sheetName)
                        throws ExcelHandlingException;
}
