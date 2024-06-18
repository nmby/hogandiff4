package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * フォルダ情報を表す不変クラスです。<br>
 *
 * @param dirPath このフォルダのパス
 * @param childBookNames このフォルダに含まれるExcelブック名
 * @param childDirInfos このフォルダに含まれる子フォルダ情報
 * @author nmby
 */
public record DirInfo(
        Path dirPath,
        List<String> childBookNames,
        List<DirInfo> childDirInfos) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * コンストラクタ
     * 
     * @param dirPath このフォルダのパス
     * @param childBookNames このフォルダに含まれるExcelブック名
     * @param childDirInfos このフォルダに含まれる子フォルダ情報
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public DirInfo(
            Path dirPath,
            List<String> childBookNames,
            List<DirInfo> childDirInfos) {
        
        Objects.requireNonNull(dirPath);
        Objects.requireNonNull(childBookNames);
        Objects.requireNonNull(childDirInfos);
        
        this.dirPath = dirPath;
        this.childBookNames = List.copyOf(childBookNames);
        this.childDirInfos = List.copyOf(childDirInfos);
    }
    
    @Override
    public String toString() {
        return dirPath.getFileName().toString();
    }
}
