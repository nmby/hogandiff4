package xyz.hotchpotch.hogandiff.gui.components;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.Factory;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.dialogs.PasswordDialog;
import xyz.hotchpotch.hogandiff.loaders.LoaderForBooks;
import xyz.hotchpotch.hogandiff.loaders.LoaderForDirs;
import xyz.hotchpotch.hogandiff.models.BookInfo;
import xyz.hotchpotch.hogandiff.models.DirInfo;
import xyz.hotchpotch.hogandiff.models.BookInfo.Status;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 比較対象ファイル／シート選択部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class TargetSelectionPane extends GridPane implements ChildController {

    // [static members] ********************************************************

    private static Path prevSelectedBookPath;

    private static boolean isDirOperation(AppMenu menu) {
        return menu == AppMenu.COMPARE_DIRS || menu == AppMenu.COMPARE_TREES;
    }

    // [instance members] ******************************************************

    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();

    @FXML
    private Label titleLabel;

    @FXML
    private Label dirPathLabel;

    @FXML
    private TextField dirPathTextField;

    @FXML
    private Button dirPathButton;

    @FXML
    private Label bookPathLabel;

    @FXML
    private TextField bookPathTextField;

    @FXML
    private Button bookPathButton;

    @FXML
    private Label sheetNameLabel;

    @FXML
    private ChoiceBox<String> sheetNameChoiceBox;

    private final BooleanProperty isReady = new SimpleBooleanProperty();
    private final BooleanProperty isBusy = new SimpleBooleanProperty();

    private TargetSelectionPane opposite;
    private MainController parent;
    private Side side;
    private Map<Path, String> readPasswords;

    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public TargetSelectionPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TargetSelectionPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }

    @Override
    public void init(MainController parent, Object... params) {
        Objects.requireNonNull(parent);

        this.parent = parent;
        this.side = (Side) params[0];
        opposite = (TargetSelectionPane) params[1];
        readPasswords = ar.settings().get(SettingKeys.CURR_READ_PASSWORDS);

        // 1.disableプロパティとvisibleプロパティのバインディング
        disableProperty().bind(parent.isRunning().or(isBusy));
        sheetNameLabel.disableProperty().bind(Bindings.createBooleanBinding(
                () -> parent.menuProp.getValue() != AppMenu.COMPARE_SHEETS,
                parent.menuProp));
        sheetNameChoiceBox.disableProperty().bind(Bindings.createBooleanBinding(
                () -> parent.menuProp.getValue() != AppMenu.COMPARE_SHEETS,
                parent.menuProp));

        dirPathLabel.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> isDirOperation(parent.menuProp.getValue()),
                parent.menuProp));
        dirPathTextField.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> isDirOperation(parent.menuProp.getValue()),
                parent.menuProp));
        dirPathButton.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> isDirOperation(parent.menuProp.getValue()),
                parent.menuProp));

        bookPathLabel.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> !isDirOperation(parent.menuProp.getValue()),
                parent.menuProp));
        bookPathTextField.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> !isDirOperation(parent.menuProp.getValue()),
                parent.menuProp));
        bookPathButton.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> !isDirOperation(parent.menuProp.getValue()),
                parent.menuProp));

        // 2.項目ごとの各種設定
        setOnDragOver(this::onDragOver);
        setOnDragDropped(this::onDragDropped);

        titleLabel.setText(side.name());

        dirPathTextField.textProperty().bind(Bindings.createStringBinding(
                () -> parent.dirInfoPropPair.get(side).getValue() != null
                        ? parent.dirInfoPropPair.get(side).getValue().dirPath().toString()
                        : null,
                parent.dirInfoPropPair.get(side)));
        dirPathButton.setOnAction(this::chooseDir);

        bookPathTextField.textProperty().bind(Bindings.createStringBinding(
                () -> parent.bookInfoPropPair.get(side).getValue() != null
                        ? parent.bookInfoPropPair.get(side).getValue().bookPath().toString()
                        : null,
                parent.bookInfoPropPair.get(side)));
        bookPathButton.setOnAction(this::chooseBook);

        parent.sheetNamePropPair.get(side).bind(sheetNameChoiceBox.valueProperty());

        isReady.bind(Bindings.createBooleanBinding(
                () -> switch (parent.menuProp.getValue()) {
                    case COMPARE_BOOKS -> parent.bookInfoPropPair.get(side).getValue() != null;
                    case COMPARE_SHEETS -> parent.bookInfoPropPair.get(side).getValue() != null
                            && parent.sheetNamePropPair.get(side).getValue() != null;
                    case COMPARE_DIRS -> parent.dirInfoPropPair.get(side).getValue() != null;
                    case COMPARE_TREES -> parent.dirInfoPropPair.get(side).getValue() != null;
                    default -> throw new AssertionError();
                },
                parent.menuProp,
                parent.dirInfoPropPair.get(side),
                parent.bookInfoPropPair.get(side),
                parent.sheetNamePropPair.get(side)));

        // 4.値変更時のイベントハンドラの設定
        // ※このコントローラだけ特殊なので3と4を入れ替える
        parent.menuProp.addListener((target, oldValue, newValue) -> {
            DirInfo dirInfo = parent.dirInfoPropPair.get(side).getValue();
            if (dirInfo != null
                    && (newValue == AppMenu.COMPARE_DIRS || newValue == AppMenu.COMPARE_TREES)
                    && (oldValue == AppMenu.COMPARE_DIRS || oldValue == AppMenu.COMPARE_TREES)) {
                setDirPath(dirInfo.dirPath(), newValue == AppMenu.COMPARE_TREES);
            }
        });
        parent.bookInfoPropPair.get(side).addListener((target, oldValue, newValue) -> {
            sheetNameChoiceBox.setItems(FXCollections.emptyObservableList());
            if (newValue != null && !newValue.sheetNames().isEmpty()) {
                sheetNameChoiceBox.setItems(FXCollections.observableList(newValue.sheetNames()));
            }
        });

        // 3.初期値の設定
        if (ar.settings().containsKey(SettingKeys.CURR_BOOK_INFOS.get(side))) {
            validateAndSetTarget(ar.settings().get(SettingKeys.CURR_BOOK_INFOS.get(side)).bookPath(), null);
        }
    }

    @Override
    public BooleanExpression isReady() {
        return isReady;
    }

    private void onDragOver(DragEvent event) {
        try {
            event.consume();
            Predicate<File> isAcceptableType = isDirOperation(parent.menuProp.getValue())
                    ? File::isDirectory
                    : File::isFile;

            if (!event.getDragboard().hasFiles()) {
                return;
            }
            File file = event.getDragboard().getFiles().get(0);
            if (!isAcceptableType.test(file)) {
                return;
            }
            // ファイルの拡張子は確認しないことにする。

            event.acceptTransferModes(TransferMode.LINK);

        } catch (RuntimeException e) {
            e.printStackTrace();
            // nop
        }
    }

    private void onDragDropped(DragEvent event) {
        try {
            isBusy.set(true);
            event.consume();

            AppMenu menu = parent.menuProp.getValue();
            Predicate<File> isAcceptableType = isDirOperation(menu)
                    ? File::isDirectory
                    : File::isFile;

            if (!event.getDragboard().hasFiles()) {
                event.setDropCompleted(false);
                return;
            }
            List<File> files = event.getDragboard().getFiles();
            if (!isAcceptableType.test(files.get(0))) {
                event.setDropCompleted(false);
                return;
            }

            if (isDirOperation(menu)) {
                setDirPath(files.get(0).toPath(), ar.settings().get(SettingKeys.COMPARE_DIRS_RECURSIVELY));
                event.setDropCompleted(true);

                if (1 < files.size() && isAcceptableType.test(files.get(1))) {
                    opposite.setDirPath(files.get(1).toPath(), ar.settings().get(SettingKeys.COMPARE_DIRS_RECURSIVELY));
                }

            } else {
                boolean dropCompleted = validateAndSetTarget(files.get(0).toPath(), null);
                event.setDropCompleted(dropCompleted);

                if (dropCompleted && 1 < files.size() && isAcceptableType.test(files.get(1))) {
                    opposite.validateAndSetTarget(files.get(1).toPath(), null);
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            event.setDropCompleted(false);
            // nop

        } finally {
            isBusy.set(false);
        }
    }

    private void chooseDir(ActionEvent event) {
        try {
            isBusy.set(true);

            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(rb.getString("gui.component.TargetSelectionPane.010"));

            DirInfo dirInfo = parent.dirInfoPropPair.get(side).getValue();
            if (dirInfo != null) {
                chooser.setInitialDirectory(dirInfo.dirPath().toFile());

            } else if (prevSelectedBookPath != null) {
                chooser.setInitialDirectory(prevSelectedBookPath.toFile().getParentFile());
            }

            File selected = chooser.showDialog(getScene().getWindow());

            if (selected != null) {
                setDirPath(selected.toPath(), ar.settings().get(SettingKeys.COMPARE_DIRS_RECURSIVELY));
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            // nop

        } finally {
            isBusy.set(false);
        }
    }

    private void chooseBook(ActionEvent event) {
        try {
            isBusy.set(true);

            FileChooser chooser = new FileChooser();
            chooser.setTitle(rb.getString("gui.component.TargetSelectionPane.020"));

            BookInfo bookInfo = parent.bookInfoPropPair.get(side).getValue();
            if (bookInfo != null) {
                File book = bookInfo.bookPath().toFile();
                chooser.setInitialDirectory(book.getParentFile());
                chooser.setInitialFileName(book.getName());

            } else if (prevSelectedBookPath != null) {
                chooser.setInitialDirectory(prevSelectedBookPath.toFile().getParentFile());
            }

            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                    rb.getString("gui.component.TargetSelectionPane.030"),
                    "*.xls", "*.xlsx", "*.xlsm"));

            File selected = chooser.showOpenDialog(getScene().getWindow());

            if (selected != null) {
                validateAndSetTarget(selected.toPath(), null);
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
            // nop

        } finally {
            isBusy.set(false);
        }
    }

    private void setDirPath(Path newDirPath, boolean recursively) {
        if (newDirPath == null) {
            parent.dirInfoPropPair.get(side).setValue(null);
            return;
        }

        try {
            LoaderForDirs dirLoader = Factory.dirLoader(
                    ar.settings().getAltered(SettingKeys.COMPARE_DIRS_RECURSIVELY, recursively));
            DirInfo newDirInfo = dirLoader.loadDirInfo(newDirPath);
            parent.dirInfoPropPair.get(side).setValue(newDirInfo);
            prevSelectedBookPath = newDirPath;

        } catch (Exception e) {
            e.printStackTrace();
            parent.dirInfoPropPair.get(side).setValue(null);
            new Alert(
                    AlertType.ERROR,
                    "%s%n%s".formatted(
                            rb.getString("gui.component.TargetSelectionPane.060"),
                            newDirPath),
                    ButtonType.OK)
                    .showAndWait();
            return;
        }

    }

    private boolean validateAndSetTarget(Path newBookPath, String sheetName) {
        if (newBookPath == null) {
            parent.bookInfoPropPair.get(side).setValue(null);
            return true;
        }

        BookInfo newBookInfo = readBookInfo(newBookPath);

        if (newBookInfo.status() == Status.LOAD_COMPLETED) {
            parent.bookInfoPropPair.get(side).setValue(newBookInfo);
            prevSelectedBookPath = newBookPath;

        } else {
            parent.bookInfoPropPair.get(side).setValue(null);
            readPasswords.remove(newBookPath);
            new Alert(
                    AlertType.ERROR,
                    "%s%n%s".formatted(
                            rb.getString("gui.component.TargetSelectionPane.040"),
                            newBookPath),
                    ButtonType.OK)
                    .showAndWait();
            return false;
        }

        if (sheetName == null) {
            sheetNameChoiceBox.setValue(null);

        } else if (sheetNameChoiceBox.getItems().contains(sheetName)) {
            sheetNameChoiceBox.setValue(sheetName);

        } else {
            sheetNameChoiceBox.setValue(null);
            new Alert(
                    AlertType.ERROR,
                    "%s%n%s".formatted(
                            rb.getString("gui.component.TargetSelectionPane.050"),
                            sheetName),
                    ButtonType.OK)
                    .showAndWait();
            return false;
        }
        return true;
    }

    private BookInfo readBookInfo(Path newBookPath) {
        assert newBookPath != null;

        try {
            String readPassword = readPasswords.get(newBookPath);
            LoaderForBooks loader = Factory.bookLoader(newBookPath);

            while (true) {
                BookInfo bookInfo = loader.loadBookInfo(newBookPath, readPassword);

                switch (bookInfo.status()) {
                    case LOAD_COMPLETED:
                        readPasswords.put(newBookPath, readPassword);
                        return bookInfo;

                    case LOAD_FAILED:
                        return bookInfo;

                    case NEEDS_PASSWORD:
                        PasswordDialog dialog = new PasswordDialog(newBookPath, readPassword);
                        Optional<String> newPassword = dialog.showAndWait();
                        if (!newPassword.isPresent()) {
                            return bookInfo;
                        }
                        readPassword = newPassword.get();
                }
            }
        } catch (Exception e) {
            return BookInfo.ofLoadFailed(newBookPath);
        }
    }
}
