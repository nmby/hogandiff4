package xyz.hotchpotch.hogandiff.gui.component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * 比較対象指定部分の画面部品です。<br>
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
        A("A", SettingKeys.CURR_BOOK_OPEN_INFO1, SettingKeys.CURR_SHEET_NAME1, SettingKeys.CURR_DIR_PATH1),
        
        /** 比較対象B */
        B("B", SettingKeys.CURR_BOOK_OPEN_INFO2, SettingKeys.CURR_SHEET_NAME2, SettingKeys.CURR_DIR_PATH2);
        
        // [instance members] --------------------------------------------------
        
        /** どちら側かを著すタイトル */
        public final String title;
        
        /** ブックパス設定項目 */
        public final Key<BookOpenInfo> bookOpenInfoKey;
        
        /** シート名設定項目 */
        public final Key<String> sheetNameKey;
        
        /** フォルダパス設定項目 */
        public final Key<Path> dirPathKey;
        
        Side(
                String title,
                Key<BookOpenInfo> bookOpenInfoKey,
                Key<String> sheetNameKey,
                Key<Path> dirPathKey) {
            
            this.title = title;
            this.bookOpenInfoKey = bookOpenInfoKey;
            this.sheetNameKey = sheetNameKey;
            this.dirPathKey = dirPathKey;
        }
    }
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private TargetSelectionPane targetSelectionPane1;
    
    @FXML
    private TargetSelectionPane targetSelectionPane2;
    
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
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent, "parent");
        
        // 1.disableプロパティのバインディング
        disableProperty().bind(parent.isRunning());
        
        // 2.項目ごとの各種設定
        targetSelectionPane1.init(parent, Side.A, targetSelectionPane2);
        targetSelectionPane2.init(parent, Side.B, targetSelectionPane1);
        
        // 3.初期値の設定
        // nop
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
    
    @Override
    public BooleanExpression isReady() {
        return targetSelectionPane1.isReady().and(targetSelectionPane2.isReady());
    }
}
