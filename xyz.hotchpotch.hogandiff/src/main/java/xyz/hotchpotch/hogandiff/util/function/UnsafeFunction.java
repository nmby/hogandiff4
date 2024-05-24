package xyz.hotchpotch.hogandiff.util.function;

import java.util.Objects;
import java.util.function.Function;

/**
 * {@link Function} のチェック例外をスローできるバージョンです。<br>
 * つまり、{@link Function#apply(Object)} はチェック例外をスローできませんが、
 * {@link UnsafeFunction#apply(Object)} はスローできます。<br>
 *
 * @param <T> 入力の型
 * @param <R> 出力の型
 * @param <E> スローしうるチェック例外の型
 * @author nmby
 * @see Function
 */
@FunctionalInterface
public interface UnsafeFunction<T, R, E extends Exception> {
    
    // [static members] ********************************************************
    
    /**
     * 正常な出力または例外を保持するレコードです。<br>
     * 
     * @author nmby
     * 
     * @param <R> 出力の型
     * @param result 正常な出力
     * @param thrown 例外
     */
    public static record ResultOrThrown<R>(R result, Exception thrown) {
    };
    
    /**
     * 常に入力引数を返す関数を返します。<br>
     * 
     * @param <T> 関数の入力および出力の型
     * @param <E> スローしうるチェック例外の型
     * @return 常に入力引数を返す関数
     */
    public static <T, E extends Exception> UnsafeFunction<T, T, E> identity() {
        return t -> t;
    }
    
    /**
     * {@link Function} を {@link UnsafeFunction} に変換します。<br>
     * 
     * @param <T> 入力の型
     * @param <R> 出力の型
     * @param <E> スローしうるチェック例外の型
     * @param safer 関数
     * @return 型だけが変換された関数
     * @throws NullPointerException {@code safer} が {@code null} の場合
     */
    public static <T, R, E extends Exception> UnsafeFunction<T, R, E> from(Function<T, R> safer) {
        Objects.requireNonNull(safer, "safer");
        
        return safer::apply;
    }
    
    // [instance members] ******************************************************
    
    /**
     * {@link Function#apply} のチェック例外をスローできるバージョンです。<br>
     * 
     * @param t 関数の引数
     * @return {@code t} 関数の結果
     * @throws E 何らかのチェック例外
     */
    R apply(T t) throws E;
    
    /**
     * {@link Function#compose(Function)} の説明を参照してください。<br>
     * 
     * @param <V> {@code before} 関数および合成関数の入力の型
     * @param before この関数を適用する前に適用する関数
     * @return まず {@code before} を適用し、次にこの関数を適用する合成関数
     * @throws NullPointerException {@code before} が {@code null} の場合
     */
    default <V> UnsafeFunction<V, R, E> compose(
            UnsafeFunction<? super V, ? extends T, ? extends E> before) {
        
        Objects.requireNonNull(before, "before");
        
        return v -> apply(before.apply(v));
    }
    
    /**
     * {@link Function#compose(Function)} の説明を参照してください。<br>
     * 
     * @param <V> {@code before} 関数および合成関数の入力の型
     * @param before この関数を適用する前に適用する関数
     * @return まず {@code before} を適用し、次にこの関数を適用する合成関数
     * @throws NullPointerException {@code before} が {@code null} の場合
     */
    default <V> UnsafeFunction<V, R, E> compose(Function<? super V, ? extends T> before) {
        Objects.requireNonNull(before, "before");
        
        return v -> apply(before.apply(v));
    }
    
    /**
     * {@link Function#andThen(Function)} の説明を参照してください。<br>
     * 
     * @param <V> {@code after} 関数および合成関数の出力の型
     * @param after この関数を適用した後に適用する関数
     * @return まずこの関数を適用し、次に {@code after} 関数を適用する合成関数
     * @throws NullPointerException {@code after} が {@code null} の場合
     */
    default <V> UnsafeFunction<T, V, E> andThen(
            UnsafeFunction<? super R, ? extends V, ? extends E> after) {
        
        Objects.requireNonNull(after, "after");
        
        return t -> after.apply(apply(t));
    }
    
    /**
     * {@link Function#andThen(Function)} の説明を参照してください。<br>
     * 
     * @param <V> {@code after} 関数および合成関数の出力の型
     * @param after この関数を適用した後に適用する関数
     * @return まずこの関数を適用し、次に {@code after} 関数を適用する合成関数
     * @throws NullPointerException {@code after} が {@code null} の場合
     */
    default <V> UnsafeFunction<T, V, E> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after, "after");
        
        return t -> after.apply(apply(t));
    }
    
    /**
     * この {@link UnsafeFunction} を {@link Function} に変換します。<br>
     * この {@link UnsafeFunction} がスローするチェック例外は、
     * {@link RuntimeException} にラップされます。<br>
     * 
     * @return 型だけが変換された関数
     */
    default Function<T, R> toFunction() {
        return toFunction(RuntimeException::new);
    }
    
    /**
     * この {@link UnsafeFunction} を {@link Function} に変換します。<br>
     * この {@link UnsafeFunction} がスローするチェック例外は、
     * 指定されたラッパーで非チェック例外にラップされます。<br>
     * 
     * @param wrapper チェック例外を非チェック例外に変換するラッパー
     * @return スローする例外が変換された関数
     * @throws NullPointerException {@code wrapper} が {@code null} の場合
     */
    default Function<T, R> toFunction(
            Function<? super Exception, ? extends RuntimeException> wrapper) {
        
        Objects.requireNonNull(wrapper, "wrapper");
        
        return t -> {
            try {
                return apply(t);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw wrapper.apply(e);
            }
        };
    }
    
    /**
     * この {@link UnsafeFunction} を、関数の実行結果または例外を保持するタプル {@link ResultOrThrown} を返す
     * {@link Function} に変換します。<br>
     * 
     * @return 関数の実行結果または例外を保持するタプル {@link ResultOrThrown} を返す {@link Function}
     */
    default Function<T, ResultOrThrown<R>> convert() {
        return t -> {
            try {
                return new ResultOrThrown<>(apply(t), null);
            } catch (Exception e) {
                return new ResultOrThrown<>(null, e);
            }
        };
    }
}
