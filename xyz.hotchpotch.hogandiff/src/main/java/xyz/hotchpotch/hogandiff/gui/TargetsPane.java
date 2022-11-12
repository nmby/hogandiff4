package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * 比較対象選択部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class TargetsPane extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
    /**
     * 比較対象A, Bのどちら側かを著す列挙型です。<br>
     * 
     * @author nmby
     */
    public static enum Side {
        
        // [static members] ----------------------------------------------------
        
        /** 比較対象A */
        A("A", SettingKeys.CURR_BOOK_INFO1, SettingKeys.CURR_SHEET_NAME1),
        
        /** 比較対象B */
        B("B", SettingKeys.CURR_BOOK_INFO2, SettingKeys.CURR_SHEET_NAME2);
        
        // [instance members] --------------------------------------------------
        
        /** どちら側かを著すタイトル */
        public final String title;
        
        /** ブックパス設定項目 */
        public final Key<BookInfo> bookInfoKey;
        
        /** シート名設定項目 */
        public final Key<String> sheetNameKey;
        
        Side(String title, Key<BookInfo> bookInfoKey, Key<String> sheetNameKey) {
            this.title = title;
            this.bookInfoKey = bookInfoKey;
            this.sheetNameKey = sheetNameKey;
        }
    }
    
    // [instance members] ******************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private TargetSelectionParts targetSelectionParts1;
    
    @FXML
    private TargetSelectionParts targetSelectionParts2;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public TargetsPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TargetsPane.fxml"), rb);
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
        targetSelectionParts1.init(parent, Side.A, targetSelectionParts2);
        targetSelectionParts2.init(parent, Side.B, targetSelectionParts1);
        
        // 3.初期値の設定
        // nop
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
    
    @Override
    public BooleanExpression isReady() {
        return targetSelectionParts1.isReady.and(targetSelectionParts2.isReady);
    }
}
