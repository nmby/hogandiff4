package xyz.hotchpotch.hogandiff.logic;

public sealed interface PairingInfo
        permits PairingInfoBooks, PairingInfoDirs {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /**
     * 対応付けがなされているかどうかを返します。<br>
     * 
     * @return 対応付けがなされている場合 {@code true}
     */
    boolean isPaired();
}
