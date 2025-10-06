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
    
    APP_0010("AppMain.010"),
    APP_0020("AppResource.010"),
    APP_0030("AppTaskBase.030"),
    APP_0040("AppTaskBase.050"),
    APP_0050("AppTaskBase.060"),
    APP_0060("AppTaskBase.070"),
    APP_0070("AppTaskBase.080"),
    APP_0080("AppTaskBase.090"),
    APP_0090("AppTaskBase.100"),
    APP_0100("AppTaskBase.110"),
    APP_0110("AppTaskBase.120"),
    APP_0120("AppTaskBase.150"),
    APP_0130("AppTaskBase.160"),
    APP_0140("AppTaskBase.170"),
    APP_0150("AppTaskBase.180"),
    APP_0160("AppTaskBase.200"),
    APP_0170("CompareBooksTask.010"),
    APP_0180("CompareBooksTask.040"),
    APP_0190("CompareBooksTask.050"),
    APP_0200("CompareBooksTask.060"),
    APP_0210("CompareBooksTask.070"),
    APP_0220("CompareBooksTask.080"),
    APP_0230("CompareBooksTask.090"),
    APP_0240("CompareDirsTask.010"),
    APP_0250("CompareDirsTask.020"),
    APP_0260("CompareDirsTask.050"),
    APP_0270("CompareDirsTask.070"),
    APP_0280("CompareDirsTask.080"),
    APP_0290("CompareSheetsTask.010"),
    APP_0300("CompareSheetsTask.020"),
    APP_0310("CompareSheetsTask.030"),
    APP_0320("CompareTreesTask.010"),
    APP_0330("CompareTreesTask.040"),
    APP_0340("CompareTreesTask.050"),
    APP_0350("CompareTreesTask.070"),
    APP_0360("CompareTreesTask.080"),
    APP_0370("CompareTreesTask.090"),
    APP_0380("CompareTreesTask.100"),
    APP_0390("CompareTreesTask.110"),
    APP_0400("excel.BResult.010"),
    APP_0410("excel.BResult.020"),
    APP_0420("excel.BResult.030"),
    APP_0430("excel.BResult.040"),
    APP_0440("excel.BResult.050"),
    APP_0450("excel.BResult.060"),
    APP_0460("excel.BResult.070"),
    APP_0470("excel.DResult.010"),
    APP_0480("excel.DResult.020"),
    APP_0490("excel.DResult.030"),
    APP_0500("excel.DResult.040"),
    APP_0510("excel.DResult.050"),
    APP_0520("excel.DResult.060"),
    APP_0530("excel.DResult.070"),
    APP_0540("excel.DResult.080"),
    APP_0550("excel.DResult.090"),
    APP_0560("excel.DResult.100"),
    APP_0570("excel.poi.usermodel.BookResultBookCreator.010"),
    APP_0580("excel.poi.usermodel.BookResultBookCreator.020"),
    APP_0590("excel.poi.usermodel.BookResultBookCreator.030"),
    APP_0600("excel.poi.usermodel.BookResultBookCreator.040"),
    APP_0610("excel.poi.usermodel.BookResultBookCreator.050"),
    APP_0620("excel.poi.usermodel.BookResultBookCreator.060"),
    APP_0630("excel.poi.usermodel.BookResultBookCreator.070"),
    APP_0640("excel.poi.usermodel.BookResultBookCreator.080"),
    APP_0650("excel.poi.usermodel.BookResultBookCreator.090"),
    APP_0660("excel.poi.usermodel.TreeResultBookCreator.010"),
    APP_0670("excel.poi.usermodel.TreeResultBookCreator.020"),
    APP_0680("excel.SheetType.010"),
    APP_0690("excel.SheetType.020"),
    APP_0700("excel.SheetType.030"),
    APP_0710("excel.SheetType.040"),
    APP_0720("excel.SResult.010"),
    APP_0730("excel.SResult.020"),
    APP_0740("excel.SResult.030"),
    APP_0750("excel.SResult.040"),
    APP_0760("excel.SResult.050"),
    APP_0770("excel.SResult.060"),
    APP_0780("excel.SResult.070"),
    APP_0790("excel.SResult.080"),
    APP_0800("excel.SResult.090"),
    APP_0810("excel.TreeResult.010"),
    APP_0820("excel.TreeResult.020"),
    APP_0830("excel.TreeResult.030"),
    APP_0840("excel.TreeResult.040"),
    APP_0850("excel.TreeResult.050"),
    APP_0860("fx.EditComparisonPane.010"),
    APP_0870("fx.GoogleFilePickerDialog.010"),
    APP_0880("fx.GoogleFilePickerDialog.020"),
    APP_0890("fx.GoogleFilePickerDialogPane.070"),
    APP_0900("fx.GoogleFilePickerDialogPane.080"),
    APP_0910("fx.GoogleFilePickerDialogPane.090"),
    APP_0920("fx.GoogleRevisionSelectorDialog.010"),
    APP_0930("google.GoogleFileFetcher.010"),
    APP_0940("google.GoogleFileFetcher.020"),
    APP_0950("gui.component.GooglePane.010"),
    APP_0960("gui.component.GooglePane.020"),
    APP_0970("gui.component.GooglePane.030"),
    APP_0980("gui.component.GooglePane.040"),
    APP_0990("gui.component.GooglePane.050"),
    APP_1000("gui.component.GooglePane.060"),
    APP_1010("gui.component.GooglePane.070"),
    APP_1020("gui.component.GooglePane.080"),
    APP_1030("gui.component.GooglePane.090"),
    APP_1040("gui.component.LinkPane.010"),
    APP_1050("gui.component.SettingsPane2.010"),
    APP_1060("gui.component.SettingsPane2.020"),
    APP_1070("gui.component.SettingsPane2.030"),
    APP_1080("gui.component.SettingsPane2.040"),
    APP_1091("gui.component.SettingsPane2.051"),
    APP_1092("gui.component.SettingsPane2.052"),
    APP_1093("gui.component.SettingsPane2.053"),
    APP_1100("gui.component.SettingsPane2.060"),
    APP_1110("gui.component.TargetSelectionPane.010"),
    APP_1120("gui.component.TargetSelectionPane.020"),
    APP_1130("gui.component.TargetSelectionPane.030"),
    APP_1140("gui.component.TargetSelectionPane.040"),
    APP_1150("gui.component.TargetSelectionPane.050"),
    APP_1160("gui.component.TargetSelectionPane.060"),
    APP_1170("gui.dialogs.SettingDetailsDialogPane.010"),
    APP_1180("gui.MainController.010"),
    APP_1190("gui.MainController.020"),
    APP_1200("gui.MainController.030"),
    APP_1210("gui.MainController.040"),
    APP_1220("gui.MainController.050"),
    APP_1230("gui.MainController.060"),
    APP_1240("gui.MainController.070"),
    APP_1250("gui.PasswordDialog.010"),
    APP_1260("gui.PasswordDialogPane.010"),
    APP_1270("gui.UpdateChecker.010"),
    APP_1280("gui.UpdateChecker.020"),
    APP_1290("gui.UpdateChecker.030");
    
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
