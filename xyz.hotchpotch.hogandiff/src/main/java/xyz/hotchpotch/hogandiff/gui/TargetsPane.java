package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;

import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 比較対象選択部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class TargetsPane extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private TargetSelectionParts targetSelectionParts1;
    
    @FXML
    private TargetSelectionParts targetSelectionParts2;
    
    public TargetsPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TargetsPane.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent) {
        Objects.requireNonNull(parent, "parent");
        
        targetSelectionParts1.init(parent, "A");
        targetSelectionParts2.init(parent, "B");
        
        disableProperty().bind(parent.isRunning);
    }
    
    @Override
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        targetSelectionParts1.applySettings(
                settings, SettingKeys.CURR_BOOK_PATH1,
                SettingKeys.CURR_SHEET_NAME1);
        targetSelectionParts2.applySettings(
                settings, SettingKeys.CURR_BOOK_PATH2,
                SettingKeys.CURR_SHEET_NAME2);
    }
    
    @Override
    public void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        
        targetSelectionParts1.gatherSettings(
                builder,
                SettingKeys.CURR_BOOK_PATH1,
                SettingKeys.CURR_SHEET_NAME1);
        targetSelectionParts2.gatherSettings(
                builder,
                SettingKeys.CURR_BOOK_PATH2,
                SettingKeys.CURR_SHEET_NAME2);
    }
    
    @Override
    public BooleanExpression isReady() {
        return targetSelectionParts1.isReady.and(targetSelectionParts2.isReady);
    }
}
