package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;

public class MenuPane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private RadioButton compareBooksRadioButton;
    
    @FXML
    private RadioButton compareSheetsRadioButton;
    
    public MenuPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MenuPane.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent) {
        Objects.requireNonNull(parent, "parent");
        
        parent.menu.bind(Bindings.createObjectBinding(
                () -> compareBooksRadioButton.isSelected()
                        ? AppMenu.COMPARE_BOOKS
                        : AppMenu.COMPARE_SHEETS,
                compareBooksRadioButton.selectedProperty()));
        
    }
    
    @Override
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        if (settings.containsKey(SettingKeys.CURR_MENU)) {
            compareBooksRadioButton.setSelected(
                    settings.get(SettingKeys.CURR_MENU) == AppMenu.COMPARE_BOOKS);
        }
    }
    
    @Override
    public void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
    }
}
