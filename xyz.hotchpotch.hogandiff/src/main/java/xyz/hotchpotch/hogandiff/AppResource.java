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

/**
 * アプリケーションのリソースを保持するクラスです。<br>
 * 
 * @author nmby
 */
public class AppResource {
    
    // static members **********************************************************
    
    /** プロパティファイルの相対パス */
    private static final Path APP_PROP_PATH;
    static {
        String osName = System.getProperty("os.name").toLowerCase();
        APP_PROP_PATH = osName.startsWith("mac")
                ? Path.of(System.getProperty("user.home"), "xyz.hotchpotch.hogandiff.properties")
                : Path.of("hogandiff.properties");
    }
    
    /**
     * プロパティファイルを読み込み、プロパティセットを返します。<br>
     * プロパティファイルが存在しない場合は、空のプロパティセットを返します。<br>
     * 
     * @return プロパティセット
     */
    private static Properties loadProperties() {
        if (Files.exists(APP_PROP_PATH)) {
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
        
        Locale appLocale = settings.get(SettingKeys.APP_LOCALE);
        this.rb = ResourceBundle.getBundle("messages", appLocale);
    }
    
    public Settings settings() {
        return settings;
    }
    
    public ResourceBundle get() {
        return rb;
    }
    
    public boolean storeSettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        Properties properties = settings.toProperties();
        
        this.properties = properties;
        this.settings = settings;
        
        return storeProperties();
    }
    
    private boolean storeProperties() {
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
    
    public boolean storeLocale(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        
        Settings.Key<Locale> key = SettingKeys.APP_LOCALE;
        
        properties.setProperty(key.name(), key.encoder().apply(locale));
        
        return storeProperties();
    }
    
    public void reflectArgs(String[] args) {
        Objects.requireNonNull(args, "args");
        
        // アプリケーション実行時引数から設定を抽出する。
        Optional<Settings> fromArgs = AppArgsParser.parseArgs(args);
        
        // アプリケーション実行時引数から設定を抽出できた場合は、
        // その内容で既存の内容を上書きする。
        // つまり、アプリケーション実行時引数で指定された内容を優先させる。
        if (fromArgs.isPresent()) {
            Settings.Builder builder = Settings.builder(settings);
            builder.setAll(fromArgs.get());
            this.settings = builder.build();
            
        } else if (0 < args.length) {
            System.err.println(AppArgsParser.USAGE);
        }
    }
}
