package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.util.Settings;

public class SettingsPane extends HBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private OptionsParts optionsParts;
    
    @FXML
    private Button saveSettingsButton;
    
    @FXML
    private Button executeButton;
    
    public SettingsPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SettingsPane.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /*package*/ void init(
            ReadOnlyBooleanProperty isReady,
            EventHandler<ActionEvent> executor) {
        
        Objects.requireNonNull(isReady, "isReady");
        
        optionsParts.init();
        
        // 各種設定の変更有無に応じて「設定の保存」ボタンの有効／無効を切り替える。
        saveSettingsButton.disableProperty().bind(
                optionsParts.hasSettingsChanged.not());
        
        // 「設定を保存」ボタンのイベントハンドラを登録する。
        saveSettingsButton.setOnAction(event -> {
            Settings.Builder builder = Settings.builder();
            optionsParts.gatherSettings(builder);
            Properties properties = builder.build().toProperties();
            AppMain.storeProperties(properties);
            optionsParts.hasSettingsChanged.set(false);
        });
        
        // 各種設定状況に応じて「実行」ボタンの有効／無効を切り替える。
        executeButton.disableProperty().bind(isReady.not());
        
        // 「実行」ボタンのイベントハンドラを登録する。
        executeButton.setOnAction(executor);
    }
    
    /*package*/ void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        optionsParts.applySettings(settings);
    }
    
    /*package*/ void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        
        optionsParts.gatherSettings(builder);
    }
}
