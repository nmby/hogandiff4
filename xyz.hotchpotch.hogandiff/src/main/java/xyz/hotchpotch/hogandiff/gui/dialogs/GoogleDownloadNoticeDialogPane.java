package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.ErrorReporter;
import xyz.hotchpotch.hogandiff.SettingKeys;

/**
 * Googleドライブからのダウンロードに関する注意事項を表示するダイアログボックス要素です。<br>
 * 
 * @author nmby
 */
public class GoogleDownloadNoticeDialogPane extends VBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    @FXML
    private Label msgLabel;
    
    @FXML
    private Hyperlink localDirHyperlink;
    
    @FXML
    private CheckBox dontShowNoMoreCheckBox;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public GoogleDownloadNoticeDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GoogleDownloadNoticeDialogPane.fxml"), ar.get());
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * このダイアログボックス要素を初期化します。<br>
     */
    public void init() {
        try {
            // 1.プロパティのバインディング
            
            // 2.イベントハンドラの設定
            Path localDir = ar.settings().get(SettingKeys.WORK_DIR_BASE).resolve("googleDrive");
            localDirHyperlink.setOnAction(_ -> {
                try {
                    Desktop.getDesktop().open(localDir.toFile());
                } catch (IOException e1) {
                    ErrorReporter.reportIfEnabled(e1, "GoogleDownloadNoticeDialogPane::init-1");
                }
            });
            
            dontShowNoMoreCheckBox.setOnAction(_ -> ar.changeSetting(
                    SettingKeys.SHOW_GOOGLE_DL_NOTICE,
                    !dontShowNoMoreCheckBox.isSelected()));
            
            // 3.初期値の設定
            localDirHyperlink.setText(localDir.toString());
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "GoogleDownloadNoticeDialogPane::init-1");
            throw e;
        }
    }
}
