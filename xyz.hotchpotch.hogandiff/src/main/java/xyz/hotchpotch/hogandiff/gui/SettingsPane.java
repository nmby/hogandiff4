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
import xyz.hotchpotch.hogandiff.util.Settings;

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
        
        optionsParts.init(parent);
        
        // 各種設定の変更有無に応じて「設定の保存」ボタンの有効／無効を切り替える。
        saveSettingsButton.disableProperty().bind(parent.hasSettingsChanged.not());
        
        // 各種設定状況に応じて「実行」ボタンの有効／無効を切り替える。
        executeButton.disableProperty().bind(parent.isReady.not());
        
        // 「実行」ボタンのイベントハンドラを登録する。
        executeButton.setOnAction(event -> parent.execute());
        
        disableProperty().bind(parent.isRunning);
    }
    
    @Override
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        optionsParts.applySettings(settings);
    }
}
