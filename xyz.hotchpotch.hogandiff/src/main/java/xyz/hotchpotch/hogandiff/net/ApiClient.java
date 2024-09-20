package xyz.hotchpotch.hogandiff.net;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.Stats;

/**
 * Web API向けの通信を担うクラスです。<br>
 * 
 * @author nmby
 */
public class ApiClient {
    
    // [static members] ********************************************************
    
    private static final List<String> endpointUrls = List.of(
            "https://api.hogandiff.hotchpotch.info/postStats",
            "https://hogandiff-prod1.firebaseapp.com/postStats",
            "https://hogandiff-prod1.web.app/postStats");
    
    // [instance members] ******************************************************
    
    /**
     * 比較実行結果の統計情報をWeb API向けにPOSTします。<br>
     * 
     * @param stats 比較実行結果の統計情報
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public void sendStatsAsync(Stats stats) {
        Objects.requireNonNull(stats);
        
        HttpClient httpClient = HttpClient.newBuilder()
                .proxy(ProxySelector.getDefault())
                .build();
        
        Thread.startVirtualThread(() -> {
            for (String endpointUrl : endpointUrls) {
                boolean postResult = postStats(endpointUrl, httpClient, stats);
                if (postResult) {
                    return;
                }
                boolean getResult = getStats(endpointUrl, httpClient, stats);
                if (getResult) {
                    return;
                }
            }
            System.err.println("Failed to send stats.");
        });
    }
    
    private boolean postStats(String endpointUrl, HttpClient httpClient, Stats stats) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(stats.toJsonString(), StandardCharsets.UTF_8))
                    .build();
            
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return 200 <= response.statusCode() && response.statusCode() < 300;
            
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
    
    private boolean getStats(String endpointUrl, HttpClient httpClient, Stats stats) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl + "?" + stats.toUrlParamString()))
                    .GET()
                    .build();
            
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return 200 <= response.statusCode() && response.statusCode() < 300;
            
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }
}
