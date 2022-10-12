package xyz.hotchpotch.hogandiff.gui;

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

/**
 * 実行状況表示部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class ReportingPane extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private ProgressBar reportProgressBar;
    
    @FXML
    private TextArea reportTextArea;
    
    public ReportingPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ReportingPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /*package*/ void bind(Task<Void> task) {
        Objects.requireNonNull(task, "task");
        
        reportProgressBar.progressProperty().bind(task.progressProperty());
        reportTextArea.textProperty().bind(task.messageProperty());
    }
    
    /*package*/ void unbind() {
        reportProgressBar.progressProperty().unbind();
        reportProgressBar.setProgress(0D);
        reportTextArea.textProperty().unbind();
    }
}
