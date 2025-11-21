package xyz.hotchpotch.hogandiff.gui.components;

import java.io.IOException;
import java.util.Objects;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.ErrorReporter;
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
    
    @FXML
    private ProgressBar reportingProgressBar;
    
    @FXML
    private TextArea reportingTextArea;
    
    @FXML
    private Separator reportingSeparator;
    
    private ScrollBar scrollBar;
    private MainController parent;
    private double startY;
    private double startHeight;
    private double minHeight;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public ReportingPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ReportingPane.fxml"), ar.get());
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * この画面部品の内容を初期化します。<br>
     * 
     * @param parent
     *            このアプリケーションのコントローラ
     * @throws NullPointerException
     *             {@code parent} が {@code null} の場合
     */
    public void init(MainController parent) {
        Objects.requireNonNull(parent);
        
        try {
            this.parent = parent;
            
            // 1.disableプロパティのバインディング
            
            // 2.項目ごとの各種設定
            reportingSeparator.setOnMousePressed(event -> {
                startY = event.getScreenY();
                startHeight = AppMain.stage.getHeight();
                minHeight = parent.showSettings().get()
                        ? AppMain.STAGE_HEIGHT_OPEN
                        : AppMain.STAGE_HEIGHT_CLOSE;
            });
            reportingSeparator.setOnMouseDragged(event -> {
                double d = event.getScreenY() - startY;
                AppMain.stage.setHeight(Math.max(startHeight + d, minHeight));
            });
            
            // 3.初期値の設定
            
            // 4.値変更時のイベントハンドラの設定
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "ReportingPane#init-1");
            throw e;
        }
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
        
        try {
            reportingProgressBar.progressProperty().bind(task.progressProperty());
            reportingTextArea.textProperty().bind(task.messageProperty());
            
            if (scrollBar == null) {
                reportingTextArea.lookupAll(".scroll-bar").stream()
                        .filter(n -> n instanceof ScrollBar s && s.getOrientation() == Orientation.VERTICAL)
                        .map(n -> (ScrollBar) n)
                        .findAny()
                        .ifPresent(bar -> {
                            scrollBar = bar;
                            reportingTextArea.textProperty().addListener((_, _, _) -> bar.setValue(bar.getMax()));
                            bar.valueProperty().addListener((_, oldValue, newValue) -> {
                                if (newValue.doubleValue() == 0d
                                        && oldValue.doubleValue() == bar.getMax()
                                        && !bar.pressedProperty().get()
                                        && parent.isRunning().get()) {
                                    bar.setValue(bar.getMax());
                                }
                            });
                        });
            }
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "ReporingPane#bind-1");
            throw e;
        }
    }
    
    /**
     * このコンポーネントとタスクをアンバインドします。<br>
     */
    public void unbind() {
        try {
            reportingProgressBar.progressProperty().unbind();
            reportingProgressBar.setProgress(0D);
            reportingTextArea.textProperty().unbind();
            
        } catch (Exception e) {
            ErrorReporter.reportIfEnabled(e, "ReporingPane#unbind-1");
            throw e;
        }
    }
}
