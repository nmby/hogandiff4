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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.google.GoogleCredential;
import xyz.hotchpotch.hogandiff.google.GoogleHandlingException;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;

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
            // TODO: 多言語化する
            Optional<ButtonType> result = new Alert(
                    AlertType.CONFIRMATION,
                    "方眼DiffとGoogleドライブの連携を解除します。\nよろしいですか？")
                            .showAndWait();
            
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
            
            try {
                parent.googleCredential.getValue().deleteCredential();
                parent.googleCredential.setValue(null);
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("方眼Diff");
                alert.setHeaderText("資格情報を削除しました。");
                alert.setContentText(""
                        // TODO: 文言改善する
                        + "このPCに保存されていたGoogleアカウント連携のための資格情報を削除しました。\n\n"
                        + "Google側の設定では引き続き方眼Diffとの連携が許可されています。\n"
                        + "連携許可を完全に取り消したい場合はGoogleアカウントのサイトから設定を行ってください。\n"
                        + "参考：https://hogandiff.hotchpotch.xyz/");
                alert.showAndWait();
                
            } catch (GoogleHandlingException e) {
                parent.googleCredential.setValue(null);
                e.printStackTrace();
                
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("方眼Diff");
                alert.setHeaderText("資格情報の削除に失敗しました。");
                alert.setContentText(""
                        // TODO: 文言改善する
                        + "資格情報の削除に失敗しました。\n"
                        + "時間をおいてから再度お試しください。\n\n"
                        + "失敗し続ける場合はウェブサイトからお問い合わせください。\n"
                        + "https://hogandiff.hotchpotch.xyz/");
                alert.showAndWait();
            }
        });
        
        // 3.初期値の設定
        parent.googleCredential.setValue(GoogleCredential.get(false));
        
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
                // TODO: 既存のAlert利用箇所もこの書き方にする
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Google連携エラー");
                alert.setHeaderText("Google連携処理中にエラーが発生しました。");
                alert.setContentText(""
                        // TODO: 文言改善する
                        + "エラー：" + exception.getMessage() + "\n\n"
                        + "時間をおいて再度お試しください。\n"
                        + "失敗し続ける場合はウェブサイトからお問い合わせください。\n"
                        + "https://hogandiff.hotchpotch.xyz/");
                alert.showAndWait();
            });
        }
    }
}
