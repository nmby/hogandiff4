package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher.GoogleFileMetadata2;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo;
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
    private final ResourceBundle rb = ar.get();
    
    /**
     * 新しいダイアログを構成します。<br>
     */
    public GoogleRevisionSelectorDialog(GoogleFileMetadata2 metadata) throws IOException {
        GoogleRevisionSelectorDialogPane dialogPane = new GoogleRevisionSelectorDialogPane();
        dialogPane.init(metadata);
        
        DialogPane me = getDialogPane();
        me.setContent(dialogPane);
        me.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);
        
        this.setTitle(rb.getString("fx.GoogleFilePickerDialog.010"));
        this.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    GoogleFileFetcher fetcher = new GoogleFileFetcher();
                    return fetcher.downloadFile2(
                            metadata,
                            dialogPane.revisionChoiceBox.getValue().getRevisionId(),
                            ar.settings().get(SettingKeys.WORK_DIR_BASE).resolve("googleDrive"));
                    
                } catch (GoogleHandlingException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        });
    }
}
