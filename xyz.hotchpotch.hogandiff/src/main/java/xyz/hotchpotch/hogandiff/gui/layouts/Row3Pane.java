package xyz.hotchpotch.hogandiff.gui.layouts;

import java.io.IOException;
import java.util.Objects;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.components.ReportingPane;
import xyz.hotchpotch.hogandiff.gui.components.TogglePane;

/**
 * メインビュー三段目の画面部品です。<br>
 * 
 * @author nmby
 */
public class Row3Pane extends StackPane implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    @FXML
    private ReportingPane reportingPane;
    
    @FXML
    private TogglePane togglePane;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public Row3Pane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("Row3Pane.fxml"), ar.get());
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * この画面部品の内容を初期化します。<br>
     * 
     * @param controller
     *            このアプリケーションのコントローラ
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public void init(MainController controller) {
        Objects.requireNonNull(controller);
        
        // 1.disableプロパティのバインディング
        
        // 2.項目ごとの各種設定
        reportingPane.init(controller);
        togglePane.init(controller);
        
        // 3.初期値の設定
        
        // 4.値変更時のイベントハンドラの設定
    }
    
    /**
     * このコンポーネントとタスクをバインドします。<br>
     * 
     * @param task
     *            タスク
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public void bind(Task<Void> task) {
        Objects.requireNonNull(task);
        
        reportingPane.bind(task);
    }
    
    /**
     * このコンポーネントとタスクをアンバインドします。<br>
     */
    public void unbind() {
        reportingPane.unbind();
    }
}
