package xyz.hotchpotch.hogandiff.gui;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.NetUtil;

public class UpdateChecker {
    
    // [static members] ********************************************************
    
    private static final AppResource ar = AppMain.appResource;
    private static final ResourceBundle rb = ar.get();
    
    public static void execute(boolean force) {
        UpdateChecker checker = new UpdateChecker();
        checker.checkUpdate(force);
    }
    
    // [instance members] ******************************************************
    
    private UpdateChecker() {
    }
    
    private void checkUpdate(boolean force) {
        if (!force) {
            if (!ar.settings().get(SettingKeys.CHECK_UPDATES)) {
                return;
            }
            
            Instant lastCheckAt = ar.settings().get(SettingKeys.LAST_CHECK_UPDATES);
            int interval = ar.settings().get(SettingKeys.CHECK_UPDATES_INTERVAL_HOURS);
            if (lastCheckAt != null && Instant.now().isBefore(lastCheckAt.plus(interval, ChronoUnit.HOURS))) {
                return;
            }
        }
        
        CompletableFuture
                .supplyAsync(() -> NetUtil.getAsJson("https://nmby.github.io/hogandiff4/api/versions/latest"))
                .thenAccept(json -> {
                    if (!amILatest(json.getString("version"))) {
                        Platform.runLater(() -> {
                            new Alert(
                                    AlertType.INFORMATION,
                                    // TODO: コンテンツのリッチ化
                                    "最新バージョンがあります。",
                                    ButtonType.OK)
                                            .showAndWait();
                        });
                    } else if (force) {
                        Platform.runLater(() -> {
                            new Alert(
                                    AlertType.INFORMATION,
                                    "新規バージョンはありません。",
                                    ButtonType.OK)
                                            .showAndWait();
                        });
                    }
                    ar.changeSetting(SettingKeys.LAST_CHECK_UPDATES, Instant.now());
                })
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    return null;
                });
    }
    
    private boolean amILatest(String latestVersion) {
        Function<String, int[]> toVersionNumbers = (v) -> {
            String[] parts = v.replace("v", "").split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid version string: " + v);
            }
            int[] numbers = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                numbers[i] = Integer.parseInt(parts[i]);
            }
            return numbers;
        };
        
        int[] latest = toVersionNumbers.apply(latestVersion);
        int[] current = toVersionNumbers.apply(AppMain.VERSION);
        for (int i = 0; i < latest.length; i++) {
            if (latest[i] > current[i]) {
                return false;
            } else if (latest[i] < current[i]) {
                return true;
            }
        }
        return true;
    }
}
