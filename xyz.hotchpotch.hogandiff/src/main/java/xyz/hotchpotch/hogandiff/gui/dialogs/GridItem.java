package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.Objects;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.gui.dialogs.EditComparisonDialogPane.ItemType;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * {@link EditComparisonDialogPane} に表示する要素です。<br>
 * 
 * @author nmby
 */
public class GridItem extends HBox {
    
    // [static members] ********************************************************
    
    private static final Image needsPasswordImage = new Image(
            EditComparisonDialogPane.class.getResourceAsStream("status-locked.png"));
    
    private static final Image loadFailedImage = new Image(
            EditComparisonDialogPane.class.getResourceAsStream("status-failed.png"));
    
    private static <T extends Event> void doNothing(T event) {
        // イベントを上位ターゲットに透過させる。
    }
    
    /**
     * 比較対象なしを表す要素です。<br>
     * 次のイベントを親コンポーネントにスルーします。<br>
     *   - onDragEntered / onDragExited
     *   - onDragOver / onDragDropped
     */
    private static class BlankItem extends GridItem {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        // 不細工だ・・
        private BlankItem(
                EditComparisonDialogPane<?> pane,
                ItemType itemType,
                int idx,
                Side side) {
            
            super(pane, itemType, idx, side, null, false);
        }
        
        private BlankItem init() {
            getStyleClass().add("blankItem");
            setOnDragEntered(null);
            setOnDragExited(null);
            setOnDragOver(event -> System.out.println("over!!!"));
            setOnDragDropped(null);
            return this;
        }
    }
    
    /**
     * ロード完了済みの要素です。<br>
     * 比較相手なしの場合に次のイベントを捕捉します。<br>
     *   - onDragDetected / onDragDone
     */
    private static class LoadCompletedItem extends GridItem {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private LoadCompletedItem(
                EditComparisonDialogPane<?> pane,
                ItemType itemType,
                int idx,
                Side side,
                String name,
                boolean isPaired) {
            
            super(pane, itemType, idx, side, name, isPaired);
        }
        
        private LoadCompletedItem init() {
            itemTypeImageView.setImage(itemType.image);
            nameLabel.setText(name);
            getStyleClass().add("loadCompletedItem");
            
            if (!isPaired) {
                setOnDragDetected(super::onDragDetected);
                setOnDragDone(super::onDragDone);
            }
            return this;
        }
    }
    
    /**
     * ロード失敗済みの要素です。<br>
     */
    private static class LoadFailedItem extends GridItem {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private LoadFailedItem(
                EditComparisonDialogPane<?> pane,
                ItemType itemType,
                int idx,
                Side side,
                String name,
                boolean isPaired) {
            
            super(pane, itemType, idx, side, name, isPaired);
        }
        
        private LoadFailedItem init() {
            itemTypeImageView.setImage(itemType.image);
            nameLabel.setText(name);
            statusImageView.setImage(loadFailedImage);
            getStyleClass().add("loadCompletedItem");
            return this;
        }
    }
    
    /**
     * パスワード不明でロードできない要素です。<br>
     * 次のイベントを捕捉します。<br>
     *   - onMouseClicked
     * 比較相手なしの場合に次のイベントを捕捉します。<br>
     *   - onDragDetected / onDragDone
     */
    private static class NeedsPasswordItem extends GridItem {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private NeedsPasswordItem(
                EditComparisonDialogPane<?> pane,
                ItemType itemType,
                int idx,
                Side side,
                String name,
                boolean isPaired) {
            
            super(pane, itemType, idx, side, name, isPaired);
        }
        
        private NeedsPasswordItem init() {
            itemTypeImageView.setImage(itemType.image);
            nameLabel.setText(name);
            statusImageView.setImage(needsPasswordImage);
            getStyleClass().add("needsPasswordItem");
            
            setOnMouseClicked(event -> pane.onPasswordChallenge(idx, side));
            
            if (!isPaired) {
                setOnDragDetected(super::onDragDetected);
                setOnDragDone(super::onDragDone);
            }
            return this;
        }
    }
    
    public static GridItem of(
            EditComparisonDialogPane<?> pane,
            ItemType itemType,
            int idx,
            Side side,
            Object item,
            boolean isPaired) {
        
        Objects.requireNonNull(pane);
        Objects.requireNonNull(itemType);
        Objects.requireNonNull(side);
        
        if (item == null) {
            return new BlankItem(pane, itemType, idx, side).init();
        }
        return switch (itemType) {
            case SHEET, DIR -> new LoadCompletedItem(pane, itemType, idx, side, item.toString(), isPaired).init();
            case BOOK -> {
                BookInfo bookInfo = (BookInfo) item;
                yield switch (bookInfo.status()) {
                    // 不細工だ・・
                    case LOAD_COMPLETED -> new LoadCompletedItem(
                            pane, itemType, idx, side, item.toString(), isPaired).init();
                    case LOAD_FAILED -> new LoadFailedItem(
                            pane, itemType, idx, side, item.toString(), isPaired).init();
                    case NEEDS_PASSWORD -> new NeedsPasswordItem(
                            pane, itemType, idx, side, item.toString(), isPaired).init();
                };
            }
        };
    }
    
    // [instance members] ******************************************************
    
    @FXML
    protected ImageView itemTypeImageView;
    
    @FXML
    protected Label nameLabel;
    
    @FXML
    protected ImageView statusImageView;
    
    protected final EditComparisonDialogPane<?> pane;
    protected final ItemType itemType;
    protected final int idx;
    protected final Side side;
    protected final String name;
    protected final boolean isPaired;
    
    private GridItem(
            EditComparisonDialogPane<?> pane,
            ItemType itemType,
            int idx,
            Side side,
            String name,
            boolean isPaired) {
        
        this.pane = pane;
        this.itemType = itemType;
        this.idx = idx;
        this.side = side;
        this.name = name;
        this.isPaired = isPaired;
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("NameItem.fxml"), null);
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            // nop
        }
    }
    
    private void onDragDetected(MouseEvent event) {
        try {
            event.consume();
            Dragboard board = startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString("%s-%s-%d".formatted(itemType, side, idx));
            board.setContent(content);
            getStyleClass().add("dragging");
            
        } catch (RuntimeException e) {
            e.printStackTrace();
            // nop
        }
    }
    
    private void onDragDone(DragEvent event) {
        try {
            getStyleClass().remove("dragging");
            
        } catch (RuntimeException e) {
            e.printStackTrace();
            // nop
        }
    }
}
