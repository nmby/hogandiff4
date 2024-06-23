package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * フォルダ情報を表す不変クラスです。<br>
 *
 * @param dirPath このフォルダのパス
 * @param childDirInfos このフォルダに含まれる子フォルダ情報
 * @param childBookInfos このフォルダに含まれるExcelブック名
 * @author nmby
 */
public record DirInfo(
        Path dirPath,
        List<DirInfo> childDirInfos,
        List<BookInfo> childBookInfos) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param dirPath このフォルダのパス
     * @param childDirInfos このフォルダに含まれる子フォルダ情報
     * @param childBookInfos このフォルダに含まれるExcelブック名
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public DirInfo(
            Path dirPath,
            List<DirInfo> childDirInfos,
            List<BookInfo> childBookInfos) {
        
        Objects.requireNonNull(dirPath);
        Objects.requireNonNull(childDirInfos);
        Objects.requireNonNull(childBookInfos);
        
        this.dirPath = dirPath;
        this.childDirInfos = List.copyOf(childDirInfos);
        this.childBookInfos = List.copyOf(childBookInfos);
    }
    
    @Override
    public String toString() {
        return dirPath.getFileName().toString();
    }
}
