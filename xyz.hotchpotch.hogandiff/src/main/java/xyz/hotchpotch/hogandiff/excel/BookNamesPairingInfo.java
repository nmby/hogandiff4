package xyz.hotchpotch.hogandiff.excel;

import java.util.List;
import java.util.Map;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * Excelブックの組み合わせ情報を保持する不変クラスです。<br>
 * 
 * @param bookNamePairs Excelブック名の組み合わせ
 * @param sheetNamesPairingInfos シート名の組み合わせ情報
 * @author nmby
 */
public record BookNamesPairingInfo(
        List<Pair<String>> bookNamePairs,
        Map<Pair<String>, SheetNamesPairingInfo> sheetNamesPairingInfos) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
}
