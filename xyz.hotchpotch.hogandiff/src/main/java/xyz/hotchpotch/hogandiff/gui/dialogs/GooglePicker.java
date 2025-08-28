package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONObject;

import com.sun.net.httpserver.HttpServer;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.logic.google.GoogleCredential;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher.GoogleFileMetadata;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo.GoogleFileId;
import xyz.hotchpotch.hogandiff.logic.google.GoogleHandlingException;
import xyz.hotchpotch.hogandiff.util.EnvConfig;

public class GooglePicker {
    
    // [static members] ********************************************************
    
    private static final AppResource ar = AppMain.appResource;
    private static final ResourceBundle rb = ar.get();
    
    private static final String API_KEY = EnvConfig.get("GOOGLE_PICKER_API_KEY");
    private static final String APP_ID = EnvConfig.get("GOOGLE_CLOUD_PROJECT_ID");
    private static final int PICKER_TIMEOUT_SECONDS = 300;
    
    private static String getHtml(String accessToken) {
        return """
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
                                const docsView = new google.picker.DocsView(google.picker.ViewId.DOCS)
                                    .setIncludeFolders(true)
                                    .setMimeTypes(
                                        'application/vnd.google-apps.spreadsheet,' +
                                        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,' +
                                        'application/vnd.ms-excel,' +
                                        'application/vnd.ms-excel.sheet.macroenabled.12'
                                    )
                                    .setMode(google.picker.DocsViewMode.LIST);
                
                                const picker = new google.picker.PickerBuilder()
                                    .setOAuthToken('%s')
                                    .setDeveloperKey('%s')
                                    .setAppId('%s')
                                    .addView(docsView)
                                    .setTitle('%s')
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
                                        url: file.url,
                                        name: file.name,
                                        mimeType: file.mimeType
                                    })
                                }).then(() => window.close());
                            } else if (data.action == google.picker.Action.CANCEL) {
                                fetch('/callback', {
                                    method: 'POST',
                                    headers: {'Content-Type': 'application/json'},
                                    body: JSON.stringify({
                                        cancelled: true
                                    })
                                }).then(() => window.close());
                            }
                        }
                
                        window.addEventListener('beforeunload', function(event) {
                            navigator.sendBeacon('/callback', JSON.stringify({
                                cancelled: true
                            }));
                        });
                
                        window.onload = function() {
                            gapi.load('picker', {'callback': onPickerApiLoad});
                        };
                    </script>
                </body>
                </html>
                """.formatted(
                        accessToken,
                        API_KEY, APP_ID,
                        rb.getString("fx.GoogleFilePickerDialog.020"));
    }
    
    // [instance members] ******************************************************
    
    private HttpServer server;
    private CompletableFuture<String> fileSelectionFuture;
    
    public CompletableFuture<GoogleFileInfo> downloadAndGetFileInfo() throws GoogleHandlingException {
        try {
            return openPicker()
                    .thenCompose(fileId -> {
                        if (fileId == null) {
                            return CompletableFuture.completedFuture(null);
                        }
                        
                        // メタデータ取得（バックグラウンドスレッドで実行可能）
                        CompletableFuture<GoogleFileMetadata> metadataFuture = CompletableFuture.supplyAsync(() -> {
                            try {
                                GoogleFileFetcher fetcher = new GoogleFileFetcher();
                                return fetcher.fetchMetadata(fileId);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        
                        // ダイアログ表示（JavaFXアプリケーションスレッドで実行）
                        return metadataFuture.thenCompose(metadata -> {
                            CompletableFuture<GoogleFileInfo> dialogFuture = new CompletableFuture<>();
                            
                            Platform.runLater(() -> {
                                try {
                                    GoogleRevisionSelectorDialog dialog = new GoogleRevisionSelectorDialog(metadata);
                                    Optional<GoogleFileInfo> modified = dialog.showAndWait();
                                    dialogFuture.complete(modified.orElse(null));
                                } catch (Exception e) {
                                    dialogFuture.completeExceptionally(e);
                                }
                            });
                            
                            return dialogFuture;
                        });
                    })
                    .thenApply(fileInfo -> {
                        if (fileInfo != null & ar.settings().get(SettingKeys.SHOW_GOOGLE_DL_NOTICE)) {
                            Platform.runLater(() -> {
                                try {
                                    GoogleDownloadNoticeDialogPane content = new GoogleDownloadNoticeDialogPane();
                                    content.init();
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle(rb.getString("AppMain.010"));
                                    alert.setHeaderText(rb.getString("gui.component.GooglePane.090"));
                                    alert.getDialogPane().setContent(content);
                                    alert.showAndWait();
                                    
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    // nop
                                }
                            });
                        }
                        return fileInfo;
                    });
            
        } catch (Exception e) {
            throw new GoogleHandlingException(e);
        }
    }
    
    /**
     * Google Pickerを開いてファイル選択結果を取得
     * @param credential Google認証情報
     * @return 選択されたファイルの情報
     */
    private CompletableFuture<GoogleFileId> openPicker() {
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
                .thenCompose(v -> fileSelectionFuture
                        .orTimeout(PICKER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .handle((result, throwable) -> {
                            if (throwable instanceof TimeoutException) {
                                // タイムアウト時はキャンセル扱い
                                return null;
                            } else if (throwable != null) {
                                throw new RuntimeException(throwable);
                            }
                            return result;
                        }))
                .whenComplete((result, error) -> {
                    stopServer();
                    fileSelectionFuture = null;
                })
                .thenApply(jsonResult -> {
                    if (jsonResult == null) {
                        return null;
                    }
                    JSONObject jsonObject = new JSONObject(jsonResult);
                    
                    if (jsonObject.has("cancelled")) {
                        return null;
                    } else {
                        String id = jsonObject.get("id").toString();
                        String url = jsonObject.get("url").toString();
                        String name = jsonObject.get("name").toString();
                        String mimeType = jsonObject.get("mimeType").toString();
                        return new GoogleFileId(id, url, name, mimeType);
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
            String html = getHtml(credential.credential().getAccessToken());
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