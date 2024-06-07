package xyz.hotchpotch.hogandiff.excel;

import java.util.List;

import xyz.hotchpotch.hogandiff.util.Pair;

/**
 * シートの組み合わせ情報を保持する不変クラスです。<br>
 * 
 * @param sheetNamePairs シート名の組み合わせ
 * @author nmby
 */
public record SheetNamesPairingInfo(
        List<Pair<String>> sheetNamePairs) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
}
