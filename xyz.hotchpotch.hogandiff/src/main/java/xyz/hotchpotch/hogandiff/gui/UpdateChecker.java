package xyz.hotchpotch.hogandiff.gui;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.VersionMaster;
import xyz.hotchpotch.hogandiff.util.NetUtil;

/**
 * このアプリケーションの更新チェック機能を提供します。<br>
 * 
 * @author nmby
 */
public class UpdateChecker {
    
    // [static members] ********************************************************
    
    private static final AppResource ar = AppMain.appResource;
    private static final ResourceBundle rb = ar.get();
    
    /**
     * 更新チェックを実行します。<br>
     * {@code force} に {@code true} が指定されている場合は、強制的にチェックします。<br>
     * {@code force} に {@code false} が指定されている場合は、
     * ユーザーが更新チェックを無効にしている場合、過去数時間以内にチェックしている場合はスキップします。<br>
     * 
     * @param force 強制的にチェックする場合は {@code true}
     */
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
                    String latestVersion = json.getString("version");
                    if (VersionMaster.compareVersion(latestVersion) < 0) {
                        Platform.runLater(() -> {
                            Hyperlink link = UIUtil.createHyperlink(AppMain.WEB_URL);
                            VBox content = new VBox(10);
                            content.getChildren().addAll(
                                    new Label(rb.getString("gui.UpdateChecker.020")
                                            .formatted(VersionMaster.APP_VERSION, latestVersion)),
                                    link);
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle(rb.getString("AppMain.010"));
                            alert.setHeaderText(rb.getString("gui.UpdateChecker.010"));
                            alert.getDialogPane().setContent(content);
                            alert.showAndWait();
                        });
                    } else if (force) {
                        Platform.runLater(() -> {
                            new Alert(
                                    AlertType.INFORMATION,
                                    rb.getString("gui.UpdateChecker.030").formatted(VersionMaster.APP_VERSION),
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
}
