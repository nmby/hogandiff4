package xyz.hotchpotch.hogandiff.excel;

//あんまり意味はないが sealed を使ってみる
public sealed interface Result
        permits SheetResult, BookResult, DirResult, TreeResult {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
}
