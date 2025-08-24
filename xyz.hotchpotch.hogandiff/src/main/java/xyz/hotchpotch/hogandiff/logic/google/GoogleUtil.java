package xyz.hotchpotch.hogandiff.logic.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;

public class GoogleUtil {
    
    // [static members] ********************************************************
    
    public static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    
    public static final HttpTransport HTTP_TRANSPORT;
    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    // [instance members] ******************************************************
    
    private GoogleUtil() {
    }
}
