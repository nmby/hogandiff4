package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo.GoogleMetadata;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo.GoogleRevision;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileType;
import xyz.hotchpotch.hogandiff.logic.google.GoogleHandlingException;
import xyz.hotchpotch.hogandiff.util.EnvConfig;

/**
 * Google Pickerを操作するためのユーティリティクラスです。<br>
 * 
 * @author nmby
 */
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
                                const docsView1 = new google.picker.DocsView(google.picker.ViewId.DOCS)
                                    .setLabel('%s')
                                    .setEnableDrives(true)
                                    .setMimeTypes(
                                        'application/vnd.google-apps.spreadsheet,' +
                                        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,' +
                                        'application/vnd.ms-excel,' +
                                        'application/vnd.ms-excel.sheet.macroenabled.12'
                                    )
                                    .setMode(google.picker.DocsViewMode.LIST);
                
                                const docsView2 = new google.picker.DocsView(google.picker.ViewId.DOCS)
                                    .setLabel('%s')
                                    .setMimeTypes(
                                        'application/vnd.google-apps.spreadsheet,' +
                                        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,' +
                                        'application/vnd.ms-excel,' +
                                        'application/vnd.ms-excel.sheet.macroenabled.12'
                                    )
                                    .setMode(google.picker.DocsViewMode.LIST);
                
                                const docsView3 = new google.picker.DocsView(google.picker.ViewId.DOCS)
                                    .setLabel('%s')
                                    .setStarred(true)
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
                                    .addView(docsView1)
                                    .addView(docsView2)
                                    .addView(docsView3)
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
                                }).then(() => {
                                    if (window.opener) {
                                        window.opener.focus();
                                    }
                                    setTimeout(() => window.close(), 100);
                                });
                            } else if (data.action == google.picker.Action.CANCEL) {
                                fetch('/callback', {
                                    method: 'POST',
                                    headers: {'Content-Type': 'application/json'},
                                    body: JSON.stringify({
                                        cancelled: true
                                    })
                                }).then(() => {
                                    if (window.opener) {
                                        window.opener.focus();
                                    }
                                    setTimeout(() => window.close(), 100);
                                });
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
                rb.getString("fx.GoogleFilePickerDialogPane.070"),
                rb.getString("fx.GoogleFilePickerDialogPane.080"),
                rb.getString("fx.GoogleFilePickerDialogPane.090"),
                accessToken,
                API_KEY, APP_ID,
                rb.getString("fx.GoogleFilePickerDialog.020"));
    }
    
    // [instance members] ******************************************************
    
    private HttpServer server;
    private CompletableFuture<String> fileSelectionFuture;
    
    /**
     * Google Pickerを開き、選択されたファイルの情報を取得します。<br>
     * 
     * @return 選択されたファイルの情報。キャンセルされた場合は {@code null}
     * @throws GoogleHandlingException ファイル情報の取得に失敗した場合
     */
    public CompletableFuture<GoogleFileInfo> downloadAndGetFileInfo() throws GoogleHandlingException {
        try {
            return openPicker()
                    .thenCompose(metadata -> {
                        if (metadata == null) {
                            return CompletableFuture.completedFuture(null);
                        }
                        
                        // メタデータ取得（バックグラウンドスレッドで実行可能）
                        CompletableFuture<List<GoogleRevision>> metadataFuture = CompletableFuture.supplyAsync(() -> {
                            try {
                                GoogleFileFetcher fetcher = new GoogleFileFetcher();
                                return fetcher.fetchRevisions(metadata.id());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        
                        // ダイアログ表示（JavaFXアプリケーションスレッドで実行）
                        return metadataFuture.thenCompose(revisions -> {
                            CompletableFuture<GoogleFileInfo> dialogFuture = new CompletableFuture<>();
                            
                            Platform.runLater(() -> {
                                try {
                                    GoogleRevisionSelectorDialog dialog = new GoogleRevisionSelectorDialog(metadata,
                                            revisions);
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
    private CompletableFuture<GoogleMetadata> openPicker() {
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
                    
                    Platform.runLater(() -> {
                        AppMain.stage.toFront();
                        AppMain.stage.requestFocus();
                    });
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
                        GoogleFileType type = calcType(jsonObject.get("mimeType").toString(), name);
                        return new GoogleMetadata(id, url, name, type);
                    }
                });
    }
    
    private GoogleFileType calcType(String mimeType, String fileName) {
        // .xlsm ファイルはGoogleドライブ上では GoogleFileType.EXCEL_XLSX として管理されるようなので
        // ファイル名に基づいて補正する。
        GoogleFileType candidateType = GoogleFileType.of(mimeType);
        return candidateType == GoogleFileType.EXCEL_XLSX && fileName.endsWith(".xlsm")
                ? GoogleFileType.EXCEL_XLSM
                : candidateType;
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