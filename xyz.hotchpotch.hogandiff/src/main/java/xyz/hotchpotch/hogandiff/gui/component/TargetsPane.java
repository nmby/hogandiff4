package xyz.hotchpotch.hogandiff.gui.component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.BookCompareInfo;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.util.Pair;
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
        A("A", SettingKeys.CURR_BOOK_INFO1, SettingKeys.CURR_DIR_INFO1),
        
        /** 比較対象B */
        B("B", SettingKeys.CURR_BOOK_INFO2, SettingKeys.CURR_DIR_INFO2);
        
        // [instance members] --------------------------------------------------
        
        /** どちら側かを著すタイトル */
        public final String title;
        
        /** ブックパス設定項目 */
        public final Key<BookInfo> bookInfoKey;
        
        /** フォルダパス設定項目 */
        public final Key<DirInfo> dirInfoKey;
        
        Side(
                String title,
                Key<BookInfo> bookInfoKey,
                Key<DirInfo> dirInfoKey) {
            
            this.title = title;
            this.bookInfoKey = bookInfoKey;
            this.dirInfoKey = dirInfoKey;
        }
    }
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private TargetSelectionPane targetSelectionPane1;
    
    @FXML
    private TargetSelectionPane targetSelectionPane2;
    
    private final Property<BookCompareInfo> bookCompareInfo = new SimpleObjectProperty<>();
    
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
        
        ar.changeSetting(SettingKeys.CURR_READ_PASSWORDS, new HashMap<>());
        
        // 1.disableプロパティのバインディング
        disableProperty().bind(parent.isRunning());
        
        // 2.項目ごとの各種設定
        targetSelectionPane1.init(parent, Side.A, targetSelectionPane2);
        targetSelectionPane2.init(parent, Side.B, targetSelectionPane1);
        
        bookCompareInfo.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = parent.menu().getValue();
                    BookInfo bookInfoA = targetSelectionPane1.bookInfo().getValue();
                    BookInfo bookInfoB = targetSelectionPane2.bookInfo().getValue();
                    Pair<BookInfo> bookInfoPair = Pair.of(bookInfoA, bookInfoB);
                    String sheetNameA = targetSelectionPane1.sheetName().getValue();
                    String sheetNameB = targetSelectionPane2.sheetName().getValue();
                    Pair<String> sheetNamePair = Pair.of(sheetNameA, sheetNameB);
                    
                    return switch (menu) {
                        case COMPARE_SHEETS -> bookInfoPair.isPaired() && sheetNamePair.isPaired()
                                ? BookCompareInfo.ofSingle(bookInfoPair, sheetNamePair)
                                : null;
                        case COMPARE_BOOKS -> bookInfoPair.isPaired()
                                ? BookCompareInfo.of(bookInfoPair, Factory.sheetNamesMatcher2(ar.settings()))
                                : null;
                        case COMPARE_DIRS, COMPARE_TREES -> null;
                        default -> throw new AssertionError();
                    };
                },
                parent.menu(),
                targetSelectionPane1.bookInfo(),
                targetSelectionPane2.bookInfo(),
                targetSelectionPane1.sheetName(),
                targetSelectionPane2.sheetName()));
        
        bookCompareInfo.addListener((target, oldValue, newValue) -> {
            ar.changeSetting(SettingKeys.CURR_BOOK_COMPARE_INFO, newValue);
        });
        
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
