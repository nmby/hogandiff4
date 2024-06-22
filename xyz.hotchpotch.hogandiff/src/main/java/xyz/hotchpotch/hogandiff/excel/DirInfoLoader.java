package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;

import xyz.hotchpotch.hogandiff.excel.common.StandardDirInfoLoader;

/**
 * フォルダ情報を抽出するローダーを表します。<br>
 * これは、{@link #loadDirInfo(Path)} を関数メソッドに持つ関数型インタフェースです。<br>
 *
 * @author nmby
 */
@FunctionalInterface
public interface DirInfoLoader {
    
    // [static members] ********************************************************
    
    /**
     * フォルダ情報を抽出するローダーを返します。<br>
     * 
     * @param recursively 子フォルダも再帰的に抽出するか
     * @return フォルダ情報を抽出するローダー
     */
    public static DirInfoLoader of(boolean recursively) {
        return new StandardDirInfoLoader(recursively);
    }
    
    // [instance members] ******************************************************
    
    /**
     * 指定されたパスのフォルダ情報を返します。<br>
     * 
     * @param path フォルダのパス
     * @return フォルダ情報
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    DirInfo loadDirInfo(Path path) throws ExcelHandlingException;
}