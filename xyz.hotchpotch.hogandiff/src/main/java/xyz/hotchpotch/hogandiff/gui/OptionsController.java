package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;

public class OptionsController extends VBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private CheckBox checkConsiderRowGaps;
    
    @FXML
    private CheckBox checkConsiderColumnGaps;
    
    @FXML
    private CheckBox checkCompareCellContents;
    
    @FXML
    private RadioButton radioCompareOnValue;
    
    @FXML
    private RadioButton radioCompareOnFormula;
    
    @FXML
    private CheckBox checkCompareCellComments;
    
    @FXML
    private CheckBox checkShowPaintedSheets;
    
    @FXML
    private CheckBox checkShowResultText;
    
    @FXML
    private CheckBox checkExitWhenFinished;
    
    private BooleanProperty hasSettingsChanged = new SimpleBooleanProperty(false);
    
    public OptionsController() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("OptionsView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    public void init() {
        checkConsiderRowGaps.setOnAction(event -> hasSettingsChanged.set(true));
        checkConsiderColumnGaps.setOnAction(event -> hasSettingsChanged.set(true));
        checkCompareCellContents.setOnAction(event -> hasSettingsChanged.set(true));
        radioCompareOnValue.setOnAction(event -> hasSettingsChanged.set(true));
        radioCompareOnFormula.setOnAction(event -> hasSettingsChanged.set(true));
        checkCompareCellComments.setOnAction(event -> hasSettingsChanged.set(true));
        checkShowPaintedSheets.setOnAction(event -> hasSettingsChanged.set(true));
        checkShowResultText.setOnAction(event -> hasSettingsChanged.set(true));
        checkExitWhenFinished.setOnAction(event -> hasSettingsChanged.set(true));
        
        // 「セル内容を比較する」が選択された場合のみ、「値／数式」の選択を有効にする。
        radioCompareOnValue.disableProperty().bind(checkCompareCellContents.selectedProperty().not());
        radioCompareOnFormula.disableProperty().bind(checkCompareCellContents.selectedProperty().not());
    }
    
    public BooleanProperty hasSettingsChangedProperty() {
        return hasSettingsChanged;
    }
    
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        if (settings.containsKey(SettingKeys.CONSIDER_ROW_GAPS)) {
            checkConsiderRowGaps.setSelected(settings.get(SettingKeys.CONSIDER_ROW_GAPS));
        }
        if (settings.containsKey(SettingKeys.CONSIDER_COLUMN_GAPS)) {
            checkConsiderColumnGaps.setSelected(settings.get(SettingKeys.CONSIDER_COLUMN_GAPS));
        }
        if (settings.containsKey(SettingKeys.COMPARE_CELL_CONTENTS)) {
            checkCompareCellContents.setSelected(settings.get(SettingKeys.COMPARE_CELL_CONTENTS));
        }
        if (settings.containsKey(SettingKeys.COMPARE_CELL_COMMENTS)) {
            checkCompareCellComments.setSelected(settings.get(SettingKeys.COMPARE_CELL_COMMENTS));
        }
        if (settings.containsKey(SettingKeys.COMPARE_ON_FORMULA_STRING)) {
            radioCompareOnFormula.setSelected(settings.get(SettingKeys.COMPARE_ON_FORMULA_STRING));
        }
        if (settings.containsKey(SettingKeys.SHOW_PAINTED_SHEETS)) {
            checkShowPaintedSheets.setSelected(settings.get(SettingKeys.SHOW_PAINTED_SHEETS));
        }
        if (settings.containsKey(SettingKeys.SHOW_RESULT_TEXT)) {
            checkShowResultText.setSelected(settings.get(SettingKeys.SHOW_RESULT_TEXT));
        }
        if (settings.containsKey(SettingKeys.EXIT_WHEN_FINISHED)) {
            checkExitWhenFinished.setSelected(settings.get(SettingKeys.EXIT_WHEN_FINISHED));
        }
    }
    
    public void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        
        builder.set(SettingKeys.CONSIDER_ROW_GAPS, checkConsiderRowGaps.isSelected());
        builder.set(SettingKeys.CONSIDER_COLUMN_GAPS, checkConsiderColumnGaps.isSelected());
        builder.set(SettingKeys.COMPARE_CELL_CONTENTS, checkCompareCellContents.isSelected());
        builder.set(SettingKeys.COMPARE_CELL_COMMENTS, checkCompareCellComments.isSelected());
        builder.set(SettingKeys.COMPARE_ON_FORMULA_STRING, radioCompareOnFormula.isSelected());
        builder.set(SettingKeys.SHOW_PAINTED_SHEETS, checkShowPaintedSheets.isSelected());
        builder.set(SettingKeys.SHOW_RESULT_TEXT, checkShowResultText.isSelected());
        builder.set(SettingKeys.EXIT_WHEN_FINISHED, checkExitWhenFinished.isSelected());
    }
}
