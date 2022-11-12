package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;

/**
 * 処理内容選択メニュー部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class MenuPane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private ToggleGroup compareBooksOrSheets;
    
    @FXML
    private RadioButton compareBooksRadioButton;
    
    @FXML
    private RadioButton compareSheetsRadioButton;
    
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
    public void init(MainController parent) {
        Objects.requireNonNull(parent, "parent");
        
        // 1.disableプロパティのバインディング
        disableProperty().bind(parent.isRunning);
        
        // 2.項目ごとの各種設定
        compareBooksRadioButton.setUserData(AppMenu.COMPARE_BOOKS);
        compareSheetsRadioButton.setUserData(AppMenu.COMPARE_SHEETS);
        
        parent.menu.bind(Bindings.createObjectBinding(
                () -> (AppMenu) compareBooksOrSheets.getSelectedToggle().getUserData(),
                compareBooksOrSheets.selectedToggleProperty()));
        
        // 3.初期値の設定
        if (!ar.settings().containsKey(SettingKeys.CURR_MENU)) {
            ar.changeSetting(SettingKeys.CURR_MENU, AppMenu.COMPARE_BOOKS);
        }
        AppMenu menu = ar.settings().get(SettingKeys.CURR_MENU);
        compareBooksOrSheets.selectToggle(
                menu == AppMenu.COMPARE_BOOKS ? compareBooksRadioButton : compareSheetsRadioButton);
        
        // 4.値変更時のイベントハンドラの設定
        compareBooksOrSheets.selectedToggleProperty().addListener(
                (target, oldValue, newValue) -> ar
                        .changeSetting(SettingKeys.CURR_MENU, (AppMenu) newValue.getUserData()));
    }
}
