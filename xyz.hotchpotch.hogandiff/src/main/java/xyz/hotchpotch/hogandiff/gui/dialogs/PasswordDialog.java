package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;

/**
 * ユーザーにパスワード入力を求めるダイアログボックスです。<br>
 * 
 * @author nmby
 */
public class PasswordDialog extends Dialog<String> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * 新しいダイアログを構成します。<br>
     * 
     * @param bookPath 開こうとしているExcelブックのパス
     * @param readPassword 開こうとしているExcelブックの読み取りパスワード
     * @throws IOException 子要素の構成に失敗した場合
     */
    public PasswordDialog(
            Path bookPath,
            String readPassword)
            throws IOException {
        
        Objects.requireNonNull(bookPath, "bookPath");
        // readPassword may be null.
        
        PasswordDialogPane passwordDialogPane = new PasswordDialogPane();
        passwordDialogPane.init(this, bookPath, readPassword);
        
        DialogPane me = getDialogPane();
        me.setContent(passwordDialogPane);
        me.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);
        me.lookupButton(ButtonType.OK).disableProperty()
                .bind(passwordDialogPane.passwordField.textProperty().isEmpty());
        
        this.setTitle(rb.getString("gui.PasswordDialog.010"));
        this.setResultConverter(buttonType -> buttonType == ButtonType.OK
                ? passwordDialogPane.passwordField.getText()
                : null);
        
        passwordDialogPane.passwordField.requestFocus();
    }
}
