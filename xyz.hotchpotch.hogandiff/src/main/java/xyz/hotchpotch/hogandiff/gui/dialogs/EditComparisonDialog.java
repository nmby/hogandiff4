package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.BookInfoComparison;
import xyz.hotchpotch.hogandiff.excel.Comparison;
import xyz.hotchpotch.hogandiff.excel.DirInfoComparison;

/**
 * 比較対象の組み合わせを編集するためのダイアログボックスです。<br>
 * 
 * @param <T> 比較情報の型
 * @author nmby
 */
public class EditComparisonDialog<T extends Comparison> extends Dialog<T> {
    
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
    public EditComparisonDialog(T compareInfo) throws IOException {
        Objects.requireNonNull(compareInfo);
        
        @SuppressWarnings("unchecked")
        EditComparisonDialogPane<T> editCompareInfoDialogPane = (EditComparisonDialogPane<T>) switch (compareInfo) {
            case BookInfoComparison bookInfoComparison -> {
                EditBookInfoComparisonDialogPane pane = new EditBookInfoComparisonDialogPane(bookInfoComparison);
                pane.init();
                yield pane;
            }
            case DirInfoComparison dirInfoComparison -> {
                EditDirInfoComparisonDialogPane pane = new EditDirInfoComparisonDialogPane(dirInfoComparison);
                pane.init();
                yield pane;
            }
        };
        
        editCompareInfoDialogPane.getStylesheets().add(
                getClass().getResource("editCompareInfoDialog.css").toExternalForm());
        
        DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(editCompareInfoDialogPane);
        dialogPane.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);
        
        setTitle(rb.getString("fx.EditCompareInfoPane.010"));
        setResizable(true);
        setResultConverter(buttonType -> buttonType == ButtonType.OK
                ? editCompareInfoDialogPane.getResult()
                : null);
    }
}
