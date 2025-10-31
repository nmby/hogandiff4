package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.ErrorReporter;
import xyz.hotchpotch.hogandiff.Msg;

/**
 * ユーザーにパスワード入力を求めるダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class PasswordDialogPane extends VBox {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final AppResource ar = AppMain.appResource;
    
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
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public PasswordDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("PasswordDialogPane.fxml"), ar.get());
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * このダイアログボックス要素を初期化します。<br>
     * 
     * @param parent
     *            親要素
     * @param bookName
     *            開こうとしているExcelブックの名前
     * @param readPassword
     *            開こうとしているExcelブックの読み取りパスワード
     */
    /*package*/ void init(
            PasswordDialog parent,
            String bookName,
            String readPassword) {
        
        assert parent != null;
        assert bookName != null;
        // readPassword may be null.
        
        try {
            errorMsgLabel.setVisible(readPassword != null);
            mainMsgLabel.setText(Msg.APP_1260.get().formatted(bookName));
            passwordField.textProperty().setValue(readPassword);
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "PasswordDialogPane#init-1");
            throw e;
        }
    }
}
