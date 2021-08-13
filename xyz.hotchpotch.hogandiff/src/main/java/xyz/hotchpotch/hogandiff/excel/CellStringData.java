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
    
    public CellStringData {
        Objects.requireNonNull(content, "content");
        if (row < 0 || column < 0) {
            throw new IllegalArgumentException("row==%d, column==%d".formatted(row, column));
        }
    }
    
    @Override
    public CellData addComment(String comment) {
        if (this.comment != null) {
            throw new IllegalStateException();
        }
        
        return new CellStringData(row, column, content, comment);
    }
    
    @Override
    public boolean hasComment() {
        return comment != null;
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
    
    @Override
    public boolean dataEquals(CellData cell) {
        if (cell instanceof CellStringData) {
            return contentEquals(cell) && commentEquals(cell);
        }
        return false;
    }
    
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
        return String.format(
                "%s: %s%s",
                address(),
                content,
                comment == null ? "" : " [comment: " + comment + "]");
    }
}
