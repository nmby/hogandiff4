package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;

public class MenuPane extends HBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private RadioButton compareBooksRadioButton;
    
    @FXML
    private RadioButton compareSheetsRadioButton;
    
    /*package*/ final Property<AppMenu> menu = new SimpleObjectProperty<>();
    
    public MenuPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MenuPane.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /*package*/ void init() {
        menu.bind(Bindings.createObjectBinding(
                () -> compareBooksRadioButton.isSelected()
                        ? AppMenu.COMPARE_BOOKS
                        : AppMenu.COMPARE_SHEETS,
                compareBooksRadioButton.selectedProperty()));
        
    }
    
    /*package*/ void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        if (settings.containsKey(SettingKeys.CURR_MENU)) {
            compareBooksRadioButton.setSelected(
                    settings.get(SettingKeys.CURR_MENU) == AppMenu.COMPARE_BOOKS);
        }
    }
    
    /*package*/ void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        
        builder.set(SettingKeys.CURR_MENU, menu.getValue());
    }
}
