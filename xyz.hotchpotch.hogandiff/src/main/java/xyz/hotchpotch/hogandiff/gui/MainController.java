package xyz.hotchpotch.hogandiff.gui;

import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppTask;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * このアプリケーションのコントローラです。<br>
 *
 * @author nmby
 */
public class MainController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    // 比較対象選択エリア -----------------------
    
    @FXML
    private MenuPane paneMenu;
    
    @FXML
    private Pane paneSelectTargets;
    
    @FXML
    private TargetBookSheetParts targetBookSheet1;
    
    @FXML
    private TargetBookSheetParts targetBookSheet2;
    
    // 設定エリア ---------------------------
    
    @FXML
    private Pane paneSettings;
    
    @FXML
    private OptionsParts paneOptions;
    
    @FXML
    private Button buttonSaveSettings;
    
    @FXML
    private Button buttonExecute;
    
    // レポートエリア -------------------------
    
    @FXML
    private Pane paneReporting;
    
    @FXML
    private ProgressBar progressReport;
    
    @FXML
    private TextArea textReport;
    
    // Utilエリア -------------------------
    
    @FXML
    private UtilPane paneUtil;
    
    // その他プロパティ --------------------------
    
    private BooleanProperty isReady = new SimpleBooleanProperty(false);
    private BooleanProperty isRunning = new SimpleBooleanProperty(false);
    
    // その他メンバ --------------------------
    
    private Factory factory;
    
    /**
     * 実行の準備が整っているかを返します。<br>
     * 
     * @return 実行の準備が整っている場合は {@code true}
     */
    public boolean isReady() {
        return isReady.getValue();
    }
    
    /**
     * このコントローラオブジェクトを初期化します。<br>
     */
    public void initialize() {
        factory = Factory.of();
        
        initProperties();
        initSettingsArea();
        initExecutionArea();
        
        paneMenu.init();
        targetBookSheet1.init(
                factory,
                "A",
                paneMenu.menuProperty());
        
        targetBookSheet2.init(
                factory,
                "B",
                paneMenu.menuProperty());
        
        paneOptions.init();
        paneUtil.init(SettingKeys.WORK_DIR_BASE.defaultValueSupplier().get());
    }
    
    private void initProperties() {
        // 比較メニューの選択状態を反映させる。
        
        // 各種コントローラの設定状況に応じて「実行」可能な状態か否かを反映させる。
        isReady.bind(targetBookSheet1.isReadyProperty().and(targetBookSheet2.isReadyProperty()));
        
        // 以下のプロパティについては、バインディングで値を反映させるのではなく
        // 相手方のイベントハンドラで値を設定する。
        //      ・hasSettingsChanged
        //      ・isRunning
    }
    
    private void initSettingsArea() {
        
        // 各種設定の変更有無に応じて「設定の保存」ボタンの有効／無効を切り替える。
        buttonSaveSettings.disableProperty().bind(
                paneOptions.hasSettingsChangedProperty().not());
        
        // 「設定を保存」ボタンのイベントハンドラを登録する。
        buttonSaveSettings.setOnAction(event -> {
            Settings settings = gatherSettings();
            Properties properties = settings.toProperties();
            AppMain.storeProperties(properties);
            paneOptions.hasSettingsChangedProperty().set(false);
        });
    }
    
    private void initExecutionArea() {
        // 各種設定状況に応じて「実行」ボタンの有効／無効を切り替える。
        buttonExecute.disableProperty().bind(isReady.not());
        
        // 実行中は全コントローラを無効にする。
        paneMenu.disableProperty().bind(isRunning);
        paneSelectTargets.disableProperty().bind(isRunning);
        paneSettings.disableProperty().bind(isRunning);
        paneUtil.disableProperty().bind(isRunning);
        
        // レポートエリアは常に有効にすることにする。
        //paneReporting.disableProperty().bind(isRunning.not());
        
        // 「実行」ボタンのイベントハンドラを登録する。
        buttonExecute.setOnAction(event -> execute());
    }
    
    /**
     * 指定された設定の内容で各種コントローラの状態を変更します。<br>
     * 
     * @param settings 設定
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        paneMenu.applySettings(settings);
        targetBookSheet1.applySettings(settings, SettingKeys.CURR_BOOK_PATH1, SettingKeys.CURR_SHEET_NAME1);
        targetBookSheet2.applySettings(settings, SettingKeys.CURR_BOOK_PATH2, SettingKeys.CURR_SHEET_NAME2);
        paneOptions.applySettings(settings);
    }
    
    private Settings gatherSettings() {
        Settings.Builder builder = Settings.builder();
        
        paneMenu.gatherSettings(builder);
        targetBookSheet1.gatherSettings(builder, SettingKeys.CURR_BOOK_PATH1, SettingKeys.CURR_SHEET_NAME1);
        targetBookSheet2.gatherSettings(builder, SettingKeys.CURR_BOOK_PATH2, SettingKeys.CURR_SHEET_NAME2);
        paneOptions.gatherSettings(builder);
        
        builder.setDefaultValue(SettingKeys.REDUNDANT_COLOR);
        builder.setDefaultValue(SettingKeys.DIFF_COLOR);
        builder.setDefaultValue(SettingKeys.REDUNDANT_COMMENT_COLOR);
        builder.setDefaultValue(SettingKeys.DIFF_COMMENT_COLOR);
        builder.setDefaultValue(SettingKeys.REDUNDANT_SHEET_COLOR);
        builder.setDefaultValue(SettingKeys.DIFF_SHEET_COLOR);
        builder.setDefaultValue(SettingKeys.SAME_SHEET_COLOR);
        builder.setDefaultValue(SettingKeys.WORK_DIR_BASE);
        builder.setDefaultValue(SettingKeys.CURR_TIMESTAMP);
        
        return builder.build();
    }
    
    /**
     * 比較処理を実行します。<br>
     * 
     * @throws IllegalStateException 必要な設定がなされておらず実行できない場合
     */
    public void execute() {
        if (!isReady.getValue()) {
            throw new IllegalStateException("I'm not ready.");
        }
        
        Settings settings = gatherSettings();
        AppMenu menu = settings.get(SettingKeys.CURR_MENU);
        
        if (!menu.isValidTargets(settings)) {
            new Alert(
                    AlertType.WARNING,
                    "同じブック同士／シート同士を比較することはできません。",
                    ButtonType.OK)
                            .showAndWait();
            return;
        }
        
        isRunning.set(true);
        
        Task<Void> task = AppTask.of(settings, Factory.of());
        progressReport.progressProperty().bind(task.progressProperty());
        textReport.textProperty().bind(task.messageProperty());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(task);
        
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            executor.shutdown();
            progressReport.progressProperty().unbind();
            progressReport.setProgress(0D);
            textReport.textProperty().unbind();
            if (settings.get(SettingKeys.EXIT_WHEN_FINISHED)) {
                Platform.exit();
            } else {
                isRunning.set(false);
            }
        });
        
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event -> {
            executor.shutdown();
            progressReport.progressProperty().unbind();
            progressReport.setProgress(0D);
            textReport.textProperty().unbind();
            new Alert(
                    AlertType.WARNING,
                    task.getException().getMessage(),
                    ButtonType.OK)
                            .showAndWait();
            isRunning.set(false);
        });
    }
}
