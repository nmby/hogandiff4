package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.logic.google.GoogleCredential;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher.GoogleFileMetadata;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher.RevisionMapper;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo;
import xyz.hotchpotch.hogandiff.logic.google.GoogleHandlingException;
import xyz.hotchpotch.hogandiff.logic.google.GoogleUtil;

/**
 * ユーザーにGoogleドライブ上のファイル選択を求めるダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class GoogleFilePickerDialogPane extends VBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    /* package */ TextField fileUrlTextField;
    
    @FXML
    private Label errorLabel;
    
    @FXML
    private Label fileNameLabel;
    
    @FXML
    private Button fetchFileInfoButton;
    
    @FXML
    /* package */ ChoiceBox<RevisionMapper> revisionChoiceBox;
    
    private BooleanProperty processing = new SimpleBooleanProperty();
    
    /* package */ Property<GoogleFileMetadata> fileMetadata = new SimpleObjectProperty<>();
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public GoogleFilePickerDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GoogleFilePickerDialogPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * このダイアログボックス要素を初期化します。<br>
     * 
     * @param parent   親要素
     * @param fileUrl  GoogleドライブファイルのURL
     * @param revision GoogleドライブファイルのリビジョンID
     */
    /* package */ void init(
            GoogleFilePickerDialog parent,
            GoogleFileInfo googleFileInfo,
            GoogleCredential credential) {
        
        // 1.プロパティのバインディング
        fileUrlTextField.disableProperty().bind(processing);
        
        fetchFileInfoButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> processing.get() || fileMetadata.getValue() != null
                        || fileUrlTextField.getText() == null
                        || !GoogleUtil.isGDFileUrl(fileUrlTextField.getText()),
                processing, fileMetadata, fileUrlTextField.textProperty()));
        
        errorLabel.textProperty().bind(Bindings.createStringBinding(
                () -> fileUrlTextField.getText() == null
                        || GoogleUtil.isGDFileUrl(fileUrlTextField.getText())
                                ? null
                                : rb.getString("fx.GoogleFilePickerDialogPane.010"),
                fileUrlTextField.textProperty()));
        
        fileNameLabel.textProperty().bind(Bindings.createStringBinding(
                () -> fileMetadata.getValue() == null
                        ? null
                        : fileMetadata.getValue().fileName(),
                fileMetadata));
        
        revisionChoiceBox.disableProperty().bind(Bindings.createBooleanBinding(
                () -> processing.get() || revisionChoiceBox.getItems().isEmpty(),
                processing, revisionChoiceBox.itemsProperty()));
        
        revisionChoiceBox.itemsProperty().bind(Bindings.createObjectBinding(
                () -> fileMetadata.getValue() == null
                        ? FXCollections.emptyObservableList()
                        : FXCollections.observableList(fileMetadata.getValue().revisions()),
                fileMetadata));
        
        // 2.イベントハンドラの設定
        fileUrlTextField.textProperty().addListener((target, oldValue, newValue) -> fileMetadata.setValue(null));
        
        GoogleFileFetcher fetcher = GoogleFileFetcher.of(credential);
        
        fetchFileInfoButton.setOnAction(event -> {
            processing.set(true);
            
            try {
                // processing.set(true) により fetchFileInfoButton が直ちに disabled になってほしいがならないため、
                // この糞な sleep を挟むことにする。
                // 実装方法が間違っているのだろうが、よく分からないのでいつか直す。
                // TODO: Thread::sleep を使わない方法で実装する
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // nop
            }
            Platform.runLater(() -> {
                try {
                    GoogleFileMetadata fileMetadata = fetcher.fetchMetadata(fileUrlTextField.getText());
                    this.fileMetadata.setValue(fileMetadata);
                    
                } catch (GoogleHandlingException e) {
                    new Alert(
                            AlertType.ERROR,
                            rb.getString("fx.GoogleFilePickerDialogPane.070")
                                    .formatted(fileUrlTextField.getText()),
                            ButtonType.OK)
                                    .showAndWait();
                    
                    e.printStackTrace();
                    return;
                    
                } finally {
                    processing.set(false);
                }
            });
        });
        
        revisionChoiceBox.itemsProperty().addListener((target, oldValue, newValue) -> {
            if (oldValue.isEmpty() && !newValue.isEmpty()) {
                revisionChoiceBox.setValue(newValue.get(0));
            }
        });
        
        // 3.初期値の設定
        if (googleFileInfo != null) {
            fileUrlTextField.setText(googleFileInfo.fileUrl());
        } else {
            fileUrlTextField.setText(null);
        }
    }
}
