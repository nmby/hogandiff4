package xyz.hotchpotch.hogandiff.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
// FIXME: defaultValueまわりのポリシーが一貫していないので整理する
public class Settings {
    
    // [static members] ********************************************************
    
    /**
     * 設定項目を表す不変クラスです。<br>
     *
     * @param <T> 設定値の型
     * @param name キーの名前
     * @param defaultValueSupplier デフォルト値のサプライヤ
     * @param encoder 設定値を文字列に変換するエンコーダ
     * @param decoder 文字列を設定値に変換するデコーダ
     * @param storable 設定値をプロパティファイルに保存する場合は {@code true}
     * @author nmby
     */
    public static record Key<T>(
            String name,
            Supplier<? extends T> defaultValueSupplier,
            Function<? super T, String> encoder,
            Function<String, ? extends T> decoder,
            boolean storable) {
        
        // [static members] ----------------------------------------------------
        
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
        
        private final Map<Key<?>, Optional<?>> map;
        
        private Builder(Map<Key<?>, Optional<?>> original) {
            this.map = new HashMap<>(original);
        }
        
        /**
         * このビルダーに設定を追加します。<br>
         * {@link Optional} を利用してください。<br>
         * 
         * @param <T> 設定値の型
         * @param key 設定項目
         * @param value 設定値（{@code null} 許容）
         * @return このビルダー
         * @throws NullPointerException {@code key} が {@code null} の場合
         */
        public <T> Builder set(Key<T> key, T value) {
            Objects.requireNonNull(key, "key");
            
            map.put(key, Optional.ofNullable(value));
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
        
        Map<Key<?>, Optional<?>> map = keys.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        key -> {
                            if (properties.containsKey(key.name())) {
                                String value = properties.getProperty(key.name());
                                return Optional.ofNullable(key.decoder().apply(value));
                            } else {
                                return Optional.ofNullable(key.defaultValueSupplier().get());
                            }
                        }));
        
        return new Builder(map);
    }
    
    /**
     * 指定された設定項目セットに同じ名前の設定項目が含まれるか調べ、
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
            throw exceptionSupplier.apply("duplicated names : " + duplicatedNames);
        }
    }
    
    // [instance members] ******************************************************
    
    private final Map<Key<?>, Optional<?>> map;
    
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
     * @return 設定値。{@code key} が設定されていない場合は {@code null}
     * @throws NullPointerException {@code key} が {@code null} の場合
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Key<T> key) {
        Objects.requireNonNull(key, "key");
        
        Optional<T> value = (Optional<T>) map.get(key);
        return value == null ? null : value.orElse(null);
    }
    
    /**
     * 指定された設定項目の値を返します。
     * 設定項目が設定されていない場合はデフォルト値を返します。<br>
     * 
     * @param <T> 設定値の型
     * @param key 設定項目
     * @return 設定値
     * @throws NullPointerException {@code key} が {@code null} の場合
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(Key<T> key) {
        Objects.requireNonNull(key, "key");
        
        return map.containsKey(key)
                ? (T) map.get(key).orElse(null)
                : key.defaultValueSupplier.get();
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
                .map(key -> "%s : %s".formatted(key.name(), encodeItem(key)))
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
    
    /**
     * この設定に変更を加えた新たな設定を返します。
     * （このオブジェクト自体は変更されません。）<br>
     * 
     * @param <T> 設定値の型
     * @param key 設定項目
     * @param value 設定値
     * @return 新たな設定
     * @throws NullPointerException {@code key} が {@code null} の場合
     */
    public <T> Settings getAltered(Key<T> key, T value) {
        Objects.requireNonNull(key, "key");
        
        return Settings.builder().setAll(this).set(key, value).build();
    }
}
