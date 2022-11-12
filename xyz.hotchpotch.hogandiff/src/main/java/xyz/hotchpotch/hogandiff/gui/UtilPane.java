package xyz.hotchpotch.hogandiff.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.function.UnsafeConsumer;

/**
 * 画面フッタ部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class UtilPane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private Button showWorkDirButton;
    
    @FXML
    private Button deleteOldWorkDirButton;
    
    @FXML
    private Button changeWorkDirButton;
    
    @FXML
    private Hyperlink toWebSiteHyperlink;
    
    private Property<Path> workDirBase = new SimpleObjectProperty<>();
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public UtilPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("UtilPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent) {
        Objects.requireNonNull(parent, "parent");
        
        workDirBase.addListener(
                (target, oldValue, newValue) -> ar.changeSetting(SettingKeys.WORK_DIR_BASE, workDirBase.getValue()));
        
        showWorkDirButton.setOnAction(event -> {
            try {
                if (!Files.isDirectory(workDirBase.getValue())) {
                    Files.createDirectories(workDirBase.getValue());
                }
                Desktop.getDesktop().open(workDirBase.getValue().toFile());
                
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(
                        AlertType.WARNING,
                        "%s%n%s".formatted(rb.getString("gui.UtilPane.010"), workDirBase.getValue()),
                        ButtonType.OK)
                                .showAndWait();
            }
        });
        
        deleteOldWorkDirButton.setOnAction(event -> {
            Optional<ButtonType> result = new Alert(
                    AlertType.CONFIRMATION,
                    "%s%n%s".formatted(rb.getString("gui.UtilPane.020"), workDirBase.getValue()))
                            .showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                Desktop desktop = Desktop.getDesktop();
                UnsafeConsumer<Path> deleteAction = desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)
                        ? path -> desktop.moveToTrash(path.toFile())
                        : Files::deleteIfExists;
                
                try (Stream<Path> children = Files.list(workDirBase.getValue())) {
                    children.forEach(path -> {
                        try {
                            deleteAction.accept(path);
                        } catch (Exception e) {
                            // nop
                            // 使用中などの理由で削除できないファイルがある場合は
                            // それを飛ばして削除処理を継続する
                        }
                    });
                } catch (Exception e) {
                    //nop
                }
            }
        });
        
        changeWorkDirButton.setOnAction(event -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            
            dirChooser.setTitle(rb.getString("gui.UtilPane.060"));
            dirChooser.setInitialDirectory(workDirBase.getValue().toFile());
            
            File newDir = null;
            try {
                newDir = dirChooser.showDialog(getScene().getWindow());
            } catch (IllegalArgumentException e) {
                newDir = SettingKeys.WORK_DIR_BASE.defaultValueSupplier().get().toFile();
            }
            
            if (newDir != null) {
                Path newPath = newDir.toPath();
                if (!newPath.endsWith(AppMain.APP_DOMAIN)) {
                    newPath = newPath.resolve(AppMain.APP_DOMAIN);
                }
                if (newPath.equals(workDirBase.getValue())) {
                    return;
                }
                
                if (!Files.isDirectory(newPath)) {
                    try {
                        Files.createDirectory(newPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                        new Alert(
                                AlertType.WARNING,
                                "%s%n%s".formatted(rb.getString("gui.UtilPane.030"), newPath),
                                ButtonType.OK)
                                        .showAndWait();
                        return;
                    }
                }
                workDirBase.setValue(newPath);
            }
        });
        
        toWebSiteHyperlink.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(URI.create(AppMain.WEB_URL));
                
            } catch (Exception e) {
                e.printStackTrace();
                new Alert(
                        AlertType.WARNING,
                        "%s%n%s".formatted(rb.getString("gui.UtilPane.050"), AppMain.WEB_URL),
                        ButtonType.OK)
                                .showAndWait();
            }
        });
        
        disableProperty().bind(parent.isRunning);
    }
    
    @Override
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        workDirBase.setValue(settings.getOrDefault(SettingKeys.WORK_DIR_BASE));
    }
}
