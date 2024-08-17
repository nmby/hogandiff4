package xyz.hotchpotch.hogandiff.net;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import xyz.hotchpotch.hogandiff.Stats;

/**
 * Web API向けの通信を担うクラスです。<br>
 * 
 * @author nmby
 */
public class ApiClient {
    
    // [static members] ********************************************************
    
    private static final String endpointUrl = "https://api.hogandiff.hotchpotch.info/postStats";
    
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
            boolean postResult = postStats(stats, httpClient);
            if (postResult) {
                return;
            }
            boolean getResult = getStats(stats, httpClient);
            if (getResult) {
                return;
            }
            System.err.println("Failed to send stats.");
        });
    }
    
    private boolean postStats(Stats stats, HttpClient httpClient) {
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
    
    private boolean getStats(Stats stats, HttpClient httpClient) {
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
