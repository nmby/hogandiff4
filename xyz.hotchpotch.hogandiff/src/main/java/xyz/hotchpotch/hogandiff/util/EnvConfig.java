package xyz.hotchpotch.hogandiff.util;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * 環境変数周りのユーティリティクラスです。<br>
 * 
 * @author nmby
 */
public class EnvConfig {
    
    // [static members] ********************************************************
    
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("/")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
    
    /**
     * 指定された環境変数の値を返します。<br>
     * 指定された環境変数が存在しない場合は {@code null} を返します。<br>
     * 
     * @param key 環境変数の名前
     * @return 環境変数の値
     */
    public static String get(String key) {
        return dotenv.get(key);
    }
    
    /**
     * 指定された環境変数の値を返します。<br>
     * 指定された環境変数が存在しない場合は指定されたデフォルト値を返します。<br>
     * 
     * @param key 環境変数の名前
     * @param defaultValue デフォルト値
     * @return 環境変数の値
     */
    public static String get(String key, String defaultValue) {
        String value = dotenv.get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 指定された環境変数の値を整数値として返します。<br>
     * 指定された環境変数が存在しない場合、または数値として評価できない場合は、
     * 指定されたデフォルト値を返します。<br>
     * 
     * @param key 環境変数の名前
     * @param defaultValue デフォルト値
     * @return 環境変数の値
     */
    public static int getInt(String key, int defaultValue) {
        String value = dotenv.get(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 指定された環境変数の値を真偽値として返します。<br>
     * 指定された環境変数が存在しない場合は、指定されたデフォルト値を返します。<br>
     * 
     * @param key 環境変数の名前
     * @param defaultValue デフォルト値
     * @return 環境変数の値
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = dotenv.get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    // [instance members] ******************************************************
    
    private EnvConfig() {
    }
}
