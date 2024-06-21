package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.BookCompareInfo;

/**
 * 比較対象の組み合わせを編集するためのダイアログボックスです。<br>
 * 
 * @author nmby
 */
public class EditCompareInfoDialog extends Dialog<BookCompareInfo> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    /**
     * 新しいダイアログを構成します。<br>
     * 
     * @param compareInfo 比較情報
     * @throws IOException ダイアログの構成に失敗した場合
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public EditCompareInfoDialog(BookCompareInfo compareInfo) throws IOException {
        Objects.requireNonNull(compareInfo);
        
        EditBookCompareInfoDialogPane editCompareInfoDialogPane = new EditBookCompareInfoDialogPane();
        editCompareInfoDialogPane.init(this, compareInfo);
        editCompareInfoDialogPane.getStylesheets().add(getClass().getResource("editCompareInfoDialog.css").toExternalForm());
        
        DialogPane me = getDialogPane();
        me.setContent(editCompareInfoDialogPane);
        me.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);
        
        this.setTitle(rb.getString("fx.EditCompareInfoPane.010"));
        this.setResizable(true);
        this.setResultConverter(buttonType -> buttonType == ButtonType.OK
                ? editCompareInfoDialogPane.getResult()
                : null);
    }
}
