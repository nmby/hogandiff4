package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.ErrorReporter;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.util.Triple.Side3;

/**
 * 比較対象指定部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class Targets2Pane extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    @FXML
    private TargetSelectionPane targetSelectionPaneA;
    
    @FXML
    private TargetSelectionPane targetSelectionPaneB;
    
    private MainController controller;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public Targets2Pane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Targets2Pane.fxml"), ar.get());
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
        
        this.controller = controller;
        
        try {
            ar.changeSetting(SettingKeys.CURR_READ_PASSWORDS, new HashMap<>());
            
            // 1.disableプロパティのバインディング
            disableProperty().bind(controller.isRunning());
            
            // 2.項目ごとの各種設定
            targetSelectionPaneA.init(controller, this, Side3.A, targetSelectionPaneB);
            targetSelectionPaneB.init(controller, this, Side3.B, targetSelectionPaneA);
            
            // 3.初期値の設定
            
            // 4.値変更時のイベントハンドラの設定
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "TargetsPane#init-1");
            throw e;
        }
    }
    
    @Override
    public BooleanExpression isReady() {
        return targetSelectionPaneA.isReady().and(targetSelectionPaneB.isReady());
    }
}
