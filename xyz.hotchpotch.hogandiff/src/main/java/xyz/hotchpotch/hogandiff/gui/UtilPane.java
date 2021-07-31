package xyz.hotchpotch.hogandiff.gui;

import java.awt.Desktop;
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
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.util.Settings;
import xyz.hotchpotch.hogandiff.util.function.UnsafeConsumer;

public class UtilPane extends HBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @FXML
    private Button showWorkDirButton;
    
    @FXML
    private Button deleteOldWorkDirButton;
    
    @FXML
    private Hyperlink toWebSiteHyperlink;
    
    private Path workDir;
    
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
                // nop
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
        
        toWebSiteHyperlink.setOnAction(event -> {
            try {
                Desktop.getDesktop().browse(URI.create("https://hogandiff.hotchpotch.xyz/"));
            } catch (Exception e) {
                // nop
            }
        });
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
