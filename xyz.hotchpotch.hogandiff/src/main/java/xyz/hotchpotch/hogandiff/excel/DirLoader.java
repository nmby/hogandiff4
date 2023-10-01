package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;

import xyz.hotchpotch.hogandiff.excel.common.StandardDirLoader;

/**
 * フォルダの情報を抽出するローダーを表します。<br>
 * これは、{@link #loadDir(Path)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface DirLoader {
    
    // [static members] ********************************************************
    
    /**
     * フォルダ情報を抽出するローダーを返します。<br>
     * 
     * @param recursively 子フォルダも再帰的に抽出するか
     * @return フォルダ情報を抽出するローダー
     */
    public static DirLoader of(boolean recursively) {
        return StandardDirLoader.of(recursively);
    }
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたフォルダの情報を返します。<br>
     * 
     * @param path フォルダのパス
     * @return フォルダの情報
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    DirInfo loadDir(Path path) throws ExcelHandlingException;
}
