package xyz.hotchpotch.hogandiff.excel;

import java.util.List;

import xyz.hotchpotch.hogandiff.excel.SheetResult.Stats;

// sealed を使ってみる
public sealed interface Result
        permits SheetResult, BookResult, DirResult, TreeResult {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    List<Stats> getSheetStats();
}
