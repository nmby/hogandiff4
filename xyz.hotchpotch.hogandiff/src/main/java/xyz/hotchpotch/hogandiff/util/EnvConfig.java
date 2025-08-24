package xyz.hotchpotch.hogandiff.util;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {
    
    // [static members] ********************************************************
    
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("/")
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
    
    public static String get(String key) {
        return dotenv.get(key);
    }
    
    public static String get(String key, String defaultValue) {
        String value = dotenv.get(key);
        return value != null ? value : defaultValue;
    }
    
    public static int getInt(String key, int defaultValue) {
        String value = dotenv.get(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = dotenv.get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    // [instance members] ******************************************************
    
    private EnvConfig() {
    }
}
