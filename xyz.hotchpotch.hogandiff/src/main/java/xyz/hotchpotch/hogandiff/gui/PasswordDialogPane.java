package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;

/**
 * ユーザーにパスワード入力を求めるダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class PasswordDialogPane extends VBox {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private Label errorMsgLabel;
    
    @FXML
    private Label mainMsgLabel;
    
    /** パスワード入力欄 */
    @FXML
    /*package*/ PasswordField passwordField;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public PasswordDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("PasswordDialogPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * このダイアログボックス要素を初期化します。<br>
     * 
     * @param parent 親要素
     * @param bookPath 開こうとしているExcelブックのパス
     * @param readPassword 開こうとしているExcelブックの読み取りパスワード
     */
    /*package*/ void init(
            PasswordDialog parent,
            Path bookPath,
            String readPassword) {
        
        assert parent != null;
        assert bookPath != null;
        // readPassword may be null.
        
        errorMsgLabel.setVisible(readPassword != null);
        mainMsgLabel.setText(
                rb.getString("gui.PasswordDialogPane.010").formatted(bookPath.getFileName()));
        passwordField.textProperty().setValue(readPassword);
    }
}
