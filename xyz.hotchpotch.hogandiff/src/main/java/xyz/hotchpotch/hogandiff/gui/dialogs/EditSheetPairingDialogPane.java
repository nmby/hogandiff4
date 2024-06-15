package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
            
            gridPane.add(pair.hasA()
                    ? new NameLabel(i, Side.A, pair.a())
                    : new DummyLabel(i, Side.A),
                    0, i);
            if (pair.isPaired()) {
                gridPane.add(new UnpairButton(i), 1, i);
            }
            gridPane.add(pair.hasB()
                    ? new NameLabel(i, Side.B, pair.b())
                    : new DummyLabel(i, Side.B),
                    2, i);
        }
    }
    
    private void unpair(int i) {
        System.out.println("unpair: " + i);
        
        Pair<String> paired = currentPairs.get(i);
        Pair<String> unpairedA = Pair.of(paired.a(), null);
        Pair<String> unpairedB = Pair.of(null, paired.b());
        
        currentPairs.add(i + 1, unpairedA);
        currentPairs.add(i + 2, unpairedB);
        currentPairs.remove(i);
        
        drawGrid();
    }
    
    public BookCompareInfo getResult() {
        return BookCompareInfo.of(bookInfoPair, currentPairs);
    }
    
    private class NameLabel extends Label {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        private final Side side;
        
        private NameLabel(int idx, Side side, String name) {
            this.idx = idx;
            this.side = side;
            setText(name);
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
        }
    }
    
    private class UnpairButton extends Button {
        
        // static members ------------------------------------------------------
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        
        private UnpairButton(int idx) {
            this.idx = idx;
            
            setOnAction(event -> unpair(idx));
        }
    }
}
