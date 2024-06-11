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
        List<DirInfo> children) {
    
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
    public DirInfo(
            Path dirPath,
            List<String> bookNames,
            List<DirInfo> children) {
        
        Objects.requireNonNull(dirPath);
        Objects.requireNonNull(bookNames);
        Objects.requireNonNull(children);
        
        this.dirPath = dirPath;
        this.bookNames = List.copyOf(bookNames);
        this.children = List.copyOf(children);
    }
}
