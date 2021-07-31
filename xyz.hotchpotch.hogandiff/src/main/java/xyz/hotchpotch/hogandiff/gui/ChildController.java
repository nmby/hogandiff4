package xyz.hotchpotch.hogandiff.gui;

import xyz.hotchpotch.hogandiff.util.Settings;

/*package*/ interface ChildController {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    default void init(MainController parent) {
    }
    
    default void applySettings(Settings settings) {
    }
    
    default void gatherSettings(Settings.Builder builder) {
    }
}
