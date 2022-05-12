package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

public class BookInfo {
    
    // [static members] ********************************************************
    
    public static BookInfo of(
            Path bookPath) {
        
        Objects.requireNonNull(bookPath, "bookPath");
        
        return new BookInfo(
                bookPath,
                BookType.of(bookPath));
    }
    
    // [instance members] ******************************************************
    
    private final Path bookPath;
    private final BookType bookType;
    
    private BookInfo(
            Path bookPath,
            BookType bookType) {
        
        assert bookPath != null && Files.isRegularFile(bookPath, LinkOption.NOFOLLOW_LINKS);
        assert bookType != null && bookType == BookType.of(bookPath);
        
        this.bookPath = bookPath;
        this.bookType = bookType;
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
