package xyz.hotchpotch.hogandiff.logic._google;

import java.util.stream.Stream;

public enum GoogleFileType {
    
    // [static members] ********************************************************
    
    GOOGLE_SPREADSHEET("application/vnd.google-apps.spreadsheet"),
    EXCEL_BOOK_NEW("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    EXCEL_BOOK_OLD("application/vnd.ms-excel");
    
    public static GoogleFileType of(String mimeType) {
        return Stream.of(GoogleFileType.values())
                .filter(type -> mimeType.equals(type.mimeType))
                .findFirst()
                .orElseThrow();
    }
    
    // [instance members] ******************************************************
    
    private final String mimeType;
    
    private GoogleFileType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public String mimeType() {
        return mimeType;
    }
}
