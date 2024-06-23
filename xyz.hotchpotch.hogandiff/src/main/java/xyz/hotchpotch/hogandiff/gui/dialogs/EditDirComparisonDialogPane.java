package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.excel.BookComparison;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.DirComparison;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * フォルダ比較情報編集ダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class EditDirComparisonDialogPane extends EditComparisonDialogPane<DirComparison> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    private final DirComparison dirComparison;
    private final List<Pair<DirInfo>> currChildDirInfoPairs;
    private final List<Pair<BookInfo>> currChildBookInfoPairs;
    private final Map<Pair<DirInfo>, Optional<DirComparison>> currChildDirComparisons;
    private final Map<Pair<BookInfo>, Optional<BookComparison>> currChildBookComparisons;
    
    /**
     * コンストラクタ<br>
     * 
     * @param dirComparison フォルダ比較情報
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditDirComparisonDialogPane(DirComparison dirComparison) throws IOException {
        super();
        this.dirComparison = dirComparison;
        this.currChildDirInfoPairs = new ArrayList<>(dirComparison.childDirInfoPairs());
        this.currChildBookInfoPairs = new ArrayList<>(dirComparison.childBookInfoPairs());
        this.currChildDirComparisons = new HashMap<>(dirComparison.childDirComparisons());
        this.currChildBookComparisons = new HashMap<>(dirComparison.childBookComparisons());
    }
    
    /*package*/ void init() throws IOException {
        super.init(dirComparison.parentDirInfoPair());
        
        updateChildren();
    }
    
    private void updateChildren() {
        currentChildPairs.clear();
        currentChildPairs.addAll(currChildDirInfoPairs);
        currentChildPairs.addAll(currChildBookInfoPairs);
        drawGrid();
    }
    
    private Optional<DirComparison> createDirComparison(Pair<DirInfo> dirInfoPair) {
        return Optional.of(
                DirComparison.calculate(
                        dirInfoPair,
                        Factory.dirInfosMatcher(ar.settings()),
                        Factory.bookInfosMatcher(ar.settings()),
                        Factory.sheetNamesMatcher(ar.settings()),
                        ar.settings().get(SettingKeys.CURR_READ_PASSWORDS)));
    }
    
    private Optional<BookComparison> createBookComparison(Pair<BookInfo> bookInfoPair) {
        return Optional.of(BookComparison.calculate(bookInfoPair, Factory.sheetNamesMatcher(ar.settings())));
    }
    
    @Override
    protected void unpair(int i) {
        int childDirs = currChildDirInfoPairs.size();
        int childBooks = currChildBookInfoPairs.size();
        
        if (0 <= i && i < childDirs) {
            Pair<DirInfo> paired = currChildDirInfoPairs.get(i);
            assert paired.isPaired();
            
            Pair<DirInfo> unpairedA = Pair.of(paired.a(), null);
            Pair<DirInfo> unpairedB = Pair.of(null, paired.b());
            
            currChildDirInfoPairs.add(i + 1, unpairedA);
            currChildDirInfoPairs.add(i + 2, unpairedB);
            currChildDirInfoPairs.remove(i);
            
            currChildDirComparisons.remove(paired);
            currChildDirComparisons.put(unpairedA, createDirComparison(unpairedA));
            currChildDirComparisons.put(unpairedB, createDirComparison(unpairedB));
            
        } else if (childDirs <= i && i < childDirs + childBooks) {
            int j = i - childDirs;
            
            Pair<BookInfo> paired = currChildBookInfoPairs.get(j);
            assert paired.isPaired();
            
            Pair<BookInfo> unpairedA = Pair.of(paired.a(), null);
            Pair<BookInfo> unpairedB = Pair.of(null, paired.b());
            
            currChildBookInfoPairs.add(j + 1, unpairedA);
            currChildBookInfoPairs.add(j + 2, unpairedB);
            currChildBookInfoPairs.remove(j);
            
            Matcher<String> sheetNamesMatcher = Factory.sheetNamesMatcher(ar.settings());
            BookComparison bookComparisonA = BookComparison.calculate(unpairedA, sheetNamesMatcher);
            BookComparison bookComparisonB = BookComparison.calculate(unpairedA, sheetNamesMatcher);
            
            currChildBookComparisons.remove(paired);
            currChildBookComparisons.put(unpairedA, Optional.of(bookComparisonA));
            currChildBookComparisons.put(unpairedB, Optional.of(bookComparisonB));
            
        } else {
            throw new AssertionError();
        }
        
        updateChildren();
    }
    
    @Override
    protected void makePair(int src, int dst) {
        assert src != dst;
        
        int childDirs = currChildDirInfoPairs.size();
        int childBooks = currChildBookInfoPairs.size();
        
        if (0 <= src && src < childDirs) {
            assert 0 <= dst && dst < childDirs;
            
            Pair<DirInfo> srcPair = currChildDirInfoPairs.get(src);
            Pair<DirInfo> dstPair = currChildDirInfoPairs.get(dst);
            assert !srcPair.isPaired();
            assert !dstPair.isPaired();
            assert srcPair.hasA() != srcPair.hasB();
            assert dstPair.hasA() != dstPair.hasB();
            assert srcPair.hasA() == dstPair.hasB();
            assert srcPair.hasB() == dstPair.hasA();
            
            Pair<DirInfo> paired = Pair.of(
                    srcPair.hasA() ? srcPair.a() : dstPair.a(),
                    srcPair.hasB() ? srcPair.b() : dstPair.b());
            
            currChildDirInfoPairs.remove(dst);
            currChildDirInfoPairs.add(dst, paired);
            currChildDirInfoPairs.remove(src);
            
            currChildDirComparisons.remove(srcPair);
            currChildDirComparisons.remove(dstPair);
            currChildDirComparisons.put(paired, createDirComparison(paired));
            
        } else if (childDirs <= src && src < childDirs + childBooks) {
            assert childDirs <= dst && dst < childDirs + childBooks;
            
            int src2 = src - childDirs;
            int dst2 = dst - childDirs;
            
            Pair<BookInfo> srcPair = currChildBookInfoPairs.get(src2);
            Pair<BookInfo> dstPair = currChildBookInfoPairs.get(dst2);
            assert !srcPair.isPaired();
            assert !dstPair.isPaired();
            assert srcPair.hasA() != srcPair.hasB();
            assert dstPair.hasA() != dstPair.hasB();
            assert srcPair.hasA() == dstPair.hasB();
            assert srcPair.hasB() == dstPair.hasA();
            
            Pair<BookInfo> paired = Pair.of(
                    srcPair.hasA() ? srcPair.a() : dstPair.a(),
                    srcPair.hasB() ? srcPair.b() : dstPair.b());
            
            currChildBookInfoPairs.remove(dst2);
            currChildBookInfoPairs.add(dst2, paired);
            currChildBookInfoPairs.remove(src2);
            
            currChildBookComparisons.remove(srcPair);
            currChildBookComparisons.remove(dstPair);
            currChildBookComparisons.put(paired, createBookComparison(paired));
            
        } else {
            throw new AssertionError();
        }
        
        updateChildren();
    }
    
    @Override
    protected void onClickPaired(int i) {
        int childDirs = currChildDirInfoPairs.size();
        int childBooks = currChildBookInfoPairs.size();
        
        try {
            if (0 <= i && i < childDirs) {
                Pair<DirInfo> paired = currChildDirInfoPairs.get(i);
                assert paired.isPaired();
                
                DirComparison comparison = currChildDirComparisons.get(paired).orElseThrow();
                EditComparisonDialog<DirComparison> dialog = new EditComparisonDialog<>(comparison);
                Optional<DirComparison> modified = dialog.showAndWait();
                if (modified.isPresent()) {
                    currChildDirComparisons.put(paired, modified);
                }
                
            } else if (childDirs <= i && i < childDirs + childBooks) {
                int j = i - childDirs;
                
                Pair<BookInfo> paired = currChildBookInfoPairs.get(j);
                assert paired.isPaired();
                
                BookComparison comparison = currChildBookComparisons.get(paired).orElseThrow();
                EditComparisonDialog<BookComparison> dialog = new EditComparisonDialog<>(comparison);
                Optional<BookComparison> modified = dialog.showAndWait();
                if (modified.isPresent()) {
                    currChildBookComparisons.put(paired, modified);
                }
                
            } else {
                throw new AssertionError();
            }
            
            updateChildren();
            
        } catch (Exception e) {
        }
    }
    
    @Override
    public DirComparison getResult() {
        return new DirComparison(
                dirComparison.parentDirInfoPair(),
                currChildDirInfoPairs,
                currChildDirComparisons,
                currChildBookInfoPairs,
                currChildBookComparisons);
    }
}
