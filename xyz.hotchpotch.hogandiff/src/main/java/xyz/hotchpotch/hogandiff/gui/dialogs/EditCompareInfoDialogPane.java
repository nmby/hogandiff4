package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/*package*/ abstract class EditCompareInfoDialogPane<P, C> extends VBox {
    
    // [static members] ********************************************************
    
    protected enum ItemType {
        
        // static members ------------------------------------------------------
        
        /** フォルダ */
        DIR("icon-folder.png"),
        
        /** Excelブック */
        BOOK("icon-book.png"),
        
        /** シート */
        SHEET("icon-sheet.png");
        
        public static ItemType of(Object item) {
            if (item instanceof DirInfo) {
                return DIR;
                
            } else if (item instanceof BookInfo) {
                return BOOK;
                
            } else if (item instanceof String) {
                return SHEET;
            }
            throw new IllegalArgumentException();
        }
        
        // instance members ----------------------------------------------------
        
        private final Image image;
        
        private ItemType(String imagePath) {
            this.image = new Image(ItemType.class.getResourceAsStream(imagePath));
        }
        
        public ImageView createImageView(double size) {
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(size);
            return imageView;
        }
    }
    
    // [instance members] ******************************************************
    
    protected ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    protected GridPane parentGridPane;
    
    @FXML
    protected Label parentLabelA;
    
    @FXML
    protected Label parentLabelB;
    
    @FXML
    protected GridPane childGridPane;
    
    protected ItemType parentType;
    protected Pair<P> parentPair;
    protected List<Pair<C>> childPairs;
    protected final List<Pair<?>> currentChildPairs = new ArrayList<>();
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditCompareInfoDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EditCompareInfoDialogPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * このダイアログボックス要素を初期化します。<br>
     * 
     * @param dialog 親要素
     * @param bookPath 開こうとしているExcelブックのパス
     * @param readPassword 開こうとしているExcelブックの読み取りパスワード
     */
    /*package*/ void init(
            EditCompareInfoDialog dialog,
            ItemType parentType,
            Pair<P> parentPair,
            List<Pair<C>> childPairs)
            throws IOException {
        
        this.parentType = parentType;
        this.parentPair = parentPair;
        this.childPairs = childPairs;
        
        // コンテンツの長さが異なると均等にサイジングされないため、わざわざBindingとして実装することにする
        parentGridPane.getColumnConstraints().get(0).prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> (parentGridPane.getWidth() - 50) / 2,
                parentGridPane.widthProperty()));
        parentGridPane.getColumnConstraints().get(2).prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> (parentGridPane.getWidth() - 50) / 2,
                parentGridPane.widthProperty()));
        childGridPane.getColumnConstraints().get(0).prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> (childGridPane.getWidth() - 50) / 2,
                childGridPane.widthProperty()));
        childGridPane.getColumnConstraints().get(2).prefWidthProperty().bind(Bindings.createDoubleBinding(
                () -> (childGridPane.getWidth() - 50) / 2,
                childGridPane.widthProperty()));
        
        parentLabelA.setGraphic(parentType.createImageView(24));
        parentLabelB.setGraphic(parentType.createImageView(24));
        parentLabelA.setText("【A】 " + parentPair.a().toString());
        parentLabelB.setText("【B】 " + parentPair.b().toString());
    }
    
    protected void drawGrid() {
        childGridPane.getChildren().clear();
        
        for (int i = 0; i < currentChildPairs.size(); i++) {
            Pair<?> pair = currentChildPairs.get(i);
            
            if (pair.isPaired()) {
                ItemType itemType = ItemType.of(pair.a());
                childGridPane.add(new PairedNameLabel(itemType, pair.a().toString()), 0, i);
                childGridPane.add(new UnpairButton(i), 1, i);
                childGridPane.add(new PairedNameLabel(itemType, pair.b().toString()), 2, i);
                
            } else if (pair.hasA()) {
                ItemType itemType = ItemType.of(pair.a());
                childGridPane.add(new BlankLabel(), 2, i);
                childGridPane.add(new UnpairedPane(i, Side.B), 0, i, 3, 1);
                childGridPane.add(new UnpairedNameLabel(itemType, i, Side.A, pair.a().toString()), 0, i);
                
            } else if (pair.hasB()) {
                ItemType itemType = ItemType.of(pair.b());
                childGridPane.add(new BlankLabel(), 0, i);
                childGridPane.add(new UnpairedPane(i, Side.A), 0, i, 3, 1);
                childGridPane.add(new UnpairedNameLabel(itemType, i, Side.B, pair.b().toString()), 2, i);
            } else {
                // nop
            }
        }
    }
    
    protected abstract void unpair(int i);
    
    protected abstract void makePair(int src, int dst);
    
    protected class UnpairButton extends Button {
        
        // static members ------------------------------------------------------
        
        private static Image linkOffImage = new Image(
                EditCompareInfoDialogPane.class.getResourceAsStream("link-off.png"));
        
        // instance members ----------------------------------------------------
        
        protected UnpairButton(int idx) {
            ImageView linkOffImageView = new ImageView(linkOffImage);
            linkOffImageView.setPreserveRatio(true);
            linkOffImageView.setFitWidth(16);
            setGraphic(linkOffImageView);
            getStyleClass().add("unpairButton");
            
            setOnAction(event -> unpair(idx));
        }
    }
    
    protected class PairedNameLabel extends Label {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        protected PairedNameLabel(ItemType itemType, String name) {
            setGraphic(itemType.createImageView(18));
            setGraphicTextGap(5);
            setText(name);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("childLabel");
            getStyleClass().add("pairedNameLabel");
        }
    }
    
    protected class UnpairedNameLabel extends Label {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        private final Side side;
        
        protected UnpairedNameLabel(ItemType itemType, int idx, Side side, String name) {
            this.idx = idx;
            this.side = side;
            setGraphic(itemType.createImageView(18));
            setGraphicTextGap(5);
            setText(name);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("childLabel");
            getStyleClass().add("unpairedNameLabel");
            setOnDragDetected(this::onDragDetected);
        }
        
        private void onDragDetected(MouseEvent event) {
            try {
                event.consume();
                Dragboard board = startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString("%s%d".formatted(side, idx));
                board.setContent(content);
                
            } catch (RuntimeException e) {
                e.printStackTrace();
                // nop
            }
        }
    }
    
    protected class BlankLabel extends Label {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        protected BlankLabel() {
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("childLabel");
            getStyleClass().add("dummyLabel");
        }
    }
    
    protected class UnpairedPane extends Pane {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        private final Side lackedSide;
        
        protected UnpairedPane(int idx, Side lackedSide) {
            this.idx = idx;
            this.lackedSide = lackedSide;
            setMaxHeight(Double.MAX_VALUE);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("unpairedPane");
            setOnDragEntered(this::onDragEntered);
            setOnDragOver(this::onDragOver);
            setOnDragExited(this::onDragExited);
            setOnDragDropped(this::onDragDropped);
        }
        
        /**
         * {@code A0}, {@code B77} 形式の id を受け取り、idx 成分を返す。
         * 但し、id に含まれる side 成分が自らの lackedSide と異なる場合は
         * 受け入れ不可として null を返す。<br>
         * 
         * @param id {@code A0}, {@code B77} 形式のソースノードの位置を表す文字列
         * @return ソースノードを受け入れ可能な場合にその idx 成分
         */
        private Integer getIdx(String id) {
            if (id == null || id.length() < 2) {
                return null;
            }
            try {
                Side side = Side.valueOf(id.substring(0, 1));
                int idx = Integer.parseInt(id.substring(1));
                if (side != this.lackedSide || idx == this.idx) {
                    return null;
                }
                return idx;
                
            } catch (RuntimeException e) {
                return null;
            }
        }
        
        private void onDragEntered(DragEvent event) {
            try {
                event.consume();
                
                if (!event.getDragboard().hasString()) {
                    return;
                }
                String id = event.getDragboard().getString();
                Integer idx = getIdx(id);
                if (idx == null) {
                    return;
                }
                
                UnpairedPane target = (UnpairedPane) event.getTarget();
                target.getStyleClass().add("dragging");
                
            } catch (RuntimeException e) {
                e.printStackTrace();
                // nop
            }
        }
        
        private void onDragOver(DragEvent event) {
            try {
                event.consume();
                
                if (!event.getDragboard().hasString()) {
                    return;
                }
                String id = event.getDragboard().getString();
                Integer idx = getIdx(id);
                if (idx == null) {
                    return;
                }
                event.acceptTransferModes(TransferMode.MOVE);
                
            } catch (RuntimeException e) {
                e.printStackTrace();
                // nop
            }
        }
        
        private void onDragExited(DragEvent event) {
            try {
                event.consume();
                
                UnpairedPane target = (UnpairedPane) event.getTarget();
                target.getStyleClass().remove("dragging");
                
            } catch (RuntimeException e) {
                e.printStackTrace();
                // nop
            }
        }
        
        private void onDragDropped(DragEvent event) {
            try {
                event.consume();
                
                if (!event.getDragboard().hasString()) {
                    event.setDropCompleted(false);
                    return;
                }
                String id = event.getDragboard().getString();
                Integer idx = getIdx(id);
                if (idx == null) {
                    event.setDropCompleted(false);
                    return;
                }
                
                makePair(idx, this.idx);
                event.setDropCompleted(true);
                
            } catch (RuntimeException e) {
                e.printStackTrace();
                event.setDropCompleted(false);
                // nop
            }
        }
    }
}
