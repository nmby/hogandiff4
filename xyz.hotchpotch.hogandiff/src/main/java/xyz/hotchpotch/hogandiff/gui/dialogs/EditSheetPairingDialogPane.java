package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

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
public class EditSheetPairingDialogPane extends VBox {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private Label parentLabelA;
    
    @FXML
    private Label parentLabelB;
    
    @FXML
    private GridPane gridPane;
    
    private Pair<BookInfo> bookInfoPair;
    private List<Pair<String>> currentPairs;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditSheetPairingDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EditPairingDialogPane2.fxml"), rb);
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
    /*package*/ void init(EditSheetPairingDialog parent, BookCompareInfo compareInfo) throws IOException {
        parentLabelA.setText("【A】" + compareInfo.parentPair().a());
        parentLabelB.setText("【B】" + compareInfo.parentPair().b());
        
        bookInfoPair = compareInfo.parentPair();
        currentPairs = new ArrayList<>(compareInfo.childPairs());
        drawGrid();
    }
    
    private void drawGrid() {
        gridPane.getChildren().clear();
        
        for (int i = 0; i < currentPairs.size(); i++) {
            Pair<String> pair = currentPairs.get(i);
            
            if (pair.isPaired()) {
                gridPane.add(new PairedNameLabel(i, Side.A, pair.a()), 0, i);
                gridPane.add(new UnpairButton(i), 1, i);
                gridPane.add(new PairedNameLabel(i, Side.B, pair.b()), 2, i);
                
            } else if (pair.hasA()) {
                gridPane.add(new UnpairedNameLabel(i, Side.A, pair.a()), 0, i);
                gridPane.add(new DummyLabel(i, Side.B), 2, i);
                
            } else {
                gridPane.add(new DummyLabel(i, Side.A), 0, i);
                gridPane.add(new UnpairedNameLabel(i, Side.B, pair.b()), 2, i);
            }
        }
    }
    
    private void unpair(int i) {
        Pair<String> paired = currentPairs.get(i);
        Pair<String> unpairedA = Pair.of(paired.a(), null);
        Pair<String> unpairedB = Pair.of(null, paired.b());
        
        currentPairs.add(i + 1, unpairedA);
        currentPairs.add(i + 2, unpairedB);
        currentPairs.remove(i);
        
        drawGrid();
    }
    
    private void makePair(int src, int dst) {
        assert src != dst;
        assert 0 <= src && src < currentPairs.size();
        assert 0 <= dst && dst < currentPairs.size();
        
        Pair<String> srcPair = currentPairs.get(src);
        Pair<String> dstPair = currentPairs.get(dst);
        assert !srcPair.isPaired();
        assert !dstPair.isPaired();
        assert srcPair.hasA() != srcPair.hasB();
        assert dstPair.hasA() != dstPair.hasB();
        assert srcPair.hasA() == dstPair.hasB();
        assert srcPair.hasB() == dstPair.hasA();
        
        Pair<String> paired = Pair.of(
                srcPair.hasA() ? srcPair.a() : dstPair.a(),
                srcPair.hasB() ? srcPair.b() : dstPair.b());
        
        currentPairs.remove(dst);
        currentPairs.add(dst, paired);
        currentPairs.remove(src);
        
        drawGrid();
    }
    
    public BookCompareInfo getResult() {
        return BookCompareInfo.of(bookInfoPair, currentPairs);
    }
    
    private class PairedNameLabel extends Label {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        private final Side side;
        
        private PairedNameLabel(int idx, Side side, String name) {
            this.idx = idx;
            this.side = side;
            setText(name);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("nameLabel");
            getStyleClass().add("pairedNameLabel");
        }
    }
    
    private class UnpairedNameLabel extends Label {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        private final Side side;
        private final String name;
        
        private UnpairedNameLabel(int idx, Side side, String name) {
            this.idx = idx;
            this.side = side;
            this.name = name;
            setText(name);
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("nameLabel");
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
        
        private final int idx;
        private final Side side;
        
        private DummyLabel(int idx, Side side) {
            this.idx = idx;
            this.side = side;
            setMaxWidth(Double.MAX_VALUE);
            getStyleClass().add("dummyLabel");
            setOnDragOver(this::onDragOver);
            setOnDragDropped(this::onDragDropped);
        }
        
        private Integer getIdx(String id) {
            if (id == null || id.length() < 2) {
                return null;
            }
            try {
                Side side = Side.valueOf(id.substring(0, 1));
                int idx = Integer.parseInt(id.substring(1));
                if (side != this.side || idx == this.idx) {
                    return null;
                }
                return idx;
                
            } catch (RuntimeException e) {
                return null;
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
        
        private void onDragDropped(DragEvent event) {
            try {
                // TODO: event.consume()の位置がおかしい気がするので見直す
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
    
    private class UnpairButton extends Button {
        
        // static members ------------------------------------------------------
        
        private static Image linkOffImage = new Image(UnpairButton.class.getResourceAsStream("link-off.png"));
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        
        private UnpairButton(int idx) {
            this.idx = idx;
            ImageView linkOffImageView = new ImageView(linkOffImage);
            setGraphic(linkOffImageView);
            getStyleClass().add("unpairButton");
            
            setOnAction(event -> unpair(idx));
        }
    }
}
