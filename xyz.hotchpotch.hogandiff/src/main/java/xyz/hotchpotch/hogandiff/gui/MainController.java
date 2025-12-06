package xyz.hotchpotch.hogandiff.gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.ApplicationException;
import xyz.hotchpotch.hogandiff.CompareMenu;
import xyz.hotchpotch.hogandiff.ErrorReporter;
import xyz.hotchpotch.hogandiff.Msg;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.layouts.Row1Pane;
import xyz.hotchpotch.hogandiff.gui.layouts.Row2Pane;
import xyz.hotchpotch.hogandiff.gui.layouts.Row3Pane;
import xyz.hotchpotch.hogandiff.gui.layouts.Row4Pane;
import xyz.hotchpotch.hogandiff.logic.BookInfo;
import xyz.hotchpotch.hogandiff.logic.DirInfo;
import xyz.hotchpotch.hogandiff.logic.Factory;
import xyz.hotchpotch.hogandiff.logic.PairingInfoBooks;
import xyz.hotchpotch.hogandiff.logic.PairingInfoDirs;
import xyz.hotchpotch.hogandiff.logic.google.GoogleCredential;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Triple;
import xyz.hotchpotch.hogandiff.util.Triple.Side3;

/**
 * このアプリケーションのコントローラです。<br>
 *
 * @author nmby
 */
