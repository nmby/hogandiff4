package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;

import xyz.hotchpotch.hogandiff.excel.common.CombinedCellsLoader;
import xyz.hotchpotch.hogandiff.excel.poi.eventmodel.HSSFCellsLoaderWithPoiEventApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.CellsLoaderWithPoiUserApi;
import xyz.hotchpotch.hogandiff.excel.poi.usermodel.PoiUtil;
import xyz.hotchpotch.hogandiff.excel.sax.XSSFCellsLoaderWithSax;

/**
 * Excelシートからセルデータを抽出するローダーを表します。<br>
 * これは、{@link #loadCells(BookOpenInfo, String)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface CellsLoader {
    
    // [static members] ********************************************************
    
    /**
     * Excelシートからセルデータを抽出するローダーを返します。<br>
     * 
     * @param bookOpenInfo Excelブックオープン情報
     * @param useCachedValue 数式ではなく値で比較する場合は {@code true}
     * @return Excelシートからセルデータを抽出するローダー
     * @throws NullPointerException {@code bookOpenInfo} が {@code null} の場合
     * @throws UnsupportedOperationException
     *              {@code bookOpenInfo} がサポート対象外の形式の場合
     */
    public static CellsLoader of(BookOpenInfo bookOpenInfo, boolean useCachedValue) {
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        
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
        
        return switch (bookOpenInfo.bookType()) {
            case XLS -> useCachedValue
                    ? CombinedCellsLoader.of(List.of(
                            () -> HSSFCellsLoaderWithPoiEventApi.of(useCachedValue),
                            () -> CellsLoaderWithPoiUserApi.of(converter)))
                    : CellsLoaderWithPoiUserApi.of(converter);
        
            case XLSX, XLSM -> useCachedValue
                    ? CombinedCellsLoader.of(List.of(
                            () -> XSSFCellsLoaderWithSax.of(useCachedValue, bookOpenInfo),
                            () -> CellsLoaderWithPoiUserApi.of(converter)))
                    : CellsLoaderWithPoiUserApi.of(converter);
        
            // FIXME: [No.2 .xlsbのサポート]
            case XLSB -> throw new UnsupportedOperationException("unsupported book type: " + bookOpenInfo.bookType());
            default -> throw new AssertionError("unknown book type: " + bookOpenInfo.bookType());
        };
    }
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたExcelシートに含まれるセルのセットを返します。<br>
     * 
     * @param bookPath Excepブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @param bookOpenInfo Excelブックの情報
     * @param sheetName シート名
     * @return 指定されたExcelシートに含まれるセルのセット
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    Set<CellData> loadCells(
            Path bookPath,
            String readPassword,
            String sheetName)
            throws ExcelHandlingException;
}
