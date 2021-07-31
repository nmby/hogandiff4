package xyz.hotchpotch.hogandiff.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import xyz.hotchpotch.hogandiff.AppMenu;
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
public class TargetBookSheetParts extends GridPane {
    
    // [static members] ********************************************************
    
    private static Path prevSelectedBookPath;
    
    // [instance members] ******************************************************
    
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
    
    private final Property<Path> bookPath = new SimpleObjectProperty<>();
    private final StringProperty sheetName = new SimpleStringProperty();
    
    private Factory factory;
    private ReadOnlyProperty<AppMenu> menu;
    
    public TargetBookSheetParts() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TargetBookSheetParts.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /*package*/ void init(
            MainController parent,
            String title) {
        
        assert parent != null;
        assert title != null;
        
        factory = parent.factory;
        menu = parent.menu;
        
        titleLabel.setText(title);
        bookPathTextField.setOnDragOver(this::onDragOver);
        bookPathTextField.setOnDragDropped(this::onDragDropped);
        bookPathButton.setOnAction(this::chooseBook);
        sheetNameLabel.disableProperty().bind(Bindings.createBooleanBinding(
                () -> menu.getValue() == AppMenu.COMPARE_BOOKS,
                menu));
        sheetNameChoiceBox.disableProperty().bind(Bindings.createBooleanBinding(
                () -> menu.getValue() == AppMenu.COMPARE_BOOKS,
                menu));
        
        bookPath.bind(Bindings.createObjectBinding(
                () -> bookPathTextField.getText().isEmpty() ? null : Path.of(bookPathTextField.getText()),
                bookPathTextField.textProperty()));
        sheetName.bind(sheetNameChoiceBox.valueProperty());
        isReady.bind(Bindings.createBooleanBinding(
                () -> bookPath.getValue() != null
                        && (sheetName.getValue() != null || menu.getValue() == AppMenu.COMPARE_BOOKS),
                bookPath, sheetName, menu));
    }
    
    /*package*/ void applySettings(
            Settings settings,
            Key<Path> keyBookPath,
            Key<String> keySheetName) {
        
        assert settings != null;
        assert keyBookPath != null;
        assert keySheetName != null;
        
        if (settings.containsKey(keyBookPath)) {
            validateAndSetTarget(
                    settings.get(keyBookPath),
                    settings.containsKey(keySheetName)
                            ? settings.get(keySheetName)
                            : null);
        }
    }
    
    /*package*/ void gatherSettings(
            Settings.Builder builder,
            Key<Path> keyBookPath,
            Key<String> keySheetName) {
        
        assert builder != null;
        assert keyBookPath != null;
        assert keySheetName != null;
        
        if (bookPath.getValue() != null) {
            builder.set(keyBookPath, bookPath.getValue());
        }
        if (menu.getValue() == AppMenu.COMPARE_SHEETS && sheetName.getValue() != null) {
            builder.set(keySheetName, sheetName.getValue());
        }
    }
    
    private void onDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.LINK);
        }
        event.consume();
    }
    
    private void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            Path dropped = db.getFiles().get(0).toPath();
            if (!Files.isDirectory(dropped)) {
                validateAndSetTarget(dropped, null);
            }
        }
        event.setDropCompleted(db.hasFiles());
        event.consume();
    }
    
    private void chooseBook(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("比較対象ブックの選択");
        
        if (bookPath.getValue() != null) {
            chooser.setInitialDirectory(bookPath.getValue().toFile().getParentFile());
            chooser.setInitialFileName(bookPath.getValue().toFile().getName());
            
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
    
    private void validateAndSetTarget(Path newBookPath, String sheetName) {
        if (newBookPath == null) {
            bookPathTextField.setText("");
            sheetNameChoiceBox.setItems(FXCollections.emptyObservableList());
            return;
        }
        
        try {
            BookLoader loader = factory.bookLoader(newBookPath);
            List<String> sheetNames = loader.loadSheetNames(newBookPath);
            
            bookPathTextField.setText(newBookPath.toString());
            sheetNameChoiceBox.setItems(FXCollections.observableList(sheetNames));
            prevSelectedBookPath = newBookPath;
            
        } catch (PasswordHandlingException e) {
            bookPathTextField.setText("");
            sheetNameChoiceBox.setItems(FXCollections.emptyObservableList());
            new Alert(
                    AlertType.WARNING,
                    "パスワード付きファイルには対応していません：\n" + newBookPath,
                    ButtonType.OK)
                            .showAndWait();
            return;
            
        } catch (Exception e) {
            bookPathTextField.setText("");
            sheetNameChoiceBox.setItems(FXCollections.emptyObservableList());
            new Alert(
                    AlertType.ERROR,
                    "ファイルを読み込めません：\n" + newBookPath,
                    ButtonType.OK)
                            .showAndWait();
            return;
        }
        
        if (sheetName == null) {
            sheetNameChoiceBox.setValue(null);
            
        } else if (sheetNameChoiceBox.getItems().contains(sheetName)) {
            sheetNameChoiceBox.setValue(sheetName);
            
        } else {
            sheetNameChoiceBox.setValue(null);
            new Alert(
                    AlertType.ERROR,
                    "シートが見つかりません：\n" + sheetName,
                    ButtonType.OK)
                            .showAndWait();
        }
    }
}
