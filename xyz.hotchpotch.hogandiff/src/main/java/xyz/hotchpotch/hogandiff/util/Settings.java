package xyz.hotchpotch.hogandiff.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * アプリケーションで必要となる様々な型の各種設定値を
 * 一元的に管理するための不変クラスです。<br>
 *
 * @author nmby
 */
public class Settings {
    
    // [static members] ********************************************************
    
    /**
     * 設定項目を表す不変クラスです。<br>
     *
     * @param <T> 設定値の型
     * @author nmby
     */
    // java16で正式導入されたRecordを使ってみる。
    // 外形的にはこのクラスがRecordであることは全く問題がないが、
    // 思想的?にはRecordじゃない気もするけど、まぁ試しに使ってみる。
    public static record Key<T> (
            String name,
            Supplier<? extends T> defaultValueSupplier,
            Function<? super T, String> encoder,
            Function<String, ? extends T> decoder,
            boolean storable) {
        
        // [static members] ----------------------------------------------------
        
        /**
         * 新しい設定項目を定義します。<br>
         * 
         * @param <T> 設定値の型
         * @param name 設定項目の名前
         * @param defaultValueSupplier 設定項目のデフォルト値のサプライヤ
         * @param encoder 設定値を文字列に変換するエンコーダー
         * @param decoder 文字列を設定値に変換するエンコーダー
         * @param storable この設定項目の値がプロパティファイルへの保存対象の場合は {@code true}
         * @return 新しい設定項目
         * @throws NullPointerException
         *          {@code name}, {@code defaultValueSupplier}, {@code encoder}, {@code decoder}
         *          のいずれかが {@code null} の場合
         */
        @Deprecated
        public static <T> Key<T> defineAs(
                String name,
                Supplier<? extends T> defaultValueSupplier,
                Function<? super T, String> encoder,
                Function<String, ? extends T> decoder,
                boolean storable) {
            
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(defaultValueSupplier, "defaultValueSupplier");
            Objects.requireNonNull(encoder, "encoder");
            Objects.requireNonNull(decoder, "decoder");
            
            return new Key<>(name, defaultValueSupplier, encoder, decoder, storable);
        }
        
        // [instance members] --------------------------------------------------
        
        /**
         * 新しい設定項目を定義します。<br>
         * 
         * @param name 設定項目の名前
         * @param defaultValueSupplier 設定項目のデフォルト値のサプライヤ
         * @param encoder 設定値を文字列に変換するエンコーダー
         * @param decoder 文字列を設定値に変換するエンコーダー
         * @param storable この設定項目の値がプロパティファイルへの保存対象の場合は {@code true}
         * @throws NullPointerException
         *          {@code name}, {@code defaultValueSupplier}, {@code encoder}, {@code decoder}
         *          のいずれかが {@code null} の場合
         */
        // java16で正式導入されたRecordを使ってみたいが故にこのクラスをRecordとしているが、
        // 本来はコンストラクタを公開する必要がない。ぐぬぬ
        // recordを使う欲の方が上回ったのでコンストラクタを公開しちゃう。ぐぬぬ
        public Key {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(defaultValueSupplier, "defaultValueSupplier");
            Objects.requireNonNull(encoder, "encoder");
            Objects.requireNonNull(decoder, "decoder");
        }
    }
    
    /**
     * {@link Settings} クラスのビルダーです。<br>
     *
     * @author nmby
     */
    public static class Builder {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        // Map<Key<?>, Object> と Map<Key<?>, ?> の違いがいまいち良く分からん...
        // FIXME: [No.91 内部実装改善] 要お勉強
        private final Map<Key<?>, Object> map;
        
        private Builder(Map<Key<?>, ?> original) {
            this.map = new HashMap<>(original);
        }
        
        /**
         * このビルダーに設定を追加します。<br>
         * {@code null} 値は許容されません。値が無い可能性のある設定値を管理したい場合は
         * {@link Optional} を利用してください。<br>
         * 
         * @param <T> 設定値の型
         * @param key 設定項目
         * @param value 設定値
         * @return このビルダー
         * @throws NullPointerException
         *              {@code key}, {@code value} のいずれかが {@code null} の場合
         */
        public <T> Builder set(Key<T> key, T value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            
            map.put(key, value);
            return this;
        }
        
        /**
         * このビルダーに設定をデフォルト値で追加します。<br>
         * 
         * @param <T> 設定値の型
         * @param key 設定項目
         * @return このビルダー
         * @throws NullPointerException {@code key} が {@code null} の場合
         */
        public <T> Builder setDefaultValue(Key<T> key) {
            Objects.requireNonNull(key, "key");
            
            map.put(key, key.defaultValueSupplier.get());
            return this;
        }
        
        /**
         * 指定された設定に含まれる設定項目をこのビルダーにすべて追加します。<br>
         * 設定項目がすでに設定されている場合は、上書きします。<br>
         * 
         * @param other 設定
         * @return このビルダー
         * @throws NullPointerException {@code other} が {@code null} の場合
         */
        public Builder setAll(Settings other) {
            Objects.requireNonNull(other, "other");
            
            map.putAll(other.map);
            return this;
        }
        
        /**
         * このビルダーで {@link Settings} オブジェクトを生成します。<br>
         * 
         * @return 新しい {@link Settings} オブジェクト
         * @throws IllegalStateException このビルダーに同じ名前の設定項目が含まれる場合
         * @throws NullPointerException このビルダーに {@code null} 値が含まれる場合
         */
        public Settings build() {
            return new Settings(this);
        }
    }
    
