package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

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
    public static final String VERSION = "v0.22.2";
    
    /** このアプリケーションのドメイン（xyz.hotchpotch.hogandiff） */
    public static final String APP_DOMAIN = AppMain.class.getPackageName();
    
    /** このアプリケーションのWebサイトのURL */
    public static final String WEB_URL = "https://hogandiff.hotchpotch.xyz/";
    
    /** このアプリケーションで利用するリソース */
    public static final AppResource appResource = AppResource.fromProperties();
    
    /** メインステージ */
    public static Stage stage;
    
    // FIXME: [No.10 UIサイズ] コンポーネントの実効サイズを動的に取得する方法を見つける
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
        
        announceNewFeature();
        
        // Zip bomb対策の制限の緩和。規定値の0.01から0.001に変更する。
        // いささか乱暴ではあるものの、ファイルを開く都度ではなくここで一括で設定してしまう。
        ZipSecureFile.setMinInflateRatio(0.001);
        
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("gui/MainView.fxml"),
                appResource.get());
        Parent root = loader.load();
        Scene scene = new Scene(root);
        String cssPath = getClass().getResource("gui/application.css").toExternalForm().replace(" ", "%20");
        root.getStylesheets().add(cssPath);
        Image icon = new Image(getClass().getResourceAsStream("gui/favicon.png"));
        Settings settings = appResource.settings();
        
        primaryStage.setScene(scene);
        primaryStage.getIcons().add(icon);
        primaryStage.setTitle(
                appResource.get().getString("AppMain.010")
                        + "  -  "
                        + VERSION);
        
        primaryStage.setMinHeight(
                settings.get(SettingKeys.SHOW_SETTINGS)
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
    
    private void announceNewFeature() {
        // UUIDが未採番の場合は採番する。
        UUID uuid = appResource.settings().get(SettingKeys.CLIENT_UUID);
        if (uuid == null) {
            appResource.changeSetting(SettingKeys.CLIENT_UUID, UUID.randomUUID());
        }
        
        // 前回までの利用Versionを調べ、新バージョンの初回起動の場合は新バージョンに応じた処理を行う。
        String prevVersion = appResource.settings().get(SettingKeys.APP_VERSION);
        if (!VERSION.equals(prevVersion)) {
            
            assert VERSION.equals("v0.22.2");
            // v0.22.2 では次の2点を行う。
            //  ・新機能紹介ページの表示
            //  ・統計情報の収集に同意していない場合に、設定エリアを開く（同意促進）
            
            try {
                Desktop.getDesktop().browse(URI.create("https://hogandiff.hotchpotch.xyz/releasenotes/v0-22-2/"));
            } catch (IOException e) {
                e.printStackTrace();
                // nop
            }
            if (!appResource.settings().get(SettingKeys.CONSENTED_STATS_COLLECTION)) {
                appResource.changeSetting(SettingKeys.SHOW_SETTINGS, true);
                stage.setMinHeight(STAGE_HEIGHT_OPEN);
            }
            
            appResource.changeSetting(SettingKeys.APP_VERSION, VERSION);
        }
    }
}
