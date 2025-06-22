package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.util.Objects;
import java.util.function.Function;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import xyz.hotchpotch.hogandiff.gui.dialogs.EditComparisonDialogPane.ItemType;
import xyz.hotchpotch.hogandiff.main.models.BookInfo;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * {@link EditComparisonDialogPane} の {@link GridPane} の行要素です。<br>
 * 
 * @author nmby
 */
public class GridRow extends Pane {

    // [static members] ********************************************************

    // [instance members] ******************************************************

    private final EditComparisonDialogPane<?> pane;
    private final int idx;
    private final Pair<?> srcPair;
    private final ItemType itemType;
    private final Pair<GridItem> itemPair;
    private final UnpairButton unpairButton;

    /**
     * コンストラクタ
     * 
     * @param pane    親Pane
     * @param idx     GridPaneにおける行インデックス
     * @param srcPair この {@link GridRow} が描画を担当するオブジェクト
     * @throws NullPointerException     パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code srcPair} が空の場合
     */
    public GridRow(
            EditComparisonDialogPane<?> pane,
            int idx,
            Pair<?> srcPair) {

        Objects.requireNonNull(pane);
        Objects.requireNonNull(srcPair);
        if (!srcPair.hasA() && !srcPair.hasB()) {
            throw new IllegalArgumentException();
        }

        this.pane = pane;
        this.idx = idx;
        this.srcPair = srcPair;
        this.itemType = srcPair.hasA() ? ItemType.of(srcPair.a()) : ItemType.of(srcPair.b());

        Function<Side, GridItem> getGridItem = side -> {
            if (!srcPair.has(side)) {
                return new BlankItem(side);
            }
            if (itemType == ItemType.SHEET || itemType == ItemType.DIR) {
                return new LoadCompletedItem(side);
            }
            BookInfo bookInfo = (BookInfo) srcPair.get(side);
            return switch (bookInfo.status()) {
                case LOAD_COMPLETED -> new LoadCompletedItem(side);
                case LOAD_FAILED -> new LoadFailedItem(side);
                case NEEDS_PASSWORD -> new NeedsPasswordItem(side);
            };
        };
        this.itemPair = Pair.of(
                getGridItem.apply(Side.A),
                getGridItem.apply(Side.B));

        this.unpairButton = srcPair.isPaired()
                ? new UnpairButton()
                : null;

        setMaxHeight(Double.MAX_VALUE);
        setMaxWidth(Double.MAX_VALUE);
        getStyleClass().add("gridRow");

        if (!srcPair.isPaired()) {
            setOnDragEntered(this::onDragEntered);
            setOnDragExited(this::onDragExited);
            setOnDragOver(this::onDragOver);
            setOnDragDropped(this::onDragDropped);
        }
    }

    /**
     * {@link GridPane} に配置すべき子 {@link GridItem} を返します。<br>
     * 
     * @return {@link GridPane} に配置すべき子 {@link GridItem}
     */
    public Pair<GridItem> itemPair() {
        return itemPair;
    }

    /**
     * ペア解消ボタンを返します。<br>
     * 
     * @return ペア解消ボタン
     */
    public UnpairButton unpairButton() {
        return unpairButton;
    }

    private Integer getIdx(String token) {
        if (token == null) {
            return null;
        }
        try {
            String[] elem = token.split("-");
            if (elem.length != 3) {
                return null;
            }
            ItemType itemType = ItemType.valueOf(elem[0]);
            Side side = Side.valueOf(elem[1]);
            int idx = Integer.parseInt(elem[2]);
            if (itemType != this.itemType || srcPair.get(side) != null || idx == this.idx) {
                return null;
            }
            return idx;

        } catch (RuntimeException e) {
            return null;
        }
    }

    private void onDragEntered(DragEvent event) {
        try {
            if (!event.getDragboard().hasString()) {
                return;
            }
            String token = event.getDragboard().getString();
            Integer idx = getIdx(token);
            if (idx == null) {
                return;
            }

            getStyleClass().add("gridRowActivated");
            event.consume();

        } catch (RuntimeException e) {
            e.printStackTrace();
            // nop
        }
    }

    private void onDragExited(DragEvent event) {
        try {
            if (!event.getDragboard().hasString()) {
                return;
            }
            String token = event.getDragboard().getString();
            Integer idx = getIdx(token);
            if (idx == null) {
                return;
            }

            getStyleClass().remove("gridRowActivated");
            event.consume();

        } catch (RuntimeException e) {
            e.printStackTrace();
            // nop
        }
    }

    private void onDragOver(DragEvent event) {
        try {
            if (!event.getDragboard().hasString()) {
                return;
            }
            String token = event.getDragboard().getString();
            Integer idx = getIdx(token);
            if (idx == null) {
                return;
            }

            event.acceptTransferModes(TransferMode.MOVE);
            event.consume();

        } catch (RuntimeException e) {
            e.printStackTrace();
            // nop
        }
    }

