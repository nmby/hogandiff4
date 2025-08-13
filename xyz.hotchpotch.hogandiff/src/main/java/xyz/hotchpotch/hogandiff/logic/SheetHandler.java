package xyz.hotchpotch.hogandiff.logic;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * そのクラスがExcelシートを処理することを表す注釈です。<br>
 *
 * @author nmby
 */
@Retention(RUNTIME)
@Target({ TYPE })
public @interface SheetHandler {

    /**
     * そのクラスが処理できるExcelシートの種類を表します。<br>
     * 
     * @return そのクラスが処理できるExcelシートの種類
     */
    SheetType[] targetTypes() default {
            SheetType.WORKSHEET,
            SheetType.CHART_SHEET,
            SheetType.DIALOG_SHEET,
            SheetType.MACRO_SHEET };
}
