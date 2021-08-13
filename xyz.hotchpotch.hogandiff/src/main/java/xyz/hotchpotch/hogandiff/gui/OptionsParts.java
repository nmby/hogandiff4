package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * 各種オプション指定部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class OptionsParts extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private CheckBox considerRowGapsCheckBox;
    
    @FXML
    private CheckBox considerColumnGapsCheckBox;
    
    @FXML
    private CheckBox compareCellContentsCheckBox;
    
    @FXML
    private RadioButton compareOnValueRadioButton;
    
    @FXML
    private RadioButton compareOnFormulaRadioButton;
    
    @FXML
    private CheckBox compareCellCommentsCheckBox;
    
    @FXML
    private CheckBox showPaintedSheetsCheckBox;
    
    @FXML
    private CheckBox showResultTextCheckBox;
    
    @FXML
    private CheckBox exitWhenFinishedCheckBox;
    
    @FXML
    private CheckBox saveMemoryCheckBox;
    
    public OptionsParts() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("OptionsParts.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent) {
        Objects.requireNonNull(parent, "parent");
        
        considerRowGapsCheckBox.setOnAction(event -> parent.hasSettingsChanged.set(true));
        considerColumnGapsCheckBox.setOnAction(event -> parent.hasSettingsChanged.set(true));
        compareCellContentsCheckBox.setOnAction(event -> parent.hasSettingsChanged.set(true));
        compareOnValueRadioButton.setOnAction(event -> parent.hasSettingsChanged.set(true));
        compareOnFormulaRadioButton.setOnAction(event -> parent.hasSettingsChanged.set(true));
        compareCellCommentsCheckBox.setOnAction(event -> parent.hasSettingsChanged.set(true));
        showPaintedSheetsCheckBox.setOnAction(event -> parent.hasSettingsChanged.set(true));
        showResultTextCheckBox.setOnAction(event -> parent.hasSettingsChanged.set(true));
        exitWhenFinishedCheckBox.setOnAction(event -> parent.hasSettingsChanged.set(true));
        saveMemoryCheckBox.setOnAction(event -> parent.hasSettingsChanged.set(true));
        
        // 「セル内容を比較する」が選択された場合のみ、「値／数式」の選択を有効にする。
        compareOnValueRadioButton.disableProperty().bind(
                compareCellContentsCheckBox.selectedProperty().not());
        compareOnFormulaRadioButton.disableProperty().bind(
                compareCellContentsCheckBox.selectedProperty().not());
    }
    
    @Override
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        BiConsumer<Key<Boolean>, Consumer<Boolean>> applicator = (key, setter) -> {
            if (settings.containsKey(key)) {
                setter.accept(settings.get(key));
            }
        };
        
        applicator.accept(SettingKeys.CONSIDER_ROW_GAPS, considerRowGapsCheckBox::setSelected);
        applicator.accept(SettingKeys.CONSIDER_COLUMN_GAPS, considerColumnGapsCheckBox::setSelected);
        applicator.accept(SettingKeys.COMPARE_CELL_CONTENTS, compareCellContentsCheckBox::setSelected);
        applicator.accept(SettingKeys.COMPARE_CELL_COMMENTS, compareCellCommentsCheckBox::setSelected);
        applicator.accept(SettingKeys.COMPARE_ON_FORMULA_STRING, compareOnFormulaRadioButton::setSelected);
        applicator.accept(SettingKeys.SHOW_PAINTED_SHEETS, showPaintedSheetsCheckBox::setSelected);
        applicator.accept(SettingKeys.SHOW_RESULT_TEXT, showResultTextCheckBox::setSelected);
        applicator.accept(SettingKeys.EXIT_WHEN_FINISHED, exitWhenFinishedCheckBox::setSelected);
        applicator.accept(SettingKeys.SAVE_MEMORY, saveMemoryCheckBox::setSelected);
    }
    
    @Override
    public void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        
        builder.set(SettingKeys.CONSIDER_ROW_GAPS, considerRowGapsCheckBox.isSelected());
        builder.set(SettingKeys.CONSIDER_COLUMN_GAPS, considerColumnGapsCheckBox.isSelected());
        builder.set(SettingKeys.COMPARE_CELL_CONTENTS, compareCellContentsCheckBox.isSelected());
        builder.set(SettingKeys.COMPARE_CELL_COMMENTS, compareCellCommentsCheckBox.isSelected());
        builder.set(SettingKeys.COMPARE_ON_FORMULA_STRING, compareOnFormulaRadioButton.isSelected());
        builder.set(SettingKeys.SHOW_PAINTED_SHEETS, showPaintedSheetsCheckBox.isSelected());
        builder.set(SettingKeys.SHOW_RESULT_TEXT, showResultTextCheckBox.isSelected());
        builder.set(SettingKeys.EXIT_WHEN_FINISHED, exitWhenFinishedCheckBox.isSelected());
        builder.set(SettingKeys.SAVE_MEMORY, saveMemoryCheckBox.isSelected());
    }
}