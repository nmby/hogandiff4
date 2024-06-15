package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.BookCompareInfo;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.DirCompareInfo;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.TreeCompareInfo;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 比較対象指定部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class TargetsPane extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
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
        
        ar.changeSetting(SettingKeys.CURR_READ_PASSWORDS, new HashMap<>());
        
        // 1.disableプロパティのバインディング
        disableProperty().bind(parent.isRunning());
        
        // 2.項目ごとの各種設定
        targetSelectionPane1.init(parent, Side.A, targetSelectionPane2);
        targetSelectionPane2.init(parent, Side.B, targetSelectionPane1);
        
        parent.sheetCompareInfo.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = parent.menu.getValue();
                    Pair<BookInfo> bookInfoPair = parent.bookInfoPair.map(Property::getValue);
                    Pair<String> sheetNamePair = parent.sheetNamePair.map(Property::getValue);
                    BookCompareInfo prevValue = ar.settings().get(SettingKeys.CURR_SHEET_COMPARE_INFO);
                    
                    switch (menu) {
                        case COMPARE_SHEETS:
                            if (!bookInfoPair.isPaired() || !sheetNamePair.isPaired()) {
                                return null;
                            }
                            if (prevValue != null
                                    && bookInfoPair.equals(prevValue.parentPair())
                                    && sheetNamePair.equals(prevValue.childPairs().get(0))) {
                                return prevValue;
                            } else {
                                return BookCompareInfo.ofSingle(bookInfoPair, sheetNamePair);
                            }
                        case COMPARE_BOOKS:
                        case COMPARE_DIRS:
                        case COMPARE_TREES:
                            return prevValue;
                        default:
                            throw new AssertionError();
                    }
                },
                parent.menu,
                parent.bookInfoPair.a(),
                parent.bookInfoPair.b(),
                parent.sheetNamePair.a(),
                parent.sheetNamePair.b()));
        
        parent.bookCompareInfo.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = parent.menu.getValue();
                    Pair<BookInfo> bookInfoPair = parent.bookInfoPair.map(Property::getValue);
                    BookCompareInfo prevValue = ar.settings().get(SettingKeys.CURR_BOOK_COMPARE_INFO);
                    
                    switch (menu) {
                        case COMPARE_BOOKS:
                            if (!bookInfoPair.isPaired()) {
                                return null;
                            }
                            if (prevValue != null && bookInfoPair.equals(prevValue.parentPair())) {
                                return prevValue;
                            } else {
                                return BookCompareInfo.calculate(
                                        bookInfoPair,
                                        Factory.sheetNamesMatcher(ar.settings()));
                            }
                        case COMPARE_SHEETS:
                        case COMPARE_DIRS:
                        case COMPARE_TREES:
                            return prevValue;
                        default:
                            throw new AssertionError();
                    }
                },
                parent.menu,
                parent.bookInfoPair.a(),
                parent.bookInfoPair.b()));
        
        parent.dirCompareInfo.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = parent.menu.getValue();
                    Pair<DirInfo> dirInfoPair = parent.dirInfoPair.map(Property::getValue);
                    DirCompareInfo prevValue = ar.settings().get(SettingKeys.CURR_DIR_COMPARE_INFO);
                    
                    switch (menu) {
                        case COMPARE_DIRS:
                            if (!dirInfoPair.isPaired()) {
                                return null;
                            }
                            if (prevValue != null && dirInfoPair.equals(prevValue.parentPair())) {
                                return prevValue;
                            } else {
                                return DirCompareInfo.calculate(
                                        dirInfoPair,
                                        Factory.bookNamesMatcher(ar.settings()),
                                        Factory.sheetNamesMatcher(ar.settings()),
                                        ar.settings().get(SettingKeys.CURR_READ_PASSWORDS));
                            }
                        case COMPARE_SHEETS:
                        case COMPARE_BOOKS:
                        case COMPARE_TREES:
                            return prevValue;
                        default:
                            throw new AssertionError();
                    }
                },
                parent.menu,
                parent.dirInfoPair.a(),
                parent.dirInfoPair.b()));
        
        parent.treeCompareInfo.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = parent.menu.getValue();
                    Pair<DirInfo> dirInfoPair = parent.dirInfoPair.map(Property::getValue);
                    TreeCompareInfo prevValue = ar.settings().get(SettingKeys.CURR_TREE_COMPARE_INFO);
                    
                    switch (menu) {
                        case COMPARE_TREES:
                            if (!dirInfoPair.isPaired()) {
                                return null;
                            }
                            if (prevValue != null && dirInfoPair.equals(prevValue.parentPair())) {
                                return prevValue;
                            } else {
                                return TreeCompareInfo.calculate(
                                        dirInfoPair,
                                        Factory.dirsMatcher(ar.settings()),
                                        Factory.bookNamesMatcher(ar.settings()),
                                        Factory.sheetNamesMatcher(ar.settings()),
                                        ar.settings().get(SettingKeys.CURR_READ_PASSWORDS));
                            }
                        case COMPARE_SHEETS:
                        case COMPARE_BOOKS:
                        case COMPARE_DIRS:
                            return prevValue;
                        default:
                            throw new AssertionError();
                    }
                },
                parent.menu,
                parent.dirInfoPair.a(),
                parent.dirInfoPair.b()));
        
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