public class MainController extends VBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    
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
    
    /** 現在選択されている比較メニュー */
    public final Property<CompareMenu> propCompareMenu = new SimpleObjectProperty<>();
    
    // TODO: そろそろ根本的に作り直したいところ...
    
    /** シート名のトリプル */
    public final Triple<StringProperty> sheetNamePropTriple = new Triple<>(
            new SimpleStringProperty(),
            new SimpleStringProperty(),
            new SimpleStringProperty());
    
    /** Excelブック情報のトリプル */
    public final Triple<Property<BookInfo>> bookInfoPropTriple = new Triple<>(
            new SimpleObjectProperty<>(),
            new SimpleObjectProperty<>(),
            new SimpleObjectProperty<>());
    
    /** フォルダ情報のトリプル */
    public final Triple<Property<DirInfo>> dirInfoPropTriple = new Triple<>(
            new SimpleObjectProperty<>(),
            new SimpleObjectProperty<>(),
            new SimpleObjectProperty<>());
    
    /** Googleアカウント資格情報 */
    public final Property<GoogleCredential> propGoogleCredential = new SimpleObjectProperty<>();
    
    /** 設定エリアを表示するか */
    public final BooleanProperty propShowSettings = new SimpleBooleanProperty();
    
    /** メインウィンドウの最小高さ */
    public final SimpleDoubleProperty propMinHeight = new SimpleDoubleProperty();
    
    private Task<Void> currentTask = null;
    
    /**
     * このコントローラオブジェクトを初期化します。<br>
     */
    public void initialize() {
        
        // 1.disableプロパティのバインディング
        
        // 2.項目ごとの各種設定
        row1Pane.init(this);
        row2Pane.init(this);
        row3Pane.init(this);
        row4Pane.init(this);
        
        isReady.bind(row1Pane.isReady()
                .and(row2Pane.isReady())
                .and(row3Pane.isReady())
                .and(row4Pane.isReady()));
        
        propCompareMenu.addListener((_, _, _) -> updateActiveComparison(Side3.O));
        sheetNamePropTriple.forEach((prop, side3) -> prop.addListener((_, _, _) -> updateActiveComparison(side3)));
        bookInfoPropTriple.forEach((prop, side3) -> prop.addListener((_, _, _) -> updateActiveComparison(side3)));
        dirInfoPropTriple.forEach((prop, side3) -> prop.addListener((_, _, _) -> updateActiveComparison(side3)));
        
        propMinHeight.bind(Bindings.createDoubleBinding(
                () -> 241d
                        + (propCompareMenu.getValue().compareWay() == CompareMenu.CompareWay.THREE_WAY ? 65d : 0d)
                        + (propShowSettings.get() ? 186d : 0d),
                propCompareMenu, propShowSettings));
        
        propMinHeight.addListener((_, oldValue, newValue) -> {
            AppMain.stage.setHeight(AppMain.stage.getHeight() + newValue.doubleValue() - oldValue.doubleValue());
            AppMain.stage.setMinHeight(newValue.doubleValue());
        });
        
        // 3.初期値の設定
        
        // 4.値変更時のイベントハンドラの設定
        
        // 5.その他
        UpdateChecker.execute(false);
    }
    
    /**
     * 現在選択されている比較メニューに応じて、対応する比較情報を更新します。<br>
     * 
     * @param side3
     *            更新された側
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public void updateActiveComparison(Side3 side3) {
        Objects.requireNonNull(side3);
        
        try {
            CompareMenu menu = propCompareMenu.getValue();
            
            switch (menu.compareWay()) {
            case TWO_WAY:
                switch (menu.compareObject()) {
                case COMPARE_SHEETS -> updateSheetComparison2(Side3.O);
                case COMPARE_BOOKS -> updateBookComparison2(Side3.O);
                case COMPARE_DIRS -> updateDirComparison2(Side3.O);
                case COMPARE_TREES -> updateTreeComparison2(Side3.O);
                default -> throw new AssertionError("Unreachable code: " + menu.compareObject());
                }
                break;
            
            case THREE_WAY:
                Consumer<Side3> action = switch (menu.compareObject()) {
                case COMPARE_SHEETS -> this::updateSheetComparison2;
                case COMPARE_BOOKS -> this::updateBookComparison2;
                case COMPARE_DIRS -> this::updateDirComparison2;
                case COMPARE_TREES -> this::updateTreeComparison2;
                default -> throw new AssertionError("Unreachable code: " + menu.compareObject());
                };
                if (side3 == Side3.O) {
                    action.accept(Side3.A);
                    action.accept(Side3.B);
                } else {
                    action.accept(side3);
                }
                break;
            
            default:
                throw new AssertionError("Unreachable code: " + menu.compareWay());
            }
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "MainController#updateActiveComparison-1");
            throw e;
        }
    }
    
    private void updateSheetComparison2(Side3 side3) {
        Pair<BookInfo> bookInfoPair = bookInfoPropTriple.map(Property::getValue).toPair(side3);
        Pair<String> sheetNamePair = sheetNamePropTriple.map(Property::getValue).toPair(side3);
        
        ar.changeSetting(SettingKeys.CURR_SHEET_COMPARE_INFOS.get(side3),
                bookInfoPair.isPaired() && sheetNamePair.isPaired()
                        ? new PairingInfoBooks(bookInfoPair, List.of(sheetNamePair))
                        : null);
    }
    
    private void updateBookComparison2(Side3 side3) {
        Pair<BookInfo> bookInfoPair = bookInfoPropTriple.map(Property::getValue).toPair(side3);
        
        ar.changeSetting(SettingKeys.CURR_BOOK_COMPARE_INFOS.get(side3),
                bookInfoPair.isPaired()
                        ? PairingInfoBooks.calculate(
                                bookInfoPair,
                                Factory.sheetNamesMatcher(ar.settings()))
                        : null);
    }
    
    private void updateDirComparison2(Side3 side3) {
        Pair<DirInfo> dirInfoPair = dirInfoPropTriple.map(Property::getValue).toPair(side3);
        
        ar.changeSetting(SettingKeys.CURR_DIR_COMPARE_INFOS.get(side3),
                dirInfoPair.isPaired()
                        ? PairingInfoDirs.calculate(
                                dirInfoPair,
                                Factory.dirInfosMatcher(ar.settings()),
                                Factory.bookInfosMatcher(ar.settings()),
                                Factory.sheetNamesMatcher(ar.settings()),
                                ar.settings().get(SettingKeys.CURR_READ_PASSWORDS))
                        : null);
    }
    
    private void updateTreeComparison2(Side3 side3) {
        Pair<DirInfo> dirInfoPair = dirInfoPropTriple.map(Property::getValue).toPair(side3);
        
        ar.changeSetting(SettingKeys.CURR_TREE_COMPARE_INFOS.get(side3),
                dirInfoPair.isPaired()
                        ? PairingInfoDirs.calculate(
                                dirInfoPair,
                                Factory.dirInfosMatcher(ar.settings()),
                                Factory.bookInfosMatcher(ar.settings()),
                                Factory.sheetNamesMatcher(ar.settings()),
                                ar.settings().get(SettingKeys.CURR_READ_PASSWORDS))
                        : null);
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
    private void alertPasswordUnlocked() {
        if (isPasswordUsed()) {
            new Alert(
                    AlertType.WARNING,
                    Msg.APP_1190.get(),
                    ButtonType.OK)
                            .showAndWait();
        }
    }
    
    private boolean isPasswordUsed() {
        try {
            CompareMenu menu = ar.settings().get(SettingKeys.CURR_MENU);
            Map<Path, String> readPasswords = ar.settings().get(
                    SettingKeys.CURR_READ_PASSWORDS);
            
            switch (menu.compareObject()) {
            case COMPARE_SHEETS, COMPARE_BOOKS:
                Triple<Path> bookPathTriple = bookInfoPropTriple.map(Property::getValue).map(BookInfo::bookPath);
                return readPasswords.get(bookPathTriple.a()) != null
                        || readPasswords.get(bookPathTriple.b()) != null
                        || (menu.compareWay() == CompareMenu.CompareWay.THREE_WAY
                                && readPasswords.get(bookPathTriple.o()) != null);
            
            case COMPARE_DIRS, COMPARE_TREES:
                Triple<DirInfo> dirInfoTriple = dirInfoPropTriple.map(Property::getValue);
                return isPasswordUsed(dirInfoTriple.a())
                        || isPasswordUsed(dirInfoTriple.b())
                        || (menu.compareWay() == CompareMenu.CompareWay.THREE_WAY
                                && isPasswordUsed(dirInfoTriple.o()));
            
            default:
                throw new AssertionError("Unreachable code: " + menu.compareObject());
            }
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "MainController#isPasswordUsed-1");
            throw e;
        }
    }
    
    private boolean isPasswordUsed(DirInfo dirInfo) {
        Map<Path, String> readPasswords = ar.settings().get(
                SettingKeys.CURR_READ_PASSWORDS);
        
        boolean isUsed = dirInfo.childBookInfos().stream()
                .map(BookInfo::bookPath)
                .anyMatch(readPasswords::containsKey);
        
        if (isUsed) {
            return true;
        }
        
        return dirInfo.childDirInfos().stream()
                .anyMatch(this::isPasswordUsed);
    }
    
    /**
     * 比較処理を実行します。<br>
     * 
     * @throws IllegalStateException
     *             必要な設定がなされておらず実行できない場合
     */
    public void execute() {
        CompareMenu compareMenu = ar.settings().get(SettingKeys.CURR_MENU);
        
        if (!compareMenu.isValidTargets(ar.settings())) {
            new Alert(
                    AlertType.WARNING,
                    Msg.APP_1180.get(),
                    ButtonType.OK)
                            .showAndWait();
            return;
        }
        
        isRunning.set(true);
        
        Path workDir = createWorkDir(ar.settings());
        if (workDir == null) {
            new Alert(
                    AlertType.WARNING,
                    Msg.APP_1240.get(),
                    ButtonType.OK)
                            .showAndWait();
            
            isRunning.set(false);
            return;
        }
        
        currentTask = compareMenu.getTask(ar.settings());
        row3Pane.bind(currentTask);
        
        currentTask.setOnSucceeded(_ -> {
            row3Pane.unbind();
            
            // パスワード付きファイルの場合は解除され保存されていることの注意喚起を行う
            alertPasswordUnlocked();
            
            if (ar.settings().get(SettingKeys.EXIT_WHEN_FINISHED)) {
                Platform.exit();
            } else {
                isRunning.set(false);
            }
        });
        
        currentTask.setOnFailed(_ -> {
            Throwable e = currentTask.getException();
            e.printStackTrace();
            row3Pane.unbind();
            
            // エラー送信ONの場合はエラー情報を送信する
            ErrorReporter.reportIfEnabled(e, "MainContoroller#execute");
            
            // パスワード付きファイルの場合は解除され保存されていることの注意喚起を行う
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
                                Msg.APP_0150.get(),
                                e.getClass().getName(),
                                e.getMessage()),
                        ButtonType.OK)
                                .showAndWait();
            }
            
            isRunning.set(false);
        });
        
        taskExecutor.submit(currentTask);
    }
    
    private final ExecutorService taskExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ComparisonTask");
        t.setDaemon(true);
        return t;
    });
    
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
                        "%s%n%s%n%n%s".formatted(Msg.APP_1210.get(), workDirBase, Msg.APP_1220.get()),
                        ButtonType.OK)
                                .showAndWait();
                
                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle(Msg.APP_1060.get());
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
