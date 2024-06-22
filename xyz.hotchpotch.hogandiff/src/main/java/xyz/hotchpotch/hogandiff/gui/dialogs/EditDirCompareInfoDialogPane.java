package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.AppResource;
import xyz.hotchpotch.hogandiff.SettingKeys;
import xyz.hotchpotch.hogandiff.excel.BookCompareInfo;
import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.DirCompareInfo;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.Factory;
import xyz.hotchpotch.hogandiff.excel.SheetNamesLoader;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * フォルダ比較情報編集ダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class EditDirCompareInfoDialogPane extends EditCompareInfoDialogPane<DirCompareInfo> {
    
    // static members **********************************************************
    
    // instance members ********************************************************
    
    private final AppResource ar = AppMain.appResource;
    
    private final DirCompareInfo dirCompareInfo;
    private final List<Pair<DirInfo>> currChildDirInfoPairs;
    private final List<Pair<Path>> currChildBookPathPairs;
    private final Map<Pair<DirInfo>, Optional<DirCompareInfo>> currChildDirCompareInfos;
    private final Map<Pair<Path>, Optional<BookCompareInfo>> currChildBookCompareInfos;
    
    /**
     * コンストラクタ<br>
     * 
     * @param dirCompareInfo フォルダ比較情報
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditDirCompareInfoDialogPane(DirCompareInfo dirCompareInfo) throws IOException {
        super();
        this.dirCompareInfo = dirCompareInfo;
        this.currChildDirInfoPairs = new ArrayList<>(dirCompareInfo.childDirInfoPairs());
        this.currChildBookPathPairs = new ArrayList<>(dirCompareInfo.childBookPathPairs());
        this.currChildDirCompareInfos = new HashMap<>(dirCompareInfo.childDirCompareInfos());
        this.currChildBookCompareInfos = new HashMap<>(dirCompareInfo.childBookCompareInfos());
    }
    
    /*package*/ void init() throws IOException {
        super.init(dirCompareInfo.parentDirInfoPair());
        
        updateChildren();
    }
    
    private void updateChildren() {
        currentChildPairs.clear();
        currentChildPairs.addAll(currChildDirInfoPairs);
        currentChildPairs.addAll(currChildBookPathPairs);
        drawGrid();
    }
    
    private Optional<DirCompareInfo> createDirCompareInfo(Pair<DirInfo> dirInfoPair) {
        return Optional.of(
                DirCompareInfo.calculate(
                        dirInfoPair,
                        Factory.dirInfosMatcher(ar.settings()),
                        Factory.bookPathsMatcher(ar.settings()),
                        Factory.sheetNamesMatcher(ar.settings()),
                        ar.settings().get(SettingKeys.CURR_READ_PASSWORDS)));
    }
    
    private Optional<BookCompareInfo> createBookCompareInfo(Pair<Path> bookPathPair) {
        Map<Path, String> readPasswords = ar.settings().get(SettingKeys.CURR_READ_PASSWORDS);
        
        Pair<BookInfo> bookInfoPair = Side.map(
                side -> {
                    try {
                        if (bookPathPair.has(side)) {
                            Path bookPath = bookPathPair.get(side);
                            String readPassword = readPasswords.get(bookPath);
                            SheetNamesLoader sheetNamesLoader = Factory.sheetNamesLoader(bookPath, readPassword);
                            return sheetNamesLoader.loadSheetNames(bookPath, readPassword);
                        } else {
                            return null;
                        }
                    } catch (ExcelHandlingException e) {
                        return null;
                    }
                });
        
        return Optional.of(BookCompareInfo.calculate(bookInfoPair, Factory.sheetNamesMatcher(ar.settings())));
    }
    
    @Override
    protected void unpair(int i) {
        int childDirs = currChildDirInfoPairs.size();
        int childBooks = currChildBookPathPairs.size();
        
        if (0 <= i && i < childDirs) {
            Pair<DirInfo> paired = currChildDirInfoPairs.get(i);
            assert paired.isPaired();
            
            Pair<DirInfo> unpairedA = Pair.of(paired.a(), null);
            Pair<DirInfo> unpairedB = Pair.of(null, paired.b());
            
            currChildDirInfoPairs.add(i + 1, unpairedA);
            currChildDirInfoPairs.add(i + 2, unpairedB);
            currChildDirInfoPairs.remove(i);
            
            currChildDirCompareInfos.remove(paired);
            currChildDirCompareInfos.put(unpairedA, createDirCompareInfo(unpairedA));
            currChildDirCompareInfos.put(unpairedB, createDirCompareInfo(unpairedB));
            
        } else if (childDirs <= i && i < childDirs + childBooks) {
            int j = i - childDirs;
            
            Pair<Path> paired = currChildBookPathPairs.get(j);
            assert paired.isPaired();
            
            Pair<Path> unpairedA = Pair.of(paired.a(), null);
            Pair<Path> unpairedB = Pair.of(null, paired.b());
            
            currChildBookPathPairs.add(j + 1, unpairedA);
            currChildBookPathPairs.add(j + 2, unpairedB);
            currChildBookPathPairs.remove(j);
            
            currChildBookCompareInfos.remove(paired);
            currChildBookCompareInfos.put(unpairedA, createBookCompareInfo(unpairedA));
            currChildBookCompareInfos.put(unpairedB, createBookCompareInfo(unpairedB));
            
        } else {
            throw new AssertionError();
        }
        
        updateChildren();
    }
    
    @Override
    protected void makePair(int src, int dst) {
        assert src != dst;
        
        int childDirs = currChildDirInfoPairs.size();
        int childBooks = currChildBookPathPairs.size();
        
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
            
            currChildDirCompareInfos.remove(srcPair);
            currChildDirCompareInfos.remove(dstPair);
            currChildDirCompareInfos.put(paired, createDirCompareInfo(paired));
            
        } else if (childDirs <= src && src < childDirs + childBooks) {
            assert childDirs <= dst && dst < childDirs + childBooks;
            
            int src2 = src - childDirs;
            int dst2 = dst - childDirs;
            
            Pair<Path> srcPair = currChildBookPathPairs.get(src2);
            Pair<Path> dstPair = currChildBookPathPairs.get(dst2);
            assert !srcPair.isPaired();
            assert !dstPair.isPaired();
            assert srcPair.hasA() != srcPair.hasB();
            assert dstPair.hasA() != dstPair.hasB();
            assert srcPair.hasA() == dstPair.hasB();
            assert srcPair.hasB() == dstPair.hasA();
            
            Pair<Path> paired = Pair.of(
                    srcPair.hasA() ? srcPair.a() : dstPair.a(),
                    srcPair.hasB() ? srcPair.b() : dstPair.b());
            
            currChildBookPathPairs.remove(dst2);
            currChildBookPathPairs.add(dst2, paired);
            currChildBookPathPairs.remove(src2);
            
            currChildBookCompareInfos.remove(srcPair);
            currChildBookCompareInfos.remove(dstPair);
            currChildBookCompareInfos.put(paired, createBookCompareInfo(paired));
            
        } else {
            throw new AssertionError();
        }
        
        updateChildren();
    }
    
    @Override
    public DirCompareInfo getResult() {
        return new DirCompareInfo(
                dirCompareInfo.parentDirInfoPair(),
                currChildDirInfoPairs,
                currChildDirCompareInfos,
                currChildBookPathPairs,
                currChildBookCompareInfos);
    }
}
