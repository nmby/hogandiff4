package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * フォルダを表します。<br>
 *
 * @author nmby
 */
public record DirData(
        Path path,
        List<String> fileNames,
        List<DirData> children) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    public DirData(
            Path path,
            List<String> fileNames,
            List<DirData> children) {
        
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(fileNames, "fileNames");
        Objects.requireNonNull(children, "children");
        
        this.path = path;
        this.fileNames = List.copyOf(fileNames);
        this.children = List.copyOf(children);
    }
}
