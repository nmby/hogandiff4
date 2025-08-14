package xyz.hotchpotch.hogandiff.logic.google;

import java.util.stream.Stream;

public enum GoogleFileType {
    
    // [static members] ********************************************************
    
    GOOGLE_SPREADSHEET("application/vnd.google-apps.spreadsheet", ".xlsx"),
    EXCEL_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    EXCEL_XLSM("application/vnd.ms-excel.sheet.macroenabled.12", ".xlsm"),
    EXCEL_XLS("application/vnd.ms-excel", ".xls");
    
    public static GoogleFileType of(String mimeType) {
        return Stream.of(GoogleFileType.values())
                .filter(type -> mimeType.toLowerCase().equals(type.mimeType))
                .findFirst()
                .orElseThrow();
    }
    
    // [instance members] ******************************************************
    
    private final String mimeType;
    private final String ext;
    
    private GoogleFileType(String mimeType, String ext) {
        this.mimeType = mimeType;
        this.ext = ext;
    }
    
    public String mimeType() {
        return mimeType;
    }
    
    public String ext() {
        return ext;
    }
}
