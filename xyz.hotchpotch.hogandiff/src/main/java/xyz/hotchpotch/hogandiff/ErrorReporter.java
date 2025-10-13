package xyz.hotchpotch.hogandiff;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.json.JSONObject;

import xyz.hotchpotch.hogandiff.util.NetUtil;

/**
 * エラー情報をサーバに送信するためのユーティリティクラスです。<br>
 * 
 * @author nmby
 */
public class ErrorReporter {
    
    // [static members] ********************************************************
    
    private static final String errorReportUrl = "https://api.hogandiff.hotchpotch.info/errorReport";
    
    private static final AppResource ar = AppMain.appResource;
    
    /**
     * 指定された例外情報をサーバに送信します。<br>
     * 送信は非同期で行われ、メインスレッドをブロックしません。<br>
     * 
     * @param th
     *            例外情報
     * @param tag
     *            タグ（例外の発生箇所を特定するための文字列）
     * @throws NullPointerException
     *             パラメータに {@code null} が指定された場合
     */
    public static void reportIfEnabled(Throwable th, String tag) {
        reportIfEnabled(th, tag, Map.of());
    }
    
    /**
     * 指定された例外情報をサーバに送信します。<br>
     * 送信は非同期で行われ、メインスレッドをブロックしません。<br>
     * 
     * @param th
     *            例外情報
     * @param tag
     *            タグ（例外の発生箇所を特定するための文字列）
     * @param additionalContents
     *            追加情報（キーと値のペア）
     * @throws NullPointerException
     *             パラメータに {@code null} が指定された場合
     */
    public static void reportIfEnabled(Throwable th, String tag, Map<String, String> additionalContents) {
        Objects.requireNonNull(th);
        Objects.requireNonNull(tag);
        Objects.requireNonNull(additionalContents);
        
        th.printStackTrace();
        
        if (!ar.settings().get(SettingKeys.SEND_ERROR_INFO)) {
            return;
        }
        
        JSONObject postData = new JSONObject();
        postData.put("uuid", ar.settings().get(SettingKeys.CLIENT_UUID));
        postData.put("tag", tag);
        postData.put("timestamp", Instant.now().toString());
        postData.put("exceptionClass", th.getClass().getName());
        postData.put("message", th.getMessage());
        postData.put("stackTrace", getStackTraceAsString(th));
        additionalContents.forEach((key, value) -> postData.put(key, value));
        
        NetUtil.postDataAsync(errorReportUrl, postData)
                .thenAccept(response -> {
                    if (response.getStatusCode() < 200 || 300 <= response.getStatusCode()) {
                        System.err.println("例外レポート送信失敗: " + response.getStatusCode());
                    }
                    
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }
    
    private static String getStackTraceAsString(Throwable th) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        th.printStackTrace(pw);
        return sw.toString();
    }
    
    // [instance members] ******************************************************
    
    private ErrorReporter() {
    }
}
