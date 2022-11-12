package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;

/**
 * 各種オプション指定部分と設定保存・実行ボタンを含む画面部品です。<br>
 * 
 * @author nmby
 */
public class SettingsPane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private OptionsParts optionsParts;
    
    @FXML
    private Button saveSettingsButton;
    
    @FXML
    private Button executeButton;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public SettingsPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SettingsPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent) {
        Objects.requireNonNull(parent, "parent");
        
        // 1.disableプロパティのバインディング
        disableProperty().bind(parent.isRunning);
        saveSettingsButton.disableProperty().bind(parent.hasSettingsChanged.not());
        executeButton.disableProperty().bind(parent.isReady.not());
        
        // 2.項目ごとの各種設定
        optionsParts.init(parent);
        executeButton.setOnAction(event -> parent.execute());
        
        // 3.初期値の設定
        // nop
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
}
