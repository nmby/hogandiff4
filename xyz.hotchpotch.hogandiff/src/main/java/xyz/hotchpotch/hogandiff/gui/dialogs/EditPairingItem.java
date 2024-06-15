package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.util.Pair;

public class EditPairingItem<C> extends HBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private Label itemLabelA;
    
    @FXML
    private Label itemLabelB;
    
    @FXML
    private Button unpairButton;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditPairingItem() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EditPairingItem.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * このコンポーネントを初期化します。<br>
     */
    /*package*/ void init(Pair<C> pair) {
        itemLabelA.setText(pair.hasA() ? pair.a().toString() : null);
        itemLabelB.setText(pair.hasB() ? pair.b().toString() : null);
        unpairButton.setVisible(pair.isPaired());
    }
}
