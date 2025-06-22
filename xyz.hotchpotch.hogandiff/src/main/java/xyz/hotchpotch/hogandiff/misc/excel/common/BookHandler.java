package xyz.hotchpotch.hogandiff.misc.excel.common;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import xyz.hotchpotch.hogandiff.models.BookType;

/**
 * そのクラスがExcelブックを処理することを表す注釈です。<br>
 *
 * @author nmby
 */
@Retention(RUNTIME)
@Target({ TYPE })
public @interface BookHandler {

    /**
     * そのクラスが処理できるExcelブックの形式を表します。<br>
     * 
     * @return そのクラスが処理できるExcelブックの形式
     */
    BookType[] targetTypes() default {
            BookType.XLS,
            BookType.XLSX,
            BookType.XLSM,
            BookType.XLSB };
}
