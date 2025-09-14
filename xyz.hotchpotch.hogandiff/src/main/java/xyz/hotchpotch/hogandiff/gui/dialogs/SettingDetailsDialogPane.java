package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.gui.UpdateChecker;
import xyz.hotchpotch.hogandiff.logic.google.GoogleCredential;

/**
 * 詳細設定ダイアログの内容部分を提供します。<br>
 * 
 * @author nmby
 */
public class SettingDetailsDialogPane extends VBox {
    
    // [static members] ********************************************************
    
    private static final AppResource ar = AppMain.appResource;
    private static final ResourceBundle rb = ar.get();
    
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
    
    @FXML
    private ComboBox<LocaleItem> localeComboBox;
    
    @FXML
    private CheckBox checkUpdatesCheckBox;
    
    @FXML
    private Button checkUpdatesImmediatelyButton;
    
    @FXML
    private Button openSettingsFileButton;
    
    @FXML
    private Button resetSettingsButton;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public SettingDetailsDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("SettingDetailsDialogPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * このオブジェクトを初期化します。<br>
     */
    public void init() {
        // 1.disableプロパティのバインディング
        // nop
        
        // 2.項目ごとの各種設定
        localeComboBox.setItems(FXCollections.observableArrayList(LocaleItem.values()));
        localeComboBox.setButtonCell(cellFactory(false).call(null));
        localeComboBox.setCellFactory(cellFactory(true));
        
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
        
        checkUpdatesImmediatelyButton.setOnAction(event -> {
            UpdateChecker.execute(true);
        });
        
        openSettingsFileButton.setOnAction(event -> {
            try {
                Desktop.getDesktop().open(AppResource.APP_PROP_PATH.toFile());
            } catch (IOException e) {
                e.printStackTrace();
                // nop
            }
        });
        
        resetSettingsButton.setOnAction(event -> {
            Optional<ButtonType> result = new Alert(
                    AlertType.CONFIRMATION,
                    rb.getString("gui.dialogs.SettingDetailsDialogPane.010"))
                            .showAndWait();
            
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    Files.deleteIfExists(AppResource.APP_PROP_PATH);
                    GoogleCredential credential = GoogleCredential.get(false);
                    if (credential != null) {
                        credential.deleteCredential();
                    }
                    Platform.exit();
                } catch (Exception e) {
                    e.printStackTrace();
                    // nop
                }
            }
        });
        
        // 3.初期値の設定
        Locale locale = ar.settings().get(SettingKeys.APP_LOCALE);
        localeComboBox.setValue(LocaleItem.of(locale));
        
        checkUpdatesCheckBox.setSelected(ar.settings().get(SettingKeys.CHECK_UPDATES));
        
        // 4.値変更時のイベントハンドラの設定
        checkUpdatesCheckBox.setOnAction(event -> ar.changeSetting(
                SettingKeys.CHECK_UPDATES,
                checkUpdatesCheckBox.isSelected()));
    }
    
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
