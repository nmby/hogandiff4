package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 処理内容選択メニュー部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class LocalePane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private RadioButton localeJaRadioButton;
    
    @FXML
    private RadioButton localeEnRadioButton;
    
    public LocalePane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LocalePane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent) {
        Objects.requireNonNull(parent, "parent");
        
        //
        
        disableProperty().bind(parent.isRunning);
    }
    
    @Override
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        if (settings.containsKey(SettingKeys.APP_LOCALE)) {
            Locale locale = settings.get(SettingKeys.APP_LOCALE);
            if (locale == Locale.JAPANESE) {
                localeJaRadioButton.setSelected(true);
            } else if (locale == Locale.ENGLISH) {
                localeEnRadioButton.setSelected(true);
            } else {
                throw new AssertionError("unsupported locale : " + locale);
            }
            
        } else {
            localeJaRadioButton.setSelected(true);
        }
    }
    
    @Override
    public void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        
        if (localeJaRadioButton.isSelected()) {
            builder.set(SettingKeys.APP_LOCALE, Locale.JAPANESE);
        } else if (localeEnRadioButton.isSelected()) {
            builder.set(SettingKeys.APP_LOCALE, Locale.ENGLISH);
        } else {
            throw new AssertionError("none is selected");
        }
    }
}
