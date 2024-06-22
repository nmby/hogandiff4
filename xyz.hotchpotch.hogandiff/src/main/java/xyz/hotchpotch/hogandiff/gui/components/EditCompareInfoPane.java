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
import xyz.hotchpotch.hogandiff.excel.BookInfoComparison;
import xyz.hotchpotch.hogandiff.excel.DirInfoComparison;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.dialogs.EditComparisonDialog;

/**
 * 組み合わせ変更ボタン部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class EditCompareInfoPane extends AnchorPane implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private Button editCompareInfoButton;
    
    private MainController parent;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditCompareInfoPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EditCompareInfoPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent, "parent");
        this.parent = parent;
        
        // 1.disableプロパティのバインディング
        disableProperty().bind(parent.isRunning());
        editCompareInfoButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !parent.isReady().getValue() || parent.menuProp.getValue() == AppMenu.COMPARE_SHEETS,
                parent.menuProp, parent.isReady()));
        
        // 2.項目ごとの各種設定
        editCompareInfoButton.setOnAction(event -> editCompareInfo());
        
        // 3.初期値の設定
        // nop
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
    
    private void editCompareInfo() {
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
                    BookInfoComparison compareInfo = ar.settings().get(SettingKeys.CURR_BOOK_COMPARE_INFO);
                    EditComparisonDialog<BookInfoComparison> dialog = new EditComparisonDialog<>(compareInfo);
                    Optional<BookInfoComparison> modified = dialog.showAndWait();
                    if (modified.isPresent()) {
                        parent.bookInfoComparisonProp.unbind();
                        parent.bookInfoComparisonProp.setValue(modified.get());
                        parent.bindBookInfoComparisonProp();
                    }
                    return;
                }
                case COMPARE_DIRS: {
                    DirInfoComparison compareInfo = ar.settings().get(SettingKeys.CURR_DIR_COMPARE_INFO);
                    EditComparisonDialog<DirInfoComparison> dialog = new EditComparisonDialog<>(compareInfo);
                    Optional<DirInfoComparison> modified = dialog.showAndWait();
                    if (modified.isPresent()) {
                        parent.dirInfoComparisonProp.unbind();
                        parent.dirInfoComparisonProp.setValue(modified.get());
                        parent.bindDirInfoComparisonProp();
                    }
                    return;
                }
                case COMPARE_TREES: {
                    DirInfoComparison compareInfo = ar.settings().get(SettingKeys.CURR_TREE_COMPARE_INFO);
                    EditComparisonDialog<DirInfoComparison> dialog = new EditComparisonDialog<>(compareInfo);
                    Optional<DirInfoComparison> modified = dialog.showAndWait();
                    if (modified.isPresent()) {
                        parent.treeComparisonProp.unbind();
                        parent.treeComparisonProp.setValue(modified.get());
                        parent.bindTreeComparisonProp();
                    }
                    return;
                }
                case COMPARE_SHEETS:
                    throw new UnsupportedOperationException();
            }
            
        } catch (IOException e) {
            // TODO 自動生成された catch ブロック
            e.printStackTrace();
        }
    }
}
