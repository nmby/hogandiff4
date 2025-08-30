package xyz.hotchpotch.hogandiff.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import xyz.hotchpotch.hogandiff.AppMain;

/**
 * GUIに関するユーティリティクラスです。<br>
 * 
 * @author nmby
 */
public class UIUtil {
    
    // [static members] ********************************************************
    
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * 指定されたURLを開くハイパーリンクを生成します。<br>
     * 
     * @param url URL
     * @return ハイパーリンク
     * @throws NullPointerException 引数がnullの場合
     */
    public static Hyperlink createHyperlink(String url) {
        Objects.requireNonNull(url);
        
        Hyperlink link = new Hyperlink(url);
        setupHyperlink(link, url);
        return link;
    }
    
    /**
     * 指定されたハイパーリンクに、指定されたURLを開くアクションを設定します。<br>
     * 
     * @param link ハイパーリンク
     * @param url URL
     * @throws NullPointerException 引数がnullの場合
     */
    public static void setupHyperlink(Hyperlink link, String url) {
        Objects.requireNonNull(link);
        Objects.requireNonNull(url);
        
        link.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (IOException e) {
                e.printStackTrace();
                new Alert(
                        AlertType.WARNING,
                        "%s%n%s".formatted(rb.getString("gui.component.LinkPane.010"), AppMain.WEB_URL),
                        ButtonType.OK)
                                .showAndWait();
            }
        });
    }
    
    // [instance members] ******************************************************
    
    private UIUtil() {
    }
}
