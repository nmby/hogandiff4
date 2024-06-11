package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * フォルダ情報を表す不変クラスです。<br>
 *
 * @param dirPath このフォルダのパス
 * @param bookNames このフォルダに含まれるExcelブック名
 * @param children このフォルダに含まれる子フォルダ情報
 * @author nmby
 */
public record DirInfo(
        Path dirPath,
        List<String> bookNames,
        List<DirInfo> children)
        implements Comparable<DirInfo> {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param dirPath このフォルダのパス
     * @param bookNames このフォルダに含まれるExcelブック名
     * @param children このフォルダに含まれる子フォルダ情報
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public DirInfo {
        Objects.requireNonNull(dirPath);
        Objects.requireNonNull(bookNames);
        Objects.requireNonNull(children);
    }
    
    /**
     * {@inheritDoc}<br>
     * 
     * この実装では、{@code dirPath}、{@code children.size()}、{@code bookNames.size()}
     * に基づいて比較を行います。<br>
     */
    @Override
    public int compareTo(DirInfo o) {
        Objects.requireNonNull(o, "o");
        
        if (!Objects.equals(dirPath, o.dirPath)) {
            return dirPath.compareTo(o.dirPath);
        }
        if (children.size() != o.children.size()) {
            return Integer.compare(children.size(), o.children.size());
        }
        if (bookNames.size() != o.bookNames.size()) {
            return Integer.compare(bookNames.size(), o.bookNames.size());
        }
        return 0;
    }
}
