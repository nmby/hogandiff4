package xyz.hotchpotch.hogandiff.logic.google;

import java.util.Objects;
import java.util.regex.Pattern;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;

public class GoogleUtil {
    
    // [static members] ********************************************************
    
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^https://(?:drive\\.google\\.com/file/d/|docs\\.google\\.com/spreadsheets/d/)"
                    + "[\\w-]+(?:/(?:edit|view))?(?:[?#].*)?$");
    
    public static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    public static final HttpTransport HTTP_TRANSPORT;
    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    public static boolean isGDFileUrl(String fileUrl) {
        return URL_PATTERN.matcher(fileUrl).matches();
    }
    
    public static String extractFileId(String fileUrl) {
        Objects.requireNonNull(fileUrl);
        
        if (fileUrl.contains("/file/d/")) {
            return fileUrl.split("/file/d/")[1].split("/")[0];
            
        } else if (fileUrl.contains("/spreadsheets/d/")) {
            return fileUrl.split("/spreadsheets/d/")[1].split("/")[0];
        }
        
        throw new IllegalArgumentException("Invalid URL format");
    }
    
    // [instance members] ******************************************************
    
    private GoogleUtil() {
    }
}
