package xyz.hotchpotch.hogandiff;

import org.apache.poi.openxml4j.util.ZipSecureFile;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import xyz.hotchpotch.hogandiff.gui.MainController;

/**
 * このアプリケーションのエントリポイントです。<br>
 *
 * @author nmby
 */
public class AppMain extends Application {
    
    // [static members] ********************************************************
    
    /** このアプリケーションのバージョン */
    private static final String VERSION = "v0.13.0";
    
    /** このアプリケーションのドメイン（xyz.hotchpotch.hogandiff） */
    public static final String APP_DOMAIN = AppMain.class.getPackageName();
    
    /** このアプリケーションのWebサイトのURL */
    public static final String WEB_URL = "https://hogandiff.hotchpotch.xyz/";
    
    public static AppResource appResource;
    
    /**
     * このアプリケーションのエントリポイントです。<br>
     * 
     * @param args アプリケーション実行時引数
     */
    public static void main(String[] args) {
        appResource = AppResource.from(args);
        
        launch(args);
    }
    
    // [instance members] ******************************************************
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Zip bomb対策の制限の緩和。規定値の0.01から0.001に変更する。
        // いささか乱暴ではあるものの、ファイルを開く都度ではなくここで一括で設定してしまう。
        ZipSecureFile.setMinInflateRatio(0.001);
        
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("gui/MainView.fxml"),
                appResource.get());
        Parent root = loader.load();
        String cssPath = getClass().getResource("gui/application.css").toExternalForm();
        root.getStylesheets().add(cssPath.replace(" ", "%20"));
        Image icon = new Image(getClass().getResourceAsStream("gui/favicon.png"));
        primaryStage.getIcons().add(icon);
        primaryStage.setTitle(
                appResource.get().getString("AppMain.010")
                        + "  -  "
                        + VERSION);
        primaryStage.setScene(new Scene(root, 500, 464));
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(464);
        
        MainController controller = loader.getController();
        controller.applySettings(appResource.settings());
        
        primaryStage.show();
        
        if (controller.isReady()) {
            controller.execute();
        }
    }
}
