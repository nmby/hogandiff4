package xyz.hotchpotch.hogandiff.excel;

/**
 * Excelブックからシート名の一覧を抽出するローダーを表します。<br>
 * これは、{@link #loadSheetNames(BookOpenInfo)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface SheetNamesLoader {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたExcelブックに含まれるシート名の一覧を返します。<br>
     * 
     * @param bookOpenInfo Excelブックオープン情報
     * @return Excelブック情報
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    BookInfo loadSheetNames(BookOpenInfo bookOpenInfo) throws ExcelHandlingException;
}
