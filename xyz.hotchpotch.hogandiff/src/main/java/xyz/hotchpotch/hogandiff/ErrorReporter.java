package xyz.hotchpotch.hogandiff;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import org.json.JSONObject;

import xyz.hotchpotch.hogandiff.util.NetUtil;
import xyz.hotchpotch.hogandiff.util.function.UnsafeRunnable;

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
    
    /**
     * 処理を実行し、例外が発生した場合は {@link #reportIfEnabled(Throwable, String)}
     * を呼び出します。<br>
     * 
     * @param runnable
     *            処理
     * @param tag
     *            エラーレポート用のタグ
     * @throws Exception
     *             処理中に例外が発生した場合
     */
    public static void run(UnsafeRunnable runnable, String tag) throws Exception {
        try {
            runnable.run();
        } catch (Exception e) {
            reportIfEnabled(e, tag);
            throw e;
        }
        
    }
    
    /**
     * 処理を実行し、例外が発生した場合は {@link #reportIfEnabled(Throwable, String)}
     * を呼び出します。<br>
     * 
     * @param <T>
     *            戻り値の型
     * @param callable
     *            処理
     * @param tag
     *            エラーレポート用のタグ
     * @return 処理の戻り値
     * @throws Exception
     *             処理中に例外が発生した場合
     */
    public static <T> T call(Callable<T> callable, String tag) throws Exception {
        try {
            return callable.call();
        } catch (Exception e) {
            reportIfEnabled(e, tag);
            throw e;
        }
    }
    
    // [instance members] ******************************************************
    
    private ErrorReporter() {
    }
}
