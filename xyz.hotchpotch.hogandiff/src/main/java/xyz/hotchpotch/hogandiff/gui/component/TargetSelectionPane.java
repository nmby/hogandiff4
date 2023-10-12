package xyz.hotchpotch.hogandiff.gui.component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.PasswordHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.PasswordDialog;
import xyz.hotchpotch.hogandiff.gui.component.TargetsPane.Side;

/**
 * 比較対象ファイル／シート選択部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class TargetSelectionPane extends GridPane implements ChildController {
    
    // [static members] ********************************************************
    
    private static Path prevSelectedBookPath;
    
    private static boolean isTargetDirs(AppMenu menu) {
        return menu == AppMenu.COMPARE_DIRS || menu == AppMenu.COMPARE_TREES;
    }
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource();
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
    
    private final Property<Path> dirPath = new SimpleObjectProperty<>();
    private final Property<BookOpenInfo> bookOpenInfo = new SimpleObjectProperty<>();
    private final StringProperty sheetName = new SimpleStringProperty();
    private final BooleanProperty isReady = new SimpleBooleanProperty();
    private final BooleanProperty isBusy = new SimpleBooleanProperty();
    
    private final Factory factory = Factory.of();
    private TargetSelectionPane opposite;
    private MainController parent;
    
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
        Objects.requireNonNull(parent, "parent");
        
        Side side = (Side) params[0];
        opposite = (TargetSelectionPane) params[1];
        this.parent = parent;
        
        // 1.disableプロパティとvisibleプロパティのバインディング
        disableProperty().bind(parent.isRunning().or(isBusy));
        sheetNameLabel.disableProperty().bind(Bindings.createBooleanBinding(
                () -> parent.menu().getValue() != AppMenu.COMPARE_SHEETS,
                parent.menu()));
        sheetNameChoiceBox.disableProperty().bind(Bindings.createBooleanBinding(
                () -> parent.menu().getValue() != AppMenu.COMPARE_SHEETS,
                parent.menu()));
        
        dirPathLabel.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> isTargetDirs(parent.menu().getValue()),
                parent.menu()));
        dirPathTextField.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> isTargetDirs(parent.menu().getValue()),
                parent.menu()));
        dirPathButton.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> isTargetDirs(parent.menu().getValue()),
                parent.menu()));
        
        bookPathLabel.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> !isTargetDirs(parent.menu().getValue()),
                parent.menu()));
        bookPathTextField.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> !isTargetDirs(parent.menu().getValue()),
                parent.menu()));
        bookPathButton.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> !isTargetDirs(parent.menu().getValue()),
                parent.menu()));
        
        // 2.項目ごとの各種設定
        setOnDragOver(this::onDragOver);
        setOnDragDropped(this::onDragDropped);
        
        titleLabel.setText(side.title);
        
        dirPathTextField.textProperty().bind(Bindings.createStringBinding(
                () -> dirPath.getValue() == null ? null : dirPath.getValue().toString(),
                dirPath));
        dirPathButton.setOnAction(this::chooseDir);
        
        bookPathTextField.textProperty().bind(Bindings.createStringBinding(
                () -> bookOpenInfo.getValue() == null ? null : bookOpenInfo.getValue().bookPath().toString(),
                bookOpenInfo));
        bookPathButton.setOnAction(this::chooseBook);
        
        sheetName.bind(sheetNameChoiceBox.valueProperty());
        
        isReady.bind(Bindings.createBooleanBinding(
                () -> switch (parent.menu().getValue()) {
                    case COMPARE_BOOKS -> bookOpenInfo.getValue() != null;
                    case COMPARE_SHEETS -> bookOpenInfo.getValue() != null && sheetName.getValue() != null;
                    case COMPARE_DIRS -> dirPath.getValue() != null;
                    case COMPARE_TREES -> dirPath.getValue() != null;
                    default -> throw new AssertionError("unknown menu");
                },
                parent.menu(), bookOpenInfo, sheetName, dirPath));
        
        // 4.値変更時のイベントハンドラの設定
        // ※このコントローラだけ特殊なので3と4を入れ替える
        dirPath.addListener((target, oldValue, newValue) -> ar.changeSetting(side.dirPathKey, newValue));
        bookOpenInfo.addListener((target, oldValue, newValue) -> ar.changeSetting(side.bookOpenInfoKey, newValue));
        sheetName.addListener((target, oldValue, newValue) -> ar.changeSetting(side.sheetNameKey, newValue));
        
        // 3.初期値の設定
        if (ar.settings().containsKey(side.dirPathKey)) {
            setDirPath(ar.settings().get(side.dirPathKey));
        }
        if (ar.settings().containsKey(side.bookOpenInfoKey)) {
            validateAndSetTarget(
                    ar.settings().get(side.bookOpenInfoKey).bookPath(),
                    ar.settings().containsKey(side.sheetNameKey)
                            ? ar.settings().get(side.sheetNameKey)
                            : null);
        }
    }
    
    @Override
    public BooleanExpression isReady() {
        return isReady;
    }
    
    private void onDragOver(DragEvent event) {
        event.consume();
        Predicate<File> isAcceptableType = isTargetDirs(parent.menu().getValue())
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
    }
    
    private void onDragDropped(DragEvent event) {
        try {
            isBusy.set(true);
            event.consume();
            
            AppMenu menu = parent.menu().getValue();
            Predicate<File> isAcceptableType = isTargetDirs(menu)
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
            
            if (isTargetDirs(menu)) {
                setDirPath(files.get(0).toPath());
                event.setDropCompleted(true);
                
                if (1 < files.size() && isAcceptableType.test(files.get(1))) {
                    opposite.setDirPath(files.get(1).toPath());
                }
                
            } else {
                boolean dropCompleted = validateAndSetTarget(files.get(0).toPath(), null);
                event.setDropCompleted(dropCompleted);
                
                if (dropCompleted && 1 < files.size() && isAcceptableType.test(files.get(1))) {
                    opposite.validateAndSetTarget(files.get(1).toPath(), null);
                }
            }
        } finally {
            isBusy.set(false);
        }
    }
    
    private void chooseDir(ActionEvent event) {
        try {
            isBusy.set(true);
            
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(rb.getString("gui.component.TargetSelectionPane.010"));
            
            if (dirPath.getValue() != null) {
                chooser.setInitialDirectory(dirPath.getValue().toFile());
                
            } else if (prevSelectedBookPath != null) {
                chooser.setInitialDirectory(prevSelectedBookPath.toFile().getParentFile());
            }
            
            File selected = chooser.showDialog(getScene().getWindow());
            
            if (selected != null) {
                setDirPath(selected.toPath());
            }
        } finally {
            isBusy.set(false);
        }
    }
    
    private void chooseBook(ActionEvent event) {
        try {
            isBusy.set(true);
            
            FileChooser chooser = new FileChooser();
            chooser.setTitle(rb.getString("gui.component.TargetSelectionPane.020"));
            
            if (bookOpenInfo.getValue() != null) {
                File book = bookOpenInfo.getValue().bookPath().toFile();
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
        } finally {
            isBusy.set(false);
        }
    }
    
    private void setDirPath(Path newDirPath) {
        dirPath.setValue(newDirPath);
        if (newDirPath != null) {
            prevSelectedBookPath = newDirPath;
        }
    }
    
    private boolean validateAndSetTarget(Path newBookPath, String sheetName) {
        if (newBookPath == null) {
            bookOpenInfo.setValue(null);
            sheetNameChoiceBox.setItems(FXCollections.emptyObservableList());
            return true;
        }
        
        try {
            BookInfo bookInfo = null;
            BookOpenInfo newBookOpenInfo = new BookOpenInfo(newBookPath, null);
            
            while (true) {
                // パスワードの有無でローダーを切り替える可能性があるため、この位置で取得する。
                SheetNamesLoader loader = factory.sheetNamesLoader(newBookOpenInfo);
                
                try {
                    bookInfo = loader.loadSheetNames(newBookOpenInfo);
                    break;
                    
                } catch (PasswordHandlingException e) {
                    PasswordDialog dialog = new PasswordDialog(newBookOpenInfo);
                    Optional<String> newPassword = dialog.showAndWait();
                    if (newPassword.isPresent()) {
                        newBookOpenInfo = newBookOpenInfo.withReadPassword(newPassword.get());
                    } else {
                        throw e;
                    }
                }
            }
            
            bookOpenInfo.setValue(newBookOpenInfo);
            sheetNameChoiceBox.setItems(FXCollections.observableList(bookInfo.sheetNames()));
            prevSelectedBookPath = newBookPath;
            
        } catch (Exception e) {
            e.printStackTrace();
            bookOpenInfo.setValue(null);
            sheetNameChoiceBox.setItems(FXCollections.emptyObservableList());
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
}
