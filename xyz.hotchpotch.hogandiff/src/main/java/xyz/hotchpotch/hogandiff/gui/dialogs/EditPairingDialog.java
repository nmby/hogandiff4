package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.CompareInfo;

/**
 * 比較対象の組み合わせを編集するためのダイアログボックスです。<br>
 * 
 * @param <T> 比較情報の型
 * @author nmby
 */
public class EditPairingDialog<T extends CompareInfo<?, ?, ?>> extends Dialog<String> {
    
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
    public EditPairingDialog(T compareInfo) throws IOException {
        Objects.requireNonNull(compareInfo);
        
        EditPairingDialogPane editPairingDialogPane = new EditPairingDialogPane();
        editPairingDialogPane.init(this, compareInfo);
        
        DialogPane me = getDialogPane();
        me.setContent(editPairingDialogPane);
        me.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);
        
        this.setTitle("組み合わせ変更");
        this.setResultConverter(buttonType -> buttonType == ButtonType.OK
                ? "OK!!!"
                : null);
    }
}
