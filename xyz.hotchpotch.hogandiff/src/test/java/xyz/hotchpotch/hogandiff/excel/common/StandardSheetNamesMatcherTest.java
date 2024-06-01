package xyz.hotchpotch.hogandiff.excel.common;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.SheetNamesMatcher;
import xyz.hotchpotch.hogandiff.util.Pair;

class StandardSheetNamesMatcherTest {
    
    // [static members] ********************************************************
    
    private static final List<String> sheetNames11 = List
            .of("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");
    private static final List<String> sheetNames12 = List
            .of("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday");
    private static final List<String> sheetNames13 = List
            .of("Friday", "Monday", "Saturday", "Sunday", "Thursday", "Tuesday", "Wednesday");
    private static final List<String> sheetNames14 = List
            .of("Friday", "Monday", "Saturday", "Thursday", "Tuesday", "Wednesday");
    private static final List<String> sheetNames15 = List
            .of("Sunday", "Mon", "Tues", "Wednesday", "Thurs", "Fri", "DUMMY");
    
    private static final BookOpenInfo dummyOpenInfo = new BookOpenInfo(Path.of("dummy"), null);
    
    private static Pair<BookInfo> pairOf(List<String> sheetNames1, List<String> sheetNames2) {
        return new Pair<>(
                new BookInfo(dummyOpenInfo, sheetNames1),
                new BookInfo(dummyOpenInfo, sheetNames2));
    }
    
    // [instance members] ******************************************************
    
    @Test
    void testPairingSheetNames1() {
        SheetNamesMatcher testee = SheetNamesMatcher.of(false);
        
        assertEquals(
                Set.of(
                        new Pair<>("Sunday", "sunday"),
                        new Pair<>("Monday", "monday"),
                        new Pair<>("Tuesday", "tuesday"),
                        new Pair<>("Wednesday", "wednesday"),
                        new Pair<>("Thursday", "thursday"),
                        new Pair<>("Friday", "friday"),
                        new Pair<>("Saturday", "saturday")),
                Set.copyOf(testee.pairingSheetNames(pairOf(sheetNames11, sheetNames12))));
        
        assertEquals(
                Set.of(
                        new Pair<>("Sunday", "Sunday"),
                        new Pair<>("Monday", "Monday"),
                        new Pair<>("Tuesday", "Tuesday"),
                        new Pair<>("Wednesday", "Wednesday"),
                        new Pair<>("Thursday", "Thursday"),
                        new Pair<>("Friday", "Friday"),
                        new Pair<>("Saturday", "Saturday")),
                Set.copyOf(testee.pairingSheetNames(pairOf(sheetNames11, sheetNames13))));
        
        assertEquals(
                Set.of(
                        new Pair<>("Monday", "Monday"),
                        new Pair<>("Tuesday", "Tuesday"),
                        new Pair<>("Wednesday", "Wednesday"),
                        new Pair<>("Thursday", "Thursday"),
                        new Pair<>("Friday", "Friday"),
                        new Pair<>("Saturday", "Saturday"),
                        new Pair<>("Sunday", null)),
                Set.copyOf(testee.pairingSheetNames(pairOf(sheetNames11, sheetNames14))));
        
        assertEquals(
                Set.of(
                        new Pair<>("Sunday", "Sunday"),
                        new Pair<>("Monday", "Mon"),
                        new Pair<>("Tuesday", "Tues"),
                        new Pair<>("Wednesday", "Wednesday"),
                        new Pair<>("Thursday", "Thurs"),
                        new Pair<>("Friday", "Fri"),
                        new Pair<>("Saturday", null),
                        new Pair<>(null, "DUMMY")),
                Set.copyOf(testee.pairingSheetNames(pairOf(sheetNames11, sheetNames15))));
    }
    
    @Test
    void testPairingSheetNames2() {
        SheetNamesMatcher testee = SheetNamesMatcher.of(true);
        
        assertEquals(
                Set.of(
                        new Pair<>("Sunday", null),
                        new Pair<>("Monday", null),
                        new Pair<>("Tuesday", null),
                        new Pair<>("Wednesday", null),
                        new Pair<>("Thursday", null),
                        new Pair<>("Friday", null),
                        new Pair<>("Saturday", null),
                        new Pair<>(null, "sunday"),
                        new Pair<>(null, "monday"),
                        new Pair<>(null, "tuesday"),
                        new Pair<>(null, "wednesday"),
                        new Pair<>(null, "thursday"),
                        new Pair<>(null, "friday"),
                        new Pair<>(null, "saturday")),
                Set.copyOf(testee.pairingSheetNames(pairOf(sheetNames11, sheetNames12))));
        
        assertEquals(
                Set.of(
                        new Pair<>("Sunday", "Sunday"),
                        new Pair<>("Monday", "Monday"),
                        new Pair<>("Tuesday", "Tuesday"),
                        new Pair<>("Wednesday", "Wednesday"),
                        new Pair<>("Thursday", "Thursday"),
                        new Pair<>("Friday", "Friday"),
                        new Pair<>("Saturday", "Saturday")),
                Set.copyOf(testee.pairingSheetNames(pairOf(sheetNames11, sheetNames13))));
        
        assertEquals(
                Set.of(
                        new Pair<>("Monday", "Monday"),
                        new Pair<>("Tuesday", "Tuesday"),
                        new Pair<>("Wednesday", "Wednesday"),
                        new Pair<>("Thursday", "Thursday"),
                        new Pair<>("Friday", "Friday"),
                        new Pair<>("Saturday", "Saturday"),
                        new Pair<>("Sunday", null)),
                Set.copyOf(testee.pairingSheetNames(pairOf(sheetNames11, sheetNames14))));
        
        assertEquals(
                Set.of(
                        new Pair<>("Sunday", "Sunday"),
                        new Pair<>("Wednesday", "Wednesday"),
                        new Pair<>("Monday", null),
                        new Pair<>("Tuesday", null),
                        new Pair<>("Thursday", null),
                        new Pair<>("Friday", null),
                        new Pair<>("Saturday", null),
                        new Pair<>(null, "Mon"),
                        new Pair<>(null, "Tues"),
                        new Pair<>(null, "Thurs"),
                        new Pair<>(null, "Fri"),
                        new Pair<>(null, "DUMMY")),
                Set.copyOf(testee.pairingSheetNames(pairOf(sheetNames11, sheetNames15))));
    }
}
