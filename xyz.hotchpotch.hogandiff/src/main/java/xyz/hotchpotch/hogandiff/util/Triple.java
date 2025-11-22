package xyz.hotchpotch.hogandiff.util;

import java.util.Objects;

import xyz.hotchpotch.hogandiff.util.Pair.Side;

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
        
        // 順次実装用アダプタ
        // TODO: 実装完了時に削除する
        public static Side3 from2(Side side) {
            Objects.requireNonNull(side);
            return switch (side) {
            case A -> A;
            case B -> B;
            };
        }
        
        // [instance members] --------------------------------------------------
        
        // 順次実装用アダプタ
        // TODO: 実装完了時に削除する
        public Side to2() {
            return switch (this) {
            case O -> throw new UnsupportedOperationException();
            case A -> Side.A;
            case B -> Side.B;
            };
        }
        
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
