package xyz.hotchpotch.hogandiff.excel;

import java.util.Objects;

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
        return "%s: （省メモリモードではセル内容を表示できません）".formatted(address());
    }
}
