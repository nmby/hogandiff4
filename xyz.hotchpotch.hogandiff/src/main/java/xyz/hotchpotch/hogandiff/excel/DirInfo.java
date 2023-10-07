package xyz.hotchpotch.hogandiff.excel;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * フォルダを表します。<br>
 *
 * @author nmby
 */
// 実装メモ
// 本クラスは record にしたいところだが、次の2つの理由から普通のクラスとして実装している。
// 1. #equals と #hashCode を独自実装したいため（詳細は各メソッドのコメント参照）
// 2. 親子を一度に設定することはできず #setParent という可変メソッドを設ける必要があるため
public class DirInfo implements Comparable<DirInfo> {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    private final Path path;
    
    private DirInfo parent;
    private List<String> bookNames;
    private List<DirInfo> children;
    
    /**
     * コンストラクタ<br>
     * 
     * @param path このフォルダのパス
     * @throws NullPointerException {@code path} が {@code null} の場合
     */
    public DirInfo(Path path) {
        Objects.requireNonNull(path);
        
        this.path = path;
    }
    
    /**
     * このフォルダのパスを返します。<br>
     * 
     * @return このフォルダのパス
     */
    public Path path() {
        return path;
    }
    
    /**
     * このフォルダの親フォルダのパスを返します。<br>
     * 
     * @return このフォルダの親フォルダのパス
     */
    public DirInfo parent() {
        return parent;
    }
    
    /**
     * このフォルダの親フォルダのパスを設定します。<br>
     * 
     * @param parent このフォルダの親フォルダのパス（{@code null} 許容）
     */
    public void setParent(DirInfo parent) {
        this.parent = parent;
    }
    
    /**
     * このフォルダに含まれるExcelブック名のリストを返します。<br>
     * 
     * @return このフォルダに含まれるExcelブック名のリスト
     */
    public List<String> bookNames() {
        return List.copyOf(bookNames);
    }
    
    /**
     * このフォルダに含まれるExcelブック名のリストを設定します。<br>
     * 
     * @param bookNames このフォルダに含まれるExcelブック名のリスト
     * @throws NullPointerException {@code bookNames} が {@code null} の場合
     */
    public void setBookNames(List<String> bookNames) {
        Objects.requireNonNull(bookNames, "bookNames");
        
        this.bookNames = List.copyOf(bookNames);
    }
    
    /**
     * このフォルダの子フォルダのリストを返します。<br>
     * 
     * @return このフォルダの子フォルダのリスト
     */
    public List<DirInfo> children() {
        return List.copyOf(children);
    }
    
    /**
     * このフォルダの子フォルダのリストを設定します。<br>
     * 
     * @param children このフォルダの子フォルダのリスト
     * @throws NullPointerException {@code children} が {@code null} の場合
     */
    public void setChildren(List<DirInfo> children) {
        Objects.requireNonNull(children, "children");
        
        this.children = List.copyOf(children);
    }
    
    /**
     * <strong>注意：</strong>
     * この実装では {@link #path()} の値のみに基づいて同一性を判定します。<br>
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof DirInfo other) {
            return Objects.equals(path, other.path);
            // 循環によりStackOverflowエラーが発生するため、
            // parentはequals, hashCodeの判定対象に含めないこととする。
            // 加えて、フォルダとしての同一性はそのパスだけで判定できるため、
            // その他のメンバも判定対象に含めないこととする。
            //            return Objects.equals(path, other.path)
            //                    && Objects.equals(parent, other.parent)
            //                    && Objects.equals(bookNames, other.bookNames)
            //                    && Objects.equals(children, other.children);
            //
            // -> parent と children で参照ループを構成しているため。
            //    こういうときはどうすれば良いのん？
            // TODO: 要お勉強
        }
        return false;
    }
    
    /**
     * <strong>注意：</strong>
     * この実装では {@link #path()} の値のみに基づいてハッシュコードを計算します。<br>
     */
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
    
    /**
     * <strong>注意：</strong>
     * この実装では {@link #path()} の値のみに基づいて大小関係を判断します。<br>
     */
    @Override
    public int compareTo(DirInfo o) {
        Objects.requireNonNull(o, "o");
        return path.compareTo(o.path);
    }
}
