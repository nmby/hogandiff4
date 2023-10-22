package xyz.hotchpotch.hogandiff;

import org.apache.poi.openxml4j.util.ZipSecureFile;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * このアプリケーションのエントリポイントです。<br>
 *
 * @author nmby
 */
public class AppMain extends Application {
    
    // [static members] ********************************************************
    
    /** このアプリケーションのバージョン */
    private static final String VERSION = "v0.17.4";
    
    /** このアプリケーションのドメイン（xyz.hotchpotch.hogandiff） */
    public static final String APP_DOMAIN = AppMain.class.getPackageName();
    
    /** このアプリケーションのWebサイトのURL */
    public static final String WEB_URL = "https://hogandiff.hotchpotch.xyz/";
    
    /** このアプリケーションで利用するリソース */
    private static AppResource appResource = AppResource.fromProperties();
    
    public static AppResource appResource() {
        return appResource;
    }
    
    /** メインステージ */
    public static Stage stage;
    
    // TODO: コンポーネントの実効サイズを動的に取得する方法を見つける
    /** 設定エリアを開いたときのメインステージの最小高さ */
    public static final double STAGE_HEIGHT_OPEN = 390d;
    
    /** 設定エリアを閉じたときのメインステージの最小高さ */
    public static final double STAGE_HEIGHT_CLOSE = 232d;
    
    /** メインステージの最小幅 */
    public static final double STAGE_WIDTH = 521d;
    
    /**
     * このアプリケーションのエントリポイントです。<br>
     * 
     * @param args アプリケーション実行時引数
     */
    public static void main(String[] args) {
        appResource.reflectArgs(args);
        
        launch(args);
    }
    
    // [instance members] ******************************************************
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;
        
        // Zip bomb対策の制限の緩和。規定値の0.01から0.001に変更する。
        // いささか乱暴ではあるものの、ファイルを開く都度ではなくここで一括で設定してしまう。
        ZipSecureFile.setMinInflateRatio(0.001);
        
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("gui/MainView.fxml"),
                appResource.get());
        Parent root = loader.load();
        Scene scene = new Scene(root);
        String cssPath = getClass().getResource("gui/application.css").toExternalForm();
        root.getStylesheets().add(cssPath.replace(" ", "%20"));
        Image icon = new Image(getClass().getResourceAsStream("gui/favicon.png"));
        Settings settings = appResource.settings();
        
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(icon);
        primaryStage.setTitle(
                appResource.get().getString("AppMain.010")
                        + "  -  "
                        + VERSION);
        
        primaryStage.setMinHeight(
                settings.getOrDefault(SettingKeys.SHOW_SETTINGS)
                        ? STAGE_HEIGHT_OPEN
                        : STAGE_HEIGHT_CLOSE);
        primaryStage.setMinWidth(STAGE_WIDTH);
        
        if (settings.containsKey(SettingKeys.STAGE_HEIGHT)) {
            primaryStage.setHeight(settings.get(SettingKeys.STAGE_HEIGHT));
        }
        if (settings.containsKey(SettingKeys.STAGE_WIDTH)) {
            primaryStage.setWidth(settings.get(SettingKeys.STAGE_WIDTH));
        }
        if (settings.containsKey(SettingKeys.STAGE_MAXIMIZED)) {
            primaryStage.setMaximized(settings.get(SettingKeys.STAGE_MAXIMIZED));
        }
        primaryStage.heightProperty().addListener((target, oldValue, newValue) -> {
            if (!primaryStage.isMaximized()) {
                appResource.changeSetting(SettingKeys.STAGE_HEIGHT, (Double) newValue);
            }
        });
        primaryStage.widthProperty().addListener((target, oldValue, newValue) -> {
            if (!primaryStage.isMaximized()) {
                appResource.changeSetting(SettingKeys.STAGE_WIDTH, (Double) newValue);
            }
        });
        primaryStage.maximizedProperty().addListener((target, oldValue, newValue) -> {
            appResource.changeSetting(SettingKeys.STAGE_MAXIMIZED, newValue);
        });
        
        primaryStage.show();
        
        MainController controller = loader.getController();
        if (controller.isReady().getValue()) {
            controller.execute();
        }
    }
}
