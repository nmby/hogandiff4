package xyz.hotchpotch.hogandiff.gui.layout;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.property.ReadOnlyProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.component.LinkPane;
import xyz.hotchpotch.hogandiff.gui.component.MenuPane;

/**
 * メインビュー一段目の画面部品です。<br>
 * 
 * @author nmby
 */
public class Row1Pane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private MenuPane menuPane;
    
    @FXML
    private LinkPane linkPane;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public Row1Pane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Row1Pane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent, "parent");
        
        // 1.disableプロパティのバインディング
        // nop
        
        // 2.項目ごとの各種設定
        menuPane.init(parent);
        linkPane.init(parent);
        
        // 3.初期値の設定
        // nop
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
    
    /**
     * 選択されている比較メニューを返します。<br>
     * 
     * @return 選択されている比較メニュー
     */
    public ReadOnlyProperty<AppMenu> menu() {
        return menuPane.menu();
    }
}
