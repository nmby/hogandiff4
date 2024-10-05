package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.BookComparison;
import xyz.hotchpotch.hogandiff.excel.DirComparison;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.dialogs.EditComparisonDialog;

/**
 * 組み合わせ変更ボタン部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class EditComparisonPane extends AnchorPane implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private Button editComparisonButton;
    
    private MainController parent;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditComparisonPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EditComparisonPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent);
        this.parent = parent;
        
        // 1.disableプロパティのバインディング
        disableProperty().bind(parent.isRunning());
        editComparisonButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !parent.isReady().getValue() || parent.menuProp.getValue() == AppMenu.COMPARE_SHEETS,
                parent.menuProp, parent.isReady()));
        
        // 2.項目ごとの各種設定
        editComparisonButton.setOnAction(event -> editComparison());
        
        // 3.初期値の設定
        // nop
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
    
    private void editComparison() {
        try {
            AppMenu menu = parent.menuProp.getValue();
            if (!menu.isValidTargets(ar.settings())) {
                new Alert(
                        AlertType.WARNING,
                        rb.getString("gui.MainController.010"),
                        ButtonType.OK)
                                .showAndWait();
                return;
            }
            
            switch (menu) {
                case COMPARE_BOOKS: {
                    BookComparison comparison = ar.settings().get(SettingKeys.CURR_BOOK_COMPARE_INFO);
                    EditComparisonDialog<BookComparison> dialog = new EditComparisonDialog<>(comparison);
                    Optional<BookComparison> modified = dialog.showAndWait();
                    if (modified.isPresent()) {
                        ar.changeSetting(SettingKeys.CURR_BOOK_COMPARE_INFO, modified.get());
                    }
                    return;
                }
                case COMPARE_DIRS: {
                    DirComparison comparison = ar.settings().get(SettingKeys.CURR_DIR_COMPARE_INFO);
                    EditComparisonDialog<DirComparison> dialog = new EditComparisonDialog<>(comparison);
                    Optional<DirComparison> modified = dialog.showAndWait();
                    if (modified.isPresent()) {
                        ar.changeSetting(SettingKeys.CURR_DIR_COMPARE_INFO, modified.get());
                    }
                    return;
                }
                case COMPARE_TREES: {
                    DirComparison comparison = ar.settings().get(SettingKeys.CURR_TREE_COMPARE_INFO);
                    EditComparisonDialog<DirComparison> dialog = new EditComparisonDialog<>(comparison);
                    Optional<DirComparison> modified = dialog.showAndWait();
                    if (modified.isPresent()) {
                        ar.changeSetting(SettingKeys.CURR_TREE_COMPARE_INFO, modified.get());
                    }
                    return;
                }
                case COMPARE_SHEETS:
                    throw new AssertionError();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            // nop
        }
    }
}
