package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.Set;

/**
 * Excelシートからセルデータを抽出するローダーを表します。<br>
 * これは、{@link #loadCells(Path, String)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface SheetLoader {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたExcelシートに含まれるセルのセットを返します。<br>
     * 
     * @param bookPath Excelブックのパス
     * @param sheetName シート名
     * @return 指定されたExcelシートに含まれるセルのセット
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    Set<CellData> loadCells(Path bookPath, String sheetName)
            throws ExcelHandlingException;
}
