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
import xyz.hotchpotch.hogandiff.core.Matcher;
import xyz.hotchpotch.hogandiff.logic.Factory;
import xyz.hotchpotch.hogandiff.logic.bookloaders.LoaderForBooks;
import xyz.hotchpotch.hogandiff.logic.models.BookInfo;
import xyz.hotchpotch.hogandiff.logic.models.DirInfo;
import xyz.hotchpotch.hogandiff.logic.models.PairingInfoBooks;
import xyz.hotchpotch.hogandiff.logic.models.PairingInfoDirs;
import xyz.hotchpotch.hogandiff.logic.models.BookInfo.Status;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * フォルダ比較情報編集ダイアログボックスの要素です。<br>
 * 
 * @author nmby
 */
public class EditDirComparisonDialogPane extends EditComparisonDialogPane<PairingInfoDirs> {

    // static members **********************************************************

    // instance members ********************************************************

    private final AppResource ar = AppMain.appResource;

    private final PairingInfoDirs dirComparison;
    private final List<Pair<DirInfo>> currChildDirInfoPairs;
    private final List<Pair<BookInfo>> currChildBookInfoPairs;
    private final Map<Pair<DirInfo>, Optional<PairingInfoDirs>> currChildDirComparisons;
    private final Map<Pair<BookInfo>, Optional<PairingInfoBooks>> currChildBookComparisons;

    /**
     * コンストラクタ<br>
     * 
     * @param dirComparison フォルダ比較情報
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditDirComparisonDialogPane(PairingInfoDirs dirComparison) throws IOException {
        super();
        this.dirComparison = dirComparison;
        this.currChildDirInfoPairs = new ArrayList<>(dirComparison.childDirInfoPairs());
        this.currChildBookInfoPairs = new ArrayList<>(dirComparison.childBookInfoPairs());
        this.currChildDirComparisons = new HashMap<>(dirComparison.childDirComparisons());
        this.currChildBookComparisons = new HashMap<>(dirComparison.childBookComparisons());
    }

    /* package */ void init() throws IOException {
        super.init(dirComparison.parentDirInfoPair());

        updateChildren();
    }

    private void updateChildren() {
        currentChildPairs.clear();
        currentChildPairs.addAll(currChildDirInfoPairs);
        currentChildPairs.addAll(currChildBookInfoPairs);
        drawGrid();
    }

    private Optional<PairingInfoDirs> createDirComparison(Pair<DirInfo> dirInfoPair) {
        return Optional.of(
                PairingInfoDirs.calculate(
                        dirInfoPair,
                        Factory.dirInfosMatcher(ar.settings()),
                        Factory.bookInfosMatcher(ar.settings()),
                        Factory.sheetNamesMatcher(ar.settings()),
                        ar.settings().get(SettingKeys.CURR_READ_PASSWORDS)));
    }

    private Optional<PairingInfoBooks> createBookComparison(Pair<BookInfo> bookInfoPair) {
        return Optional.of(PairingInfoBooks.calculate(bookInfoPair, Factory.sheetNamesMatcher(ar.settings())));
    }

    @Override
    public PairingInfoDirs getResult() {
        return new PairingInfoDirs(
                dirComparison.parentDirInfoPair(),
                currChildDirInfoPairs,
                currChildDirComparisons,
                currChildBookInfoPairs,
                currChildBookComparisons);
    }

    @Override
    protected void unpair(int idx) {
        int childDirs = currChildDirInfoPairs.size();
        int childBooks = currChildBookInfoPairs.size();

        if (0 <= idx && idx < childDirs) {
            Pair<DirInfo> paired = currChildDirInfoPairs.get(idx);
            assert paired.isPaired();

            Pair<DirInfo> unpairedA = Pair.of(paired.a(), null);
            Pair<DirInfo> unpairedB = Pair.of(null, paired.b());

            currChildDirInfoPairs.add(idx + 1, unpairedA);
            currChildDirInfoPairs.add(idx + 2, unpairedB);
            currChildDirInfoPairs.remove(idx);

            currChildDirComparisons.remove(paired);
            currChildDirComparisons.put(unpairedA, createDirComparison(unpairedA));
            currChildDirComparisons.put(unpairedB, createDirComparison(unpairedB));

        } else if (childDirs <= idx && idx < childDirs + childBooks) {
            int idx2 = idx - childDirs;

            Pair<BookInfo> paired = currChildBookInfoPairs.get(idx2);
            assert paired.isPaired();

            Pair<BookInfo> unpairedA = Pair.of(paired.a(), null);
            Pair<BookInfo> unpairedB = Pair.of(null, paired.b());

            currChildBookInfoPairs.add(idx2 + 1, unpairedA);
            currChildBookInfoPairs.add(idx2 + 2, unpairedB);
            currChildBookInfoPairs.remove(idx2);

            Matcher<String> sheetNamesMatcher = Factory.sheetNamesMatcher(ar.settings());
            PairingInfoBooks bookComparisonA = PairingInfoBooks.calculate(unpairedA, sheetNamesMatcher);
            PairingInfoBooks bookComparisonB = PairingInfoBooks.calculate(unpairedA, sheetNamesMatcher);

            currChildBookComparisons.remove(paired);
            currChildBookComparisons.put(unpairedA, Optional.of(bookComparisonA));
            currChildBookComparisons.put(unpairedB, Optional.of(bookComparisonB));

        } else {
            throw new AssertionError();
        }

        updateChildren();
    }

