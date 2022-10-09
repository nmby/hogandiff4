package xyz.hotchpotch.hogandiff.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
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
import javafx.stage.FileChooser;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppMenu;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.PasswordHandlingException;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * ブック・シート選択部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class TargetSelectionParts extends GridPane {
    
    // [static members] ********************************************************
    
    private static Path prevSelectedBookPath;
    
    // [instance members] ******************************************************
    
    @FXML
    private GridPane basePane;
    
    @FXML
    private Label titleLabel;
    
    @FXML
    private TextField bookPathTextField;
    
    @FXML
    private Button bookPathButton;
    
    @FXML
    private Label sheetNameLabel;
    
    @FXML
    private ChoiceBox<String> sheetNameChoiceBox;
    
    /*package*/ final BooleanProperty isReady = new SimpleBooleanProperty();
    
    private final Property<BookInfo> bookInfo = new SimpleObjectProperty<>();
    private final StringProperty sheetName = new SimpleStringProperty();
    
    private Factory factory;
    private ReadOnlyProperty<AppMenu> menu;
    private TargetSelectionParts opposite;
    
    public TargetSelectionParts() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("TargetSelectionParts.fxml"),
                AppMain.appResource.get());
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /*package*/ void init(
            MainController parent,
            String title,
            TargetSelectionParts opposite) {
        
        assert parent != null;
        assert title != null;
        assert opposite != null && opposite != this;
        
        factory = parent.factory;
        menu = parent.menu;
        this.opposite = opposite;
        
        titleLabel.setText(title);
        basePane.setOnDragOver(this::onDragOver);
        basePane.setOnDragDropped(this::onDragDropped);
        bookPathButton.setOnAction(this::chooseBook);
        sheetNameLabel.disableProperty().bind(Bindings.createBooleanBinding(
                () -> menu.getValue() == AppMenu.COMPARE_BOOKS,
                menu));
        sheetNameChoiceBox.disableProperty().bind(Bindings.createBooleanBinding(
                () -> menu.getValue() == AppMenu.COMPARE_BOOKS,
                menu));
        
        bookPathTextField.textProperty().bind(Bindings.createStringBinding(
                () -> bookInfo.getValue() == null ? null : bookInfo.getValue().bookPath().toString(),
                bookInfo));
        sheetName.bind(sheetNameChoiceBox.valueProperty());
        isReady.bind(Bindings.createBooleanBinding(
                () -> bookInfo.getValue() != null
                        && (sheetName.getValue() != null || menu.getValue() == AppMenu.COMPARE_BOOKS),
                bookInfo, sheetName, menu));
    }
    
    /*package*/ void applySettings(
            Settings settings,
            Key<BookInfo> keyBookInfo,
            Key<String> keySheetName) {
        
        assert settings != null;
        assert keyBookInfo != null;
        assert keySheetName != null;
        
        if (settings.containsKey(keyBookInfo)) {
            validateAndSetTarget(
                    settings.get(keyBookInfo).bookPath(),
                    settings.containsKey(keySheetName)
                            ? settings.get(keySheetName)
                            : null);
        }
    }
    
    /*package*/ void gatherSettings(
            Settings.Builder builder,
            Key<BookInfo> keyBookInfo,
            Key<String> keySheetName) {
        
        assert builder != null;
        assert keyBookInfo != null;
        assert keySheetName != null;
        
        if (bookInfo.getValue() != null) {
            builder.set(keyBookInfo, bookInfo.getValue());
        }
        if (menu.getValue() == AppMenu.COMPARE_SHEETS && sheetName.getValue() != null) {
            builder.set(keySheetName, sheetName.getValue());
        }
    }
    
    private void onDragOver(DragEvent event) {
        event.consume();
        
        if (!event.getDragboard().hasFiles()) {
            return;
        }
        File file = event.getDragboard().getFiles().get(0);
        if (!file.isFile()) {
            return;
        }
        // ファイルの拡張子は確認しないことにする。
        
        event.acceptTransferModes(TransferMode.LINK);
    }
    
    private void onDragDropped(DragEvent event) {
        event.consume();
        
        if (!event.getDragboard().hasFiles()) {
            event.setDropCompleted(false);
            return;
        }
        List<File> files = event.getDragboard().getFiles();
        if (!files.get(0).isFile()) {
            event.setDropCompleted(false);
            return;
        }
        boolean dropCompleted = validateAndSetTarget(files.get(0).toPath(), null);
        event.setDropCompleted(dropCompleted);
        
        if (dropCompleted && 1 < files.size() && files.get(1).isFile()) {
            opposite.validateAndSetTarget(files.get(1).toPath(), null);
        }
    }
    
    private void chooseBook(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("比較対象ブックの選択");
        
        if (bookInfo.getValue() != null) {
            File book = bookInfo.getValue().bookPath().toFile();
            chooser.setInitialDirectory(book.getParentFile());
            chooser.setInitialFileName(book.getName());
            
        } else if (prevSelectedBookPath != null) {
            chooser.setInitialDirectory(prevSelectedBookPath.toFile().getParentFile());
        }
        
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Excel ブック", "*.xls", "*.xlsx", "*.xlsm"));
        
        File selected = chooser.showOpenDialog(getScene().getWindow());
        
        if (selected != null) {
            validateAndSetTarget(selected.toPath(), null);
        }
    }
    
    private boolean validateAndSetTarget(Path newBookPath, String sheetName) {
        if (newBookPath == null) {
            bookInfo.setValue(null);
            sheetNameChoiceBox.setItems(FXCollections.emptyObservableList());
            return true;
        }
        
        try {
            List<String> sheetNames = null;
            BookInfo newBookInfo = BookInfo.of(newBookPath, null);
            
            while (true) {
                BookLoader loader = factory.bookLoader(newBookInfo);
                
                try {
                    sheetNames = loader.loadSheetNames(newBookInfo);
                    break;
                    
                } catch (PasswordHandlingException e) {
                    PasswordDialog dialog = new PasswordDialog(newBookInfo);
                    Optional<String> newPassword = dialog.showAndWait();
                    if (newPassword.isPresent()) {
                        newBookInfo = newBookInfo.withReadPassword(newPassword.get());
                    } else {
                        throw e;
                    }
                }
            }
            
            bookInfo.setValue(newBookInfo);
            sheetNameChoiceBox.setItems(FXCollections.observableList(sheetNames));
            prevSelectedBookPath = newBookPath;
            
        } catch (Exception e) {
            e.printStackTrace();
            bookInfo.setValue(null);
            sheetNameChoiceBox.setItems(FXCollections.emptyObservableList());
            new Alert(
                    AlertType.ERROR,
                    "ファイルを読み込めません：%n%s".formatted(newBookPath),
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
                    "シートが見つかりません：%n%s".formatted(sheetName),
                    ButtonType.OK)
                            .showAndWait();
            return false;
        }
        
        return true;
    }
}
