package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.Property;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.UIUtil;
import xyz.hotchpotch.hogandiff.logic.google.GoogleCredential;
import xyz.hotchpotch.hogandiff.logic.google.GoogleHandlingException;

/**
 * Googleドライブ連携設定部分の部品です。<br>
 * 
 * @author nmby
 */
public class GooglePane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private ImageView profileImageView;
    
    @FXML
    private ImageView googleImageView;
    
    @FXML
    private Button connectGoogleButton;
    
    @FXML
    private Button disconnectGoogleButton;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public GooglePane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GooglePane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent);
        
        // 1.disabled/visibleプロパティのバインディング
        BooleanBinding isCredentialNull = Bindings.createBooleanBinding(
                () -> parent.googleCredential.getValue() == null,
                parent.googleCredential);
        
        googleImageView.visibleProperty().bind(isCredentialNull);
        googleImageView.managedProperty().bind(isCredentialNull);
        connectGoogleButton.visibleProperty().bind(isCredentialNull);
        connectGoogleButton.managedProperty().bind(isCredentialNull);
        
        profileImageView.visibleProperty().bind(isCredentialNull.not());
        profileImageView.managedProperty().bind(isCredentialNull.not());
        disconnectGoogleButton.visibleProperty().bind(isCredentialNull.not());
        disconnectGoogleButton.managedProperty().bind(isCredentialNull.not());
        
        // 2.項目ごとの各種設定
        double size = profileImageView.getFitWidth();
        Circle circle = new Circle(size / 2);
        circle.setCenterX(size / 2);
        circle.setCenterY(size / 2);
        profileImageView.setClip(circle);
        
        profileImageView.imageProperty().bind(Bindings.createObjectBinding(
                () -> {
                    GoogleCredential credential = parent.googleCredential.getValue();
                    if (credential != null) {
                        String picUrl = credential.driveUser().getPhotoLink();
                        if (picUrl != null) {
                            return new Image(picUrl);
                        }
                    }
                    return null;
                },
                parent.googleCredential));
        
        connectGoogleButton.setOnAction(event -> {
            Task<GoogleCredential> connectTask = new ConnectGoogleTask(parent.googleCredential);
            Thread connectThread = new Thread(connectTask);
            connectThread.setDaemon(true);
            connectThread.start();
        });
        
        disconnectGoogleButton.setOnAction(event -> {
            Optional<ButtonType> result = new Alert(
                    AlertType.CONFIRMATION,
                    rb.getString("gui.component.GooglePane.010"))
                            .showAndWait();
            
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
            
            try {
                parent.googleCredential.getValue().deleteCredential();
                parent.googleCredential.setValue(null);
                
                Hyperlink link = UIUtil.createHyperlink("https://myaccount.google.com/connections");
                VBox content = new VBox(10);
                content.getChildren().addAll(
                        new Label(rb.getString("gui.component.GooglePane.030")),
                        link);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(rb.getString("AppMain.010"));
                alert.setHeaderText(rb.getString("gui.component.GooglePane.020"));
                alert.getDialogPane().setContent(content);
                alert.showAndWait();
                
            } catch (GoogleHandlingException e) {
                parent.googleCredential.setValue(null);
                e.printStackTrace();
                
                Hyperlink link = UIUtil.createHyperlink("https://hogandiff.hotchpotch.xyz/inquiry");
                VBox content = new VBox(10);
                content.getChildren().addAll(
                        new Label(rb.getString("gui.component.GooglePane.050")),
                        link);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(rb.getString("AppMain.010"));
                alert.setHeaderText(rb.getString("gui.component.GooglePane.040"));
                alert.getDialogPane().setContent(content);
                alert.showAndWait();
            }
        });
        
        // 3.初期値の設定
        Thread asyncInitGoogleTask = new Thread(() -> {
            GoogleCredential credential = GoogleCredential.get(false);
            Platform.runLater(() -> {
                parent.googleCredential.setValue(credential);
            });
        });
        asyncInitGoogleTask.setDaemon(true);
        asyncInitGoogleTask.start();
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
    
    private class ConnectGoogleTask extends Task<GoogleCredential> {
        private final Property<GoogleCredential> googleCredential;
        
        private ConnectGoogleTask(Property<GoogleCredential> googleCredential) {
            this.googleCredential = googleCredential;
        }
        
        @Override
        protected GoogleCredential call() {
            return GoogleCredential.get(true);
        }
        
        @Override
        protected void succeeded() {
            googleCredential.setValue(getValue());
        }
        
        @Override
        protected void failed() {
            Throwable exception = getException();
            exception.printStackTrace();
            
            Platform.runLater(() -> {
                Hyperlink link = UIUtil.createHyperlink("https://hogandiff.hotchpotch.xyz/inquiry");
                VBox content = new VBox(10);
                content.getChildren().addAll(
                        new Label(rb.getString("gui.component.GooglePane.080").formatted(exception.getMessage())),
                        link);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(rb.getString("gui.component.GooglePane.060"));
                alert.setHeaderText(rb.getString("gui.component.GooglePane.070"));
                alert.getDialogPane().setContent(content);
                alert.showAndWait();
            });
        }
    }
}
