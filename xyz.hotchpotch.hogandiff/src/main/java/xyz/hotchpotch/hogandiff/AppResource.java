package xyz.hotchpotch.hogandiff;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * アプリケーションのリソースを保持するクラスです。<br>
 * 
 * @author nmby
 */
public class AppResource {
    
    // static members **********************************************************
    
    /** プロパティファイルの相対パス */
    private static Path APP_PROP_PATH;
    static {
        String osName = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        
        Path dir = osName.startsWith("mac")
                ? Path.of(userHome, AppMain.APP_DOMAIN)
                : Path.of(userHome, "AppData", "Roaming", AppMain.APP_DOMAIN);
        
        try {
            if (Files.notExists(dir)) {
                Files.createDirectory(dir);
            }
            APP_PROP_PATH = dir.resolve("hogandiff.properties");
            
        } catch (Exception e) {
            e.printStackTrace();
            APP_PROP_PATH = null;
        }
    }
    
    /**
     * プロパティファイルを読み込み、プロパティセットを返します。<br>
     * プロパティファイルが存在しない場合は、空のプロパティセットを返します。<br>
     * 
     * @return プロパティセット
     */
    private static Properties loadProperties() {
        if (APP_PROP_PATH != null && Files.exists(APP_PROP_PATH)) {
            try (Reader r = Files.newBufferedReader(APP_PROP_PATH)) {
                Properties properties = new Properties();
                properties.load(r);
                return properties;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new Properties();
    }
    
    /**
     * このアプリケーションで利用するリソースをプロパティファイルから構成します。<br>
     * 
     * @return このアプリケーションで利用するリソース
     */
    public static AppResource fromProperties() {
        Properties properties = loadProperties();
        Settings settings;
        
        try {
            // プロパティファイルから設定を抽出する。
            Settings.Builder builder = Settings.builder(properties, SettingKeys.storableKeys);
            settings = builder.build();
            
        } catch (RuntimeException e) {
            // 何らかの実行時例外が発生した場合は空の設定を返すことにする。
            settings = Settings.builder().build();
        }
        
        return new AppResource(properties, settings);
    }
    
    // instance members ********************************************************
    
    private Properties properties;
    private Settings settings;
    private ResourceBundle rb;
    
    private AppResource(
            Properties properties,
            Settings settings) {
        
        assert properties != null;
        assert settings != null;
        
        this.properties = properties;
        this.settings = settings;
        
        Locale appLocale = settings.getOrDefault(SettingKeys.APP_LOCALE);
        this.rb = ResourceBundle.getBundle("messages", appLocale);
    }
    
    /**
     * 設定セットを返します。<br>
     * 
     * @return 設定セット
     */
    public Settings settings() {
        return settings;
    }
    
    /**
     * リソースバンドルを返します。<br>
     * 
     * @return リソースバンドル
     */
    public ResourceBundle get() {
        return rb;
    }
    
    /**
     * このリソースにアプリケーション実行時引数の内容を反映させます。<br>
     * 
     * @param args アプリケーション実行時引数
     * @throws NullPointerException {@code args} が {@code null} の場合
     */
    public void reflectArgs(String[] args) {
        Objects.requireNonNull(args, "args");
        
        // アプリケーション実行時引数から設定を抽出する。
        Optional<Settings> fromArgs = AppArgsParser.parseArgs(args);
        
        // アプリケーション実行時引数から設定を抽出できた場合は、
        // その内容で既存の内容を上書きする。
        // つまり、アプリケーション実行時引数で指定された内容を優先させる。
        if (fromArgs.isPresent()) {
            settings = Settings.builder()
                    .setAll(settings)
                    .setAll(fromArgs.get())
                    .build();
            
        } else if (0 < args.length) {
            System.err.println(AppArgsParser.USAGE);
        }
    }
    
    private boolean storeProperties() {
        if (APP_PROP_PATH == null) {
            return false;
        }
        try (Writer w = Files.newBufferedWriter(APP_PROP_PATH)) {
            properties.store(w, null);
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(
                    AlertType.ERROR,
                    "%s%n%s".formatted(rb.getString("AppResource.010"), APP_PROP_PATH),
                    ButtonType.OK)
                            .showAndWait();
            return false;
        }
    }
    
    /**
     * 設定値を変更しプロパティファイルに記録します。<br>
     * 
     * @param <T> 設定値の型
     * @param key 設定キー
     * @param value 設定値
     * @return 保存に成功した場合は {@code true}
     * @throws NullPointerException {@code key} が {@code null} の場合
     */
    public <T> boolean changeSetting(Key<T> key, T value) {
        Objects.requireNonNull(key, "key");
        
        settings = settings.getAltered(key, value);
        
        if (key.storable()) {
            properties.setProperty(key.name(), key.encoder().apply(value));
            return storeProperties();
        } else {
            return true;
        }
    }
}
