package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpServer;

import xyz.hotchpotch.hogandiff.logic.google.GoogleCredential;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher.GoogleFileMetadata2;
import xyz.hotchpotch.hogandiff.logic.google.GoogleHandlingException;

public class GooglePicker {
    
    // [static members] ********************************************************
    
    public static record GoogleFileId(
            String id,
            String name,
            String mimeType) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
    }
    
    private static final String API_KEY = "AIzaSyBR3oJ2VekRl3NlmoXVmE9KXyXA1zEIDVk";
    
    private static final String HTML = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <script src="https://apis.google.com/js/api.js"></script>
            </head>
            <body>
                <script>
                    let pickerApiLoaded = false;
            
                    function onApiLoad() {
                        gapi.load('picker', {'callback': onPickerApiLoad});
                    }
            
                    function onPickerApiLoad() {
                        pickerApiLoaded = true;
                        createPicker();
                    }
            
                    function createPicker() {
                        if (pickerApiLoaded) {
                            const picker = new google.picker.PickerBuilder()
                                .setOAuthToken('%s')
                                .addView(new google.picker.DocsView())
                                .setDeveloperKey('%s')
                                .setCallback(pickerCallback)
                                .build();
                            picker.setVisible(true);
                        }
                    }
            
                    function pickerCallback(data) {
                        if (data.action == google.picker.Action.PICKED) {
                            const file = data.docs[0];
                            console.log('Selected file:', file);
                            fetch('/callback', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({
                                    id: file.id,
                                    name: file.name,
                                    mimeType: file.mimeType
                                })
                            }).then(() => window.close());
                        } else if (data.action == google.picker.Action.CANCEL) {
                            // キャンセル時もサーバーに通知
                            fetch('/callback', {
                                method: 'POST',
                                headers: {'Content-Type': 'application/json'},
                                body: JSON.stringify({
                                    cancelled: true
                                })
                            }).then(() => window.close());
                        }
                    }
            
                    window.onload = function() {
                        gapi.load('picker', {'callback': onPickerApiLoad});
                    };
                </script>
            </body>
            </html>
            """;
    
    // [instance members] ******************************************************
    
    private HttpServer server;
    private CompletableFuture<String> fileSelectionFuture;
    
    public CompletableFuture<GoogleFileMetadata2> getMetadata() {
        return openPicker()
                .thenApply(fileId -> {
                    if (fileId == null) {
                        return null;
                    }
                    GoogleFileFetcher fetcher = new GoogleFileFetcher();
                    try {
                        return fetcher.fetchMetadata2(fileId);
                    } catch (GoogleHandlingException e) {
                        throw new RuntimeException("Failed to fetch file metadata", e);
                    }
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
        
    }
    
    /**
     * Google Pickerを開いてファイル選択結果を取得
     * @param credential Google認証情報
     * @return 選択されたファイルの情報
     */
    public CompletableFuture<GoogleFileId> openPicker() {
        if (fileSelectionFuture != null && !fileSelectionFuture.isDone()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Picker is already open."));
        }
        fileSelectionFuture = new CompletableFuture<>();
        
        return CompletableFuture
                .runAsync(() -> {
                    try {
                        startServer();
                        Desktop.getDesktop().browse(new URI("http://localhost:" + server.getAddress().getPort()));
                    } catch (Exception e) {
                        fileSelectionFuture.completeExceptionally(e);
                        throw new RuntimeException(e);
                    }
                })
                .thenCompose(v -> fileSelectionFuture)
                .whenComplete((result, error) -> {
                    stopServer();
                    fileSelectionFuture = null;
                })
                .thenApply(jsonResult -> {
                    JSONObject jsonObject = new JSONObject(jsonResult);
                    
                    if (jsonObject.has("cancelled")) {
                        return null;
                    } else {
                        String id = jsonObject.get("id").toString();
                        String name = jsonObject.get("name").toString();
                        String mimeType = jsonObject.get("mimeType").toString();
                        return new GoogleFileId(id, name, mimeType);
                    }
                });
    }
    
    private void startServer() throws IOException {
        if (server != null) {
            throw new IllegalStateException("Server is already running.");
        }
        GoogleCredential credential = GoogleCredential.get(false);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        
        server.createContext("/", exchange -> {
            String html = HTML.formatted(
                    credential.credential().getAccessToken(),
                    API_KEY);
            
            byte[] response = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        
        server.createContext("/callback", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes());
                
                if (fileSelectionFuture != null && !fileSelectionFuture.isDone()) {
                    fileSelectionFuture.complete(body);
                }
                
                exchange.sendResponseHeaders(200, 0);
            } else {
                exchange.sendResponseHeaders(204, -1);
            }
            exchange.close();
        });
        
        server.start();
    }
    
    private void stopServer() {
        CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> {
            if (server != null) {
                server.stop(0);
                server = null;
            }
        });
    }
}