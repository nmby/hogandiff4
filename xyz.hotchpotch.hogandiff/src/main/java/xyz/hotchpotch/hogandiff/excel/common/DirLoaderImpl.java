package xyz.hotchpotch.hogandiff.excel.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.DirData;
import xyz.hotchpotch.hogandiff.excel.DirLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.util.Tuple2;
import xyz.hotchpotch.hogandiff.util.function.UnsafeFunction;

/**
 * {@link DirLoader} の標準的な実装です。<br>
 * 
 * @author nmby
 */
public class DirLoaderImpl implements DirLoader {
    
    // [static members] ********************************************************
    
    private static final Set<String> handleableExtensions = Set.of(
            BookType.XLSX.extension(),
            BookType.XLSM.extension(),
            BookType.XLS.extension());
    
    private static boolean isHandleableExcelBook(Path path) {
        String fileName = path.getFileName().toString();
        return handleableExtensions.stream().anyMatch(x -> fileName.endsWith(x));
    }
    
    public static DirLoader of() {
        return new DirLoaderImpl();
    }
    
    // [instance members] ******************************************************
    
    private DirLoaderImpl() {
    }
    
    @Override
    public DirData loadDirs(Path path, boolean recursively) throws ExcelHandlingException {
        Objects.requireNonNull(path, "path");
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("not directory. path: " + path);
        }
        
        try {
            List<DirData> children = recursively
                    ? Files.list(path)
                            .filter(f -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                            .map(((UnsafeFunction<Path, DirData>) (p -> loadDirs(p, true))).convert())
                            .filter(t -> t.item1() != null)
                            .map(Tuple2::item1)
                            .toList()
                    : List.of();
            
            List<String> fileNames = Files.list(path)
                    .filter(f -> Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS))
                    .filter(DirLoaderImpl::isHandleableExcelBook)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
            
            return new DirData(
                    path,
                    fileNames,
                    children);
            
        } catch (IOException e) {
            throw new ExcelHandlingException(
                    "processing failed : %s (recursively:%b)".formatted(path, recursively),
                    e);
        }
    }
}
