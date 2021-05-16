package xyz.hotchpotch.hogandiff.gui;

import java.io.IOException;
import java.util.Objects;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class ReportingPane extends VBox {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private ProgressBar progressReport;
    
    @FXML
    private TextArea textReport;
    
    public ReportingPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("ReportingPane.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    public void bind(Task<Void> task) {
        Objects.requireNonNull(task, "task");
        
        progressReport.progressProperty().bind(task.progressProperty());
        textReport.textProperty().bind(task.messageProperty());
    }
    
    public void unbind() {
        progressReport.progressProperty().unbind();
        progressReport.setProgress(0D);
        textReport.textProperty().unbind();
    }
}
