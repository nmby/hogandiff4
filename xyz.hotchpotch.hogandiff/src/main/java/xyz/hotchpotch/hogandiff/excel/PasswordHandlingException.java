package xyz.hotchpotch.hogandiff.excel;

public class PasswordHandlingException extends ExcelHandlingException {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 新しい例外を生成します。<br>
     */
    public PasswordHandlingException() {
        super();
    }
    
    /**
     * 新しい例外を生成します。<br>
     * 
     * @param message 例外メッセージ
     */
    public PasswordHandlingException(String message) {
        super(message);
    }
    
    /**
     * 新しい例外を生成します。<br>
     * 
     * @param cause 原因
     */
    public PasswordHandlingException(Throwable cause) {
        super(cause);
    }
    
    /**
     * 新しい例外を生成します。<br>
     * 
     * @param message 例外メッセージ
     * @param cause 原因
     */
    public PasswordHandlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
