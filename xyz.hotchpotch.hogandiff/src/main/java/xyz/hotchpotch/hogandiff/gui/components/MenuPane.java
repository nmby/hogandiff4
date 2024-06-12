package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;

/**
 * 比較メニュー部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class MenuPane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private Label compareLabel;
    
    @FXML
    private ToggleGroup compareTarget;
    
    @FXML
    private RadioButton compareBooksRadioButton;
    
    @FXML
    private RadioButton compareSheetsRadioButton;
    
    @FXML
    private RadioButton compareDirsRadioButton;
    
    @FXML
    private CheckBox recursivelyCheckBox;
    
    private final Property<AppMenu> menu = new SimpleObjectProperty<>();
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public MenuPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MenuPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent, "parent");
        
        // 1.disableプロパティのバインディング
        disableProperty().bind(parent.isRunning());
        recursivelyCheckBox.disableProperty().bind(compareDirsRadioButton.selectedProperty().not());
        
        // 2.項目ごとの各種設定
        menu.bind(Bindings.createObjectBinding(
                () -> compareTarget.getSelectedToggle() == compareBooksRadioButton ? AppMenu.COMPARE_BOOKS
                        : compareTarget.getSelectedToggle() == compareSheetsRadioButton ? AppMenu.COMPARE_SHEETS
                                : recursivelyCheckBox.isSelected()
                                        ? AppMenu.COMPARE_TREES
                                        : AppMenu.COMPARE_DIRS,
                compareTarget.selectedToggleProperty(),
                recursivelyCheckBox.selectedProperty()));
        
        // 3.初期値の設定
        compareTarget.selectToggle(
                switch (ar.settings().get(SettingKeys.CURR_MENU)) {
                    case COMPARE_BOOKS -> compareBooksRadioButton;
                    case COMPARE_SHEETS -> compareSheetsRadioButton;
                    case COMPARE_DIRS -> compareDirsRadioButton;
                    case COMPARE_TREES -> compareDirsRadioButton;
                    default -> throw new AssertionError("unknown menu");
                });
        
        recursivelyCheckBox.setSelected(
                ar.settings().get(SettingKeys.COMPARE_DIRS_RECURSIVELY));
        
        // 4.値変更時のイベントハンドラの設定
        menu.addListener((target, oldValue, newValue) -> ar.changeSetting(SettingKeys.CURR_MENU, newValue));
        
        recursivelyCheckBox.selectedProperty()
                .addListener((target, oldValue, newValue) -> ar.changeSetting(
                        SettingKeys.COMPARE_DIRS_RECURSIVELY,
                        newValue));
    }
    
    /**
     * 選択されている比較メニューを返します。<br>
     * 
     * @return 選択されている比較メニュー
     */
    public ReadOnlyProperty<AppMenu> menu() {
        return menu;
    }
}
