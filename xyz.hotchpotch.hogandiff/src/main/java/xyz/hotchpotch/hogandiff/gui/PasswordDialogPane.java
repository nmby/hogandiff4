package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.ResourceBundle;

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
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private Label errorMsgLabel;
    
    @FXML
    private Label mainMsgLabel;
    
    @FXML
    /*package*/ PasswordField passwordField;
    
    public PasswordDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("PasswordDialogPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /*package*/ void init(
            PasswordDialog parent,
            BookInfo bookInfo) {
        
        assert bookInfo != null;
        
        errorMsgLabel.setVisible(bookInfo.getReadPassword() != null);
        mainMsgLabel.setText(
                rb.getString("gui.PasswordDialogPane.010").formatted(bookInfo.bookPath().getFileName()));
        passwordField.textProperty().setValue(bookInfo.getReadPassword());
    }
}
