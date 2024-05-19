package xyz.hotchpotch.hogandiff.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import xyz.hotchpotch.hogandiff.util.function.UnsafeConsumer;
import xyz.hotchpotch.hogandiff.util.function.UnsafeFunction;

/**
 * 同型の2つの要素を保持する不変コンテナです。<br>
 *
 * @param <T> 要素の型
 * @param a 要素a
 * @param b 要素b
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
        
        /**
         * 各側に関数を適用して得られる要素からなるペアを返します。<br>
         * 
         * @param <T> 新たな要素の型
         * @param mapper 各側に適用する関数
         * @return 新たなペア
         * @throws NullPointerException {@code mapper} が {@code null} の場合
         */
        public static <T> Pair<T> map(Function<Side, ? extends T> mapper) {
            Objects.requireNonNull(mapper, "mapper");
            
            return new Pair<>(
                    mapper.apply(A),
                    mapper.apply(B));
        }
        
        /**
         * 各側に関数を適用して得られる要素からなるペアを返します。<br>
         * 
         * @param <T> 新たな要素の型
         * @param <E> {@code mapper} がスローしうるチェック例外の型
         * @param mapper 各側に適用する関数
         * @return 新たなペア
         * @throws NullPointerException {@code mapper} が {@code null} の場合
         */
        public static <T, E extends Exception> Pair<T> unsafeMap(UnsafeFunction<Side, ? extends T, E> mapper) throws E {
            Objects.requireNonNull(mapper, "mapper");
            
            return new Pair<>(
                    mapper.apply(A),
                    mapper.apply(B));
        }
        
        /**
         * 各側に指定されたオペレーションを適用します。<br>
         * 
         * @param operation 各側に適用するオペレーション
         * @throws NullPointerException {@code operation} が {@code null} の場合
         */
        public static void forEach(Consumer<Side> operation) {
            Objects.requireNonNull(operation, "operation");
            
            operation.accept(A);
            operation.accept(B);
        }
        
        /**
         * 各側に指定されたオペレーションを適用します。<br>
         * 
         * @param <E> {@code operation} がスローしうるチェック例外の型
         * @param operation 各側に適用するオペレーション
         * @throws NullPointerException {@code operation} が {@code null} の場合
         */
        public static <E extends Exception> void unsafeForEach(UnsafeConsumer<Side, E> operation) throws E {
            Objects.requireNonNull(operation, "operation");
            
            operation.accept(A);
            operation.accept(B);
        }
        
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
    
    // [instance members] ******************************************************
    
    @Override
    public String toString() {
        return "(%s, %s)".formatted(a, b);
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
        
        return side == Side.A ? a : b;
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
        
        return (side == Side.A ? a : b) != null;
    }
    
    /**
     * 要素a, 要素bがともに存在するかを返します。<br>
     * 
     * @return 両要素ともに存在する場合は {@code true}
     */
    public boolean isPaired() {
        return a != null && b != null;
    }
    
    /**
     * 要素aだけが存在するかを返します。<br>
     * 
     * @return 要素aだけが存在する場合は {@code true}
     */
    public boolean isOnlyA() {
        return a != null && b == null;
    }
    
    /**
     * 要素bだけが存在するかを返します。<br>
     * 
     * @return 要素bだけが存在する場合は {@code true}
     */
    public boolean isOnlyB() {
        return a == null && b != null;
    }
    
    /**
     * このペアが空かを返します。<br>
     * 
     * @return このペアが空の場合は {@code true}
     */
    public boolean isEmpty() {
        return a == null && b == null;
    }
    
    /**
     * 要素aと要素bが同じであるかを返します。<br>
     * 
     * @return 要素aと要素bが同じ場合は {@code true}
     */
    public boolean isIdentical() {
        return Objects.equals(a, b);
    }
    
    /**
     * このペアの各要素に関数を適用して得られる新たな要素からなるペアを返します。<br>
     * 
     * @param <U> 新たな要素の型
     * @param mapper 各要素に適用する関数
     * @return 新たなペア
     * @throws NullPointerException {@code mapper} が {@code null} の場合
     */
    public <U> Pair<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        
        return new Pair<>(
                a == null ? null : mapper.apply(a),
                b == null ? null : mapper.apply(b));
    }
    
    /**
     * このペアの各要素に関数を適用して得られる新たな要素からなるペアを返します。<br>
     * 
     * @param <U> 新たな要素の型
     * @param <E> {@code mapper} がスローしうるチェック例外の型
     * @param mapper 各要素に適用する関数
     * @return 新たなペア
     * @throws NullPointerException {@code mapper} が {@code null} の場合
     * @throws E {@code mapper.apply} が失敗した場合
     */
    public <U, E extends Exception> Pair<U> unsafeMap(UnsafeFunction<? super T, ? extends U, E> mapper) throws E {
        Objects.requireNonNull(mapper, "mapper");
        
        return new Pair<>(
                a == null ? null : mapper.apply(a),
                b == null ? null : mapper.apply(b));
    }
    
    /**
     * このペアの各要素に指定されたオペレーションを適用します。
     * 要素が存在しない場合はオペレーションを適用しません。<br>
     * 
     * @param operation 各要素に適用するオペレーション
     * @throws NullPointerException {@code operation} が {@code null} の場合
     */
    public void forEach(Consumer<? super T> operation) {
        Objects.requireNonNull(operation, "operation");
        
        if (a != null) {
            operation.accept(a);
        }
        if (b != null) {
            operation.accept(b);
        }
    }
}
