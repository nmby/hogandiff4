package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;

import javafx.beans.binding.Bindings;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.logic._google.GoogleCredential;
import xyz.hotchpotch.hogandiff.logic._google.GoogleFileFetcher;
import xyz.hotchpotch.hogandiff.logic._google.GoogleFileInfo;
import xyz.hotchpotch.hogandiff.logic._google.GoogleHandlingException;

/**
 * ユーザーにGoogleドライブ上のファイル選択を求めるダイアログボックスです。<br>
 * 
 * @author nmby
 */
public class GoogleFilePickerDialog extends Dialog<GoogleFileInfo> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final AppResource ar = AppMain.appResource;
    //private final ResourceBundle rb = ar.get();
    
    /**
     * 新しいダイアログを構成します。<br>
     */
    public GoogleFilePickerDialog(
            String fileUrl,
            GoogleCredential credential)
            throws IOException {
        
        GoogleFilePickerDialogPane gdFilePickerDialogPane = new GoogleFilePickerDialogPane();
        gdFilePickerDialogPane.init(this, fileUrl, credential);
        
        DialogPane me = getDialogPane();
        me.setContent(gdFilePickerDialogPane);
        me.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);
        me.lookupButton(ButtonType.OK).disableProperty()
                .bind(Bindings.createBooleanBinding(
                        () -> gdFilePickerDialogPane.fileMetadata.getValue() == null,
                        gdFilePickerDialogPane.fileMetadata));
        
        this.setTitle("Googleドライブ ファイル選択");
        this.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    GoogleFileFetcher fetcher = GoogleFileFetcher.of(credential);
                    return fetcher.downloadFile(
                            gdFilePickerDialogPane.fileMetadata.getValue(),
                            gdFilePickerDialogPane.revisionChoiceBox.getValue().getRevisionId(),
                            ar.settings().get(SettingKeys.WORK_DIR_BASE).resolve("googleDrive"));
                    
                } catch (GoogleHandlingException e) {
                    // TODO 自動生成された catch ブロック
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
