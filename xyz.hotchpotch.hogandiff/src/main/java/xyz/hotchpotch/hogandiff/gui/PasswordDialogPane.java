package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.BookInfo;

public class PasswordDialogPane extends VBox {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    @FXML
    private Label errorMsgLabel;
    
    @FXML
    private Label mainMsgLabel;
    
    @FXML
    /*package*/ PasswordField passwordField;
    
    public PasswordDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("PasswordDialogPane.fxml"),
                AppMain.appResource.get());
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /*package*/ void init(
            PasswordDialog parent,
            BookInfo bookInfo) {
        
        assert bookInfo != null;
        
        errorMsgLabel.setVisible(bookInfo.getReadPassword() != null);
        mainMsgLabel.setText(bookInfo.bookPath().getFileName() + " はパスワードで保護されています。");
        passwordField.textProperty().setValue(bookInfo.getReadPassword());
    }
}
