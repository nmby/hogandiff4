package xyz.hotchpotch.hogandiff.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
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
    
    public static Hyperlink createHyperlink(String url) {
        Hyperlink link = new Hyperlink(url);
        setupHyperlink(link, url);
        return link;
    }
    
    public static void setupHyperlink(Hyperlink link, String url) {
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
