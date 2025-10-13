package xyz.hotchpotch.hogandiff.util;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;

import com.google.api.client.http.ByteArrayContent;
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
     * @param url
     *            URL
     * @return JSONオブジェクト
     * @throws NullPointerException
     *             引数に {@code null} が指定された場合
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
    
    /**
     * 指定されたURLにJSONオブジェクトの内容を送信します。<br>
     * 送信は非同期で行われ、メインスレッドをブロックしません。<br>
     * 送信に失敗してもアプリケーションの動作には影響しません。<br>
     * 
     * @param url
     *            送信先URL
     * @param json
     *            送信する内容
     * @return 非同期処理を表す {@link CompletableFuture} オブジェクト
     * @throws NullPointerException
     *             引数に {@code null} が指定された場合
     */
    public static CompletableFuture<HttpResponse> postDataAsync(String url, JSONObject json) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(json);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = requestFactory.buildPostRequest(
                        new GenericUrl(url),
                        ByteArrayContent.fromString("application/json; charset=UTF-8", json.toString()));
                request.setConnectTimeout(10000);
                request.setReadTimeout(10000);
                
                return request.execute();
                
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    // [instance members] ******************************************************
    
    private NetUtil() {
    }
}