    private void onDragDropped(DragEvent event) {
        try {
            if (!event.getDragboard().hasString()) {
                return;
            }
            String token = event.getDragboard().getString();
            Integer idx = getIdx(token);
            if (idx == null) {
                return;
            }

            pane.makePair(idx, this.idx);
            event.setDropCompleted(true);
            event.consume();

        } catch (RuntimeException e) {
            e.printStackTrace();
            // nop
        }
    }

    private class GridItem extends HBox {

        // static members ------------------------------------------------------

        // instance members ----------------------------------------------------

        protected ImageView typeImageView = new ImageView();
        protected Label nameLabel = new Label();
        protected ImageView statusImageView = new ImageView();

        protected final Side side;

        protected GridItem(Side side) {
            this.side = side;

            setMaxHeight(26.0d);
            setMinHeight(26.0d);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("gridItem");
            getChildren().addAll(typeImageView, nameLabel, statusImageView);
            setHgrow(typeImageView, Priority.NEVER);
            setHgrow(nameLabel, Priority.ALWAYS);
            setHgrow(statusImageView, Priority.NEVER);
            setMargin(typeImageView, new Insets(0, 5, 0, 0));

            if (srcPair.has(side)) {
                typeImageView.setPreserveRatio(true);
                typeImageView.setFitHeight(18.0d);
                typeImageView.setImage(itemType.image);
                nameLabel.setMaxWidth(Double.MAX_VALUE);
                nameLabel.setText(srcPair.get(side).toString());
                statusImageView.setPreserveRatio(true);
                statusImageView.setFitHeight(18.0d);
            }
            if (srcPair.has(side) && !srcPair.isPaired()) {
                setOnDragDetected(this::onDragDetected);
                setOnDragDone(this::onDragDone);
            }
            setOnDragEntered(GridRow.this::fireEvent);
            setOnDragExited(GridRow.this::fireEvent);
            setOnDragOver(GridRow.this::fireEvent);
            setOnDragDropped(GridRow.this::fireEvent);
        }

        private void onDragDetected(MouseEvent event) {
            try {
                event.consume();
                Dragboard board = startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString("%s-%s-%d".formatted(itemType, side, idx));
                board.setContent(content);
                getStyleClass().add("gridItemActivated");

            } catch (RuntimeException e) {
                e.printStackTrace();
                // nop
            }
        }

        private void onDragDone(DragEvent event) {
            try {
                event.consume();
                getStyleClass().remove("gridItemActivated");

            } catch (RuntimeException e) {
                e.printStackTrace();
                // nop
            }
        }
    }

    private class LoadCompletedItem extends GridItem {

        // static members ------------------------------------------------------

        // instance members ----------------------------------------------------

        private LoadCompletedItem(Side side) {
            super(side);

            getStyleClass().add("loadCompletedItem");
            if (srcPair.isPaired()) {
                setOnMouseClicked(event -> pane.onClickPaired(idx));
            }
        }
    }

    private class LoadFailedItem extends GridItem {

        // static members ------------------------------------------------------

        private static final Image loadFailedImage = new Image(
                GridRow.class.getResourceAsStream("status-failed.png"));

        // instance members ----------------------------------------------------

        private LoadFailedItem(Side side) {
            super(side);

            getStyleClass().add("loadFailedItem");
            statusImageView.setImage(loadFailedImage);
        }
    }

    private class NeedsPasswordItem extends GridItem {

        // static members ------------------------------------------------------

        private static final Image needsPasswordImage = new Image(
                GridRow.class.getResourceAsStream("status-locked.png"));

        // instance members ----------------------------------------------------

        private NeedsPasswordItem(Side side) {
            super(side);

            getStyleClass().add("needsPasswordItem");
            setOnMouseClicked(event -> pane.onPasswordChallenge(idx, side));
            statusImageView.setImage(needsPasswordImage);
        }
    }

    private class BlankItem extends GridItem {

        // static members ------------------------------------------------------

        // instance members ----------------------------------------------------

        private BlankItem(Side side) {
            super(side);

            getStyleClass().add("blankItem");
        }
    }

    private class UnpairButton extends Button {

        // static members ------------------------------------------------------

        private static Image linkOffImage = new Image(
                GridRow.class.getResourceAsStream("link-off.png"));

        // instance members ----------------------------------------------------

        protected UnpairButton() {
            ImageView linkOffImageView = new ImageView(linkOffImage);
            linkOffImageView.setPreserveRatio(true);
            linkOffImageView.setFitWidth(16);
            setGraphic(linkOffImageView);
            setPrefWidth(50.0d);
            getStyleClass().add("unpairButton");
            setOnAction(event -> pane.unpair(idx));
        }
    }
}
