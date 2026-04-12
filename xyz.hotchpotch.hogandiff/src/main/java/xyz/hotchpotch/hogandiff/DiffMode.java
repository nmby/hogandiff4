package xyz.hotchpotch.hogandiff;

/**
 * 比較モードを表す列挙型です。<br>
 *
 * @author nmby
 */
public enum DiffMode {

    /** 2-way diff（A と B の比較） */
    TWO_WAY,

    /** 3-way diff（起源 O に対する A と B の差分比較） */
    THREE_WAY
}
