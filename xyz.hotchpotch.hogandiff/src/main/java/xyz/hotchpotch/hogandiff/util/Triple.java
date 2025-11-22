package xyz.hotchpotch.hogandiff.util;

/**
 * 同型の3つの要素を保持する不変コンテナです。<br>
 *
 * @param <T>
 *            要素の型
 * @param o
 *            要素o
 * @param a
 *            要素a
 * @param b
 *            要素b
 * @author nmby
 */
public record Triple<T>(T o, T a, T b) {
    
    // [static members] ********************************************************
    
    /**
     * トリプルのどの側かを表す列挙型です。<br>
     *
     * @author nmby
     */
    public static enum Side3 {
        
        // [static members] ----------------------------------------------------
        
        /** O-side */
        O,
        
        /** A-side */
        A,
        
        /** B-side */
        B;
        
        // [instance members] --------------------------------------------------
        
        /**
         * 自身の反対側の子側を返します。<br>
         * 
         * @return 反対側の子側
         * @throws UnsupportedOperationException
         *             自身が {@code O} 側の場合
         */
        public Side3 oppositeChild() {
            return switch (this) {
            case O -> throw new UnsupportedOperationException();
            case A -> B;
            case B -> A;
            };
        }
    }
    
    // [instance members] ******************************************************
}
