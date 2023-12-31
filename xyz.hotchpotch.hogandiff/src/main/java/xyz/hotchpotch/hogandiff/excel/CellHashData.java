package xyz.hotchpotch.hogandiff.excel;

import java.util.Objects;

import xyz.hotchpotch.hogandiff.AppMain;

/**
 * セルデータ（セル内容、セルコメント）をハッシュ値で持つ {@link CellData} の実装です。<br>
 *
 * @author nmby
 */
/*package*/ record CellHashData(
        int row,
        int column,
        int contentHash,
        int commentHash)
        implements CellData {
    
    // [static members] ********************************************************
    
    // パフォーマンス劣化を防ぐためにクラス変数に予め文字列を読み込んでおく
    static final String msg010;
    static {
        // JVM実装により万一リソースバンドル読み込み前にこのクラスロードされた場合は
        // 日本語の固定文言を設定する。
        String tmp = AppMain.appResource().get().getString("excel.CellHashData.010");
        msg010 = (tmp != null) ? tmp : "（省メモリモードではセル内容を表示できません）";
    }
    
    // [instance members] ******************************************************
    
    /*package*/ CellHashData {
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("row==%d, column==%d".formatted(row, column));
        }
    }
    
    @Override
    public boolean hasComment() {
        return commentHash != 0;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException {@code comment} が {@code null} の場合
     * @throws IllegalStateException このセルデータが既にセルコメントを保持する場合
     */
    @Override
    public CellData withComment(String comment) {
        Objects.requireNonNull(comment, "comment");
        if (commentHash != 0) {
            throw new IllegalStateException();
        }
        
        return new CellHashData(row, column, contentHash, comment.hashCode());
    }
    
    @Override
    public boolean contentEquals(CellData cell) {
        if (cell instanceof CellHashData cd) {
            return contentHash == cd.contentHash;
        }
        return false;
    }
    
    @Override
    public boolean commentEquals(CellData cell) {
        if (cell instanceof CellHashData cd) {
            return commentHash == cd.commentHash;
        }
        return false;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException 指定されたセルデータの型がこのセルデータと異なる場合
     */
    @Override
    public int dataCompareTo(CellData cell) {
        if (cell instanceof CellHashData cd) {
            return contentHash != cd.contentHash
                    ? Integer.compare(contentHash, cd.contentHash)
                    : Integer.compare(commentHash, cd.commentHash);
        }
        throw new IllegalArgumentException();
    }
    
    @Override
    public String toString() {
        return "%s: %s".formatted(address(), msg010);
    }
}
