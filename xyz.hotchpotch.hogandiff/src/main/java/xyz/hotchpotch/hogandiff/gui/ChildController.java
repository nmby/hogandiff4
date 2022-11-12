package xyz.hotchpotch.hogandiff.gui;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
import xyz.hotchpotch.hogandiff.util.Settings;

/**
 * 画面部品クラスの共通的な振舞いを規定するインタフェースです。<br>
 * 
 * @author nmby
 */
/*package*/ interface ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * この画面部品の内容を初期化します。<br>
     * 
     * @param parent このアプリケーションのコントローラ
     */
    default void init(MainController parent) {
    }
    
    /**
     * 与えられた設定値を画面部品に反映させます。<br>
     * 
     * @param settings 設定値
     */
    default void applySettings(Settings settings) {
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
