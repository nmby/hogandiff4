package xyz.hotchpotch.hogandiff.gui.component;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * 比較メニュー部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class SettingsPane1 extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private CheckBox considerRowGapsCheckBox;
    
    @FXML
    private CheckBox considerColumnGapsCheckBox;
    
    @FXML
    private ToggleGroup compareValuesOrFormulas;
    
    @FXML
    private RadioButton compareValuesRadioButton;
    
    @FXML
    private RadioButton compareFormulasRadioButton;
    
    @FXML
    private CheckBox showPaintedSheetsCheckBox;
    
    @FXML
    private CheckBox showResultTextCheckBox;
    
    @FXML
    private CheckBox exitWhenFinishedCheckBox;
    
    @FXML
    private CheckBox saveMemoryCheckBox;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public SettingsPane1() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SettingsPane1.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent, "parent");
        
        // 1.disableプロパティのバインディング
        disableProperty().bind(parent.isRunning());
        
        // 2.項目ごとの各種設定
        // nop
        
        // 3.初期値の設定
        BiConsumer<Key<Boolean>, Consumer<Boolean>> applicator = (key, setter) -> setter
                .accept(ar.settings().getOrDefault(key));
        
        applicator.accept(SettingKeys.CONSIDER_ROW_GAPS, considerRowGapsCheckBox::setSelected);
        applicator.accept(SettingKeys.CONSIDER_COLUMN_GAPS, considerColumnGapsCheckBox::setSelected);
        applicator.accept(SettingKeys.COMPARE_ON_FORMULA_STRING, compareFormulasRadioButton::setSelected);
        applicator.accept(SettingKeys.SHOW_PAINTED_SHEETS, showPaintedSheetsCheckBox::setSelected);
        applicator.accept(SettingKeys.SHOW_RESULT_TEXT, showResultTextCheckBox::setSelected);
        applicator.accept(SettingKeys.EXIT_WHEN_FINISHED, exitWhenFinishedCheckBox::setSelected);
        applicator.accept(SettingKeys.SAVE_MEMORY, saveMemoryCheckBox::setSelected);
        
        // 4.値変更時のイベントハンドラの設定
        BiConsumer<CheckBox, Key<Boolean>> addListener = (target, key) -> target
                .setOnAction(event -> ar.changeSetting(key, target.isSelected()));
        
        addListener.accept(considerRowGapsCheckBox, SettingKeys.CONSIDER_ROW_GAPS);
        addListener.accept(considerColumnGapsCheckBox, SettingKeys.CONSIDER_COLUMN_GAPS);
        addListener.accept(showPaintedSheetsCheckBox, SettingKeys.SHOW_PAINTED_SHEETS);
        addListener.accept(showResultTextCheckBox, SettingKeys.SHOW_RESULT_TEXT);
        addListener.accept(exitWhenFinishedCheckBox, SettingKeys.EXIT_WHEN_FINISHED);
        addListener.accept(saveMemoryCheckBox, SettingKeys.SAVE_MEMORY);
        
        compareValuesOrFormulas.selectedToggleProperty().addListener((target, oldValue, newValue) -> ar
                .changeSetting(SettingKeys.COMPARE_ON_FORMULA_STRING, compareFormulasRadioButton.isSelected()));
    }
}
