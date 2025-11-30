package xyz.hotchpotch.hogandiff.gui.components;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.layout.GridPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.CompareMenu.CompareWay;
import xyz.hotchpotch.hogandiff.ErrorReporter;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.util.Triple.Side3;

/**
 * 比較対象指定部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class TargetsPane extends GridPane implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    @FXML
    private TargetSelectionPane targetSelectionPaneO;
    
    @FXML
    private TargetSelectionPane targetSelectionPaneA;
    
    @FXML
    private TargetSelectionPane targetSelectionPaneB;
    
    @FXML
    private EditComparisonPane editComparisonPaneAB;
    
    @FXML
    private EditComparisonPane editComparisonPaneOA;
    
    @FXML
    private EditComparisonPane editComparisonPaneOB;
    
    private MainController controller;
    
    /** 前回選択されたパス */
    public Path prevSelectedBookPath;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public TargetsPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TargetsPane.fxml"), ar.get());
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
        
        this.controller = controller;
        
        try {
            ar.changeSetting(SettingKeys.CURR_READ_PASSWORDS, new HashMap<>());
            
            // 1.disableプロパティのバインディング
            disableProperty().bind(controller.isRunning());
            
            BooleanBinding is3Way = Bindings.createBooleanBinding(
                    () -> controller.propCompareMenu.getValue().compareWay() == CompareWay.THREE_WAY,
                    controller.propCompareMenu);
            
            targetSelectionPaneO.visibleProperty().bind(is3Way);
            targetSelectionPaneO.managedProperty().bind(is3Way);
            
            editComparisonPaneAB.visibleProperty().bind(is3Way.not());
            editComparisonPaneAB.managedProperty().bind(is3Way.not());
            editComparisonPaneOA.visibleProperty().bind(is3Way);
            editComparisonPaneOA.managedProperty().bind(is3Way);
            editComparisonPaneOB.visibleProperty().bind(is3Way);
            editComparisonPaneOB.managedProperty().bind(is3Way);
            
            // 2.項目ごとの各種設定
            GridPane.setMargin(targetSelectionPaneO, new Insets(0, 0, 3, 0));
            GridPane.setMargin(targetSelectionPaneA, new Insets(0, 0, 2, 0));
            GridPane.setMargin(targetSelectionPaneB, new Insets(1, 0, 0, 0));
            
            targetSelectionPaneO.init(controller, this, Side3.O);
            targetSelectionPaneA.init(controller, this, Side3.A);
            targetSelectionPaneB.init(controller, this, Side3.B);
            editComparisonPaneAB.init(controller);
            editComparisonPaneOA.init(controller);
            editComparisonPaneOB.init(controller);
            
            // 3.初期値の設定
            
            // 4.値変更時のイベントハンドラの設定
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "TargetsPane#init-1");
            throw e;
        }
    }
    
    @Override
    public BooleanExpression isReady() {
        return targetSelectionPaneA.isReady().and(targetSelectionPaneB.isReady());
    }
    
    /**
     * ドラッグ＆ドロップされたディレクトリパスを各コンポーネントに設定します。<br>
     * 
     * @param side3
     *            ドロップされたコンポーネントの側
     * @param files
     *            ドロップされたファイル一覧
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public void setDirPathForAll(Side3 side3, List<File> files) {
        Objects.requireNonNull(side3);
        Objects.requireNonNull(files);
        
        List<TargetSelectionPane> targets = switch (side3) {
        // TODO: Oにも対応する
        case O -> throw new UnsupportedOperationException();
        case A -> List.of(targetSelectionPaneA, targetSelectionPaneB);
        case B -> List.of(targetSelectionPaneB, targetSelectionPaneA);
        default -> throw new AssertionError();
        };
        
        for (int i = 0; i < targets.size() && i < files.size() && files.get(i).isDirectory(); i++) {
            TargetSelectionPane target = targets.get(i);
            Path newDirPath = files.get(i).toPath();
            target.setDirPath(newDirPath, ar.settings().get(SettingKeys.COMPARE_DIRS_RECURSIVELY));
        }
    }
    
    /**
     * ドラッグ＆ドロップされたファイルパスを各コンポーネントに設定します。<br>
     * 
     * @param side3
     *            ドロップされたコンポーネントの側
     * @param files
     *            ドロップされたファイル一覧
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public void validateAndSetTargetForAll(Side3 side3, List<File> files) {
        Objects.requireNonNull(side3);
        Objects.requireNonNull(files);
        
        List<TargetSelectionPane> targets = switch (side3) {
        // TODO: Oにも対応する
        case O -> throw new UnsupportedOperationException();
        case A -> List.of(targetSelectionPaneA, targetSelectionPaneB);
        case B -> List.of(targetSelectionPaneB, targetSelectionPaneA);
        default -> throw new AssertionError();
        };
        
        for (int i = 0; i < targets.size() && i < files.size() && files.get(i).isFile(); i++) {
            TargetSelectionPane target = targets.get(i);
            Path newBookPath = files.get(i).toPath();
            target.validateAndSetTarget(newBookPath, null, null);
        }
    }
}
