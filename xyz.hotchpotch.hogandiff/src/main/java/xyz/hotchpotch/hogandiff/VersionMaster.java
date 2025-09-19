package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import javafx.application.Platform;
import javafx.scene.control.Button;

/**
 * このアプリケーションのバージョン情報を管理するクラスです。<br>
 * 
 * @author nmby
 */
public class VersionMaster {
    
    // [static members] ********************************************************
    
    private static final AppResource ar = AppMain.appResource;
    
    /** このアプリケーションのバージョン */
    public static final String APP_VERSION = "v0.27.0";
    
    /**
     * ユーザーが現在のバージョンを初めて起動した際の処理を行います。<br>
     */
    public static void announceNewFeature1() {
        // UUIDが未採番の場合は採番する。
        UUID uuid = ar.settings().get(SettingKeys.CLIENT_UUID);
        if (uuid == null) {
            ar.changeSetting(SettingKeys.CLIENT_UUID, UUID.randomUUID());
        }
        
        // 前回までの利用Versionを調べ、新バージョンの初回起動の場合は新バージョンに応じた処理を行う。
        String prevVersion = ar.settings().get(SettingKeys.APP_VERSION);
        if (!APP_VERSION.equals(prevVersion)) {
            
            assert APP_VERSION.equals("v0.27.0");
            // v0.27.0 では次を行う。
            //  ●設定エリアの強制展開
            //  ・詳細設定ダイアログの強制表示
            //  ・新機能紹介ページの表示
            ar.changeSetting(SettingKeys.SHOW_SETTINGS, true);
        }
    }
    
    /**
     * ユーザーが現在のバージョンを初めて起動した際の処理を行います。<br>
     */
    public static void announceNewFeature2() {
        // 前回までの利用Versionを調べ、新バージョンの初回起動の場合は新バージョンに応じた処理を行う。
        String prevVersion = ar.settings().get(SettingKeys.APP_VERSION);
        if (!APP_VERSION.equals(prevVersion)) {
            
            assert APP_VERSION.equals("v0.27.0");
            // v0.27.0 では次を行う。
            //  ・設定エリアの強制展開
            //  ●詳細設定ダイアログの強制表示
            //  ●新機能紹介ページの表示
            
            try {
                Button detailsButton = ar.settings().get(SettingKeys.V0_27_0_NOTICE);
                if (detailsButton != null) {
                    Platform.runLater(() -> detailsButton.fire());
                }
                Desktop.getDesktop().browse(URI.create("https://hogandiff.hotchpotch.xyz/releasenotes/v0-27-0/"));
                
            } catch (IOException e) {
                e.printStackTrace();
                // nop
            }
            ar.changeSetting(SettingKeys.APP_VERSION, APP_VERSION);
        }
    }
    
    // [instance members] ******************************************************
    
    private VersionMaster() {
    }
}
