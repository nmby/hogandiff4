package xyz.hotchpotch.hogandiff.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import xyz.hotchpotch.hogandiff.util.function.UnsafeFunction;

/**
 * 同型の3つの要素を保持する不変コンテナです。<br>
 *
 * @param <T> 要素の型
 * @param o 起源（origin）の要素
 * @param a 要素a
 * @param b 要素b
 * @author nmby
 */
public record Triple<T>(T o, T a, T b) {

    // [static members] ********************************************************

    /**
     * トリプルのどの側かを表す列挙型です。<br>
     *
     * @author nmby
     */
    public static enum Side {

        // [static members] ----------------------------------------------------

        /** O-side（起源） */
        O,

        /** A-side */
        A,

        /** B-side */
        B;

        /**
         * 各側に関数を適用して得られる要素からなるトリプルを返します。<br>
         *
         * @param <T> 新たな要素の型
         * @param mapper 各側に適用する関数
         * @return 新たなトリプル
         * @throws NullPointerException パラメータが {@code null} の場合
         */
        public static <T> Triple<T> map(Function<Side, ? extends T> mapper) {
            Objects.requireNonNull(mapper);

            return new Triple<>(
                    mapper.apply(O),
                    mapper.apply(A),
                    mapper.apply(B));
        }

        /**
         * 各側に関数を適用して得られる要素からなるトリプルを返します。<br>
         *
         * @param <T> 新たな要素の型
         * @param <E> {@code mapper} がスローしうるチェック例外の型
         * @param mapper 各側に適用する関数
         * @return 新たなトリプル
         * @throws NullPointerException パラメータが {@code null} の場合
         */
        public static <T, E extends Exception> Triple<T> unsafeMap(
                UnsafeFunction<Side, ? extends T, E> mapper) throws E {

            Objects.requireNonNull(mapper);

            return new Triple<>(
                    mapper.apply(O),
                    mapper.apply(A),
                    mapper.apply(B));
        }

        /**
         * 各側に指定されたオペレーションを適用します。<br>
         *
         * @param operation 各側に適用するオペレーション
         * @throws NullPointerException パラメータが {@code null} の場合
         */
        public static void forEach(Consumer<Side> operation) {
            Objects.requireNonNull(operation);

            operation.accept(O);
            operation.accept(A);
            operation.accept(B);
        }
    }

    /**
     * 新しいトリプルを返します。<br>
     *
     * @param <T> 要素の型
     * @param o 起源（origin）の要素
     * @param a 要素a
     * @param b 要素b
     * @return 新しいトリプル
     */
    public static <T> Triple<T> of(T o, T a, T b) {
        return new Triple<>(o, a, b);
    }

    /**
     * 指定された側だけの要素を持つ新しいトリプルを返します。<br>
     *
     * @param <T> 要素の型
     * @param side 要素の側
     * @param value 指定された側の要素
     * @return 新しいトリプル
     * @throws NullPointerException {@code side} が {@code null} の場合
     */
    public static <T> Triple<T> ofOnly(Side side, T value) {
        Objects.requireNonNull(side);
        return switch (side) {
            case O -> new Triple<>(value, null, null);
            case A -> new Triple<>(null, value, null);
            case B -> new Triple<>(null, null, value);
        };
    }

    /**
     * 空のトリプルを返します。<br>
     *
     * @param <T> 要素の型
     * @return 空のトリプル
     */
    public static <T> Triple<T> empty() {
        return new Triple<>(null, null, null);
    }

    // [instance members] ******************************************************

    @Override
    public String toString() {
        return "(%s, %s, %s)".formatted(o, a, b);
    }

    /**
     * 指定された側の要素がある場合はその値を返し、そうでない場合は例外をスローします。<br>
     *
     * @param side 要素の側
     * @return 指定された側の要素
     * @throws NullPointerException パラメータが {@code null} の場合
     * @throws NoSuchElementException 指定された側の要素が無い場合
     */
    public T get(Side side) {
        Objects.requireNonNull(side);

        T value = switch (side) {
            case O -> o;
            case A -> a;
            case B -> b;
        };
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    /**
     * 要素oが存在するかを返します。<br>
     *
     * @return 要素oが存在する場合は {@code true}
     */
    public boolean hasO() {
        return o != null;
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
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public boolean has(Side side) {
        Objects.requireNonNull(side);

        return switch (side) {
            case O -> o != null;
            case A -> a != null;
            case B -> b != null;
        };
    }

    /**
     * 要素o, 要素a, 要素bがすべて存在するかを返します。<br>
     *
     * @return 全要素が存在する場合は {@code true}
     */
    public boolean isPaired() {
        return o != null && a != null && b != null;
    }

    /**
     * 要素oだけが存在するかを返します。<br>
     *
     * @return 要素oだけが存在する場合は {@code true}
     */
    public boolean isOnlyO() {
        return o != null && a == null && b == null;
    }

    /**
     * 要素aだけが存在するかを返します。<br>
     *
     * @return 要素aだけが存在する場合は {@code true}
     */
    public boolean isOnlyA() {
        return o == null && a != null && b == null;
    }

    /**
     * 要素bだけが存在するかを返します。<br>
     *
     * @return 要素bだけが存在する場合は {@code true}
     */
    public boolean isOnlyB() {
        return o == null && a == null && b != null;
    }

    /**
     * このトリプルが空かを返します。<br>
     *
     * @return このトリプルが空の場合は {@code true}
     */
    public boolean isEmpty() {
        return o == null && a == null && b == null;
    }

    /**
     * 要素o、要素a、要素bがすべて同じであるかを返します。<br>
     *
     * @return 全要素が同じ場合は {@code true}
     */
    public boolean isIdentical() {
        return Objects.equals(o, a) && Objects.equals(a, b);
    }

    /**
     * このトリプルの各要素に関数を適用して得られる新たな要素からなるトリプルを返します。<br>
     *
     * @param <U> 新たな要素の型
     * @param mapper 各要素に適用する関数
     * @return 新たなトリプル
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public <U> Triple<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);

        return new Triple<>(
                o == null ? null : mapper.apply(o),
                a == null ? null : mapper.apply(a),
                b == null ? null : mapper.apply(b));
    }

    /**
     * このトリプルの各要素に関数を適用して得られる新たな要素からなるトリプルを返します。<br>
     *
     * @param <U> 新たな要素の型
     * @param <E> {@code mapper} がスローしうるチェック例外の型
     * @param mapper 各要素に適用する関数
     * @return 新たなトリプル
     * @throws NullPointerException パラメータが {@code null} の場合
     * @throws E {@code mapper.apply} が失敗した場合
     */
    public <U, E extends Exception> Triple<U> unsafeMap(
            UnsafeFunction<? super T, ? extends U, E> mapper) throws E {

        Objects.requireNonNull(mapper);

        return new Triple<>(
                o == null ? null : mapper.apply(o),
                a == null ? null : mapper.apply(a),
                b == null ? null : mapper.apply(b));
    }

    /**
     * このトリプルの各要素に指定されたオペレーションを適用します。
     * 要素が存在しない場合はオペレーションを適用しません。<br>
     *
     * @param operation 各要素に適用するオペレーション
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public void forEach(Consumer<? super T> operation) {
        Objects.requireNonNull(operation);

        if (o != null) {
            operation.accept(o);
        }
        if (a != null) {
            operation.accept(a);
        }
        if (b != null) {
            operation.accept(b);
        }
    }
}
