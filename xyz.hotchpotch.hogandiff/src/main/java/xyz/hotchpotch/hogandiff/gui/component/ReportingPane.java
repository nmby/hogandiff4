package xyz.hotchpotch.hogandiff.gui.component;

import java.io.IOException;
import java.util.Objects;
import java.util.ResourceBundle;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;

/**
 * レポート表示部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class ReportingPane extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private ProgressBar reportingProgressBar;
    
    @FXML
    private TextArea reportingTextArea;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public ReportingPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ReportingPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent, "parent");
        
        // 1.disableプロパティのバインディング
        //disableProperty().bind(parent.isRunning().not());
        
        // 2.項目ごとの各種設定
        // nop
        
        // 3.初期値の設定
        // nop
        
        // 4.値変更時のイベントハンドラの設定
        // nop
    }
    
    /**
     * このコンポーネントとタスクをバインドします。<br>
     * 
     * @param task タスク
     */
    public void bind(Task<Void> task) {
        Objects.requireNonNull(task, "task");
        
        reportingProgressBar.progressProperty().bind(task.progressProperty());
        reportingTextArea.textProperty().bind(task.messageProperty());
    }
    
    /**
     * このコンポーネントとタスクをアンバインドします。<br>
     */
    public void unbind() {
        reportingProgressBar.progressProperty().unbind();
        reportingProgressBar.setProgress(0D);
        reportingTextArea.textProperty().unbind();
    }
}
