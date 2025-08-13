package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.UIUtil;

/**
 * Webサイトへのリンク部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class LinkPane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private Hyperlink toWebSiteHyperlink;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public LinkPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("LinkPane.fxml"), rb);
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
        UIUtil.setupHyperlink(toWebSiteHyperlink, AppMain.WEB_URL);
        
        // 3.初期値の設定
        // nop
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
}
