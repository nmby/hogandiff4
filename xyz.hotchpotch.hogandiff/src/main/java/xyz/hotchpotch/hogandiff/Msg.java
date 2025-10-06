package xyz.hotchpotch.hogandiff;

import java.util.ResourceBundle;

/**
 * 本アプリで利用するユーザー向けメッセージを一覧管理するクラスです。<br>
 * メッセージの利用有無を静的に管理しやすいようにクラス化しています。<br>
 * 
 * @author nmby
 */
public enum Msg {
    
    // [static members] ********************************************************
    
    APP_0010("APP_0010"),
    APP_0020("APP_0020"),
    APP_0030("APP_0030"),
    APP_0040("APP_0040"),
    APP_0050("APP_0050"),
    APP_0060("APP_0060"),
    APP_0070("APP_0070"),
    APP_0080("APP_0080"),
    APP_0090("APP_0090"),
    APP_0100("APP_0100"),
    APP_0110("APP_0110"),
    APP_0120("APP_0120"),
    APP_0130("APP_0130"),
    APP_0140("APP_0140"),
    APP_0150("APP_0150"),
    APP_0160("APP_0160"),
    APP_0170("APP_0170"),
    APP_0180("APP_0180"),
    APP_0190("APP_0190"),
    APP_0200("APP_0200"),
    APP_0210("APP_0210"),
    APP_0220("APP_0220"),
    APP_0230("APP_0230"),
    APP_0240("APP_0240"),
    APP_0250("APP_0250"),
    APP_0260("APP_0260"),
    APP_0270("APP_0270"),
    APP_0280("APP_0280"),
    APP_0290("APP_0290"),
    APP_0300("APP_0300"),
    APP_0310("APP_0310"),
    APP_0320("APP_0320"),
    APP_0330("APP_0330"),
    APP_0340("APP_0340"),
    APP_0350("APP_0350"),
    APP_0360("APP_0360"),
    APP_0370("APP_0370"),
    APP_0380("APP_0380"),
    APP_0390("APP_0390"),
    APP_0400("APP_0400"),
    APP_0410("APP_0410"),
    APP_0420("APP_0420"),
    APP_0430("APP_0430"),
    APP_0440("APP_0440"),
    APP_0450("APP_0450"),
    APP_0460("APP_0460"),
    APP_0470("APP_0470"),
    APP_0480("APP_0480"),
    APP_0490("APP_0490"),
    APP_0500("APP_0500"),
    APP_0510("APP_0510"),
    APP_0520("APP_0520"),
    APP_0530("APP_0530"),
    APP_0540("APP_0540"),
    APP_0550("APP_0550"),
    APP_0560("APP_0560"),
    APP_0570("APP_0570"),
    APP_0580("APP_0580"),
    APP_0590("APP_0590"),
    APP_0600("APP_0600"),
    APP_0610("APP_0610"),
    APP_0620("APP_0620"),
    APP_0630("APP_0630"),
    APP_0640("APP_0640"),
    APP_0650("APP_0650"),
    APP_0660("APP_0660"),
    APP_0670("APP_0670"),
    APP_0680("APP_0680"),
    APP_0690("APP_0690"),
    APP_0700("APP_0700"),
    APP_0710("APP_0710"),
    APP_0720("APP_0720"),
    APP_0730("APP_0730"),
    APP_0740("APP_0740"),
    APP_0750("APP_0750"),
    APP_0760("APP_0760"),
    APP_0770("APP_0770"),
    APP_0780("APP_0780"),
    APP_0790("APP_0790"),
    APP_0800("APP_0800"),
    APP_0810("APP_0810"),
    APP_0820("APP_0820"),
    APP_0830("APP_0830"),
    APP_0840("APP_0840"),
    APP_0850("APP_0850"),
    APP_0860("APP_0860"),
    APP_0870("APP_0870"),
    APP_0880("APP_0880"),
    APP_0890("APP_0890"),
    APP_0900("APP_0900"),
    APP_0910("APP_0910"),
    APP_0920("APP_0920"),
    APP_0930("APP_0930"),
    APP_0940("APP_0940"),
    APP_0950("APP_0950"),
    APP_0960("APP_0960"),
    APP_0970("APP_0970"),
    APP_0980("APP_0980"),
    APP_0990("APP_0990"),
    APP_1000("APP_1000"),
    APP_1010("APP_1010"),
    APP_1020("APP_1020"),
    APP_1030("APP_1030"),
    APP_1040("APP_1040"),
    APP_1050("APP_1050"),
    APP_1060("APP_1060"),
    APP_1070("APP_1070"),
    APP_1080("APP_1080"),
    APP_1091("APP_1091"),
    APP_1092("APP_1092"),
    APP_1093("APP_1093"),
    APP_1100("APP_1100"),
    APP_1110("APP_1110"),
    APP_1120("APP_1120"),
    APP_1130("APP_1130"),
    APP_1140("APP_1140"),
    APP_1150("APP_1150"),
    APP_1160("APP_1160"),
    APP_1170("APP_1170"),
    APP_1180("APP_1180"),
    APP_1190("APP_1190"),
    APP_1200("APP_1200"),
    APP_1210("APP_1210"),
    APP_1220("APP_1220"),
    APP_1230("APP_1230"),
    APP_1240("APP_1240"),
    APP_1250("APP_1250"),
    APP_1260("APP_1260"),
    APP_1270("APP_1270"),
    APP_1280("APP_1280"),
    APP_1290("APP_1290");
    
    private static final ResourceBundle rb = AppMain.appResource.get();
    
    // [instance members] ******************************************************
    
    private final String id;
    
    private Msg(String id) {
        this.id = id;
    }
    
    /**
     * 指定されたIDのメッセージを返します。<br>
     * 
     * @return 指定されたIDのメッセージ
     */
    public String get() {
        return rb.getString(id);
    }
}
