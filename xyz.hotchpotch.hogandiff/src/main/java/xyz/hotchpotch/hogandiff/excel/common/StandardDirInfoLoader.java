package xyz.hotchpotch.hogandiff.excel.common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.DirInfo;
import xyz.hotchpotch.hogandiff.excel.DirInfoLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.util.function.UnsafeFunction;
import xyz.hotchpotch.hogandiff.util.function.UnsafeFunction.ResultOrThrown;

/**
 * {@link DirInfoLoader} の標準的な実装です。<br>
 * 
 * @author nmby
 */
public class StandardDirInfoLoader implements DirInfoLoader {
    
    // [static members] ********************************************************
    
    private static final Set<String> handleableExtensions = Set.of(
            BookType.XLSX.extension(),
            BookType.XLSM.extension(),
            BookType.XLS.extension());
    
    private static boolean isHandleableExcelBook(Path path) {
        String fileName = path.getFileName().toString();
        return handleableExtensions.stream().anyMatch(x -> fileName.endsWith(x));
    }
    
    // [instance members] ******************************************************
    
    private final boolean recursively;
    
    /**
     * コンストラクタ
     * 
     * @param recursively 子フォルダの情報も再帰的にロードするか
     */
    public StandardDirInfoLoader(boolean recursively) {
        this.recursively = recursively;
    }
    
    @Override
    public DirInfo loadDirInfo(Path path) throws ExcelHandlingException {
        Objects.requireNonNull(path, "path");
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("not directory. path: " + path);
        }
        
        return loadDir2(path);
    }
    
    private DirInfo loadDir2(Path path) throws ExcelHandlingException {
        assert path != null;
        assert Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
        
        try {
            List<Path> childBookPaths = Files.list(path)
                    .filter(f -> Files.isRegularFile(f, LinkOption.NOFOLLOW_LINKS))
                    .filter(StandardDirInfoLoader::isHandleableExcelBook)
                    .sorted()
                    .toList();
            
            List<DirInfo> childDirInfos = recursively
                    ? Files.list(path)
                            .filter(f -> Files.isDirectory(f, LinkOption.NOFOLLOW_LINKS))
                            .map(((UnsafeFunction<Path, DirInfo, ExcelHandlingException>) (this::loadDir2)).convert())
                            .filter(r -> r.result() != null)
                            .map(ResultOrThrown::result)
                            .sorted()
                            .toList()
                    : List.of();
            
            return new DirInfo(path, childBookPaths, childDirInfos);
            
        } catch (IOException e) {
            throw new ExcelHandlingException(
                    "processing failed : %s (recursively:%b)".formatted(path, recursively),
                    e);
        }
    }
}
