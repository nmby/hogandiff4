package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.logic.google.GoogleCredential;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileInfo;
import xyz.hotchpotch.hogandiff.logic.google.GoogleHandlingException;

/**
 * ユーザーにGoogleドライブ上のファイル選択を求めるダイアログボックスです。<br>
 * 
 * @author nmby
 */
public class GoogleFilePickerDialog extends Dialog<GoogleFileInfo> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    /**
     * 新しいダイアログを構成します。<br>
     */
    public GoogleFilePickerDialog(
            GoogleFileInfo googleFileInfo,
            GoogleCredential credential)
            throws IOException {
        
        GoogleFilePickerDialogPane gdFilePickerDialogPane = new GoogleFilePickerDialogPane();
        gdFilePickerDialogPane.init(this, googleFileInfo, credential);
        
        DialogPane me = getDialogPane();
        me.setContent(gdFilePickerDialogPane);
        me.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);
        me.lookupButton(ButtonType.OK).disableProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> gdFilePickerDialogPane.fileMetadata.getValue() == null,
                        gdFilePickerDialogPane.fileMetadata));
        
        this.setTitle(rb.getString("fx.GoogleFilePickerDialog.010"));
        this.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    GoogleFileFetcher fetcher = GoogleFileFetcher.of(credential);
                    return fetcher.downloadFile(
                            gdFilePickerDialogPane.fileMetadata.getValue(),
                            gdFilePickerDialogPane.revisionChoiceBox.getValue().getRevisionId(),
                            ar.settings().get(SettingKeys.WORK_DIR_BASE).resolve("googleDrive"));
                    
                } catch (GoogleHandlingException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        });
        
        gdFilePickerDialogPane.fileUrlTextField.requestFocus();
    }
}
