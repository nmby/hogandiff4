package xyz.hotchpotch.hogandiff.util;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

/**
 * 2つの {@link int} 値を保持する不変クラスです。<br>
 * 
 * @author nmby
 */
// 実装メモ：
// このクラスは記憶領域の節約に主眼を置いて設計されています。
public abstract class IntPair {
    
    // [static members] ********************************************************
    
    private static class Both extends IntPair {
        private final int a;
        private final int b;
        
        private Both(int a, int b) {
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
            Objects.requireNonNull(mapper, "mapper");
            
            return new Both(mapper.applyAsInt(a), mapper.applyAsInt(b));
        }
        
        @Override
        public String toString() {
            return "(%d, %d)".formatted(a, b);
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof Both p) {
                return a == p.a && b == p.b;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return 31 * a + b;
        }
    }
    
    private static class OnlyA extends IntPair {
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
            Objects.requireNonNull(mapper, "mapper");
            
            return new OnlyA(mapper.applyAsInt(a));
        }
        
        @Override
        public String toString() {
            return "(%d, null)".formatted(a);
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof OnlyA p) {
                return a == p.a;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return 31 * a;
        }
    }
    
    private static class OnlyB extends IntPair {
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
            Objects.requireNonNull(mapper, "mapper");
            
            return new OnlyB(mapper.applyAsInt(b));
        }
        
        @Override
        public String toString() {
            return "(null, %d)".formatted(b);
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof OnlyB p) {
                return b == p.b;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return b;
        }
    }
    
    private static class Empty extends IntPair {
        
        @Override
        public String toString() {
            return "(null, null)";
        }
        
        @Override
        public boolean equals(Object o) {
            return o instanceof Empty;
        }
        
        @Override
        public int hashCode() {
            return 0;
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
        return new Both(a, b);
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
    
    // [instance members] ******************************************************
    
    private IntPair() {
    }
    
    public int a() {
        throw new NoSuchElementException();
    }
    
    public int b() {
        throw new NoSuchElementException();
    }
    
    public boolean hasA() {
        return false;
    }
    
    public boolean hasB() {
        return false;
    }
    
    public final boolean isPaired() {
        return hasA() && hasB();
    }
    
    public final boolean isOnlyA() {
        return hasA() && !hasB();
    }
    
    public final boolean isOnlyB() {
        return !hasA() && hasB();
    }
    
    public IntPair map(IntUnaryOperator mapper) {
        Objects.requireNonNull(mapper, "mapper");
        
        return this;
    }
}
