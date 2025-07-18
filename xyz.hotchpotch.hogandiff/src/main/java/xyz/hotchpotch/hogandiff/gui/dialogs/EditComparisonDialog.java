package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.logic.models.PairingInfo;
import xyz.hotchpotch.hogandiff.logic.models.PairingInfoBooks;
import xyz.hotchpotch.hogandiff.logic.models.PairingInfoDirs;

/**
 * 比較対象の組み合わせを編集するためのダイアログボックスです。<br>
 * 
 * @param <T> 比較情報の型
 * @author nmby
 */
public class EditComparisonDialog<T extends PairingInfo> extends Dialog<T> {

    // static members **********************************************************

    // instance members ********************************************************

    private final ResourceBundle rb = AppMain.appResource.get();

    /**
     * 新しいダイアログを構成します。<br>
     * 
     * @param comparison 比較情報
     * @throws IOException          ダイアログの構成に失敗した場合
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public EditComparisonDialog(T comparison) throws IOException {
        Objects.requireNonNull(comparison);

        @SuppressWarnings("unchecked")
        EditComparisonDialogPane<T> editComparisonDialogPane = (EditComparisonDialogPane<T>) switch (comparison) {
            case PairingInfoBooks bookComparison -> {
                EditBookComparisonDialogPane pane = new EditBookComparisonDialogPane(bookComparison);
                pane.init();
                yield pane;
            }
            case PairingInfoDirs dirComparison -> {
                EditDirComparisonDialogPane pane = new EditDirComparisonDialogPane(dirComparison);
                pane.init();
                yield pane;
            }
        };

        editComparisonDialogPane.getStylesheets().add(
                getClass().getResource("editComparisonDialog.css").toExternalForm());

        widthProperty().addListener((target, oldValue, newValue) -> {
            editComparisonDialogPane.setMaxWidth(newValue.doubleValue() - 20);
            editComparisonDialogPane.setMinWidth(newValue.doubleValue() - 20);
        });

        setTitle(rb.getString("fx.EditComparisonPane.010"));
        setResizable(true);
        setResultConverter(buttonType -> buttonType == ButtonType.OK
                ? editComparisonDialogPane.getResult()
                : null);

        DialogPane dialogPane = getDialogPane();
        dialogPane.setContent(editComparisonDialogPane);
        dialogPane.getButtonTypes().setAll(
                ButtonType.OK,
                ButtonType.CANCEL);
    }
}
