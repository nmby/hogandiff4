package xyz.hotchpotch.hogandiff.models;

import java.util.ResourceBundle;

import xyz.hotchpotch.hogandiff.AppMain;

/**
 * Excelシートの種類を表す列挙型です。<br>
 * 
 * @author nmby
 */
public enum SheetType {

    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    /** ワークシート */
    WORKSHEET("excel.SheetType.010"),

    /** グラフシート */
    CHART_SHEET("excel.SheetType.020"),

    /** MS Excel 5.0 ダイアログシート */
    DIALOG_SHEET("excel.SheetType.030"),

    /** Excel 4.0 マクロシート */
    MACRO_SHEET("excel.SheetType.040");

    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++

    private final String description;

    private SheetType(String descriptionKey) {
        assert descriptionKey != null;

        ResourceBundle rb = AppMain.appResource.get();
        this.description = rb.getString(descriptionKey);
    }

    /**
     * このシート種別の説明を返します。<br>
     * 
     * @return このシート種別の説明
     */
    public String description() {
        return description;
    }
}
