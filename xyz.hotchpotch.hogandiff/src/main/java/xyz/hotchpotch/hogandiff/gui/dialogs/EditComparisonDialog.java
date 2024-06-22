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
     * @param comparison 比較情報
     * @throws IOException ダイアログの構成に失敗した場合
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public EditComparisonDialog(T comparison) throws IOException {
        Objects.requireNonNull(comparison);
        
        @SuppressWarnings("unchecked")
        EditComparisonDialogPane<T> editComparisonDialogPane = (EditComparisonDialogPane<T>) switch (comparison) {
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
        
        editComparisonDialogPane.getStylesheets().add(
                getClass().getResource("editComparisonDialog.css").toExternalForm());
        
        DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(editComparisonDialogPane);
        dialogPane.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);
        
        setTitle(rb.getString("fx.EditComparisonPane.010"));
        setResizable(true);
        setResultConverter(buttonType -> buttonType == ButtonType.OK
                ? editComparisonDialogPane.getResult()
                : null);
    }
}
