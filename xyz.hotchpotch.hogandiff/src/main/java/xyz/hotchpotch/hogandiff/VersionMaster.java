package xyz.hotchpotch.hogandiff;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.function.Function;

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
    
    /** v0.27.0新機能アナウンス機能用変数 */
    public static Button for_v0_27_0;
    
    /**
     * このアプリの現在のバージョンと指定されたバージョンを比較します。<br>
     * 
     * @param comparisonTarget 比較対象のバージョン
     * @return 現在のバージョンの方が新しい場合は {@code 1}、同じ場合は {@code 0}、古い場合は {@code -1}
     */
    public static int compareVersion(String comparisonTarget) {
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
        
        int[] target = toVersionNumbers.apply(comparisonTarget);
        int[] me = toVersionNumbers.apply(APP_VERSION);
        for (int i = 0; i < target.length; i++) {
            if (me[i] > target[i]) {
                return 1;
            } else if (me[i] < target[i]) {
                return -1;
            }
        }
        return 0;
    }
    
    /**
     * ユーザーが現在のバージョンを初めて起動した際の処理を行います。<br>
     */
    public static void announceNewFeature1() {
        // 前回までの利用Versionを調べ、新バージョンの初回起動の場合は新バージョンに応じた処理を行う。
        String prevVersion = ar.settings().get(SettingKeys.APP_VERSION);
        if (prevVersion == null || compareVersion(prevVersion) > 0) {
            
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
        if (prevVersion == null || compareVersion(prevVersion) > 0) {
            
            assert APP_VERSION.equals("v0.27.0");
            // v0.27.0 では次を行う。
            //  ・設定エリアの強制展開
            //  ●詳細設定ダイアログの強制表示
            //  ●新機能紹介ページの表示
            
            try {
                if (for_v0_27_0 != null) {
                    Platform.runLater(() -> for_v0_27_0.fire());
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
