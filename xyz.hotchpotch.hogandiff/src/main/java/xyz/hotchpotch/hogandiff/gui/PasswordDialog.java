package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;

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
     * @param bookOpenInfo 開こうとしているExcelブック
     * @throws IOException 子要素の構成に失敗した場合
     */
    public PasswordDialog(BookOpenInfo bookOpenInfo) throws IOException {
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        
        PasswordDialogPane passwordDialogPane = new PasswordDialogPane();
        passwordDialogPane.init(this, bookOpenInfo);
        
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