    @Override
    protected void makePair(int srcIdx, int dstIdx) {
        assert srcIdx != dstIdx;

        int childDirs = currChildDirInfoPairs.size();
        int childBooks = currChildBookInfoPairs.size();

        if (0 <= srcIdx && srcIdx < childDirs) {
            assert 0 <= dstIdx && dstIdx < childDirs;

            Pair<DirInfo> srcPair = currChildDirInfoPairs.get(srcIdx);
            Pair<DirInfo> dstPair = currChildDirInfoPairs.get(dstIdx);
            assert !srcPair.isPaired();
            assert !dstPair.isPaired();
            assert srcPair.hasA() != srcPair.hasB();
            assert dstPair.hasA() != dstPair.hasB();
            assert srcPair.hasA() == dstPair.hasB();
            assert srcPair.hasB() == dstPair.hasA();

            Pair<DirInfo> paired = Pair.of(
                    srcPair.hasA() ? srcPair.a() : dstPair.a(),
                    srcPair.hasB() ? srcPair.b() : dstPair.b());

            currChildDirInfoPairs.remove(dstIdx);
            currChildDirInfoPairs.add(dstIdx, paired);
            currChildDirInfoPairs.remove(srcIdx);

            currChildDirComparisons.remove(srcPair);
            currChildDirComparisons.remove(dstPair);
            currChildDirComparisons.put(paired, createDirComparison(paired));

        } else if (childDirs <= srcIdx && srcIdx < childDirs + childBooks) {
            assert childDirs <= dstIdx && dstIdx < childDirs + childBooks;

            int src2 = srcIdx - childDirs;
            int dst2 = dstIdx - childDirs;

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
    protected void onClickPaired(int idx) {
        int childDirs = currChildDirInfoPairs.size();
        int childBooks = currChildBookInfoPairs.size();

        try {
            if (0 <= idx && idx < childDirs) {
                Pair<DirInfo> paired = currChildDirInfoPairs.get(idx);
                assert paired.isPaired();

                PairingInfoDirs comparison = currChildDirComparisons.get(paired).orElseThrow();
                EditComparisonDialog<PairingInfoDirs> dialog = new EditComparisonDialog<>(comparison);
                Optional<PairingInfoDirs> modified = dialog.showAndWait();
                if (modified.isPresent()) {
                    currChildDirComparisons.put(paired, modified);
                }

            } else if (childDirs <= idx && idx < childDirs + childBooks) {
                int j = idx - childDirs;

                Pair<BookInfo> paired = currChildBookInfoPairs.get(j);
                assert paired.isPaired();

                PairingInfoBooks comparison = currChildBookComparisons.get(paired).orElseThrow();
                EditComparisonDialog<PairingInfoBooks> dialog = new EditComparisonDialog<>(comparison);
                Optional<PairingInfoBooks> modified = dialog.showAndWait();
                if (modified.isPresent()) {
                    currChildBookComparisons.put(paired, modified);
                }

            } else {
                throw new AssertionError();
            }

            updateChildren();

        } catch (Exception e) {
            e.printStackTrace();
            // nop
        }
    }

    @Override
    protected void onPasswordChallenge(int idx, Side side) {
        int childDirs = currChildDirInfoPairs.size();
        int childBooks = currChildBookInfoPairs.size();
        int idx2 = idx - childDirs;
        assert 0 <= idx2 && idx2 < childBooks;

        Pair<BookInfo> currBookInfoPair = currChildBookInfoPairs.get(idx2);
        assert currBookInfoPair.get(side).status() == Status.NEEDS_PASSWORD;

        Path bookPath = currBookInfoPair.get(side).bookPath();
        Map<Path, String> readPasswords = ar.settings().get(SettingKeys.CURR_READ_PASSWORDS);

        try {
            String readPassword = readPasswords.get(bookPath);
            LoaderForBooks loader = Factory.bookLoader(bookPath);
            BookInfo newBookInfo = null;

            while (true) {
                newBookInfo = loader.loadBookInfo(bookPath, readPassword);

                if (newBookInfo.status() == Status.LOAD_COMPLETED) {
                    break;
                }

                PasswordDialog dialog = new PasswordDialog(bookPath, readPassword);
                Optional<String> newPassword = dialog.showAndWait();
                if (newPassword.isPresent()) {
                    readPassword = newPassword.get();
                } else {
                    return;
                }
            }

            // 読み込みに成功した場合
            readPasswords.put(bookPath, readPassword);
            ar.changeSetting(SettingKeys.CURR_READ_PASSWORDS, readPasswords);

            Pair<BookInfo> newBookInfoPair = Pair.of(
                    side == Side.A ? newBookInfo : currBookInfoPair.a(),
                    side == Side.B ? newBookInfo : currBookInfoPair.b());
            PairingInfoBooks newBookComparison = PairingInfoBooks.calculate(
                    newBookInfoPair,
                    Factory.sheetNamesMatcher(ar.settings()));

            currChildBookInfoPairs.remove(idx2);
            currChildBookInfoPairs.add(idx2, newBookInfoPair);
            currChildBookComparisons.put(newBookInfoPair, Optional.of(newBookComparison));

            updateChildren();

        } catch (Exception e) {
            // nop
        }
    }
}
