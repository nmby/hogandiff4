package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.util.Settings;

public class TargetsPane extends VBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private TargetBookSheetParts targetBookSheet1;
    
    @FXML
    private TargetBookSheetParts targetBookSheet2;
    
    private BooleanProperty isReady = new SimpleBooleanProperty();
    
    public TargetsPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TargetsPane.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    public ReadOnlyBooleanProperty isReadyProperty() {
        return isReady;
    }
    
    public void init(
            Factory factory,
            ReadOnlyProperty<AppMenu> menu) {
        
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(menu, "menu");
        
        targetBookSheet1.init(factory, "A", menu);
        targetBookSheet2.init(factory, "B", menu);
        
        isReady.bind(targetBookSheet1.isReadyProperty().and(targetBookSheet2.isReadyProperty()));
    }
    
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        targetBookSheet1.applySettings(settings, SettingKeys.CURR_BOOK_PATH1, SettingKeys.CURR_SHEET_NAME1);
        targetBookSheet2.applySettings(settings, SettingKeys.CURR_BOOK_PATH2, SettingKeys.CURR_SHEET_NAME2);
    }
    
    public void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        
        targetBookSheet1.gatherSettings(builder, SettingKeys.CURR_BOOK_PATH1, SettingKeys.CURR_SHEET_NAME1);
        targetBookSheet2.gatherSettings(builder, SettingKeys.CURR_BOOK_PATH2, SettingKeys.CURR_SHEET_NAME2);
    }
}
