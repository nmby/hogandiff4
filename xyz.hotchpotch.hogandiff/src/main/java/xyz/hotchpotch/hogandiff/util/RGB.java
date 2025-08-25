package xyz.hotchpotch.hogandiff.util;

/**
 * RGB色を表すレコードです。<br>
 * 
 * @author nmby
 */
public record RGB(int red, int green, int blue) {
    
    // [static members] ********************************************************
    
    public static RGB fromInt(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return new RGB(r, g, b);
    }
    
    public static RGB fromHex(String hex) {
        if (hex == null) {
            throw new IllegalArgumentException("hex is null.");
        }
        if (!hex.matches("#?[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("hex: " + hex);
        }
        
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        int r = Integer.parseInt(h.substring(0, 2), 16);
        int g = Integer.parseInt(h.substring(2, 4), 16);
        int b = Integer.parseInt(h.substring(4, 6), 16);
        return new RGB(r, g, b);
    }
    
    // [instance members] ******************************************************
    
    public RGB {
        if (red < 0 || 255 < red) {
            throw new IllegalArgumentException("red: " + red);
        }
        if (green < 0 || 255 < green) {
            throw new IllegalArgumentException("green: " + green);
        }
        if (blue < 0 || 255 < blue) {
            throw new IllegalArgumentException("blue: " + blue);
        }
    }
    
    public int toInt() {
        return (red << 16) | (green << 8) | blue;
    }
    
    public String toHex() {
        return String.format("#%02X%02X%02X", red, green, blue);
    }
}
