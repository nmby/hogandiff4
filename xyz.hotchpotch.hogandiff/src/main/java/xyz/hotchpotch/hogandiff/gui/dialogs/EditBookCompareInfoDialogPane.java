package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import xyz.hotchpotch.hogandiff.excel.BookCompareInfo;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * ユーザーにパスワード入力を求めるダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class EditBookCompareInfoDialogPane extends EditCompareInfoDialogPane {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private Pair<BookInfo> parentPair;
    private List<Pair<String>> currentChildPairs;
    
    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditBookCompareInfoDialogPane() throws IOException {
        super();
    }
    
    /**
     * このダイアログボックス要素を初期化します。<br>
     * 
     * @param parent 親要素
     * @param bookPath 開こうとしているExcelブックのパス
     * @param readPassword 開こうとしているExcelブックの読み取りパスワード
     */
    /*package*/ void init(
            EditCompareInfoDialog parent,
            BookCompareInfo compareInfo)
            throws IOException {
        
        super.init(ItemType.BOOK, parent);
        
        parentLabelA.setText("【A】 " + compareInfo.parentBookInfoPair().a().toString());
        parentLabelB.setText("【B】 " + compareInfo.parentBookInfoPair().b().toString());
        
        parentPair = compareInfo.parentBookInfoPair();
        currentChildPairs = new ArrayList<>(compareInfo.childSheetNamePairs());
        
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
    
    @Override
    protected void unpair(int i) {
        Pair<String> paired = currentChildPairs.get(i);
        Pair<String> unpairedA = Pair.of(paired.a(), null);
        Pair<String> unpairedB = Pair.of(null, paired.b());
        
        currentChildPairs.add(i + 1, unpairedA);
        currentChildPairs.add(i + 2, unpairedB);
        currentChildPairs.remove(i);
        
        drawGrid();
    }
    
    @Override
    protected void makePair(int src, int dst) {
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
}
