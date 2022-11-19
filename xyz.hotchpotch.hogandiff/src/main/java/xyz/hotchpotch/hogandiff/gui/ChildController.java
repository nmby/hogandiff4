package xyz.hotchpotch.hogandiff.gui;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * 画面部品クラスの共通的な振舞いを規定するインタフェースです。<br>
 * 
 * @author nmby
 */
public interface ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * この画面部品の内容を初期化します。<br>
     * 
     * @param parent このアプリケーションのコントローラ
     * @param params 追加パラメータ
     */
    default void init(MainController parent, Object... params) {
    }
    
    /**
     * この画面部品が比較処理を実行できる状態であるかを返します。<br>
     * 
     * @return 比較処理を実行できる状態の場合は {@code true}
     */
    default BooleanExpression isReady() {
        return new SimpleBooleanProperty(true);
    }
}
