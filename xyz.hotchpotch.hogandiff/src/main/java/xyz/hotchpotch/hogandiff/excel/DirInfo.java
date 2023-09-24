package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * フォルダを表します。<br>
 *
 * @author nmby
 */
public class DirInfo implements Comparable<DirInfo> {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final Path path;
    
    private DirInfo parent;
    private List<String> bookNames;
    private List<DirInfo> children;
    
    public DirInfo(Path path) {
        Objects.requireNonNull(path);
        
        this.path = path;
    }
    
    public Path path() {
        return path;
    }
    
    public DirInfo parent() {
        return parent;
    }
    
    public void setParent(DirInfo parent) {
        this.parent = parent;
    }
    
    public List<String> bookNames() {
        return List.copyOf(bookNames);
    }
    
    public void setBookNames(List<String> bookNames) {
        this.bookNames = List.copyOf(bookNames);
    }
    
    public List<DirInfo> children() {
        return List.copyOf(children);
    }
    
    public void setChildren(List<DirInfo> children) {
        this.children = List.copyOf(children);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof DirInfo other) {
            return Objects.equals(path, other.path);
            // ある程度の深さのフォルダツリーにおいてStackOverflowエラーが発生したため、
            // 子要素はequals, hashCodeの対象に含めないこととする。
            //            return Objects.equals(path, other.path)
            //                    && Objects.equals(parent, other.parent)
            //                    && Objects.equals(bookNames, other.bookNames)
            //                    && Objects.equals(children, other.children);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(path);
        // ある程度の深さのフォルダツリーにおいてStackOverflowエラーが発生したため、
        // 子要素はequals, hashCodeの対象に含めないこととする。
        //        return Objects.hash(
        //                path,
        //                parent,
        //                bookNames,
        //                children);
    }
    
    @Override
    public int compareTo(DirInfo o) {
        Objects.requireNonNull(o, "o");
        return path.compareTo(o.path);
    }
}
