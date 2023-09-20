package xyz.hotchpotch.hogandiff.excel;

import java.util.Set;

/**
 * Excelシートからセルデータを抽出するローダーを表します。<br>
 * これは、{@link #loadCells(BookInfo, String)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface CellsLoader {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたExcelシートに含まれるセルのセットを返します。<br>
     * 
     * @param bookInfo Excelブックの情報
     * @param sheetName シート名
     * @return 指定されたExcelシートに含まれるセルのセット
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    Set<CellData> loadCells(BookInfo bookInfo, String sheetName)
            throws ExcelHandlingException;
}
