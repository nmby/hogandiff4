package xyz.hotchpotch.hogandiff.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import xyz.hotchpotch.hogandiff.Stats;

/**
 * Web API向けの通信を担うクラスです。<br>
 * 
 * @author nmby
 */
public class ApiClient {
    
    // [static members] ********************************************************
    
    private static final String endpointUrl = "https://api-hogandiff.hotchpotch.xyz/postStats";
    
    // [instance members] ******************************************************
    
    private final HttpClient httpClient = HttpClient.newHttpClient();
    
    /**
     * 比較実行結果の統計情報をWeb API向けにPOSTします。<br>
     * 
     * @param stats 比較実行結果の統計情報
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public void sendStatsAsync(Stats stats) {
        Objects.requireNonNull(stats);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(stats.toJsonString(), StandardCharsets.UTF_8))
                .build();
        
        CompletableFuture<HttpResponse<Void>> future = httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.discarding());
        
        future.thenAccept(response -> {
            if (response.statusCode() < 200 || 300 <= response.statusCode()) {
                System.err.println("Failed to send stats. Status code: " + response.statusCode());
            }
        }).exceptionally(e -> {
            System.err.println("An error occurred while sending stats: " + e.getMessage());
            return null;
        });
    }
}
