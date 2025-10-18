package xyz.hotchpotch.hogandiff.util.function;

import java.util.Objects;
import java.util.function.Function;

/**
 * {@link Runnable} のチェック例外をスローできるバージョンです。<br>
 * 
 * @author nmby
 */
@FunctionalInterface
public interface UnsafeRunnable {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 例外をスローする可能性のある処理を実行します。<br>
     * 
     * @throws Exception
     *             処理中に例外が発生した場合
     */
    void run() throws Exception;
    
    /**
     * この {@link UnsafeRunnable} を {@link Runnable} に変換します。<br>
     * この {@link UnsafeRunnable} がスローするチェック例外は、
     * {@link RuntimeException} にラップされます。<br>
     * 
     * @return 変換後の {@link Runnable}
     */
    default Runnable toRunnable() {
        return toRunnable(RuntimeException::new);
    }
    
    /**
     * この {@link UnsafeRunnable} を {@link Runnable} に変換します。<br>
     * この {@link UnsafeRunnable} がスローするチェック例外は、
     * 指定されたラッパーで非チェック例外にラップされます。<br>
     * 
     * @param wrapper
     *            チェック例外を非チェック例外に変換するラッパー
     * @return 変換後の {@link Runnable}
     * @throws NullPointerException
     *             パラメータが {@code null} の場合
     */
    default Runnable toRunnable(Function<? super Exception, ? extends RuntimeException> wrapper) {
        Objects.requireNonNull(wrapper);
        
        return () -> {
            try {
                run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw wrapper.apply(e);
            }
        };
    }
}
