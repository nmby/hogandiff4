package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.StackPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.CompareMenu;
import xyz.hotchpotch.hogandiff.CompareMenu.CompareObject;
import xyz.hotchpotch.hogandiff.ErrorReporter;
import xyz.hotchpotch.hogandiff.Msg;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.dialogs.EditComparisonDialog;
import xyz.hotchpotch.hogandiff.logic.PairingInfoBooks;
import xyz.hotchpotch.hogandiff.logic.PairingInfoDirs;
import xyz.hotchpotch.hogandiff.util.Triple;
import xyz.hotchpotch.hogandiff.util.Triple.Side3;

/**
 * 組み合わせ変更ボタン部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class EditComparisonPane extends StackPane implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    @FXML
    private Button editComparisonButton;
    
    private MainController controller;
    private Side3 side3;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public EditComparisonPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EditComparisonPane.fxml"), ar.get());
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * この画面部品の内容を初期化します。<br>
     * 
     * @param controller
     *            このアプリケーションのコントローラ
     * @param side3
     *            このコンポーネントの側
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public void init(MainController controller, Side3 side3) {
        Objects.requireNonNull(controller);
        Objects.requireNonNull(side3);
        
        try {
            this.controller = controller;
            this.side3 = side3;
            
            // 1.disableプロパティのバインディング
            editComparisonButton.disableProperty().bind(Bindings.createBooleanBinding(
                    () -> {
                        CompareObject compareObject = controller.propCompareMenu.getValue().compareObject();
                        
                        Triple<? extends ObservableValue<?>> targetTriple = switch (compareObject) {
                        case COMPARE_SHEETS -> controller.sheetNamePropTriple;
                        case COMPARE_BOOKS -> controller.bookInfoPropTriple;
                        case COMPARE_DIRS -> controller.dirInfoPropTriple;
                        default -> throw new AssertionError();
                        };
                        
                        boolean isPaired = switch (side3) {
                        case O -> targetTriple.a().getValue() != null && targetTriple.b().getValue() != null;
                        case A -> targetTriple.o().getValue() != null && targetTriple.a().getValue() != null;
                        case B -> targetTriple.o().getValue() != null && targetTriple.b().getValue() != null;
                        };
                        return controller.isRunning().getValue()
                                || controller.propCompareMenu.getValue().compareObject() == CompareObject.COMPARE_SHEETS
                                || !isPaired;
                    },
                    // FIXME: 不細工なのでなんとかしたい...
                    controller.isRunning(),
                    controller.propCompareMenu,
                    controller.sheetNamePropTriple.o(),
                    controller.sheetNamePropTriple.a(),
                    controller.sheetNamePropTriple.b(),
                    controller.bookInfoPropTriple.o(),
                    controller.bookInfoPropTriple.a(),
                    controller.bookInfoPropTriple.b(),
                    controller.dirInfoPropTriple.o(),
                    controller.dirInfoPropTriple.a(),
                    controller.dirInfoPropTriple.b()));
            
            // 2.項目ごとの各種設定
            editComparisonButton.setOnAction(_ -> editComparison());
            
            // 3.初期値の設定
            
            // 4.値変更時のイベントハンドラの設定
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "EditComparisonPane#init-1");
            throw e;
        }
    }
    
    private void editComparison() {
        try {
            CompareMenu menu = controller.propCompareMenu.getValue();
            if (!menu.isValidTargets(ar.settings())) {
                new Alert(
                        AlertType.WARNING,
                        Msg.APP_1180.get(),
                        ButtonType.OK)
                                .showAndWait();
                return;
            }
            
            switch (menu.compareWay()) {
            case TWO_WAY:
                switch (menu.compareObject()) {
                case COMPARE_BOOKS: {
                    PairingInfoBooks comparison = ar.settings().get(SettingKeys.CURR_BOOK_COMPARE_INFO_AB);
                    EditComparisonDialog<PairingInfoBooks> dialog = new EditComparisonDialog<>(comparison);
                    Optional<PairingInfoBooks> modified = dialog.showAndWait();
                    if (modified.isPresent()) {
                        ar.changeSetting(SettingKeys.CURR_BOOK_COMPARE_INFO_AB, modified.get());
                    }
                    return;
                }
                case COMPARE_DIRS: {
                    PairingInfoDirs comparison = ar.settings().get(SettingKeys.CURR_DIR_COMPARE_INFO_AB);
                    EditComparisonDialog<PairingInfoDirs> dialog = new EditComparisonDialog<>(comparison);
                    Optional<PairingInfoDirs> modified = dialog.showAndWait();
                    if (modified.isPresent()) {
                        ar.changeSetting(SettingKeys.CURR_DIR_COMPARE_INFO_AB, modified.get());
                    }
                    return;
                }
                case COMPARE_TREES: {
                    PairingInfoDirs comparison = ar.settings().get(SettingKeys.CURR_TREE_COMPARE_INFO_AB);
                    EditComparisonDialog<PairingInfoDirs> dialog = new EditComparisonDialog<>(comparison);
                    Optional<PairingInfoDirs> modified = dialog.showAndWait();
                    if (modified.isPresent()) {
                        ar.changeSetting(SettingKeys.CURR_TREE_COMPARE_INFO_AB, modified.get());
                    }
                    return;
                }
                case COMPARE_SHEETS:
                    throw new AssertionError();
                default:
                    throw new AssertionError("Unreachable code: " + menu.compareObject());
                }
                
            case THREE_WAY:
                throw new UnsupportedOperationException("Three-way comparison is not supported yet.");
            
            default:
                throw new AssertionError("Unreachable code: " + menu.compareWay());
            
            }
            
        } catch (IOException e) {
            ErrorReporter.reportIfEnabled(e, "EditComparisonPane::editComparison-1");
        }
    }
}