    /**
     * このクラスのビルダーを返します。<br>
     * 
     * @return 新しいビルダー
     */
    public static Builder builder() {
        return new Builder(Map.of());
    }
    
    /**
     * 指定された設定で初期化された、このクラスのビルダーを返します。<br>
     * 
     * @param original 初期設定
     * @return 新しいビルダー
     * @throws NullPointerException {@code original} が {@code null} の場合
     */
    public static Builder builder(Settings original) {
        Objects.requireNonNull(original, "original");
        
        return new Builder(original.map);
    }
    
    /**
     * 指定されたプロパティセットと設定項目セットで初期化された、
     * このクラスのビルダーを返します。<br>
     * 具体的には、指定された設定項目セットに含まれる設定項目名のプロパティが
     * 指定されたプロパティセットに含まれる場合はそのプロパティ値を初期値とし、
     * 含まれない場合はその設定項目のデフォルト値を初期値として、ビルダーを構成します。<br>
     * 
     * @param properties プロパティセット
     * @param keys 設定項目セット
     * @return 新しいビルダー
     * @throws NullPointerException
     *              {@code properties}, {@code keys} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code keys} に同じ名前の設定項目が含まれる場合
     */
    public static Builder builder(Properties properties, Set<Key<?>> keys) {
        Objects.requireNonNull(properties, "properties");
        Objects.requireNonNull(keys, "keys");
        ifDuplicatedThenThrow(keys, IllegalArgumentException::new);
        
        Map<Key<?>, Object> map = keys.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        key -> {
                            if (properties.containsKey(key.name())) {
                                String value = properties.getProperty(key.name());
                                return key.decoder().apply(value);
                            } else {
                                return key.defaultValueSupplier().get();
                            }
                        }));
        
        return new Builder(map);
    }
    
    /**
     * 指定された設定項目セットに同じ名前の設定項目が含まれるが調べ、
     * 含まれる場合は例外をスローします。<br>
     * 
     * @param keys 設定項目セット
     * @param exceptionSupplier 例外生成関数
     * @throws RuntimeException 同じ名前の設定項目が含まれる場合
     */
    private static void ifDuplicatedThenThrow(
            Set<Key<?>> keys,
            Function<String, ? extends RuntimeException> exceptionSupplier) {
        
        assert keys != null;
        
        Map<String, Long> nameToCount = keys.stream()
                .map(Key::name)
                .collect(Collectors.groupingBy(
                        Function.identity(),
                        Collectors.counting()));
        
        String duplicatedNames = nameToCount.entrySet().stream()
                .filter(entry -> 2 <= entry.getValue())
                .map(Entry::getKey)
                .collect(Collectors.joining(", "));
        
        if (0 < duplicatedNames.length()) {
            throw exceptionSupplier.apply("次の名前が重複定義されています：" + duplicatedNames);
        }
    }
    
    // [instance members] ******************************************************
    
    private final Map<Key<?>, ?> map;
    
    private Settings(Builder builder) {
        assert builder != null;
        
        map = Map.copyOf(builder.map);
        
        ifDuplicatedThenThrow(map.keySet(), IllegalStateException::new);
    }
    
    /**
     * 指定された設定項目の値を返します。<br>
     * 
     * @param <T> 設定値の型
     * @param key 設定項目
     * @return 設定値
     * @throws NullPointerException {@code item} が {@code null} の場合
     * @throws NoSuchElementException {@code item} が設定されていない場合
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Key<T> key) {
        Objects.requireNonNull(key, "key");
        
        if (map.containsKey(key)) {
            return (T) map.get(key);
        } else {
            throw new NoSuchElementException("設定されていません：" + key.name());
        }
    }
    
    /**
     * この設定に指定された設定項目が含まれているかを返します。<br>
     * 
     * @param key 設定項目
     * @return この設定に指定された設定項目が含まれている場合は {@code true}
     * @throws NullPointerException {@code key} が {@code null} の場合
     */
    public boolean containsKey(Key<?> key) {
        Objects.requireNonNull(key, "key");
        
        return map.containsKey(key);
    }
    
    /**
     * この設定に含まれる設定項目のセットを返します。<br>
     * 
     * @return この設定に含まれる設定項目のセット
     */
    public Set<Key<?>> keySet() {
        return map.keySet();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Settings other) {
            return map.equals(other.map);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return map.hashCode();
    }
    
    @Override
    public String toString() {
        return map.keySet().stream()
                .map(key -> String.format("%s : %s", key.name(), encodeItem(key)))
                // 再現性を確保するためにソートすることにする。
                .sorted()
                .collect(Collectors.joining(System.lineSeparator()));
    }
    
    private <T> String encodeItem(Key<T> key) {
        assert key != null;
        assert map.containsKey(key);
        
        T value = get(key);
        return key.encoder().apply(value);
    }
    
    /**
     * この設定に含まれる設定項目のうちプロパティファイルに保存可能なものを
     * プロパティセットに抽出します。<br>
     * 
     * @return 保存可能な設定項目を含むプロパティセット
     */
    public Properties toProperties() {
        Properties properties = new Properties();
        
        map.keySet().stream()
                .filter(Key::storable)
                .forEach(key -> properties.setProperty(key.name(), encodeItem(key)));
        
        return properties;
    }
}
