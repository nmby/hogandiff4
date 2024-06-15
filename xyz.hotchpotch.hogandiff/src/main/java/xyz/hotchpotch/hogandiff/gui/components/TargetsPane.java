package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
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
                    BookInfo bookInfoA = parent.bookInfoPair.a().getValue();
                    BookInfo bookInfoB = parent.bookInfoPair.b().getValue();
                    Pair<BookInfo> bookInfoPair = Pair.of(bookInfoA, bookInfoB);
                    String sheetNameA = parent.sheetNamePair.a().getValue();
                    String sheetNameB = parent.sheetNamePair.b().getValue();
                    Pair<String> sheetNamePair = Pair.of(sheetNameA, sheetNameB);
                    
                    return switch (menu) {
                        case COMPARE_SHEETS -> bookInfoPair.isPaired() && sheetNamePair.isPaired()
                                ? BookCompareInfo.ofSingle(bookInfoPair, sheetNamePair)
                                : null;
                        case COMPARE_BOOKS, COMPARE_DIRS, COMPARE_TREES -> null;
                        default -> throw new AssertionError();
                    };
                },
                parent.menu,
                parent.bookInfoPair.a(),
                parent.bookInfoPair.b(),
                parent.sheetNamePair.a(),
                parent.sheetNamePair.b()));
        
        parent.bookCompareInfo.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = parent.menu.getValue();
                    BookInfo bookInfoA = parent.bookInfoPair.a().getValue();
                    BookInfo bookInfoB = parent.bookInfoPair.b().getValue();
                    Pair<BookInfo> bookInfoPair = Pair.of(bookInfoA, bookInfoB);
                    
                    return switch (menu) {
                        case COMPARE_BOOKS -> bookInfoPair.isPaired()
                                ? BookCompareInfo.calculate(bookInfoPair, Factory.sheetNamesMatcher(ar.settings()))
                                : null;
                        case COMPARE_SHEETS, COMPARE_DIRS, COMPARE_TREES -> null;
                        default -> throw new AssertionError();
                    };
                },
                parent.menu,
                parent.bookInfoPair.a(),
                parent.bookInfoPair.b()));
        
        parent.dirCompareInfo.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = parent.menu.getValue();
                    DirInfo dirInfoA = parent.dirInfoPair.a().getValue();
                    DirInfo dirInfoB = parent.dirInfoPair.b().getValue();
                    Pair<DirInfo> dirInfoPair = Pair.of(dirInfoA, dirInfoB);
                    
                    return switch (menu) {
                        case COMPARE_DIRS -> dirInfoPair.isPaired()
                                ? DirCompareInfo.calculate(
                                        dirInfoPair,
                                        Factory.bookNamesMatcher(ar.settings()),
                                        Factory.sheetNamesMatcher(ar.settings()),
                                        ar.settings().get(SettingKeys.CURR_READ_PASSWORDS))
                                : null;
                        case COMPARE_SHEETS, COMPARE_BOOKS, COMPARE_TREES -> null;
                        default -> throw new AssertionError();
                    };
                },
                parent.menu,
                parent.dirInfoPair.a(),
                parent.dirInfoPair.b()));
        
        parent.treeCompareInfo.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = parent.menu.getValue();
                    DirInfo topDirInfoA = parent.dirInfoPair.a().getValue();
                    DirInfo topDirInfoB = parent.dirInfoPair.b().getValue();
                    Pair<DirInfo> topDirInfoPair = Pair.of(topDirInfoA, topDirInfoB);
                    
                    return switch (menu) {
                        case COMPARE_SHEETS, COMPARE_BOOKS, COMPARE_DIRS -> null;
                        case COMPARE_TREES -> topDirInfoPair.isPaired()
                                ? TreeCompareInfo.calculate(
                                        topDirInfoPair,
                                        Factory.dirsMatcher(ar.settings()),
                                        Factory.bookNamesMatcher(ar.settings()),
                                        Factory.sheetNamesMatcher(ar.settings()),
                                        ar.settings().get(SettingKeys.CURR_READ_PASSWORDS))
                                : null;
                        default -> throw new AssertionError();
                    };
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
