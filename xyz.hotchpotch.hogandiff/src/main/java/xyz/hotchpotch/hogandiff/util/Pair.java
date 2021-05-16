package xyz.hotchpotch.hogandiff.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * 同型の2つの要素を保持する不変クラスです。<br>
 *
 * @param <T> 要素の型
 * @author nmby
 */
public record Pair<T>(T a, T b) {
    
    // [static members] ********************************************************
    
    private static final Pair<?> EMPTY = new Pair<>(null, null);
    
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
     * 新たなペアを生成します。<br>
     * 
     * @param <T> 要素の型
     * @param a 要素a
     * @param b 要素b
     * @return 新たなペア
     * @throws NullPointerException {@code a}, {@code b} のいずれかが {@code null} の場合
     */
    public static <T> Pair<T> flatOf(Optional<? extends T> a, Optional<? extends T> b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        
        return new Pair<>(a.orElse(null), b.orElse(null));
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
    
    /**
     * 要素aだけが存在し要素bが欠けているペアを生成します。<br>
     * 
     * @param <T> 要素の型
     * @param a 要素a
     * @return 新たなペア
     * @throws NullPointerException {@code a} が {@code null} の場合
     */
    public static <T> Pair<T> onlyA(T a) {
        Objects.requireNonNull(a, "a");
        
        return new Pair<>(a, null);
    }
    
    /**
     * 要素bだけが存在し要素aが欠けているペアを生成します。<br>
     * 
     * @param <T> 要素の型
     * @param b 要素b
     * @return 新たなペア
     * @throws NullPointerException {@code b} が {@code null} の場合
     */
    public static <T> Pair<T> onlyB(T b) {
        Objects.requireNonNull(b, "b");
        
        return new Pair<>(null, b);
    }
    
    /**
     * 指定された側の要素だけが存在し、他方が欠けているペアを生成します。<br>
     * 
     * @param <T> 要素の型
     * @param side 要素の側
     * @param value 要素の値
     * @return 新たなペア
     * @throws NullPointerException {@code side}, {@code value} のいずれかが {@code null} の場合
     */
    public static <T> Pair<T> only(Side side, T value) {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(value, "value");
        
        return side == Side.A ? onlyA(value) : onlyB(value);
    }
    
    /**
     * 空のペアを返します。<br>
     * 
     * @param <T> 要素の型
     * @return 空のペア
     */
    @SuppressWarnings("unchecked")
    public static <T> Pair<T> empty() {
        return (Pair<T>) EMPTY;
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
     * 要素aがある場合はその値を返し、そうでない場合は {@code other} を返します。<br>
     * 
     * @param other 要素aが無い場合に返される値（{@code null} 許容）
     * @return 要素aがある場合はその値、そうでない場合は {@code other}
     */
    public T aOrElse(T other) {
        return a == null ? other : a;
    }
    
    /**
     * 要素bがある場合はその値を返し、そうでない場合は {@code other} を返します。<br>
     * 
     * @param other 要素bが無い場合に返される値（{@code null} 許容）
     * @return 要素bがある場合はその値、そうでない場合は {@code other}
     */
    public T bOrElse(T other) {
        return b == null ? other : b;
    }
    
    /**
     * 指定された側の要素がある場合はその値を返し、そうでない場合は {@code other} を返します。<br>
     * 
     * @param side 要素の側
     * @param other 指定された側の要素が無い場合に返される値（{@code null} 許容）
     * @return 指定された側の要素がある場合はその値、そうでない場合は {@code other}
     * @throws NullPointerException {@code side} が {@code null} の場合
     */
    public T orElse(Side side, T other) {
        Objects.requireNonNull(side, "side");
        
        return side == Side.A ? aOrElse(other) : bOrElse(other);
    }
    
    /**
     * 要素aが存在するかを返します。<br>
     * 
     * @return 要素aが存在する場合は {@code true}
     */
    public boolean isPresentA() {
        return a != null;
    }
    
    /**
     * 要素bが存在するかを返します。<br>
     * 
     * @return 要素bが存在する場合は {@code true}
     */
    public boolean isPresentB() {
        return b != null;
    }
    
    /**
     * 指定された側の要素が存在するかを返します。<br>
     * 
     * @param side 要素の側
     * @return 指定された側の要素が存在する場合は {@code true}
     * @throws NullPointerException {@code side} が {@code null} の場合
     */
    public boolean isPresent(Side side) {
        Objects.requireNonNull(side, "side");
        
        return side == Side.A ? isPresentA() : isPresentB();
    }
    
    /**
     * 要素a, 要素bがともに存在するかを返します。<br>
     * 
     * @return 両要素ともに存在する場合は {@code true}
     */
    public boolean isPaired() {
        return isPresentA() && isPresentB();
    }
    
    /**
     * 要素aだけが存在するかを返します。<br>
     * 
     * @return 要素aだけが存在する場合は {@code true}
     */
    public boolean isOnlyA() {
        return isPresentA() && !isPresentB();
    }
    
    /**
     * 要素bだけが存在するかを返します。<br>
     * 
     * @return 要素bだけが存在する場合は {@code true}
     */
    public boolean isOnlyB() {
        return !isPresentA() && isPresentB();
    }
    
    /**
     * 指定された側の要素だけが存在するかを返します。<br>
     * 
     * @param side 要素の側
     * @return 指定された側の要素だけが存在する場合は {@code true}
     * @throws NullPointerException {@code side} が {@code null} の場合
     */
    public boolean isOnly(Side side) {
        Objects.requireNonNull(side, "side");
        
        return side == Side.A ? isOnlyA() : isOnlyB();
    }
    
    /**
     * ペアが空（どちらの要素も存在しない）かを返します。<br>
     * 
     * @return ペアが空の（どちらの要素も存在しない）場合は {@code true}
     */
    public boolean isEmpty() {
        return !isPresentA() && !isPresentB();
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
     * 要素a, 要素bを入れ替えたペアを生成して返します。<br>
     * 
     * @return 要素a, 要素bを入れ替えたペア
     */
    public Pair<T> reversed() {
        return new Pair<>(b, a);
    }
    
    /**
     * このペアの要素に {@code mapper} をそれぞれ適用して得られる値を要素とする
     * 新しいペアを生成して返します。<br>
     * 
     * @param <U> 新しいペアの要素の型
     * @param mapper 変換関数
     * @return 新しいペア
     * @throws NullPointerException {@code mapper} が {@code null} の場合
     */
    public <U> Pair<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        
        return new Pair<>(
                a == null ? null : mapper.apply(a),
                b == null ? null : mapper.apply(b));
    }
}
