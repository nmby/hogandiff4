package xyz.hotchpotch.hogandiff.logic.google;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Googleドライブ上のファイル種別を表します。<br>
 * 
 * @author nmby
 */
public enum GoogleFileType {
    
    // [static members] ********************************************************
    
    /** Googleスプレッドシート */
    GOOGLE_SPREADSHEET("application/vnd.google-apps.spreadsheet", ".xlsx"),
    
    /** Excelファイル（.xlsx） */
    EXCEL_XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
    
    /** Excelファイル（.xlsm） */
    EXCEL_XLSM("application/vnd.ms-excel.sheet.macroenabled.12", ".xlsm"),
    
    /** Excelファイル（.xls） */
    EXCEL_XLS("application/vnd.ms-excel", ".xls");
    
    /**
     * 指定されたMIMEタイプに対応する {@link GoogleFileType} を返します。<br>
     * 
     * @param mimeType MIMEタイプ
     * @return Googleファイル種別
     * @throws NullPointerException パラメータに {@code null} が含まれる場合
     * @throws java.util.NoSuchElementException 対応する {@link GoogleFileType} が存在しない場合
     */
    public static GoogleFileType of(String mimeType) {
        Objects.requireNonNull(mimeType);
        
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
    
    /**
     * MIMEタイプを返します。<br>
     * 
     * @return MIMEタイプ
     */
    public String mimeType() {
        return mimeType;
    }
    
    /**
     * 拡張子を返します。<br>
     * 
     * @return 拡張子
     */
    public String ext() {
        return ext;
    }
}
