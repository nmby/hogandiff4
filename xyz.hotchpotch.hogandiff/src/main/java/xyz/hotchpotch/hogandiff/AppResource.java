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
import xyz.hotchpotch.hogandiff.logic.BookInfo;
import xyz.hotchpotch.hogandiff.logic.Factory;
import xyz.hotchpotch.hogandiff.logic.PairingInfoBooks;
import xyz.hotchpotch.hogandiff.logic.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.Settings.Key;

/**
 * アプリケーションのリソースを保持するクラスです。<br>
 * ここで言うリソースとは、以下の3つを含みます。<br>
 * <ul>
 * <li>実行時引数から得た設定</li>
 * <li>プロパティファイルから得た設定</li>
 * <li>リソースバンドルファイルから得られたメッセージリソース</li>
 * </ul>
 * 
 * @author nmby
 */
public class AppResource {
    
    // static members **********************************************************
    
    /** ユーザーディレクトリ */
    public static final Path USER_HOME;
    static {
        String osName = System.getProperty("os.name").toLowerCase();
        String userHome = System.getProperty("user.home");
        
        Path dir = osName.startsWith("mac")
                ? Path.of(userHome, AppMain.APP_DOMAIN)
                : Path.of(userHome, "AppData", "Roaming", AppMain.APP_DOMAIN);
        
        if (Files.notExists(dir)) {
            try {
                Files.createDirectory(dir);
            } catch (Exception e) {
                e.printStackTrace();
                dir = null;
            }
        }
        USER_HOME = dir;
    }
    
    /** プロパティファイルの相対パス */
    /** プロパティファイルの相対パス */
    private static Path APP_PROP_PATH = USER_HOME != null
            ? USER_HOME.resolve("hogandiff.properties")
            : null;
    
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
     * このアプリケーションで利用するリソースを構成して返します。<br>
     * 
     * @return このアプリケーションで利用するリソース
     */
    public static AppResource fromProperties() {
        Properties properties = loadProperties();
        Settings settings;
        
        try {
            settings = Settings.builder(properties, SettingKeys.storableKeys).build();
        } catch (RuntimeException e) {
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
     * @param <T>   設定値の型
     * @param key   設定キー
     * @param value 設定値（{@code null} 許容）
     * @return 保存に成功した場合は {@code true}
     * @throws NullPointerException {@code key} が {@code null} の場合
     */
    public <T> boolean changeSetting(Key<T> key, T value) {
        Objects.requireNonNull(key);
        
        settings = settings.getAltered(key, value);
        
        if (key.storable()) {
            properties.setProperty(key.name(), key.encoder().apply(value));
            return storeProperties();
        } else {
            return true;
        }
    }
    
    /**
     * このリソースセットにアプリケーション実行時引数から得られる内容を上書きで反映します。<br>
     * 
     * @param args アプリケーション実行時引数
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public void reflectArgs(String[] args) {
        Objects.requireNonNull(args);
        
        Optional<Settings> fromArgs = AppArgsParser.parseArgs(args);
        
        if (fromArgs.isPresent()) {
            settings = Settings.builder().setAll(settings).setAll(fromArgs.get()).build();
            properties = settings.toProperties();
            storeProperties();
            
            // コンソールから起動した際は CURR_BOOK_COMPARE_INFO が未設定のため、
            // ここで処理を行うこととする。
            // FIXME: [No.X 内部実装改善] より適した位置を見つけて整理する。
            BookInfo bookInfoA = loadBookInfo(settings.get(SettingKeys.CURR_BOOK_INFO1).bookPath());
            BookInfo bookInfoB = loadBookInfo(settings.get(SettingKeys.CURR_BOOK_INFO2).bookPath());
            PairingInfoBooks bookComparison = PairingInfoBooks.calculate(
                    Pair.of(bookInfoA, bookInfoB),
                    Factory.sheetNamesMatcher(settings));
            settings = settings.getAltered(SettingKeys.CURR_BOOK_COMPARE_INFO, bookComparison);
        }
    }
    
    private BookInfo loadBookInfo(Path bookPath) {
        SheetNamesLoader loader = Factory.bookLoader(bookPath);
        return loader.loadBookInfo(bookPath, null);
    }
}
