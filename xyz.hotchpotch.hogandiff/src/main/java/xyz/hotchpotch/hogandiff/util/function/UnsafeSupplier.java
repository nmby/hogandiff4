package xyz.hotchpotch.hogandiff.util.function;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link Supplier} のチェック例外をスローできるバージョンです。<br>
 * つまり、{@link Supplier#get()} はチェック例外をスローできませんが、
 * {@link UnsafeSupplier#get()} はスローできます。<br>
 *
 * @param <T> 生成される値の型
 * @param <E> スローしうるチェック例外の型
 * @author nmby
 * @see Supplier
 */
@FunctionalInterface
public interface UnsafeSupplier<T, E extends Exception> {
    
    // [static members] ********************************************************
    
    /**
     * {@link Supplier} を {@link UnsafeSupplier} に変換します。<br>
     * 
     * @param <T> 生成される値の型
     * @param <E> スローしうるチェック例外の型
     * @param safer サプライヤ
     * @return 型だけが変換されたサプライヤ
     * @throws NullPointerException {@code safer} が {@code null} の場合
     */
    public static <T, E extends Exception> UnsafeSupplier<T, E> from(Supplier<T> safer) {
        Objects.requireNonNull(safer, "safer");
        
        return safer::get;
    }
    
    // [instance members] ******************************************************
    
    /**
     * {@link Supplier#get} のチェック例外をスローできるバージョンです。<br>
     * 値を取得します。<br>
     * 
     * @return 出力値
     * @throws E 何らかのチェック例外
     */
    T get() throws E;
    
    /**
     * この {@link UnsafeSupplier} を {@link Supplier} に変換します。<br>
     * この {@link UnsafeSupplier} がスローするチェック例外は、
     * {@link RuntimeException} にラップされます。<br>
     * 
     * @return スローする例外が変換されたサプライヤ
     */
    default Supplier<T> toSupplier() {
        return toSupplier(RuntimeException::new);
    }
    
    /**
     * この {@link UnsafeSupplier} を {@link Supplier} に変換します。<br>
     * この {@link UnsafeSupplier} がスローするチェック例外は、
     * 指定されたラッパーで非チェック例外にラップされます。<br>
     * 
     * @param wrapper チェック例外を非チェック例外に変換するラッパー
     * @return スローする例外が変換されたサプライヤ
     * @throws NullPointerException {@code wrapper} が {@code null} の場合
     */
    default Supplier<T> toSupplier(
            Function<? super Exception, ? extends RuntimeException> wrapper) {
        
        Objects.requireNonNull(wrapper, "wrapper");
        
        return () -> {
            try {
                return get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw wrapper.apply(e);
            }
        };
    }
}
