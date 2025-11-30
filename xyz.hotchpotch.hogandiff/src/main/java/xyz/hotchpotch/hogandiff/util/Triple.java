package xyz.hotchpotch.hogandiff.util;

import java.util.Objects;
import java.util.function.Function;

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
    
    /**
     * 指定された側の要素を返します。<br>
     * 
     * @param side
     *            値を取得する側
     * @return 指定された側の要素
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public T get(Side3 side) {
        Objects.requireNonNull(side);
        return switch (side) {
        case O -> o;
        case A -> a;
        case B -> b;
        };
    }
    
    /**
     * 要素 {@code a}, {@code b} の両方が {@code null} でない場合に {@code true} を返します。<br>
     * 
     * @return 要素 {@code a} と要素 {@code b} の両方が {@code null} でない場合に {@code true}
     */
    public boolean hasAB() {
        return a != null && b != null;
    }
    
    /**
     * 要素 {@code a}, {@code b} からなるペアを返します。<br>
     * 
     * @return 要素 {@code a}, {@code b} からなるペア
     */
    public Pair<T> toPairAB() {
        return new Pair<>(a, b);
    }
    
    /**
     * 2つの要素からなるペアを返します。<br>
     * 
     * @param side3
     *            除外する側
     * @return 指定された側の要素を除く2つの要素からなるペア
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public Pair<T> toPair(Side3 side3) {
        Objects.requireNonNull(side3);
        return switch (side3) {
        case O -> new Pair<>(a, b);
        case A -> new Pair<>(o, a);
        case B -> new Pair<>(o, b);
        };
    }
    
    /**
     * このトリプルの要素それぞれに変換処理を施して得られるトリプルを返します。<br>
     * 
     * @param <U>
     *            変換後の要素の型
     * @param mapper
     *            変換処理
     * @return 新たなトリプル
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    public <U> Triple<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        return new Triple<>(
                o == null ? null : mapper.apply(o),
                a == null ? null : mapper.apply(a),
                b == null ? null : mapper.apply(b));
    }
}
