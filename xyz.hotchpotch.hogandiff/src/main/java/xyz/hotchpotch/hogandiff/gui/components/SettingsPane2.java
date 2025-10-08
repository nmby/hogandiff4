package xyz.hotchpotch.hogandiff.gui.components;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.Msg;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.VersionMaster;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.gui.dialogs.SettingDetailsDialogPane;
import xyz.hotchpotch.hogandiff.util.function.UnsafeConsumer;

/**
 * 比較メニュー部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class SettingsPane2 extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    @FXML
    private GooglePane googlePane;
    
    @FXML
    private Button openWorkDirButton;
    
    @FXML
    private Button changeWorkDirButton;
    
    @FXML
    private Button deleteWorkDirButton;
    
    @FXML
    private Button detailsButton;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException
     *             FXMLファイルの読み込みに失敗した場合
     */
    public SettingsPane2() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SettingsPane2.fxml"), ar.get());
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    @Override
    public void init(MainController parent, Object... param) {
        Objects.requireNonNull(parent);
        
        // 1.disableプロパティのバインディング
        disableProperty().bind(parent.isRunning());
        
        // 2.項目ごとの各種設定
        googlePane.init(parent);
        
        openWorkDirButton.setOnAction(openDir);
        changeWorkDirButton.setOnAction(changeDir);
        deleteWorkDirButton.setOnAction(deleteDir);
        
        detailsButton.setOnAction(_ -> {
            try {
                SettingDetailsDialogPane detailsContent = new SettingDetailsDialogPane();
                detailsContent.init();
                Dialog<Void> detailsDialog = new Dialog<>();
                detailsDialog.setTitle(Msg.APP_1100.get());
                detailsDialog.getDialogPane().setContent(detailsContent);
                detailsDialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
                detailsDialog.showAndWait();
                
            } catch (IOException e) {
                e.printStackTrace();
                // nop
            }
        });
        
        // 3.初期値の設定
        // nop
        
        // 4.値変更時のイベントハンドラの設定
        // nop
        
        // 5.その他
        VersionMaster.for_v0_27_0 = detailsButton;
    }
    
    private final EventHandler<ActionEvent> openDir = _ -> {
        Path workDirBase = ar.settings().get(SettingKeys.WORK_DIR_BASE);
        
        try {
            if (!Files.isDirectory(workDirBase)) {
                Files.createDirectories(workDirBase);
            }
            Desktop.getDesktop().open(workDirBase.toFile());
            
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(
                    AlertType.WARNING,
                    "%s%n%s".formatted(Msg.APP_1050.get(), workDirBase),
                    ButtonType.OK)
                            .showAndWait();
        }
    };
    
    private final EventHandler<ActionEvent> changeDir = _ -> {
        Path workDirBase = ar.settings().get(SettingKeys.WORK_DIR_BASE);
        
        File newDir = null;
        try {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle(Msg.APP_1060.get());
            dirChooser.setInitialDirectory(workDirBase.toFile());
            newDir = dirChooser.showDialog(getScene().getWindow());
            
        } catch (IllegalArgumentException e) {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle(Msg.APP_1060.get());
            newDir = dirChooser.showDialog(getScene().getWindow());
        }
        
        if (newDir != null) {
            Path newPath = newDir.toPath();
            if (!newPath.endsWith(AppMain.APP_DOMAIN)) {
                newPath = newPath.resolve(AppMain.APP_DOMAIN);
            }
            if (newPath.equals(workDirBase)) {
                return;
            }
            
            if (!Files.isDirectory(newPath)) {
                try {
                    Files.createDirectory(newPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    new Alert(
                            AlertType.WARNING,
                            "%s%n%s".formatted(Msg.APP_1070.get(), newPath),
                            ButtonType.OK)
                                    .showAndWait();
                    return;
                }
            }
            ar.changeSetting(SettingKeys.WORK_DIR_BASE, newPath);
        }
    };
    
    private final EventHandler<ActionEvent> deleteDir = _ -> {
        Path workDirBase = ar.settings().get(SettingKeys.WORK_DIR_BASE);
        
        Optional<ButtonType> result = new Alert(
                AlertType.CONFIRMATION,
                "%s%n%s".formatted(Msg.APP_1080.get(), workDirBase))
                        .showAndWait();
        
        if (result.isPresent() && result.get() == ButtonType.OK) {
            Desktop desktop = Desktop.getDesktop();
            UnsafeConsumer<Path, Exception> deleteAction = desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)
                    ? path -> desktop.moveToTrash(path.toFile())
                    : Files::deleteIfExists;
            
            Thread.startVirtualThread(() -> {
                try (Stream<Path> children = Files.list(workDirBase)) {
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
                    // nop
                }
            });
        }
    };
}