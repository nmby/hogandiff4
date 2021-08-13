package xyz.hotchpotch.hogandiff;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
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
    
    /** このアプリケーションのドメイン（xyz.hotchpotch.hogandiff） */
    public static final String APP_DOMAIN = AppMain.class.getPackageName();
    
    /** このアプリケーションのWebサイトのURL */
    public static final String WEB_URL = "https://hogandiff.hotchpotch.xyz/";
    
    /** このアプリケーションのバージョン */
    private static final String VERSION = "v0.9.0";
    
    /** プロパティファイルの相対パス */
    private static final Path APP_PROP_PATH = Path.of("hogandiff.properties");
    
    /**
     * このアプリケーションのエントリポイントです。<br>
     * 
     * @param args アプリケーション実行時引数
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    /**
     * プロパティファイルを読み込み、プロパティセットを返します。<br>
     * プロパティファイルが存在しない場合は、空のプロパティセットを返します。<br>
     * 
     * @return プロパティセット
     */
    public static Properties loadProperties() {
        Properties properties = new Properties();
        try (Reader r = Files.newBufferedReader(APP_PROP_PATH)) {
            properties.load(r);
        } catch (Exception e) {
            // nop
        }
        return properties;
    }
    
    /**
     * 指定されたプロパティセットをプロパティファイルに保存します。<br>
     * 
     * @param properties プロパティセット
     * @throws NullPointerException {@code properties} が {@code null} の場合
     */
    public static void storeProperties(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        
        try (Writer w = Files.newBufferedWriter(APP_PROP_PATH)) {
            properties.store(w, null);
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(
                    AlertType.ERROR,
                    "設定の保存に失敗しました。\n" + APP_PROP_PATH,
                    ButtonType.OK)
                            .showAndWait();
        }
    }
    
    // [instance members] ******************************************************
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("gui/MainView.fxml"));
        Parent root = loader.load();
        String cssPath = getClass().getResource("gui/application.css").toExternalForm();
        root.getStylesheets().add(cssPath.replace(" ", "%20"));
        Image icon = new Image(getClass().getResourceAsStream("gui/favicon.png"));
        primaryStage.getIcons().add(icon);
        primaryStage.setTitle("方眼Diff  -  " + VERSION);
        primaryStage.setScene(new Scene(root, 500, 450));
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(450);
        
        MainController controller = loader.getController();
        Settings settings = arrangeSettings();
        controller.applySettings(settings);
        
        primaryStage.show();
        
        if (controller.isReady()) {
            controller.execute();
        }
    }
    
    private Settings arrangeSettings() {
        try {
            // 1. プロパティファイルから設定を抽出する。
            Properties properties = loadProperties();
            Settings.Builder builder = Settings.builder(properties, SettingKeys.storableKeys);
            
            // 2. アプリケーション実行時引数から設定を抽出する。
            Optional<Settings> fromArgs = AppArgsParser.parseArgs(getParameters().getRaw());
            if (!getParameters().getRaw().isEmpty() && fromArgs.isEmpty()) {
                System.err.println(AppArgsParser.USAGE);
            }
            
            // 3. アプリケーション実行時引数から設定を抽出できた場合は、
            //    その内容でプロパティファイルからの内容を上書きする。
            //    つまり、アプリケーション実行時引数で指定された内容を優先させる。
            if (fromArgs.isPresent()) {
                builder.setAll(fromArgs.get());
            }
            
            return builder.build();
            
        } catch (RuntimeException e) {
            // 何らかの実行時例外が発生した場合は空の設定を返すことにする。
            return Settings.builder().build();
        }
    }
}
