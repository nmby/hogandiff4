package xyz.hotchpotch.hogandiff.gui;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppTask;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.function.UnsafeConsumer;

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
    private Pane paneSelectMenu;
    
    @FXML
    private RadioButton radioCompareBooks;
    
    @FXML
    private RadioButton radioCompareSheets;
    
    @FXML
    private Pane paneSelectTargets;
    
    @FXML
    private TargetBookSheetController targetBookSheet1;
    
    @FXML
    private TargetBookSheetController targetBookSheet2;
    
    // 設定エリア ---------------------------
    
    @FXML
    private Pane paneSettings;
    
    @FXML
    private CheckBox checkConsiderRowGaps;
    
    @FXML
    private CheckBox checkConsiderColumnGaps;
    
    @FXML
    private CheckBox checkCompareCellContents;
    
    @FXML
    private RadioButton radioCompareOnValue;
    
    @FXML
    private RadioButton radioCompareOnFormula;
    
    @FXML
    private CheckBox checkCompareCellComments;
    
    @FXML
    private CheckBox checkShowPaintedSheets;
    
    @FXML
    private CheckBox checkShowResultText;
    
    @FXML
    private CheckBox checkExitWhenFinished;
    
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
    private Pane paneUtil;
    
    @FXML
    private Button buttonShowWorkDir;
    
    @FXML
    private Button buttonDeleteOldWorkDir;
    
    @FXML
    private Hyperlink linkToWebSite;
    
    // その他プロパティ --------------------------
    
    private Property<AppMenu> menu = new SimpleObjectProperty<>();
    private BooleanProperty hasSettingsChanged = new SimpleBooleanProperty(false);
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
        initUtilArea();
        
        targetBookSheet1.init(
                factory,
                "A",
                radioCompareBooks.selectedProperty());
        
        targetBookSheet2.init(
                factory,
                "B",
                radioCompareBooks.selectedProperty());
    }
    
    private void initProperties() {
        // 比較メニューの選択状態を反映させる。
        menu.bind(Bindings.createObjectBinding(
                () -> radioCompareBooks.isSelected()
                        ? AppMenu.COMPARE_BOOKS
                        : AppMenu.COMPARE_SHEETS,
                radioCompareBooks.selectedProperty()));
        
        // 各種コントローラの設定状況に応じて「実行」可能な状態か否かを反映させる。
        isReady.bind(targetBookSheet1.isReadyProperty().and(targetBookSheet2.isReadyProperty()));
        
        // 以下のプロパティについては、バインディングで値を反映させるのではなく
        // 相手方のイベントハンドラで値を設定する。
        //      ・hasSettingsChanged
        //      ・isRunning
    }
    
    private void initSettingsArea() {
        // 各種設定を変更した場合は、それをプロパティに反映させる。
        checkConsiderRowGaps.setOnAction(event -> hasSettingsChanged.set(true));
        checkConsiderColumnGaps.setOnAction(event -> hasSettingsChanged.set(true));
        checkCompareCellContents.setOnAction(event -> hasSettingsChanged.set(true));
        radioCompareOnValue.setOnAction(event -> hasSettingsChanged.set(true));
        radioCompareOnFormula.setOnAction(event -> hasSettingsChanged.set(true));
        checkCompareCellComments.setOnAction(event -> hasSettingsChanged.set(true));
        checkShowPaintedSheets.setOnAction(event -> hasSettingsChanged.set(true));
        checkShowResultText.setOnAction(event -> hasSettingsChanged.set(true));
        checkExitWhenFinished.setOnAction(event -> hasSettingsChanged.set(true));
        
        // 「セル内容を比較する」が選択された場合のみ、「値／数式」の選択を有効にする。
        radioCompareOnValue.disableProperty().bind(checkCompareCellContents.selectedProperty().not());
        radioCompareOnFormula.disableProperty().bind(checkCompareCellContents.selectedProperty().not());
        
        // 各種設定の変更有無に応じて「設定の保存」ボタンの有効／無効を切り替える。
        buttonSaveSettings.disableProperty().bind(hasSettingsChanged.not());
        
        // 「設定を保存」ボタンのイベントハンドラを登録する。
        buttonSaveSettings.setOnAction(event -> {
            Settings settings = gatherSettings();
            Properties properties = settings.toProperties();
            AppMain.storeProperties(properties);
            hasSettingsChanged.set(false);
        });
    }
    
    private void initExecutionArea() {
        // 各種設定状況に応じて「実行」ボタンの有効／無効を切り替える。
        buttonExecute.disableProperty().bind(isReady.not());
        
        // 実行中は全コントローラを無効にする。
        paneSelectMenu.disableProperty().bind(isRunning);
        paneSelectTargets.disableProperty().bind(isRunning);
        paneSettings.disableProperty().bind(isRunning);
        paneUtil.disableProperty().bind(isRunning);
        
        // レポートエリアは常に有効にすることにする。
        //paneReporting.disableProperty().bind(isRunning.not());
        
        // 「実行」ボタンのイベントハンドラを登録する。
        buttonExecute.setOnAction(event -> execute());
    }
    
    private void initUtilArea() {
        Path workDir = SettingKeys.WORK_DIR_BASE.defaultValueSupplier().get();
        
        buttonShowWorkDir.setOnAction(event -> {
            try {
                if (!Files.isDirectory(workDir)) {
                    Files.createDirectories(workDir);
                }
                Desktop.getDesktop().open(workDir.toFile());
            } catch (Exception e) {
                // nop
            }
        });
        
        buttonDeleteOldWorkDir.setOnAction(event -> {
            Optional<ButtonType> result = new Alert(
                    AlertType.CONFIRMATION,
                    "次のフォルダの内容物を全て削除します。よろしいですか？\n" + workDir)
                            .showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try (Stream<Path> children = Files.walk(workDir)) {
                    children.filter(path -> !path.equals(workDir))
                            .sorted(Comparator.reverseOrder())
                            .forEach(UnsafeConsumer.toConsumer(Files::deleteIfExists));
                } catch (Exception e) {
                    //nop
                }
            }
        });
        
        linkToWebSite.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://hogandiff.hotchpotch.xyz/"));
            } catch (Exception e) {
                // nop
            }
        });
    }
    
    /**
     * 指定された設定の内容で各種コントローラの状態を変更します。<br>
     * 
     * @param settings 設定
     * @throws NullPointerException {@code settings} が {@code null} の場合
     */
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        if (settings.containsKey(SettingKeys.CURR_MENU)) {
            radioCompareBooks.setSelected(settings.get(SettingKeys.CURR_MENU) == AppMenu.COMPARE_BOOKS);
        }
        if (settings.containsKey(SettingKeys.CURR_BOOK_PATH1)) {
            targetBookSheet1.validateAndSetTarget(
                    settings.get(SettingKeys.CURR_BOOK_PATH1),
                    settings.containsKey(SettingKeys.CURR_SHEET_NAME1)
                            ? settings.get(SettingKeys.CURR_SHEET_NAME1)
                            : null);
        }
        if (settings.containsKey(SettingKeys.CURR_BOOK_PATH2)) {
            targetBookSheet2.validateAndSetTarget(
                    settings.get(SettingKeys.CURR_BOOK_PATH2),
                    settings.containsKey(SettingKeys.CURR_SHEET_NAME2)
                            ? settings.get(SettingKeys.CURR_SHEET_NAME2)
                            : null);
        }
        if (settings.containsKey(SettingKeys.CONSIDER_ROW_GAPS)) {
            checkConsiderRowGaps.setSelected(settings.get(SettingKeys.CONSIDER_ROW_GAPS));
        }
        if (settings.containsKey(SettingKeys.CONSIDER_COLUMN_GAPS)) {
            checkConsiderColumnGaps.setSelected(settings.get(SettingKeys.CONSIDER_COLUMN_GAPS));
        }
        if (settings.containsKey(SettingKeys.COMPARE_CELL_CONTENTS)) {
            checkCompareCellContents.setSelected(settings.get(SettingKeys.COMPARE_CELL_CONTENTS));
        }
        if (settings.containsKey(SettingKeys.COMPARE_CELL_COMMENTS)) {
            checkCompareCellComments.setSelected(settings.get(SettingKeys.COMPARE_CELL_COMMENTS));
        }
        if (settings.containsKey(SettingKeys.COMPARE_ON_FORMULA_STRING)) {
            radioCompareOnFormula.setSelected(settings.get(SettingKeys.COMPARE_ON_FORMULA_STRING));
        }
        if (settings.containsKey(SettingKeys.SHOW_PAINTED_SHEETS)) {
            checkShowPaintedSheets.setSelected(settings.get(SettingKeys.SHOW_PAINTED_SHEETS));
        }
        if (settings.containsKey(SettingKeys.SHOW_RESULT_TEXT)) {
            checkShowResultText.setSelected(settings.get(SettingKeys.SHOW_RESULT_TEXT));
        }
        if (settings.containsKey(SettingKeys.EXIT_WHEN_FINISHED)) {
            checkExitWhenFinished.setSelected(settings.get(SettingKeys.EXIT_WHEN_FINISHED));
        }
    }
    
    private Settings gatherSettings() {
        Settings.Builder builder = Settings.builder();
        
        builder.set(SettingKeys.CURR_MENU, menu.getValue());
        Path bookPath1 = targetBookSheet1.bookPathProperty().getValue();
        Path bookPath2 = targetBookSheet2.bookPathProperty().getValue();
        if (bookPath1 != null) {
            builder.set(SettingKeys.CURR_BOOK_PATH1, bookPath1);
        }
        if (bookPath2 != null) {
            builder.set(SettingKeys.CURR_BOOK_PATH2, bookPath2);
        }
        if (menu.getValue() == AppMenu.COMPARE_SHEETS) {
            String sheetName1 = targetBookSheet1.sheetNameProperty().getValue();
            String sheetName2 = targetBookSheet2.sheetNameProperty().getValue();
            if (sheetName1 != null) {
                builder.set(SettingKeys.CURR_SHEET_NAME1, sheetName1);
            }
            if (sheetName2 != null) {
                builder.set(SettingKeys.CURR_SHEET_NAME2, sheetName2);
            }
        }
        builder.set(SettingKeys.CONSIDER_ROW_GAPS, checkConsiderRowGaps.isSelected());
        builder.set(SettingKeys.CONSIDER_COLUMN_GAPS, checkConsiderColumnGaps.isSelected());
        builder.set(SettingKeys.COMPARE_CELL_CONTENTS, checkCompareCellContents.isSelected());
        builder.set(SettingKeys.COMPARE_CELL_COMMENTS, checkCompareCellComments.isSelected());
        builder.set(SettingKeys.COMPARE_ON_FORMULA_STRING, radioCompareOnFormula.isSelected());
        builder.set(SettingKeys.SHOW_PAINTED_SHEETS, checkShowPaintedSheets.isSelected());
        builder.set(SettingKeys.SHOW_RESULT_TEXT, checkShowResultText.isSelected());
        builder.set(SettingKeys.EXIT_WHEN_FINISHED, checkExitWhenFinished.isSelected());
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
