package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher.GoogleFileMetadata;
import xyz.hotchpotch.hogandiff.logic.google.GoogleFileFetcher.RevisionMapper;

/**
 * ユーザーにGoogleドライブ上のファイルのバージョン選択を求めるダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class GoogleRevisionSelectorDialogPane extends GridPane {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private Label fileUrlLabel;
    
    @FXML
    private Label fileNameLabel;
    
    @FXML
    /* package */ ChoiceBox<RevisionMapper> revisionChoiceBox;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public GoogleRevisionSelectorDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("GoogleRevisionSelectorDialogPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * このダイアログボックス要素を初期化します。<br>
     * 
     * @param parent   親要素
     * @param metadata  Googleドライブファイルのメタデータ
     */
    /* package */ void init(GoogleFileMetadata metadata) {
        // 1.プロパティのバインディング
        // nop
        
        // 2.イベントハンドラの設定
        // nop
        
        // 3.初期値の設定
        fileUrlLabel.setText(metadata.fileId().url());
        fileNameLabel.setText(metadata.fileId().name());
        revisionChoiceBox.getItems().setAll(metadata.revisions());
        revisionChoiceBox.getSelectionModel().selectFirst();
    }
}
