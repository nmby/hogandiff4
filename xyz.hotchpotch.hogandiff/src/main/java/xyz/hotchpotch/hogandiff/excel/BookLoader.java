package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;

/**
 * Excelブックからシート名の一覧を抽出するローダーを表します。<br>
 * これは、{@link #loadSheetNames(Path)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface BookLoader {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたExcelブックに含まれるシート名の一覧を返します。<br>
     * 
     * @param bookInfo Excelブックの情報
     * @return シート名の一覧
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    List<String> loadSheetNames(BookInfo bookInfo) throws ExcelHandlingException;
}
