package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;

/**
 * 設定表示切替ボタン部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class TogglePane extends AnchorPane implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private ToggleButton toggleButton;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public TogglePane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TogglePane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent);
        
        // 1.disableプロパティのバインディング
        // nop
        
        // 2.項目ごとの各種設定
        toggleButton.textProperty().bind(Bindings.createStringBinding(
                () -> toggleButton.isSelected() ? "《" : "》",
                toggleButton.selectedProperty()));
        
        // 3.初期値の設定
        toggleButton.setSelected(ar.settings().get(SettingKeys.SHOW_SETTINGS));
        
        // 4.値変更時のイベントハンドラの設定
        toggleButton.setOnAction(event -> ar
                .changeSetting(SettingKeys.SHOW_SETTINGS, toggleButton.isSelected()));
    }
    
    /**
     * 設定エリアを表示するかを返します。<br>
     * 
     * @return 設定エリアを表示する場合は {@code true}
     */
    public BooleanExpression showSettings() {
        return toggleButton.selectedProperty();
    }
}
