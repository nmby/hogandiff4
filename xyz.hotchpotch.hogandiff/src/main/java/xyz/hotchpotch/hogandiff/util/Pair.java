package xyz.hotchpotch.hogandiff.util;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * 同型の2つの要素を保持する不変クラスです。<br>
 *
 * @param <T> 要素の型
 * @author nmby
 */
public record Pair<T>(T a, T b) {
    
    // [static members] ********************************************************
    
    /**
     * ペアのどちら側かを表す列挙型です。<br>
     *
     * @author nmby
     */
    public static enum Side {
        
        // [static members] ----------------------------------------------------
        
        /** A-side */
        A,
        
        /** B-side */
        B;
        
        // [instance members] --------------------------------------------------
        
        /**
         * 自身と反対の側を返します。<br>
         * 
         * @return 自身と反対の側
         */
        public Side opposite() {
            return this == A ? B : A;
        }
    }
    
    /**
     * 新たなペアを生成します。<br>
     * 
     * @param <T> 要素の型
     * @param a 要素a
     * @param b 要素b
     * @return 新たなペア
     * @throws NullPointerException {@code a}, {@code b} のいずれかが {@code null} の場合
     */
    public static <T> Pair<T> of(T a, T b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        
        return new Pair<>(a, b);
    }
    
    /**
     * どちらかまたは両方の要素が欠けている可能性のあるペアを生成します。<br>
     * 
     * @param <T> 要素の型
     * @param a 要素a（{@code null} 許容）
     * @param b 要素b（{@code null} 許容）
     * @return 新たなペア
     */
    public static <T> Pair<T> ofNullable(T a, T b) {
        return new Pair<>(a, b);
    }
    
    // [instance members] ******************************************************
    
    @Override
    public String toString() {
        return String.format("(%s, %s)", a, b);
    }
    
    /**
     * 要素aがある場合はその値を返し、そうでない場合は例外をスローします。<br>
     * 
     * @return 要素a
     * @throws NoSuchElementException 要素aが無い場合
     */
    @Override
    public T a() {
        if (a == null) {
            throw new NoSuchElementException();
        }
        return a;
    }
    
    /**
     * 要素bがある場合はその値を返し、そうでない場合は例外をスローします。<br>
     * 
     * @return 要素b
     * @throws NoSuchElementException 要素bが無い場合
     */
    @Override
    public T b() {
        if (b == null) {
            throw new NoSuchElementException();
        }
        return b;
    }
    
    /**
     * 指定された側の要素がある場合はその値を返し、そうでない場合は例外をスローします。<br>
     * 
     * @param side 要素の側
     * @return 指定された側の要素
     * @throws NullPointerException {@code side} が {@code null} の場合
     * @throws NoSuchElementException 指定された側の要素が無い場合
     */
    public T get(Side side) {
        Objects.requireNonNull(side, "side");
        
        return side == Side.A ? a() : b();
    }
    
    /**
     * 要素aが存在するかを返します。<br>
     * 
     * @return 要素aが存在する場合は {@code true}
     */
    public boolean hasA() {
        return a != null;
    }
    
    /**
     * 要素bが存在するかを返します。<br>
     * 
     * @return 要素bが存在する場合は {@code true}
     */
    public boolean hasB() {
        return b != null;
    }
    
    /**
     * 指定された側の要素が存在するかを返します。<br>
     * 
     * @param side 要素の側
     * @return 指定された側の要素が存在する場合は {@code true}
     * @throws NullPointerException {@code side} が {@code null} の場合
     */
    public boolean has(Side side) {
        Objects.requireNonNull(side, "side");
        
        return side == Side.A ? hasA() : hasB();
    }
    
    /**
     * 要素a, 要素bがともに存在するかを返します。<br>
     * 
     * @return 両要素ともに存在する場合は {@code true}
     */
    public boolean isPaired() {
        return hasA() && hasB();
    }
    
    /**
     * 要素aだけが存在するかを返します。<br>
     * 
     * @return 要素aだけが存在する場合は {@code true}
     */
    public boolean isOnlyA() {
        return hasA() && !hasB();
    }
    
    /**
     * 要素bだけが存在するかを返します。<br>
     * 
     * @return 要素bだけが存在する場合は {@code true}
     */
    public boolean isOnlyB() {
        return !hasA() && hasB();
    }
    
    /**
     * 要素aと要素bが同じであるかを返します。<br>
     * 
     * @return 要素aと要素bが同じ場合は {@code true}
     */
    public boolean isIdentical() {
        return Objects.equals(a, b);
    }
}
