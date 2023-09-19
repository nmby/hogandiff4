package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * フォルダを表します。<br>
 *
 * @author nmby
 */
public class DirData {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final Path path;
    
    private DirData parent;
    private List<String> fileNames;
    private List<DirData> children;
    
    public DirData(Path path) {
        Objects.requireNonNull(path);
        
        this.path = path;
    }
    
    public Path getPath() {
        return path;
    }
    
    public DirData getParent() {
        return parent;
    }
    
    public void setParent(DirData parent) {
        this.parent = parent;
    }
    
    public List<String> getFileNames() {
        return List.copyOf(fileNames);
    }
    
    public void setFileNames(List<String> fileNames) {
        this.fileNames = List.copyOf(fileNames);
    }
    
    public List<DirData> getChildren() {
        return List.copyOf(children);
    }
    
    public void setChildren(List<DirData> children) {
        this.children = List.copyOf(children);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof DirData other) {
            return Objects.equals(path, other.path)
                    && Objects.equals(parent, other.parent)
                    && Objects.equals(fileNames, other.fileNames)
                    && Objects.equals(children, other.children);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(
                path,
                parent,
                fileNames,
                children);
    }
}
