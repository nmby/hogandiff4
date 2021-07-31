package xyz.hotchpotch.hogandiff.gui;

import java.util.Objects;
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
import javafx.scene.control.ButtonType;
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
    
    @FXML
    private MenuPane menuPane;
    
    @FXML
    private TargetsPane targetsPane;
    
    @FXML
    private SettingsPane settingsPane;
    
    @FXML
    private ReportingPane reportingPane;
    
    @FXML
    private UtilPane utilPane;
    
    private Factory factory;
    
    /*package*/ final BooleanProperty hasSettingsChanged = new SimpleBooleanProperty(false);
    /*package*/ final BooleanProperty isReady = new SimpleBooleanProperty(false);
    private final BooleanProperty isRunning = new SimpleBooleanProperty(false);
    
    /**
     * このコントローラオブジェクトを初期化します。<br>
     */
    public void initialize() {
        factory = Factory.of();
        isReady.bind(targetsPane.isReady);
        // 以下のプロパティについては、バインディングで値を反映させるのではなく
        // 相手方のイベントハンドラで値を設定する。
        //      ・isRunning
        
        menuPane.init();
        targetsPane.init(factory, menuPane.menu);
        settingsPane.init(this);
        utilPane.init(SettingKeys.WORK_DIR_BASE.defaultValueSupplier().get());
        
        // 実行中はレポートエリアを除く全エリアを無効にする。
        menuPane.disableProperty().bind(isRunning);
        targetsPane.disableProperty().bind(isRunning);
        settingsPane.disableProperty().bind(isRunning);
        utilPane.disableProperty().bind(isRunning);
        //paneReporting.disableProperty().bind(isRunning.not());
    }
    
    /**
     * 指定された設定の内容で各種コントローラの状態を変更します。<br>
     * 
     * @param settings 設定
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        menuPane.applySettings(settings);
        targetsPane.applySettings(settings);
        settingsPane.applySettings(settings);
    }
    
    private Settings gatherSettings() {
        Settings.Builder builder = Settings.builder();
        
        menuPane.gatherSettings(builder);
        targetsPane.gatherSettings(builder);
        settingsPane.gatherSettings(builder);
        
        return builder.build();
    }
    
    /**
     * 実行の準備が整っているかを返します。<br>
     * 
     * @return 実行の準備が整っている場合は {@code true}
     */
    public boolean isReady() {
        return isReady.getValue();
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
        reportingPane.bind(task);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(task);
        
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            executor.shutdown();
            reportingPane.unbind();
            if (settings.get(SettingKeys.EXIT_WHEN_FINISHED)) {
                Platform.exit();
            } else {
                isRunning.set(false);
            }
        });
        
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event -> {
            executor.shutdown();
            reportingPane.unbind();
            new Alert(
                    AlertType.WARNING,
                    task.getException().getMessage(),
                    ButtonType.OK)
                            .showAndWait();
            isRunning.set(false);
        });
    }
}
