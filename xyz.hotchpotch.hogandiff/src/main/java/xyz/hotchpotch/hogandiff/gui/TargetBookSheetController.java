package xyz.hotchpotch.hogandiff.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringProperty;
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
import xyz.hotchpotch.hogandiff.excel.BookLoader;
import xyz.hotchpotch.hogandiff.excel.Factory;

public class TargetBookSheetController extends GridPane {
    
    // [static members] ********************************************************
    
    private static Path prevSelectedBookPath;
    
    // [instance members] ******************************************************
    
    @FXML
    private Label labelTitle;
    
    @FXML
    private TextField textBookPath;
    
    @FXML
    private Button buttonBookPath;
    
    @FXML
    private Label labelSheetName;
    
    @FXML
    private ChoiceBox<String> choiceSheetName;
    
    private Factory factory;
    
    private Property<Path> bookPath = new SimpleObjectProperty<>();
    private StringProperty sheetName = new SimpleStringProperty();
    private BooleanProperty isReady = new SimpleBooleanProperty();
    
    public TargetBookSheetController() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("TargetBookSheetView.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    public void init(
            Factory factory,
            String title,
            BooleanProperty isCompareBooks) {
        
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(isCompareBooks, "isCompareBooks");
        
        this.factory = factory;
        labelTitle.setText(title);
        textBookPath.setOnDragOver(this::onDragOver);
        textBookPath.setOnDragDropped(this::onDragDropped);
        buttonBookPath.setOnAction(this::chooseBook);
        labelSheetName.disableProperty().bind(isCompareBooks);
        choiceSheetName.disableProperty().bind(isCompareBooks);
        
        bookPath.bind(Bindings.createObjectBinding(
                () -> textBookPath.getText().isEmpty() ? null : Path.of(textBookPath.getText()),
                textBookPath.textProperty()));
        sheetName.bind(choiceSheetName.valueProperty());
        isReady.bind(Bindings.createBooleanBinding(
                () -> bookPath.getValue() != null
                        && (sheetName.getValue() != null || isCompareBooks.getValue()),
                bookPath, sheetName, isCompareBooks));
    }
    
    public ReadOnlyProperty<Path> bookPathProperty() {
        return bookPath;
    }
    
    public ReadOnlyStringProperty sheetNameProperty() {
        return sheetName;
    }
    
    public ReadOnlyBooleanProperty isReadyProperty() {
        return isReady;
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
    
    public void validateAndSetTarget(Path newBookPath, String sheetName) {
        if (newBookPath == null) {
            textBookPath.setText("");
            choiceSheetName.setItems(FXCollections.emptyObservableList());
            return;
        }
        
        try {
            BookLoader loader = factory.bookLoader(newBookPath);
            List<String> sheetNames = loader.loadSheetNames(newBookPath);
            
            textBookPath.setText(newBookPath.toString());
            choiceSheetName.setItems(FXCollections.observableList(sheetNames));
            prevSelectedBookPath = newBookPath;
            
        } catch (Exception e) {
            textBookPath.setText("");
            choiceSheetName.setItems(FXCollections.emptyObservableList());
            new Alert(
                    AlertType.ERROR,
                    "ファイルを読み込めません：\n" + newBookPath,
                    ButtonType.OK)
                            .showAndWait();
            return;
        }
        
        if (sheetName == null) {
            choiceSheetName.setValue(null);
            
        } else if (choiceSheetName.getItems().contains(sheetName)) {
            choiceSheetName.setValue(sheetName);
            
        } else {
            choiceSheetName.setValue(null);
            new Alert(
                    AlertType.ERROR,
                    "シートが見つかりません：\n" + sheetName,
                    ButtonType.OK)
                            .showAndWait();
        }
    }
}
