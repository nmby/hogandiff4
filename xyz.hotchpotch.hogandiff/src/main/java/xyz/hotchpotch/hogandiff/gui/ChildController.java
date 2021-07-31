package xyz.hotchpotch.hogandiff.gui;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;
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
    
    default BooleanExpression isReady() {
        return new SimpleBooleanProperty(true);
    }
}
