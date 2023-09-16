package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;

/**
 * フォルダの情報を抽出するローダーを表します。<br>
 * これは、{@link #loadDirs(Path, boolean)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface DirLoader {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたフォルダの情報を返します。<br>
     * 
     * @param path フォルダのパス
     * @param recursively 子フォルダも再帰的に抽出するか
     * @return フォルダの情報
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    DirData loadDirs(Path path, boolean recursively)
            throws ExcelHandlingException;
}
