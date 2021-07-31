package xyz.hotchpotch.hogandiff.gui;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
    
    @FXML
    private Button showWorkDirButton;
    
    @FXML
    private Button deleteOldWorkDirButton;
    
    @FXML
    private Button changeWorkDirButton;
    
    @FXML
    private Hyperlink toWebSiteHyperlink;
    
    private Path workDir = SettingKeys.WORK_DIR_BASE.defaultValueSupplier().get();
    
    public UtilPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("UtilPane.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent) {
        Objects.requireNonNull(parent, "parent");
        
        showWorkDirButton.setOnAction(event -> {
            try {
                if (!Files.isDirectory(workDir)) {
                    Files.createDirectories(workDir);
                }
                Desktop.getDesktop().open(workDir.toFile());
                
            } catch (Exception e) {
                new Alert(
                        AlertType.WARNING,
                        "作業用フォルダの表示に失敗しました。\n" + workDir,
                        ButtonType.OK)
                                .showAndWait();
            }
        });
        
        deleteOldWorkDirButton.setOnAction(event -> {
            Optional<ButtonType> result = new Alert(
                    AlertType.CONFIRMATION,
                    "次のフォルダの内容物を全て削除します。よろしいですか？\n" + workDir)
                            .showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try (Stream<Path> children = Files.walk(workDir)) {
                    children.filter(path -> !path.equals(workDir))
                            .sorted(Comparator.reverseOrder())
                            .forEach(UnsafeConsumer.toConsumer(Files::deleteIfExists));
                } catch (Exception e) {
                    //nop
                }
            }
        });
        
        changeWorkDirButton.setOnAction(event -> {
            DirectoryChooser dirChooser = new DirectoryChooser();
            
            dirChooser.setTitle("作業用フォルダの変更");
            dirChooser.setInitialDirectory(workDir.toFile());
            
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
                if (newPath.equals(workDir)) {
                    return;
                }
                
                if (!Files.isDirectory(newPath)) {
                    try {
                        Files.createDirectory(newPath);
                    } catch (IOException e) {
                        new Alert(
                                AlertType.WARNING,
                                "作業用フォルダの変更に失敗しました。\n" + newPath,
                                ButtonType.OK)
                                        .showAndWait();
                        return;
                    }
                }
                workDir = newPath;
                parent.hasSettingsChanged.set(true);
            }
        });
        
        toWebSiteHyperlink.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(URI.create(AppMain.WEB_URL));
                
            } catch (Exception e) {
                new Alert(
                        AlertType.WARNING,
                        "Webページの表示に失敗しました。ご利用のブラウザでお試しください。\n"
                                + AppMain.WEB_URL,
                        ButtonType.OK)
                                .showAndWait();
            }
        });
        
        disableProperty().bind(parent.isRunning);
    }
    
    @Override
    public void applySettings(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        
        if (settings.containsKey(SettingKeys.WORK_DIR_BASE)) {
            workDir = settings.get(SettingKeys.WORK_DIR_BASE);
        } else {
            workDir = SettingKeys.WORK_DIR_BASE.defaultValueSupplier().get();
        }
    }
    
    @Override
    public void gatherSettings(Settings.Builder builder) {
        Objects.requireNonNull(builder, "builder");
        
        builder.set(SettingKeys.WORK_DIR_BASE, workDir);
    }
}
