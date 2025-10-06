package xyz.hotchpotch.hogandiff.logic;

import xyz.hotchpotch.hogandiff.Msg;

/**
 * Excelシートの種類を表す列挙型です。<br>
 * 
 * @author nmby
 */
public enum SheetType {
    
    // [static members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    /** ワークシート */
    WORKSHEET(Msg.MSG_124.get()),
    
    /** グラフシート */
    CHART_SHEET(Msg.MSG_125.get()),
    
    /** MS Excel 5.0 ダイアログシート */
    DIALOG_SHEET(Msg.MSG_126.get()),
    
    /** Excel 4.0 マクロシート */
    MACRO_SHEET(Msg.MSG_127.get());
    
    // [instance members] ++++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private final String description;
    
    private SheetType(String description) {
        assert description != null;
        this.description = description;
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
