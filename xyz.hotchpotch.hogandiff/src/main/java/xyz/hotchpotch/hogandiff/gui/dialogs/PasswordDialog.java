package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.Objects;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.Msg;

/**
 * ユーザーにパスワード入力を求めるダイアログボックスです。<br>
 * 
 * @author nmby
 */
public class PasswordDialog extends Dialog<String> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    /**
     * 新しいダイアログを構成します。<br>
     * 
     * @param bookName
     *            開こうとしているExcelブックの名前
     * @param readPassword
     *            開こうとしているExcelブックの読み取りパスワード
     * @throws IOException
     *             子要素の構成に失敗した場合
     */
    public PasswordDialog(
            String bookName,
            String readPassword)
            throws IOException {
        
        Objects.requireNonNull(bookName);
        // readPassword may be null.
        
        PasswordDialogPane passwordDialogPane = new PasswordDialogPane();
        passwordDialogPane.init(this, bookName, readPassword);
        
        DialogPane me = getDialogPane();
        me.setContent(passwordDialogPane);
        me.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);
        me.lookupButton(ButtonType.OK).disableProperty()
                .bind(passwordDialogPane.passwordField.textProperty().isEmpty());
        
        this.setTitle(Msg.MSG_118.get());
        this.setResultConverter(buttonType -> buttonType == ButtonType.OK
                ? passwordDialogPane.passwordField.getText()
                : null);
        
        passwordDialogPane.passwordField.requestFocus();
    }
}
