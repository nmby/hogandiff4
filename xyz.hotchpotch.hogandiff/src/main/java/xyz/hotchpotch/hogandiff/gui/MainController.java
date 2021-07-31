package xyz.hotchpotch.hogandiff.gui;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
    
    private List<ChildController> children;
    
    /*package*/ final Factory factory = Factory.of();
    /*package*/ final Property<AppMenu> menu = new SimpleObjectProperty<>();
    /*package*/ final BooleanProperty hasSettingsChanged = new SimpleBooleanProperty(false);
    /*package*/ final BooleanProperty isReady = new SimpleBooleanProperty(false);
    /*package*/ final BooleanProperty isRunning = new SimpleBooleanProperty(false);
    
    /**
     * このコントローラオブジェクトを初期化します。<br>
     */
    public void initialize() {
        children = List.of(
                menuPane,
                targetsPane,
                settingsPane,
                reportingPane,
                utilPane);
        
        isReady.bind(
                children.stream()
                        .map(ChildController::isReady)
                        .reduce(BooleanExpression::and)
                        .get());
        
        children.forEach(child -> child.init(this));
    }
    
    /**
     * 指定された設定の内容で各種コントローラの状態を変更します。<br>
     * 
     * @param settings 設定
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        children.forEach(child -> child.applySettings(settings));
    }
    
    /*package*/ Settings gatherSettings() {
        Settings.Builder builder = Settings.builder();
        
        builder.setDefaultValue(SettingKeys.REDUNDANT_COLOR);
        builder.setDefaultValue(SettingKeys.DIFF_COLOR);
        builder.setDefaultValue(SettingKeys.REDUNDANT_COMMENT_COLOR);
        builder.setDefaultValue(SettingKeys.DIFF_COMMENT_COLOR);
        builder.setDefaultValue(SettingKeys.REDUNDANT_SHEET_COLOR);
        builder.setDefaultValue(SettingKeys.DIFF_SHEET_COLOR);
        builder.setDefaultValue(SettingKeys.SAME_SHEET_COLOR);
        builder.setDefaultValue(SettingKeys.CURR_TIMESTAMP);
        
        builder.set(SettingKeys.CURR_MENU, menu.getValue());
        
        children.forEach(child -> child.gatherSettings(builder));
        
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
        
        Task<Void> task = AppTask.of(settings, factory);
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
