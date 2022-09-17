package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.excel.BookInfo;

public class PasswordDialog extends Dialog<String> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
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
        
        this.setTitle("パスワード指定");
        this.setResultConverter(buttonType -> buttonType == ButtonType.OK
                ? passwordDialogPane.passwordField.getText()
                : null);
        
        passwordDialogPane.passwordField.requestFocus();
    }
}
