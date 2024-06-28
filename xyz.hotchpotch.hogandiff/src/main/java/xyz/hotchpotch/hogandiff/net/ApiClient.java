package xyz.hotchpotch.hogandiff.net;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import xyz.hotchpotch.hogandiff.Report;

/**
 * Web API向けの通信を担うクラスです。<br>
 * 
 * @author nmby
 */
public class ApiClient {
    
    // [static members] ********************************************************
    
    private static final String endpointUrl = "https://asia-northeast1-hogandiff-prod1.cloudfunctions.net/postStats";
    
    // [instance members] ******************************************************
    
    /**
     * 比較実行結果レポートをWeb API向けにPOSTします。<br>
     * 
     * @param report 比較実行結果レポート
     */
    public void sendStatsAsync(Report report) {
        Thread.startVirtualThread(() -> {
            try {
                URI uri = new URI(endpointUrl);
                HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = report.toJsonString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                conn.getResponseCode();
                
            } catch (Exception e) {
                e.printStackTrace();
                // nop
            }
        });
    }
}
