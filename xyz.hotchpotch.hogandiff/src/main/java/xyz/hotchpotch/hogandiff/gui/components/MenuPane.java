package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.CompareMenu;
import xyz.hotchpotch.hogandiff.CompareMenu.CompareObject;
import xyz.hotchpotch.hogandiff.CompareMenu.CompareWay;
import xyz.hotchpotch.hogandiff.ErrorReporter;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;

/**
 * 比較メニュー部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class MenuPane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    @FXML
    private Label compareLabel;
    
    @FXML
    private ToggleGroup compareTarget;
    
    @FXML
    private RadioButton compareBooksRadioButton;
    
    @FXML
    private RadioButton compareSheetsRadioButton;
    
    @FXML
    private RadioButton compareDirsRadioButton;
    
    @FXML
    private CheckBox recursivelyCheckBox;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public MenuPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("MenuPane.fxml"), ar.get());
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
        
        try {
            // 1.disableプロパティのバインディング
            disableProperty().bind(controller.isRunning());
            recursivelyCheckBox.disableProperty().bind(compareDirsRadioButton.selectedProperty().not());
            
            // 2.項目ごとの各種設定
            controller.propCompareMenu.bind(Bindings.createObjectBinding(
                    () -> {
                        CompareObject compareObject = compareTarget.getSelectedToggle() == compareBooksRadioButton
                                ? CompareObject.COMPARE_BOOKS
                                : compareTarget.getSelectedToggle() == compareSheetsRadioButton
                                        ? CompareObject.COMPARE_SHEETS
                                        : recursivelyCheckBox.isSelected()
                                                ? CompareObject.COMPARE_TREES
                                                : CompareObject.COMPARE_DIRS;
                        CompareWay compareWay = CompareWay.TWO_WAY;
                        return new CompareMenu(compareObject, compareWay);
                    },
                    compareTarget.selectedToggleProperty(),
                    recursivelyCheckBox.selectedProperty()));
            
            // 3.初期値の設定
            compareTarget.selectToggle(
                    switch (ar.settings().get(SettingKeys.CURR_MENU).compareObject()) {
                    case COMPARE_BOOKS -> compareBooksRadioButton;
                    case COMPARE_SHEETS -> compareSheetsRadioButton;
                    case COMPARE_DIRS -> compareDirsRadioButton;
                    case COMPARE_TREES -> compareDirsRadioButton;
                    default -> throw new AssertionError("unknown menu");
                    });
            
            recursivelyCheckBox.setSelected(
                    ar.settings().get(SettingKeys.COMPARE_DIRS_RECURSIVELY));
            
            // 4.値変更時のイベントハンドラの設定
            controller.propCompareMenu
                    .addListener((_, _, newValue) -> ar.changeSetting(SettingKeys.CURR_MENU, newValue));
            
            recursivelyCheckBox.selectedProperty()
                    .addListener((_, _, newValue) -> ar.changeSetting(
                            SettingKeys.COMPARE_DIRS_RECURSIVELY,
                            newValue));
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "MenuPane#init-1");
            throw e;
        }
    }
}
