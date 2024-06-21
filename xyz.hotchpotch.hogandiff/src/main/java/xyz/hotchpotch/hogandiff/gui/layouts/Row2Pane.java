package xyz.hotchpotch.hogandiff.gui.layouts;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.components.EditCompareInfoPane;
import xyz.hotchpotch.hogandiff.gui.components.ExecutePane;
import xyz.hotchpotch.hogandiff.gui.components.TargetsPane;

/**
 * メインビュー二段目の画面部品です。<br>
 * 
 * @author nmby
 */
public class Row2Pane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private TargetsPane targetsPane;
    
    @FXML
    private EditCompareInfoPane editCompareInfoPane;
    
    @FXML
    private ExecutePane executePane;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public Row2Pane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Row2Pane.fxml"), rb);
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
        targetsPane.init(parent);
        editCompareInfoPane.init(parent);
        executePane.init(parent);
        
        // 3.初期値の設定
        // nop
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
    
    @Override
    public BooleanExpression isReady() {
        return targetsPane.isReady();
    }
}
