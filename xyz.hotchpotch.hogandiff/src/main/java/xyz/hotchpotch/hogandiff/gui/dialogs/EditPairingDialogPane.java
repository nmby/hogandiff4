package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.CompareInfo;

/**
 * ユーザーにパスワード入力を求めるダイアログボックスの要素です。<br>
 * 
 * @param <T> 比較情報の型
 * @author nmby
 */
public class EditPairingDialogPane<T extends CompareInfo<?, ?, ?>> extends VBox {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private Label parentLabelA;
    
    @FXML
    private Label parentLabelB;
    
    @FXML
    private ListView listView;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditPairingDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EditPairingDialogPane.fxml"), rb);
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
    /*package*/ void init(EditPairingDialog<T> parent, T compareInfo) {
        parentLabelA.setText("【A】" + compareInfo.parentPair().a().toString());
        parentLabelB.setText("【B】" + compareInfo.parentPair().b().toString());
    }
}
