package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.List;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.ErrorReporter;
import xyz.hotchpotch.hogandiff.Msg;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo.GoogleMetadata;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo.GoogleRevision;
import xyz.hotchpotch.hogandiff.logic.google.GoogleHandlingException;

/**
 * ユーザーにGoogleドライブ上のファイルのバージョン選択を求めるダイアログボックスです。<br>
 * 
 * @author nmby
 */
public class GoogleRevisionSelectorDialog extends Dialog<GoogleFileInfo> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    /**
     * 新しいダイアログを構成します。<br>
     * 
     * @param metadata
     *            Googleドライブ上のファイルのメタデータ
     * @param revisions
     *            Googleドライブ上のファイルのバージョン一覧
     * @throws IOException
     *             処理に失敗した場合
     */
    public GoogleRevisionSelectorDialog(
            GoogleMetadata metadata,
            List<GoogleRevision> revisions)
            throws IOException {
        
        try {
            GoogleRevisionSelectorDialogPane dialogPane = new GoogleRevisionSelectorDialogPane();
            dialogPane.init(metadata, revisions);
            
            DialogPane me = getDialogPane();
            me.setContent(dialogPane);
            me.getButtonTypes().setAll(
                    ButtonType.OK,
                    ButtonType.CANCEL);
            
            this.setTitle(Msg.APP_0870.get());
            this.setResizable(true);
            
            this.setResultConverter(buttonType -> {
                if (buttonType == ButtonType.OK) {
                    try {
                        GoogleFileFetcher fetcher = new GoogleFileFetcher();
                        return fetcher.downloadFile(
                                metadata,
                                revisions,
                                dialogPane.revisionChoiceBox.getValue().id(),
                                ar.settings().get(SettingKeys.WORK_DIR_BASE).resolve("googleDrive"));
                        
                    } catch (GoogleHandlingException e) {
                        new Alert(
                                AlertType.ERROR,
                                "%s%n%s".formatted(Msg.APP_0920.get(), e.getMessage()),
                                ButtonType.OK)
                                        .showAndWait();
                        
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    return null;
                }
            });
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "GoogleRevisionSelectorDialog#<init>-1");
            throw e;
        }
    }
}
