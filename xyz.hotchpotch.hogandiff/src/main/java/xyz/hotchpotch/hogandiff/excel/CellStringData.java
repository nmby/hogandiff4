package xyz.hotchpotch.hogandiff.excel;

import java.util.Comparator;
import java.util.Objects;

/**
 * セルデータ（セル内容、セルコメント）を {@link String} で持つ {@link CellData} の実装です。<br>
 *
 * @author nmby
 */
/*package*/ record CellStringData(
        int row,
        int column,
        String content,
        String comment)
        implements CellData {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /*package*/ CellStringData {
        Objects.requireNonNull(content, "content");
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("row==%d, column==%d".formatted(row, column));
        }
    }
    
    @Override
    public boolean hasComment() {
        return comment != null;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException {@code comment} が {@code null} の場合
     * @throws IllegalStateException このセルデータが既にセルコメントを保持する場合
     */
    @Override
    public CellData addComment(String comment) {
        Objects.requireNonNull(comment, "comment");
        if (this.comment != null) {
            throw new IllegalStateException();
        }
        
        return new CellStringData(row, column, content, comment);
    }
    
    @Override
    public boolean contentEquals(CellData cell) {
        if (cell instanceof CellStringData cd) {
            return content.equals(cd.content);
        }
        return false;
    }
    
    @Override
    public boolean commentEquals(CellData cell) {
        if (cell instanceof CellStringData cd) {
            return comment == null
                    ? cd.comment == null
                    : comment.equals(cd.comment);
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
        if (cell instanceof CellStringData cd) {
            return !contentEquals(cell)
                    ? content.compareTo(cd.content)
                    : Objects.compare(comment, cd.comment, Comparator.naturalOrder());
        }
        throw new IllegalArgumentException();
    }
    
    @Override
    public String toString() {
        return "%s: %s%s".formatted(
                address(),
                content,
                comment == null ? "" : " [comment: " + comment + "]");
    }
}
