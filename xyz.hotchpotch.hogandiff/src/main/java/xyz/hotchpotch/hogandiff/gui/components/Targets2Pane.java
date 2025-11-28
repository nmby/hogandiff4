package xyz.hotchpotch.hogandiff.gui.components;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.GridPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
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
public class Targets2Pane extends GridPane implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    @FXML
    private TargetSelectionPane targetSelectionPaneA;
    
    @FXML
    private TargetSelectionPane targetSelectionPaneB;
    
    @FXML
    private EditComparison2Pane editComparisonPane;
    
    private MainController controller;
    
    /** 前回選択されたパス */
    public Path prevSelectedBookPath;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public Targets2Pane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Targets2Pane.fxml"), ar.get());
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
            
            // 2.項目ごとの各種設定
            targetSelectionPaneA.init(controller, this, Side3.A);
            targetSelectionPaneB.init(controller, this, Side3.B);
            editComparisonPane.init(controller);
            
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
        case O -> throw new AssertionError();
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
