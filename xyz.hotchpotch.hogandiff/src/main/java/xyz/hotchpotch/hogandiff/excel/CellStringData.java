package xyz.hotchpotch.hogandiff.excel;

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
    public String toString() {
        return String.format(
                "%s: %s%s",
                address(),
                content,
                comment == null ? "" : " [comment: " + comment + "]");
    }
}
