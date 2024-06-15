package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.excel.CompareInfo;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * ユーザーにパスワード入力を求めるダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class EditPairingDialogPane2<P, C, I> extends VBox {
    
    // static members **********************************************************
    
    private static class NameLabel extends Label {
        
        // static members ------------------------------------------------------
        
        public static NameLabel of(int idx, Side side, String name) {
            NameLabel label = new NameLabel(idx, side);
            label.setText(name);
            return label;
        }
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        private final Side side;
        
        private NameLabel(int idx, Side side) {
            this.idx = idx;
            this.side = side;
        }
    }
    
    private static class DummyLabel extends Label {
        
        // static members ------------------------------------------------------
        
        public static DummyLabel of(int idx, Side side) {
            DummyLabel label = new DummyLabel(idx, side);
            return label;
        }
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        private final Side side;
        
        private DummyLabel(int idx, Side side) {
            this.idx = idx;
            this.side = side;
        }
    }
    
    private static class UnpairButton extends Button {
        
        // static members ------------------------------------------------------
        
        public static UnpairButton of(int idx) {
            UnpairButton button = new UnpairButton(idx);
            return button;
        }
        
        // instance members ----------------------------------------------------
        
        private final int idx;
        
        private UnpairButton(int idx) {
            this.idx = idx;
        }
    }
    
    // instance members ********************************************************
    
    private final ResourceBundle rb = AppMain.appResource.get();
    
    @FXML
    private Label parentLabelA;
    
    @FXML
    private Label parentLabelB;
    
    @FXML
    private GridPane gridPane;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditPairingDialogPane2() throws IOException {
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
    /*package*/ void init(EditPairingDialog<P, C, I> parent, CompareInfo<P, C, I> compareInfo) throws IOException {
        parentLabelA.setText("【A】" + compareInfo.parentPair().a().toString());
        parentLabelB.setText("【B】" + compareInfo.parentPair().b().toString());
        
        for (int i = 0; i < compareInfo.childPairs().size(); i++) {
            Pair<C> pair = compareInfo.childPairs().get(i);
            
            gridPane.add(pair.hasA()
                    ? NameLabel.of(i, Side.A, pair.a().toString())
                    : DummyLabel.of(i, Side.A),
                    0, i);
            if (pair.isPaired()) {
                gridPane.add(UnpairButton.of(i), 1, i);
            }
            gridPane.add(pair.hasB()
                    ? NameLabel.of(i, Side.B, pair.b().toString())
                    : DummyLabel.of(i, Side.B),
                    2, i);
        }
    }
}
