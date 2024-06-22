package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.List;

import xyz.hotchpotch.hogandiff.excel.BookInfoComparison;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * Excelブック比較情報編集ダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class EditBookInfoComparisonDialogPane extends EditComparisonDialogPane<BookInfoComparison> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final BookInfoComparison bookInfoComparison;
    
    /**
     * コンストラクタ<br>
     * 
     * @param bookInfoComparison Excelブック比較情報
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditBookInfoComparisonDialogPane(BookInfoComparison bookInfoComparison) throws IOException {
        super();
        this.bookInfoComparison = bookInfoComparison;
    }
    
    /*package*/ void init() throws IOException {
        super.init(bookInfoComparison.parentBookInfoPair());
        
        currentChildPairs.addAll(bookInfoComparison.childSheetNamePairs());
        drawGrid();
    }
    
    @Override
    protected void unpair(int i) {
        Pair<?> paired = currentChildPairs.get(i);
        assert paired.isPaired();
        
        Pair<?> unpairedA = Pair.of(paired.a(), null);
        Pair<?> unpairedB = Pair.of(null, paired.b());
        
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
        
        Pair<?> srcPair = currentChildPairs.get(src);
        Pair<?> dstPair = currentChildPairs.get(dst);
        assert !srcPair.isPaired();
        assert !dstPair.isPaired();
        assert srcPair.hasA() != srcPair.hasB();
        assert dstPair.hasA() != dstPair.hasB();
        assert srcPair.hasA() == dstPair.hasB();
        assert srcPair.hasB() == dstPair.hasA();
        
        Pair<?> paired = Pair.of(
                srcPair.hasA() ? srcPair.a() : dstPair.a(),
                srcPair.hasB() ? srcPair.b() : dstPair.b());
        
        currentChildPairs.remove(dst);
        currentChildPairs.add(dst, paired);
        currentChildPairs.remove(src);
        
        drawGrid();
    }
    
    @Override
    public BookInfoComparison getResult() {
        // わざわざこんなことせにゃならんのか？？
        @SuppressWarnings("unchecked")
        List<Pair<String>> casted = currentChildPairs.stream()
                .map(p -> (Pair<String>) p)
                .toList();
        return new BookInfoComparison(bookInfoComparison.parentBookInfoPair(), casted);
    }
}
