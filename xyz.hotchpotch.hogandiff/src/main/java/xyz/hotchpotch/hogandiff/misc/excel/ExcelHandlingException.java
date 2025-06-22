package xyz.hotchpotch.hogandiff.misc.excel;

/**
 * Excelファイルに対する処理に失敗したことを表す例外です。<br>
 *
 * @author nmby
 */
public class ExcelHandlingException extends Exception {

    // [static members] ********************************************************

    // [instance members] ******************************************************

    /**
     * 新しい例外を生成します。<br>
     */
    public ExcelHandlingException() {
        super();
    }

    /**
     * 新しい例外を生成します。<br>
     * 
     * @param message 例外メッセージ
     */
    public ExcelHandlingException(String message) {
        super(message);
    }

    /**
     * 新しい例外を生成します。<br>
     * 
     * @param cause 原因
     */
    public ExcelHandlingException(Throwable cause) {
        super(cause);
    }

    /**
     * 新しい例外を生成します。<br>
     * 
     * @param message 例外メッセージ
     * @param cause   原因
     */
    public ExcelHandlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
