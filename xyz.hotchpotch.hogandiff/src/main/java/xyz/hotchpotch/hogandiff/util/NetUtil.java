package xyz.hotchpotch.hogandiff.util;

import java.util.Objects;

import org.json.JSONObject;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;

/**
 * ネットワーク関連のユーティリティクラスです。<br>
 * 
 * @author nmby
 */
public class NetUtil {
    
    // [static members] ********************************************************
    
    private static final HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
    
    /**
     * 指定されたURLにGETリクエストを送り、レスポンスボディをJSONオブジェクトとして返します。<br>
     * 
     * @param url URL
     * @return JSONオブジェクト
     * @throws NullPointerException 引数に {@code null} が指定された場合
     */
    public static JSONObject getAsJson(String url) {
        Objects.requireNonNull(url);
        
        try {
            HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(url));
            request.setConnectTimeout(5000);
            request.setReadTimeout(5000);
            
            HttpResponse response = request.execute();
            String jsonString = response.parseAsString();
            return new JSONObject(jsonString);
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    // [instance members] ******************************************************
    
    private NetUtil() {
    }
}
