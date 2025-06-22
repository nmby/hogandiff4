package xyz.hotchpotch.hogandiff.gui;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
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
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.ApplicationException;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.layouts.Row1Pane;
import xyz.hotchpotch.hogandiff.gui.layouts.Row2Pane;
import xyz.hotchpotch.hogandiff.gui.layouts.Row3Pane;
import xyz.hotchpotch.hogandiff.gui.layouts.Row4Pane;
import xyz.hotchpotch.hogandiff.task.BookInfo;
import xyz.hotchpotch.hogandiff.task.DirInfo;
import xyz.hotchpotch.hogandiff.task.Factory;
import xyz.hotchpotch.hogandiff.task.PairingInfoBooks;
import xyz.hotchpotch.hogandiff.task.PairingInfoDirs;
import xyz.hotchpotch.hogandiff.task.PairingInfoDirs.PairingInfoDirsFlatten;
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
                AppMain.stage.setHeight(AppMain.stage.getHeight() + row4Pane.originalHeight());
                AppMain.stage.setMinHeight(AppMain.STAGE_HEIGHT_OPEN);
            } else {
                AppMain.stage.setHeight(AppMain.stage.getHeight() - row4Pane.originalHeight());
                AppMain.stage.setMinHeight(AppMain.STAGE_HEIGHT_CLOSE);
                row4Pane.setVisible2(false);
            }
        });

        menuProp.addListener((target, oldVal, newVal) -> updateActiveComparison());
        sheetNamePropPair.a().addListener((target, oldVal, newVal) -> updateActiveComparison());
        sheetNamePropPair.b().addListener((target, oldVal, newVal) -> updateActiveComparison());
        bookInfoPropPair.a().addListener((target, oldVal, newVal) -> updateActiveComparison());
        bookInfoPropPair.b().addListener((target, oldVal, newVal) -> updateActiveComparison());
        dirInfoPropPair.a().addListener((target, oldVal, newVal) -> updateActiveComparison());
        dirInfoPropPair.b().addListener((target, oldVal, newVal) -> updateActiveComparison());

        // 3.初期値の設定
        row4Pane.setVisible2(row3Pane.showSettings().getValue());

        // 4.値変更時のイベントハンドラの設定
        // nop
    }

    public void updateActiveComparison() {
        switch (menuProp.getValue()) {
            case COMPARE_SHEETS -> updateSheetComparison();
            case COMPARE_BOOKS -> updateBookComparison();
            case COMPARE_DIRS -> updateDirComparison();
            case COMPARE_TREES -> updateTreeComparison();
            default -> throw new AssertionError();
        }
    }

    private void updateSheetComparison() {
        Pair<BookInfo> bookInfoPair = bookInfoPropPair.map(Property::getValue);
        Pair<String> sheetNamePair = sheetNamePropPair.map(Property::getValue);
        ar.changeSetting(SettingKeys.CURR_SHEET_COMPARE_INFO,
                bookInfoPair.isPaired() && sheetNamePair.isPaired()
                        ? new PairingInfoBooks(bookInfoPair, List.of(sheetNamePair))
                        : null);
    }

    private void updateBookComparison() {
        Pair<BookInfo> bookInfoPair = bookInfoPropPair.map(Property::getValue);
        ar.changeSetting(SettingKeys.CURR_BOOK_COMPARE_INFO,
                bookInfoPair.isPaired()
                        ? PairingInfoBooks.calculate(
                                bookInfoPair,
                                Factory.sheetNamesMatcher(ar.settings()))
                        : null);
    }

    private void updateDirComparison() {
        Pair<DirInfo> dirInfoPair = dirInfoPropPair.map(Property::getValue);
        ar.changeSetting(SettingKeys.CURR_DIR_COMPARE_INFO,
                dirInfoPair.isPaired()
                        ? PairingInfoDirs.calculate(
                                dirInfoPair,
                                Factory.dirInfosMatcher(ar.settings()),
                                Factory.bookInfosMatcher(ar.settings()),
                                Factory.sheetNamesMatcher(ar.settings()),
                                ar.settings().get(SettingKeys.CURR_READ_PASSWORDS))
                        : null);
    }

    private void updateTreeComparison() {
        Pair<DirInfo> dirInfoPair = dirInfoPropPair.map(Property::getValue);
        ar.changeSetting(SettingKeys.CURR_TREE_COMPARE_INFO,
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
    private void alertPasswordUnlocked() {
        if (isPasswordUsed()) {
            new Alert(
                    AlertType.WARNING,
                    rb.getString("gui.MainController.020"),
                    ButtonType.OK)
                    .showAndWait();
        }
    }

    private boolean isPasswordUsed() {
        AppMenu menu = ar.settings().get(SettingKeys.CURR_MENU);

        Stream<Path> bookPathStream = switch (menu) {
            case COMPARE_SHEETS -> {
                PairingInfoBooks bookComparison = ar.settings().get(SettingKeys.CURR_SHEET_COMPARE_INFO);
                Pair<Path> bookPathPair = bookComparison.parentBookInfoPair().map(BookInfo::bookPath);
                yield bookPathPair.isIdentical()
                        ? Stream.of(bookPathPair.a())
                        : Stream.of(bookPathPair.a(), bookPathPair.b());
            }
            case COMPARE_BOOKS -> {
                PairingInfoBooks bookComparison = ar.settings().get(SettingKeys.CURR_BOOK_COMPARE_INFO);
                Pair<Path> bookPathPair = bookComparison.parentBookInfoPair().map(BookInfo::bookPath);
                yield Stream.of(bookPathPair.a(), bookPathPair.b()).filter(bookPath -> bookPath != null);
            }
            case COMPARE_DIRS -> {
                PairingInfoDirs dirComparison = ar.settings().get(SettingKeys.CURR_DIR_COMPARE_INFO);
                yield bookPathStream(dirComparison);
            }
            case COMPARE_TREES -> {
                PairingInfoDirsFlatten flattenDirComparison = ar.settings()
                        .get(SettingKeys.CURR_TREE_COMPARE_INFO)
                        .flatten();
                yield flattenDirComparison.dirComparisons().values().stream()
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .flatMap(this::bookPathStream);
            }
        };

        Map<Path, String> readPasswords = ar.settings().get(SettingKeys.CURR_READ_PASSWORDS);
        return bookPathStream.map(readPasswords::get).anyMatch(pw -> pw != null);
    }

    private Stream<Path> bookPathStream(PairingInfoDirs dirComparison) {
        return dirComparison.childBookInfoPairs().stream()
                .flatMap(bookInfoPair -> Stream.of(bookInfoPair.a(), bookInfoPair.b()))
                .filter(bookInfo -> bookInfo != null)
                .map(BookInfo::bookPath);
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

        // FIXME: [No.X 内部実装改善] createWorkDirもTaskの中に入れるべき
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

        Task<Void> task = menu.getTask(ar.settings());
        row3Pane.bind(task);

        task.setOnSucceeded(event -> {
            row3Pane.unbind();

            alertPasswordUnlocked();

            if (ar.settings().get(SettingKeys.EXIT_WHEN_FINISHED)) {
                Platform.exit();
            } else {
                isRunning.set(false);
            }
        });

        task.setOnFailed(event -> {
            Throwable e = task.getException();
            e.printStackTrace();
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

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
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
