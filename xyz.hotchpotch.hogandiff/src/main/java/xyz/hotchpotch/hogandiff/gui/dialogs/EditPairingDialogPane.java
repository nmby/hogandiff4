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
import xyz.hotchpotch.hogandiff.excel.BookCompareInfo;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * ユーザーにパスワード入力を求めるダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class EditPairingDialogPane extends VBox {
    
    // static members **********************************************************
    
    private enum ItemType {
        
        // static members ------------------------------------------------------
        
        DIR("icon-folder.png"), BOOK("icon-book.png"), SHEET("icon-sheet.png");
        
        // instance members ----------------------------------------------------
        
        private final Image image;
        
        ItemType(String imagePath) {
            this.image = new Image(ItemType.class.getResourceAsStream(imagePath));
        }
        
        private ImageView createImageView(double size) {
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(size);
            return imageView;
        }
    }
    
    // instance members ********************************************************
    
    private ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private GridPane parentGridPane;
    
    @FXML
    private Label parentLabelA;
    
    @FXML
    private Label parentLabelB;
    
    @FXML
    private GridPane childGridPane;
    
    private Pair<BookInfo> parentPair;
    private List<Pair<String>> currentChildPairs;
    private ItemType parentType;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditPairingDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EditPairingDialogPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }
    
    /**
     * このダイアログボックス要素を初期化します。<br>
     * 
     * @param parent 親要素
     * @param bookPath 開こうとしているExcelブックのパス
     * @param readPassword 開こうとしているExcelブックの読み取りパスワード
     */
    /*package*/ void init(
            EditPairingDialog parent,
            BookCompareInfo compareInfo)
            throws IOException {
        
        parentType = (compareInfo instanceof BookCompareInfo) ? ItemType.BOOK : ItemType.DIR;
        
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
        parentLabelA.setText("【A】 " + compareInfo.parentBookInfoPair().a().toString());
        parentLabelB.setText("【B】 " + compareInfo.parentBookInfoPair().b().toString());
        
        parentPair = compareInfo.parentBookInfoPair();
        currentChildPairs = new ArrayList<>(compareInfo.childPairs());
        
        drawGrid();
    }
    
    private void drawGrid() {
        childGridPane.getChildren().clear();
        
        for (int i = 0; i < currentChildPairs.size(); i++) {
            Pair<String> pair = currentChildPairs.get(i);
            
            if (pair.isPaired()) {
                childGridPane.add(new PairedNameLabel(ItemType.SHEET, pair.a().toString()), 0, i);
                childGridPane.add(new UnpairButton(i), 1, i);
                childGridPane.add(new PairedNameLabel(ItemType.SHEET, pair.b().toString()), 2, i);
                
            } else if (pair.hasA()) {
                childGridPane.add(new DummyLabel(), 2, i);
                childGridPane.add(new UnpairedPane(i, Side.B), 0, i, 3, 1);
                childGridPane.add(new UnpairedNameLabel(ItemType.SHEET, i, Side.A, pair.a().toString()), 0, i);
                
            } else {
                childGridPane.add(new DummyLabel(), 0, i);
                childGridPane.add(new UnpairedPane(i, Side.A), 0, i, 3, 1);
                childGridPane.add(new UnpairedNameLabel(ItemType.SHEET, i, Side.B, pair.b().toString()), 2, i);
            }
        }
    }
    
    private void unpair(int i) {
        Pair<String> paired = currentChildPairs.get(i);
        Pair<String> unpairedA = Pair.of(paired.a(), null);
        Pair<String> unpairedB = Pair.of(null, paired.b());
        
        currentChildPairs.add(i + 1, unpairedA);
        currentChildPairs.add(i + 2, unpairedB);
        currentChildPairs.remove(i);
        
        drawGrid();
    }
    
    private void makePair(int src, int dst) {
        assert src != dst;
        assert 0 <= src && src < currentChildPairs.size();
        assert 0 <= dst && dst < currentChildPairs.size();
        
        Pair<String> srcPair = currentChildPairs.get(src);
        Pair<String> dstPair = currentChildPairs.get(dst);
        assert !srcPair.isPaired();
        assert !dstPair.isPaired();
        assert srcPair.hasA() != srcPair.hasB();
        assert dstPair.hasA() != dstPair.hasB();
        assert srcPair.hasA() == dstPair.hasB();
        assert srcPair.hasB() == dstPair.hasA();
        
        Pair<String> paired = Pair.of(
                srcPair.hasA() ? srcPair.a() : dstPair.a(),
                srcPair.hasB() ? srcPair.b() : dstPair.b());
        
        currentChildPairs.remove(dst);
        currentChildPairs.add(dst, paired);
        currentChildPairs.remove(src);
        
        drawGrid();
    }
    
    /**
     * ユーザーによる編集を反映したExcelブック比較情報を返します。<br>
     * 
     * @return ユーザーによる編集を反映したExcelブック比較情報
     */
    public BookCompareInfo getResult() {
        return BookCompareInfo.of(parentPair, currentChildPairs);
    }
    
    private class UnpairedPane extends Pane {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        private final Side lackedSide;
        
        private UnpairedPane(int idx, Side lackedSide) {
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
    
    private class PairedNameLabel extends Label {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private PairedNameLabel(ItemType type, String name) {
            setGraphic(type.createImageView(18));
            setGraphicTextGap(5);
            setText(name);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("childLabel");
            getStyleClass().add("pairedNameLabel");
        }
    }
    
    private class UnpairedNameLabel extends Label {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        private final Side side;
        
        private UnpairedNameLabel(ItemType type, int idx, Side side, String name) {
            this.idx = idx;
            this.side = side;
            setGraphic(type.createImageView(18));
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
    
    private class DummyLabel extends Label {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private DummyLabel() {
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("childLabel");
            getStyleClass().add("dummyLabel");
        }
    }
    
    private class UnpairButton extends Button {
        
        // static members ------------------------------------------------------
        
        private static Image linkOffImage = new Image(UnpairButton.class.getResourceAsStream("link-off.png"));
        
        // instance members ----------------------------------------------------
        
        private UnpairButton(int idx) {
            ImageView linkOffImageView = new ImageView(linkOffImage);
            linkOffImageView.setPreserveRatio(true);
            linkOffImageView.setFitWidth(16);
            setGraphic(linkOffImageView);
            getStyleClass().add("unpairButton");
            
            setOnAction(event -> unpair(idx));
        }
    }
}
