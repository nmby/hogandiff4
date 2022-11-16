package xyz.hotchpotch.hogandiff.gui.layout;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.component.SettingsPane1;
import xyz.hotchpotch.hogandiff.gui.component.SettingsPane2;

/**
 * メインビュー四段目の画面部品です。<br>
 * 
 * @author nmby
 */
public class Row4Pane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private SettingsPane1 settingsPane1;
    
    @FXML
    private SettingsPane2 settingsPane2;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public Row4Pane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Row4Pane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent, "parent");
        
        // 1.disableプロパティのバインディング
        //visibleProperty().bind(parent.showSettings());
        
        // 2.項目ごとの各種設定
        settingsPane1.init(parent);
        settingsPane2.init(parent);
        
        // 3.初期値の設定
        //setVisible(parent.showSettings().getValue());
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
}
