package xyz.hotchpotch.hogandiff.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
import java.util.function.ToIntFunction;

import xyz.hotchpotch.hogandiff.util.Pair.Side;

/**
 * 2つの {@code int} 値を保持する不変クラスです。<br>
 * 
 * @author nmby
 */
// 実装メモ：
// このクラスは記憶領域の節約に主眼を置いて設計されています。
public abstract sealed class IntPair implements Comparable<IntPair> {
    
    // [static members] ********************************************************
    
    private static final class Same extends IntPair {
        private final int x;
        
        private Same(int x) {
            this.x = x;
        }
        
        @Override
        public int a() {
            return x;
        }
        
        @Override
        public int b() {
            return x;
        }
        
        @Override
        public boolean hasA() {
            return true;
        }
        
        @Override
        public boolean hasB() {
            return true;
        }
        
        @Override
        public IntPair map(IntUnaryOperator mapper) {
            Objects.requireNonNull(mapper);
            
            return new Same(mapper.applyAsInt(x));
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof Same p) {
                return x == p.x;
            }
            return false;
        }
    }
    
    private static final class Both extends IntPair {
        private final int a;
        private final int b;
        
        private Both(int a, int b) {
            assert a != b;
            
            this.a = a;
            this.b = b;
        }
        
        @Override
        public int a() {
            return a;
        }
        
        @Override
        public int b() {
            return b;
        }
        
        @Override
        public boolean hasA() {
            return true;
        }
        
        @Override
        public boolean hasB() {
            return true;
        }
        
        @Override
        public IntPair map(IntUnaryOperator mapper) {
            Objects.requireNonNull(mapper);
            
            int aa = mapper.applyAsInt(a);
            int bb = mapper.applyAsInt(b);
            
            return aa == bb
                    ? new Same(aa)
                    : new Both(aa, bb);
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof Both p) {
                return a == p.a && b == p.b;
            }
            return false;
        }
    }
    
    private static final class OnlyA extends IntPair {
        private final int a;
        
        private OnlyA(int a) {
            this.a = a;
        }
        
        @Override
        public int a() {
            return a;
        }
        
        @Override
        public boolean hasA() {
            return true;
        }
        
        @Override
        public IntPair map(IntUnaryOperator mapper) {
            Objects.requireNonNull(mapper);
            
            return new OnlyA(mapper.applyAsInt(a));
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof OnlyA p) {
                return a == p.a;
            }
            return false;
        }
    }
    
    private static final class OnlyB extends IntPair {
        private final int b;
        
        private OnlyB(int b) {
            this.b = b;
        }
        
        @Override
        public int b() {
            return b;
        }
        
        @Override
        public boolean hasB() {
            return true;
        }
        
        @Override
        public IntPair map(IntUnaryOperator mapper) {
            Objects.requireNonNull(mapper);
            
            return new OnlyB(mapper.applyAsInt(b));
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof OnlyB p) {
                return b == p.b;
            }
            return false;
        }
    }
    
    private static final class Empty extends IntPair {
        
        @Override
        public IntPair map(IntUnaryOperator mapper) {
            Objects.requireNonNull(mapper);
            
            return this;
        }
        
        @Override
        public boolean equals(Object o) {
            return o instanceof Empty;
        }
    }
    
    private static final IntPair EMPTY = new Empty();
    
    /**
     * 新たなペアを生成します。<br>
     * 
     * @param a 値a
     * @param b 値b
     * @return 新たなペア
     */
    public static IntPair of(int a, int b) {
        return a == b ? new Same(a) : new Both(a, b);
    }
    
    /**
     * 値aだけを保持するペアを生成します。<br>
     * 
     * @param a 値a
     * @return 新たなペア
     */
    public static IntPair onlyA(int a) {
        return new OnlyA(a);
    }
    
    /**
     * 値bだけを保持するペアを生成します。<br>
     * 
     * @param b 値b
     * @return 新たなペア
     */
    public static IntPair onlyB(int b) {
        return new OnlyB(b);
    }
    
    /**
     * 空のペアを生成します。<br>
     * 
     * @return 新たなペア
     */
    public static IntPair empty() {
        return EMPTY;
    }
    
    /**
     * 新たなペアを生成します。<br>
     * 
     * @param pair 元のペア
     * @return 新たなペア
     */
    public static IntPair from(Pair<Integer> pair) {
        return pair.isPaired() ? IntPair.of(pair.a(), pair.b())
                : pair.isOnlyA() ? IntPair.onlyA(pair.a())
                        : pair.isOnlyB() ? IntPair.onlyB(pair.b())
                                : EMPTY;
    }
    
    // [instance members] ******************************************************
    
    private IntPair() {
    }
    
    /**
     * 値aがある場合はその値を返し、そうでない場合は例外をスローします。<br>
     * 
     * @return 値a
     * @throws NoSuchElementException 値aが無い場合
     */
    public int a() {
        throw new NoSuchElementException();
    }
    
    /**
     * 値bがある場合はその値を返し、そうでない場合は例外をスローします。<br>
     * 
     * @return 値b
     * @throws NoSuchElementException 値bが無い場合
     */
    public int b() {
        throw new NoSuchElementException();
    }
    
    /**
     * 指定した側の値がある場合はその値を返し、そうでない場合は例外をスローします。<br>
     * 
     * @param side 値を取得する側
     * @return 指定した側の値
     * @throws NoSuchElementException 指定した側の値が無い場合
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public int get(Side side) {
        Objects.requireNonNull(side);
        return side == Side.A ? a() : b();
    }
    
    /**
     * 値aが存在するかを返します。<br>
     * 
     * @return 値aが存在する場合は {@code true}
     */
    public boolean hasA() {
        return false;
    }
    
    /**
     * 値bが存在するかを返します。<br>
     * 
     * @return 値bが存在する場合は {@code true}
     */
    public boolean hasB() {
        return false;
    }
    
    /**
     * 値a, 値bがともに存在するかを返します。<br>
     * 
     * @return 両値ともに存在する場合は {@code true}
     */
    public final boolean isPaired() {
        return hasA() && hasB();
    }
    
    /**
     * 値aだけが存在するかを返します。<br>
     * 
     * @return 値aだけが存在する場合は {@code true}
     */
    public final boolean isOnlyA() {
        return hasA() && !hasB();
    }
    
    /**
     * 値bだけが存在するかを返します。<br>
     * 
     * @return 値bだけが存在する場合は {@code true}
     */
    public final boolean isOnlyB() {
        return !hasA() && hasB();
    }
    
    /**
     * 指定された側の値だけが存在するかを返します。<br>
     * 
     * @param side 検査する側
     * @return 指定された側の値だけが存在する場合は {@code true}
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public final boolean isOnly(Side side) {
        Objects.requireNonNull(side);
        
        return side == Side.A ? isOnlyA() : isOnlyB();
    }
    
    /**
     * このペアが空かを返します。<br>
     * 
     * @return このペアが空の場合は {@code true}
     */
    public final boolean isEmpty() {
        return !hasA() && !hasB();
    }
    
    /**
     * 値a, 値bそれぞれに指定された演算を適用して得られるペアを返します。<br>
     * 
     * @param mapper 値a, 値bに適用する演算
     * @return 新たなペア
     * @throws NullPointerException {@code mapper} が {@code null} の場合
     */
    public abstract IntPair map(IntUnaryOperator mapper);
    
    @Override
    public String toString() {
        return "(%s, %s)".formatted(
                hasA() ? a() : null,
                hasB() ? b() : null);
    }
    
    @Override
    public int hashCode() {
        return (hasA() ? 31 * a() : 0) + (hasB() ? b() : 0);
    }
    
    @Override
    public int compareTo(IntPair o) {
        Objects.requireNonNull(o);
        
        ToIntFunction<IntPair> pairScore = p -> p.isPaired() ? 0 : p.isOnlyA() ? 1 : p.isOnlyB() ? 2 : 3;
        int myScore = pairScore.applyAsInt(this);
        int themScore = pairScore.applyAsInt(o);
        
        if (myScore != themScore) {
            return Integer.compare(myScore, themScore);
            
        } else if (hasA() && a() != o.a()) {
            return Integer.compare(a(), o.a());
            
        } else if (hasB() && b() != o.b()) {
            return Integer.compare(b(), o.b());
            
        } else {
            return 0;
        }
    }
}
