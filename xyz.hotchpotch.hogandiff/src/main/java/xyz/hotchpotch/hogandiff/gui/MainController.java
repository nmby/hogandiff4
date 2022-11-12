package xyz.hotchpotch.hogandiff.gui;

import java.util.List;
import java.util.ResourceBundle;
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
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppResource;
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
    private LocalePane localePane;
    
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
    /*package*/ final BooleanProperty isReady = new SimpleBooleanProperty(false);
    /*package*/ final BooleanProperty isRunning = new SimpleBooleanProperty(false);
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    /**
     * このコントローラオブジェクトを初期化します。<br>
     */
    public void initialize() {
        children = List.of(
                menuPane,
                localePane,
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
            throw new IllegalStateException();
        }
        
        Settings settings = ar.settings();
        AppMenu menu = settings.get(SettingKeys.CURR_MENU);
        
        if (!menu.isValidTargets(settings)) {
            new Alert(
                    AlertType.WARNING,
                    rb.getString("gui.MainController.010"),
                    ButtonType.OK)
                            .showAndWait();
            return;
        }
        
        isRunning.set(true);
        
        Task<Void> task = menu.getTask(settings, factory);
        reportingPane.bind(task);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(task);
        
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            executor.shutdown();
            reportingPane.unbind();
            
            if (settings.get(SettingKeys.CURR_BOOK_INFO1).getReadPassword() != null
                    || settings.get(SettingKeys.CURR_BOOK_INFO2).getReadPassword() != null) {
                
                new Alert(
                        AlertType.WARNING,
                        rb.getString("gui.MainController.020"),
                        ButtonType.OK)
                                .showAndWait();
            }
            
            if (settings.getOrDefault(SettingKeys.EXIT_WHEN_FINISHED)) {
                Platform.exit();
            } else {
                isRunning.set(false);
            }
        });
        
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event -> {
            Throwable e = task.getException();
            e.printStackTrace();
            executor.shutdown();
            reportingPane.unbind();
            new Alert(
                    AlertType.WARNING,
                    "%s%n%s%n%s".formatted(
                            rb.getString("gui.MainController.030"),
                            e.getClass().getName(),
                            e.getMessage()),
                    ButtonType.OK)
                            .showAndWait();
            isRunning.set(false);
        });
    }
}
