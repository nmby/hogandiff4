package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.BookInfo;

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
     * @param bookInfo 開こうとしているExcelブック
     * @throws IOException 子要素の構成に失敗した場合
     */
    public PasswordDialog(BookInfo bookInfo) throws IOException {
        Objects.requireNonNull(bookInfo, "bookInfo");
        
        PasswordDialogPane passwordDialogPane = new PasswordDialogPane();
        passwordDialogPane.init(this, bookInfo);
        
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
