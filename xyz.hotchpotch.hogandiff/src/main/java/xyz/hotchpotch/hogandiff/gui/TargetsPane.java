package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;

import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;

public class TargetsPane extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private TargetBookSheetParts targetBookSheetParts1;
    
    @FXML
    private TargetBookSheetParts targetBookSheetParts2;
    
    public TargetsPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TargetsPane.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent) {
        Objects.requireNonNull(parent, "parent");
        
        targetBookSheetParts1.init(parent.factory, "A", parent.menu);
        targetBookSheetParts2.init(parent.factory, "B", parent.menu);
        
        disableProperty().bind(parent.isRunning);
    }
    
    @Override
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        targetBookSheetParts1.applySettings(
                settings, SettingKeys.CURR_BOOK_PATH1,
                SettingKeys.CURR_SHEET_NAME1);
        targetBookSheetParts2.applySettings(
                settings, SettingKeys.CURR_BOOK_PATH2,
                SettingKeys.CURR_SHEET_NAME2);
    }
    
    @Override
    public void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        
        targetBookSheetParts1.gatherSettings(
                builder,
                SettingKeys.CURR_BOOK_PATH1,
                SettingKeys.CURR_SHEET_NAME1);
        targetBookSheetParts2.gatherSettings(
                builder,
                SettingKeys.CURR_BOOK_PATH2,
                SettingKeys.CURR_SHEET_NAME2);
    }
    
    @Override
    public BooleanExpression isReady() {
        return targetBookSheetParts1.isReady.and(targetBookSheetParts2.isReady);
    }
}
