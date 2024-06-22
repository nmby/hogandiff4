package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.List;

import xyz.hotchpotch.hogandiff.excel.BookCompareInfo;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * ユーザーにパスワード入力を求めるダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class EditBookCompareInfoDialogPane extends EditCompareInfoDialogPane<BookInfo, String> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
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
     * @param dialog 親要素
     * @param bookPath 開こうとしているExcelブックのパス
     * @param readPassword 開こうとしているExcelブックの読み取りパスワード
     */
    /*package*/ void init(
            EditCompareInfoDialog dialog,
            BookCompareInfo compareInfo)
            throws IOException {
        
        super.init(
                dialog,
                ItemType.BOOK,
                compareInfo.parentBookInfoPair(),
                compareInfo.childSheetNamePairs());
        
        currentChildPairs.addAll(childPairs);
        drawGrid();
    }
    
    @Override
    protected void unpair(int i) {
        @SuppressWarnings("unchecked")
        Pair<String> paired = (Pair<String>) currentChildPairs.get(i);
        assert paired.isPaired();
        
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
        
        @SuppressWarnings("unchecked")
        Pair<String> srcPair = (Pair<String>) currentChildPairs.get(src);
        @SuppressWarnings("unchecked")
        Pair<String> dstPair = (Pair<String>) currentChildPairs.get(dst);
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
        // わざわざこんなことせにゃならんのか？？
        @SuppressWarnings("unchecked")
        List<Pair<String>> casted = currentChildPairs.stream()
                .map(p -> (Pair<String>) p)
                .toList();
        return BookCompareInfo.of(parentPair, casted);
    }
}
