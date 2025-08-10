package xyz.hotchpotch.hogandiff.logic._google;

/**
 * Googleドライブ連携関連の処理に失敗したことを表す例外です。<br>
 *
 * @author nmby
 */
public class GoogleHandlingException extends Exception {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 新しい例外を生成します。<br>
     */
    public GoogleHandlingException() {
        super();
    }
    
    /**
     * 新しい例外を生成します。<br>
     * 
     * @param message 例外メッセージ
     */
    public GoogleHandlingException(String message) {
        super(message);
    }
    
    /**
     * 新しい例外を生成します。<br>
     * 
     * @param cause 原因
     */
    public GoogleHandlingException(Throwable cause) {
        super(cause);
    }
    
    /**
     * 新しい例外を生成します。<br>
     * 
     * @param message 例外メッセージ
     * @param cause 原因
     */
    public GoogleHandlingException(String message, Throwable cause) {
        super(message, cause);
    }
}
