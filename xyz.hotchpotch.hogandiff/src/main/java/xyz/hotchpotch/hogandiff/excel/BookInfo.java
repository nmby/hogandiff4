package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.Objects;

public class BookInfo {
    
    // [static members] ********************************************************
    
    public static BookInfo of(
            Path bookPath) {
        
        Objects.requireNonNull(bookPath, "bookPath");
        
        return new BookInfo(bookPath);
    }
    
    // [instance members] ******************************************************
    
    private final Path bookPath;
    private final BookType bookType;
    
    private BookInfo(
            Path bookPath) {
        
        assert bookPath != null;
        
        this.bookPath = bookPath;
        this.bookType = BookType.of(bookPath);
    }
    
    public Path bookPath() {
        return bookPath;
    }
    
    public BookType bookType() {
        return bookType;
    }
    
    @Override
    public String toString() {
        return bookPath.toString();
    }
}
