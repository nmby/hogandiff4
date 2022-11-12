package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 処理内容選択メニュー部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class LocalePane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    private static enum LocaleItem {
        
        // [static members] ----------------------------------------------------
        
        JA("日本語", Locale.JAPANESE),
        EN("English", Locale.ENGLISH),
        ZH("簡体中文", Locale.SIMPLIFIED_CHINESE);
        
        public static LocaleItem of(Locale locale) {
            Objects.requireNonNull(locale, "locale");
            
            return Stream.of(values())
                    .filter(item -> item.locale == locale)
                    .findFirst()
                    .orElseThrow();
        }
        
        // [instance members] --------------------------------------------------
        
        private final String name;
        private final Locale locale;
        
        LocaleItem(String name, Locale locale) {
            this.name = name;
            this.locale = locale;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private ChoiceBox<LocaleItem> localeChoiceBox;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public LocalePane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LocalePane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent) {
        Objects.requireNonNull(parent, "parent");
        
        disableProperty().bind(parent.isRunning);
        
        localeChoiceBox.setItems(FXCollections.observableArrayList(LocaleItem.values()));
        localeChoiceBox.setOnAction(event -> {
            if (ar.changeSetting(SettingKeys.APP_LOCALE, localeChoiceBox.getValue().locale)) {
                new Alert(
                        AlertType.INFORMATION,
                        "%s%n%n%s%n%n%s".formatted(
                                rb.getString("gui.LocalePane.011"),
                                rb.getString("gui.LocalePane.012"),
                                rb.getString("gui.LocalePane.013")),
                        ButtonType.OK)
                                .showAndWait();
            }
        });
    }
    
    @Override
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        EventHandler<ActionEvent> handler = localeChoiceBox.getOnAction();
        localeChoiceBox.setOnAction(null);
        
        Locale locale = settings.getOrDefault(SettingKeys.APP_LOCALE);
        localeChoiceBox.setValue(LocaleItem.of(locale));
        
        localeChoiceBox.setOnAction(handler);
    }
}
