package xyz.hotchpotch.hogandiff.gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.ApplicationException;
import xyz.hotchpotch.hogandiff.Report;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.BookCompareInfo;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.DirCompareInfo;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SheetCompareInfo;
import xyz.hotchpotch.hogandiff.excel.TreeCompareInfo;
import xyz.hotchpotch.hogandiff.gui.layouts.Row1Pane;
import xyz.hotchpotch.hogandiff.gui.layouts.Row2Pane;
import xyz.hotchpotch.hogandiff.gui.layouts.Row3Pane;
import xyz.hotchpotch.hogandiff.gui.layouts.Row4Pane;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * このアプリケーションのコントローラです。<br>
 *
 * @author nmby
 */
public class MainController extends VBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private Row1Pane row1Pane;
    
    @FXML
    private Row2Pane row2Pane;
    
    @FXML
    private Row3Pane row3Pane;
    
    @FXML
    private Row4Pane row4Pane;
    
    private final BooleanProperty isReady = new SimpleBooleanProperty(false);
    private final BooleanProperty isRunning = new SimpleBooleanProperty(false);
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    /** 現在選択されている比較メニュー */
    public final Property<AppMenu> menuProp = new SimpleObjectProperty<>();
    
    /** シート比較情報 */
    public final Property<SheetCompareInfo> sheetCompareInfoProp = new SimpleObjectProperty<>();
    
    /** Excelブック比較情報 */
    public final Property<BookCompareInfo> bookCompareInfoProp = new SimpleObjectProperty<>();
    
    /** フォルダ比較情報 */
    public final Property<DirCompareInfo> dirCompareInfoProp = new SimpleObjectProperty<>();
    
    /** フォルダツリー比較情報 */
    public final Property<TreeCompareInfo> treeCompareInfoProp = new SimpleObjectProperty<>();
    
    /** シート名のペア */
    public final Pair<StringProperty> sheetNamePropPair = Pair.of(
            new SimpleStringProperty(),
            new SimpleStringProperty());
    
    /** Excelブック情報のペア */
    public final Pair<Property<BookInfo>> bookInfoPropPair = Pair.of(
            new SimpleObjectProperty<>(),
            new SimpleObjectProperty<>());
    
    /** フォルダ情報のペア */
    public final Pair<Property<DirInfo>> dirInfoPropPair = Pair.of(
            new SimpleObjectProperty<>(),
            new SimpleObjectProperty<>());
    
    /**
     * このコントローラオブジェクトを初期化します。<br>
     */
    public void initialize() {
        
        // 1.disableプロパティのバインディング
        // nop
        
        // 2.項目ごとの各種設定
        row1Pane.init(this);
        row2Pane.init(this);
        row3Pane.init(this);
        row4Pane.init(this);
        
        isReady.bind(row1Pane.isReady()
                .and(row2Pane.isReady())
                .and(row3Pane.isReady())
                .and(row4Pane.isReady()));
        
        row3Pane.showSettings().addListener((target, oldValue, newValue) -> {
            if (newValue) {
                row4Pane.setVisible2(true);
                AppMain.stage.setHeight(AppMain.stage.getHeight() + row4Pane.originalHeight() + 3);
                AppMain.stage.setMinHeight(AppMain.STAGE_HEIGHT_OPEN);
            } else {
                AppMain.stage.setHeight(AppMain.stage.getHeight() - row4Pane.originalHeight() - 3);
                AppMain.stage.setMinHeight(AppMain.STAGE_HEIGHT_CLOSE);
                row4Pane.setVisible2(false);
            }
        });
        
        bindSheetCompareInfoProp();
        bindBookCompareInfoProp();
        bindDirCompareInfoProp();
        bindTreeCompareInfoProp();
        
        sheetCompareInfoProp.addListener(
                (target, oldVal, newVal) -> ar.changeSetting(SettingKeys.CURR_SHEET_COMPARE_INFO, newVal));
        bookCompareInfoProp.addListener(
                (target, oldVal, newVal) -> ar.changeSetting(SettingKeys.CURR_BOOK_COMPARE_INFO, newVal));
        dirCompareInfoProp.addListener(
                (target, oldVal, newVal) -> ar.changeSetting(SettingKeys.CURR_DIR_COMPARE_INFO, newVal));
        treeCompareInfoProp.addListener(
                (target, oldVal, newVal) -> ar.changeSetting(SettingKeys.CURR_TREE_COMPARE_INFO, newVal));
        
        // 3.初期値の設定
        row4Pane.setVisible2(row3Pane.showSettings().getValue());
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
    
    /**
     * sheetCompareInfoPropプロパティにデータソースをバインドします。<br>
     */
    // こんなメソッドをpublicにするのはいくらなんでもおかしい。
    // TODO: 処理構成を見直す
    public void bindSheetCompareInfoProp() {
        sheetCompareInfoProp.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = menuProp.getValue();
                    Pair<BookInfo> bookInfoPair = bookInfoPropPair.map(Property::getValue);
                    Pair<String> sheetNamePair = sheetNamePropPair.map(Property::getValue);
                    SheetCompareInfo prevValue = ar.settings().get(SettingKeys.CURR_SHEET_COMPARE_INFO);
                    
                    switch (menu) {
                        case COMPARE_SHEETS:
                            if (!bookInfoPair.isPaired() || !sheetNamePair.isPaired()) {
                                return null;
                            }
                            if (prevValue != null
                                    && bookInfoPair.equals(prevValue.parentPair())
                                    && sheetNamePair.equals(prevValue.childPairs().get(0))) {
                                return prevValue;
                            } else {
                                return SheetCompareInfo.of(bookInfoPair, sheetNamePair);
                            }
                        case COMPARE_BOOKS:
                        case COMPARE_DIRS:
                        case COMPARE_TREES:
                            return prevValue;
                        default:
                            throw new AssertionError();
                    }
                },
                menuProp,
                bookInfoPropPair.a(),
                bookInfoPropPair.b(),
                sheetNamePropPair.a(),
                sheetNamePropPair.b()));
    }
    
    /**
     * bookCompareInfoPropプロパティにデータソースをバインドします。<br>
     */
    public void bindBookCompareInfoProp() {
        bookCompareInfoProp.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = menuProp.getValue();
                    Pair<BookInfo> bookInfoPair = bookInfoPropPair.map(Property::getValue);
                    BookCompareInfo prevValue = ar.settings().get(SettingKeys.CURR_BOOK_COMPARE_INFO);
                    
                    switch (menu) {
                        case COMPARE_BOOKS:
                            if (!bookInfoPair.isPaired()) {
                                return null;
                            }
                            if (prevValue != null && bookInfoPair.equals(prevValue.parentPair())) {
                                return prevValue;
                            } else {
                                return BookCompareInfo.calculate(
                                        bookInfoPair,
                                        Factory.sheetNamesMatcher(ar.settings()));
                            }
                        case COMPARE_SHEETS:
                        case COMPARE_DIRS:
                        case COMPARE_TREES:
                            return prevValue;
                        default:
                            throw new AssertionError();
                    }
                },
                menuProp,
                bookInfoPropPair.a(),
                bookInfoPropPair.b()));
    }
    
    /**
     * dirCompareInfoPropプロパティにデータソースをバインドします。<br>
     */
    public void bindDirCompareInfoProp() {
        dirCompareInfoProp.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = menuProp.getValue();
                    Pair<DirInfo> dirInfoPair = dirInfoPropPair.map(Property::getValue);
                    DirCompareInfo prevValue = ar.settings().get(SettingKeys.CURR_DIR_COMPARE_INFO);
                    
                    switch (menu) {
                        case COMPARE_DIRS:
                            if (!dirInfoPair.isPaired()) {
                                return null;
                            }
                            if (prevValue != null && dirInfoPair.equals(prevValue.parentPair())) {
                                return prevValue;
                            } else {
                                return DirCompareInfo.calculate(
                                        dirInfoPair,
                                        Factory.bookNamesMatcher(ar.settings()),
                                        Factory.sheetNamesMatcher(ar.settings()),
                                        ar.settings().get(SettingKeys.CURR_READ_PASSWORDS));
                            }
                        case COMPARE_SHEETS:
                        case COMPARE_BOOKS:
                        case COMPARE_TREES:
                            return prevValue;
                        default:
                            throw new AssertionError();
                    }
                },
                menuProp,
                dirInfoPropPair.a(),
                dirInfoPropPair.b()));
    }
    
    /**
     * treeCompareInfoPropプロパティにデータソースをバインドします。<br>
     */
    public void bindTreeCompareInfoProp() {
        treeCompareInfoProp.bind(Bindings.createObjectBinding(
                () -> {
                    AppMenu menu = menuProp.getValue();
                    Pair<DirInfo> dirInfoPair = dirInfoPropPair.map(Property::getValue);
                    TreeCompareInfo prevValue = ar.settings().get(SettingKeys.CURR_TREE_COMPARE_INFO);
                    
                    switch (menu) {
                        case COMPARE_TREES:
                            if (!dirInfoPair.isPaired()) {
                                return null;
                            }
                            if (prevValue != null && dirInfoPair.equals(prevValue.parentPair())) {
                                return prevValue;
                            } else {
                                return TreeCompareInfo.calculate(
                                        dirInfoPair,
                                        Factory.dirsMatcher(ar.settings()),
                                        Factory.bookNamesMatcher(ar.settings()),
                                        Factory.sheetNamesMatcher(ar.settings()),
                                        ar.settings().get(SettingKeys.CURR_READ_PASSWORDS));
                            }
                        case COMPARE_SHEETS:
                        case COMPARE_BOOKS:
                        case COMPARE_DIRS:
                            return prevValue;
                        default:
                            throw new AssertionError();
                    }
                },
                menuProp,
                dirInfoPropPair.a(),
                dirInfoPropPair.b()));
    }
    
    /**
     * 設定エリアを表示するか否かを返します。<br>
     * 
     * @return 設定エリアを表示する場合は {@code true}
     */
    public BooleanExpression showSettings() {
        return row3Pane.showSettings();
    }
    
    /**
     * 比較処理を実行できる状態か否かを返します。<br>
     * 
     * @return 比較処理を実行できる状態の場合は {@code true}
     */
    public BooleanExpression isReady() {
        return isReady;
    }
    
    /**
     * 比較処理を実行中か否かを返します。<br>
     * 
     * @return 比較処理を実行中の場合は {@code true}
     */
    public BooleanExpression isRunning() {
        return isRunning;
    }
    
    // パスワード付きファイルの場合は解除され保存されていることの注意喚起を行う
    // FIXME: タスクの結果に応じて精緻に判定を行うように修正する
    private void alertPasswordUnlocked() {
        Map<Path, String> readPasswords = ar.settings().get(SettingKeys.CURR_READ_PASSWORDS);
        boolean passwordUsed = 0 < readPasswords.values().stream().filter(password -> password != null).count();
        if (passwordUsed) {
            new Alert(
                    AlertType.WARNING,
                    rb.getString("gui.MainController.020"),
                    ButtonType.OK)
                            .showAndWait();
        }
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
        
        AppMenu menu = ar.settings().get(SettingKeys.CURR_MENU);
        
        if (!menu.isValidTargets(ar.settings())) {
            new Alert(
                    AlertType.WARNING,
                    rb.getString("gui.MainController.010"),
                    ButtonType.OK)
                            .showAndWait();
            return;
        }
        
        isRunning.set(true);
        
        // FIXME: createWorkDirもTaskの中に入れるべき
        Path workDir = createWorkDir(ar.settings());
        if (workDir == null) {
            new Alert(
                    AlertType.WARNING,
                    rb.getString("gui.MainController.070"),
                    ButtonType.OK)
                            .showAndWait();
            
            isRunning.set(false);
            return;
        }
        
        Task<Report> task = menu.getTask(ar.settings());
        row3Pane.bind(task);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, event -> {
            executor.shutdown();
            row3Pane.unbind();
            
            alertPasswordUnlocked();
            
            if (ar.settings().get(SettingKeys.EXIT_WHEN_FINISHED)) {
                Platform.exit();
            } else {
                isRunning.set(false);
            }
        });
        
        task.addEventHandler(WorkerStateEvent.WORKER_STATE_FAILED, event -> {
            Throwable e = task.getException();
            e.printStackTrace();
            executor.shutdown();
            row3Pane.unbind();
            
            alertPasswordUnlocked();
            
            // エラーが発生したことを通知する
            if (e instanceof ApplicationException) {
                new Alert(
                        AlertType.WARNING,
                        "%s%n%s".formatted(
                                e.getClass().getName(),
                                e.getMessage()),
                        ButtonType.OK)
                                .showAndWait();
            } else {
                new Alert(
                        AlertType.WARNING,
                        "%s%n%s%n%s".formatted(
                                rb.getString("gui.MainController.030"),
                                e.getClass().getName(),
                                e.getMessage()),
                        ButtonType.OK)
                                .showAndWait();
            }
            
            isRunning.set(false);
        });
        
        executor.submit(task);
    }
    
    private Path createWorkDir(Settings settings) {
        final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS"));
        ar.changeSetting(SettingKeys.CURR_TIMESTAMP, timestamp);
        
        Path workDirBase = settings.get(SettingKeys.WORK_DIR_BASE);
        
        while (true) {
            try {
                Path workDir = workDirBase.resolve(timestamp);
                Files.createDirectories(workDir);
                ar.changeSetting(SettingKeys.WORK_DIR_BASE, workDirBase);
                return workDir;
                
            } catch (Exception e) {
                e.printStackTrace();
                
                new Alert(
                        AlertType.WARNING,
                        "%s%n%s%n%n%s".formatted(
                                rb.getString("gui.MainController.040"),
                                workDirBase,
                                rb.getString("gui.MainController.050")),
                        ButtonType.OK)
                                .showAndWait();
                
                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle(rb.getString("gui.MainController.060"));
                File newDir = dirChooser.showDialog(AppMain.stage);
                
                if (newDir != null) {
                    Path newPath = newDir.toPath();
                    if (!newPath.endsWith(AppMain.APP_DOMAIN)) {
                        newPath = newPath.resolve(AppMain.APP_DOMAIN);
                    }
                    workDirBase = newPath;
                    
                } else {
                    return null;
                }
            }
        }
    }
}
