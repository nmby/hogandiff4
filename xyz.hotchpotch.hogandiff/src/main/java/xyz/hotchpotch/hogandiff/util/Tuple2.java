package xyz.hotchpotch.hogandiff.util;

/**
 * 異なる型の2つの要素を保持する不変コンテナです。<br>
 * 
 * @param <T1> 要素1の型
 * @param <T2> 要素2の型
 * @param item1 要素1
 * @param item2 要素2
 */
public record Tuple2<T1, T2>(T1 item1, T2 item2) {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    @Override
    public String toString() {
        return "(%s, %s)".formatted(item1, item2);
    }
}
