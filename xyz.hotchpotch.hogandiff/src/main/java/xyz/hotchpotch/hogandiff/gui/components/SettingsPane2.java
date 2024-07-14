package xyz.hotchpotch.hogandiff.gui.components;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.ChildController;
import xyz.hotchpotch.hogandiff.gui.MainController;
import xyz.hotchpotch.hogandiff.util.function.UnsafeConsumer;

/**
 * 比較メニュー部分の画面部品です。<br>
 * 
 * @author nmby
 */
public class SettingsPane2 extends VBox implements ChildController {
    
    // [static members] ********************************************************
    
    private static final Image consentHelpImage = new Image(
            SettingsPane2.class.getResourceAsStream("help-circle.png"));
    
    private static enum LocaleItem {
        
        // [static members] ----------------------------------------------------
        
        /** 日本語 */
        JA("日本語", Locale.JAPANESE, "jp.png"),
        
        /** 英語 */
        EN("English", Locale.ENGLISH, "us.png"),
        
        /** 中国語（簡体字） */
        ZH("簡体中文", Locale.SIMPLIFIED_CHINESE, "cn.png");
        
        public static LocaleItem of(Locale locale) {
            Objects.requireNonNull(locale);
            
            return Stream.of(values())
                    .filter(item -> item.locale == locale)
                    .findFirst()
                    .orElseThrow();
        }
        
        // [instance members] --------------------------------------------------
        
        private final String text;
        private final Locale locale;
        private final Image image;
        
        LocaleItem(String text, Locale locale, String imageSrc) {
            this.text = text;
            this.locale = locale;
            this.image = new Image(imageSrc);
        }
    }
    
    // [instance members] ******************************************************
    
    private final AppResource ar = AppMain.appResource;
    private final ResourceBundle rb = ar.get();
    
    @FXML
    private ComboBox<LocaleItem> localeComboBox;
    
    @FXML
    private Button openWorkDirButton;
    
    @FXML
    private Button changeWorkDirButton;
    
    @FXML
    private Button deleteWorkDirButton;
    
    @FXML
    private CheckBox consentCheckBox;
    
    @FXML
    private Hyperlink consentHelpHyperlink;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public SettingsPane2() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SettingsPane2.fxml"), rb);
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
        localeComboBox.setItems(FXCollections.observableArrayList(LocaleItem.values()));
        localeComboBox.setButtonCell(cellFactory(false).call(null));
        localeComboBox.setCellFactory(cellFactory(true));
        
        openWorkDirButton.setOnAction(openDir);
        changeWorkDirButton.setOnAction(changeDir);
        deleteWorkDirButton.setOnAction(deleteDir);
        
        ImageView imageView = new ImageView(consentHelpImage);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(16);
        consentHelpHyperlink.setGraphic(imageView);
        consentHelpHyperlink.setOnAction(event -> {
            try {
                // 一旦、v0.21.1新機能紹介ページに飛ばすことにしておく。
                // FIXME: [No.X Webサイト改善] 恒久的な説明ページを作成しリンク先を変更する
                String url = "https://hogandiff.hotchpotch.xyz/releasenotes/v0-21-1/";
                Desktop.getDesktop().browse(URI.create(url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        // 3.初期値の設定
        Locale locale = ar.settings().get(SettingKeys.APP_LOCALE);
        localeComboBox.setValue(LocaleItem.of(locale));
        consentCheckBox.setSelected(ar.settings().get(SettingKeys.CONSENTED_STATS_COLLECTION));
        
        // 4.値変更時のイベントハンドラの設定
        localeComboBox.setOnAction(event -> {
            if (ar.changeSetting(SettingKeys.APP_LOCALE, localeComboBox.getValue().locale)) {
                new Alert(
                        AlertType.INFORMATION,
                        "%s%n%n%s%n%n%s".formatted(
                                rb.getString("gui.component.SettingsPane2.051"),
                                rb.getString("gui.component.SettingsPane2.052"),
                                rb.getString("gui.component.SettingsPane2.053")),
                        ButtonType.OK)
                                .showAndWait();
            }
        });
        consentCheckBox.setOnAction(
                event -> ar.changeSetting(SettingKeys.CONSENTED_STATS_COLLECTION, consentCheckBox.isSelected()));
    }
    
    private final EventHandler<ActionEvent> openDir = event -> {
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
                    "%s%n%s".formatted(
                            rb.getString("gui.component.SettingsPane2.010"),
                            workDirBase),
                    ButtonType.OK)
                            .showAndWait();
        }
    };
    
    private final EventHandler<ActionEvent> changeDir = event -> {
        Path workDirBase = ar.settings().get(SettingKeys.WORK_DIR_BASE);
        
        File newDir = null;
        try {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle(rb.getString("gui.component.SettingsPane2.020"));
            dirChooser.setInitialDirectory(workDirBase.toFile());
            newDir = dirChooser.showDialog(getScene().getWindow());
            
        } catch (IllegalArgumentException e) {
            DirectoryChooser dirChooser = new DirectoryChooser();
            dirChooser.setTitle(rb.getString("gui.component.SettingsPane2.020"));
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
                            "%s%n%s".formatted(
                                    rb.getString("gui.component.SettingsPane2.030"),
                                    newPath),
                            ButtonType.OK)
                                    .showAndWait();
                    return;
                }
            }
            ar.changeSetting(SettingKeys.WORK_DIR_BASE, newPath);
        }
    };
    
    private final EventHandler<ActionEvent> deleteDir = event -> {
        Path workDirBase = ar.settings().get(SettingKeys.WORK_DIR_BASE);
        
        Optional<ButtonType> result = new Alert(
                AlertType.CONFIRMATION,
                "%s%n%s".formatted(
                        rb.getString("gui.component.SettingsPane2.040"),
                        workDirBase))
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
                    //nop
                }
            });
        }
    };
    
    private Callback<ListView<LocaleItem>, ListCell<LocaleItem>> cellFactory(boolean showText) {
        return listView -> new ListCell<>() {
            @Override
            public void updateItem(LocaleItem item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    ImageView iv = new ImageView(item.image);
                    iv.setFitHeight(17);
                    iv.setPreserveRatio(true);
                    setGraphic(iv);
                    
                    if (showText) {
                        setText(item.text);
                    } else {
                        this.setAlignment(Pos.CENTER);
                    }
                }
            }
        };
    }
}
