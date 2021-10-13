package xyz.hotchpotch.hogandiff.util.function;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link Consumer} のチェック例外をスローできるバージョンです。<br>
 * つまり、{@link Consumer#accept(Object)} はチェック例外をスローできませんが、
 * {@link UnsafeConsumer#accept(Object)} はスローできます。<br>
 *
 * @param <T> 引数の型
 * @author nmby
 * @see Consumer
 */
@FunctionalInterface
public interface UnsafeConsumer<T> {
    
    // [static members] ********************************************************
    
    /**
     * {@link Consumer} を {@link UnsafeConsumer} に変換します。<br>
     * 
     * @param <T> 引数の型
     * @param safer コンシューマ
     * @return 型だけが変換されたコンシューマ
     * @throws NullPointerException {@code safer} が {@code null} の場合
     */
    public static <T> UnsafeConsumer<T> from(Consumer<T> safer) {
        Objects.requireNonNull(safer, "safer");
        
        return safer::accept;
    }
    
    // [instance members] ******************************************************
    
    /**
     * {@link Consumer#accept(Object)} の {@link Exception} をスローできるバージョンです。<br>
     * アクションを実行します。<br>
     * 
     * @param t 入力値
     * @throws Exception 何らかのチェック例外
     */
    void accept(T t) throws Exception;
    
    /**
     * この {@link UnsafeConsumer} を {@link Consumer} に変換します。<br>
     * {@code unsafer} がスローするチェック例外は、
     * {@link RuntimeException} にラップされます。<br>
     * 
     * @return スローする例外が変換されたコンシューマ
     */
    default Consumer<T> toConsumer() {
        return toConsumer(RuntimeException::new);
    }
    
    /**
     * この {@link UnsafeConsumer} を {@link Consumer} に変換します。<br>
     * {@code unsafer} がスローするチェック例外は、
     * 指定されたラッパーで非チェック例外にラップされます。<br>
     * 
     * @param wrapper チェック例外を非チェック例外に変換するラッパー
     * @return スローする例外が変換されたコンシューマ
     * @throws NullPointerException {@code wrapper} が {@code null} の場合
     */
    default Consumer<T> toConsumer(
            Function<? super Exception, ? extends RuntimeException> wrapper) {
        
        Objects.requireNonNull(wrapper, "wrapper");
        
        return t -> {
            try {
                accept(t);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw wrapper.apply(e);
            }
        };
    }
}
