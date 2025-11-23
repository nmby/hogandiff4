package xyz.hotchpotch.hogandiff.gui.layouts;

import java.io.IOException;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.components.SettingsPane1;
import xyz.hotchpotch.hogandiff.gui.components.SettingsPane2;

/**
 * メインビュー四段目の画面部品です。<br>
 * 
 * @author nmby
 */
public class Row4Pane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    @FXML
    private SettingsPane1 settingsPane1;
    
    @FXML
    private SettingsPane2 settingsPane2;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public Row4Pane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Row4Pane.fxml"), ar.get());
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * この画面部品の内容を初期化します。<br>
     * 
     * @param controller
     *            このアプリケーションのコントローラ
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public void init(MainController controller) {
        Objects.requireNonNull(controller);
        
        // 1.disableプロパティのバインディング
        visibleProperty().bind(controller.propShowSettings);
        managedProperty().bind(controller.propShowSettings);
        
        // 2.項目ごとの各種設定
        settingsPane1.init(controller);
        settingsPane2.init(controller);
        
        // 3.初期値の設定
        
        // 4.値変更時のイベントハンドラの設定
    }
}
