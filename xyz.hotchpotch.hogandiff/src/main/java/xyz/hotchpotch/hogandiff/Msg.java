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
    
    MSG_001("AppMain.010"),
    MSG_002("AppResource.010"),
    MSG_003("AppTaskBase.030"),
    MSG_005("AppTaskBase.050"),
    MSG_008("AppTaskBase.060"),
    MSG_009("AppTaskBase.080"),
    MSG_006("AppTaskBase.120"),
    MSG_015("AppTaskBase.150"),
    MSG_014("AppTaskBase.160"),
    MSG_007("AppTaskBase.180"),
    MSG_004("AppTaskBase.200"),
    MSG_016("CompareBooksTask.010"),
    MSG_017("CompareBooksTask.040"),
    MSG_012("CompareBooksTask.060"),
    MSG_013("CompareBooksTask.080"),
    MSG_019("CompareDirsTask.010"),
    MSG_021("CompareDirsTask.050"),
    MSG_020("CompareDirsTask.070"),
    MSG_022("CompareSheetsTask.010"),
    MSG_023("CompareSheetsTask.020"),
    MSG_024("CompareTreesTask.010"),
    MSG_025("CompareTreesTask.040"),
    MSG_026("CompareTreesTask.050"),
    MSG_010("CompareTreesTask.070"),
    MSG_011("CompareTreesTask.090"),
    MSG_027("excel.poi.usermodel.BookResultBookCreator.010"),
    MSG_028("excel.poi.usermodel.BookResultBookCreator.020"),
    MSG_029("excel.poi.usermodel.BookResultBookCreator.030"),
    MSG_030("excel.poi.usermodel.BookResultBookCreator.040"),
    MSG_032("excel.poi.usermodel.BookResultBookCreator.050"),
    MSG_034("excel.poi.usermodel.BookResultBookCreator.060"),
    MSG_035("excel.poi.usermodel.BookResultBookCreator.070"),
    MSG_036("excel.poi.usermodel.BookResultBookCreator.080"),
    MSG_031("excel.poi.usermodel.BookResultBookCreator.090"),
    MSG_072("excel.poi.usermodel.TreeResultBookCreator.010"),
    MSG_071("excel.poi.usermodel.TreeResultBookCreator.020"),
    MSG_037("excel.BResult.010"),
    MSG_038("excel.BResult.020"),
    MSG_039("excel.BResult.030"),
    MSG_040("excel.BResult.040"),
    MSG_041("excel.BResult.050"),
    MSG_042("excel.BResult.060"),
    MSG_043("excel.BResult.070"),
    MSG_044("excel.DResult.010"),
    MSG_052("excel.DResult.020"),
    MSG_053("excel.DResult.030"),
    MSG_054("excel.DResult.040"),
    MSG_050("excel.DResult.050"),
    MSG_046("excel.DResult.060"),
    MSG_047("excel.DResult.070"),
    MSG_048("excel.DResult.080"),
    MSG_049("excel.DResult.090"),
    MSG_045("excel.DResult.100"),
    MSG_055("excel.SResult.010"),
    MSG_056("excel.SResult.020"),
    MSG_057("excel.SResult.030"),
    MSG_058("excel.SResult.040"),
    MSG_060("excel.SResult.050"),
    MSG_061("excel.SResult.060"),
    MSG_062("excel.SResult.070"),
    MSG_063("excel.SResult.080"),
    MSG_064("excel.SResult.090"),
    MSG_065("excel.TreeResult.010"),
    MSG_067("excel.TreeResult.020"),
    MSG_068("excel.TreeResult.030"),
    MSG_069("excel.TreeResult.040"),
    MSG_066("excel.TreeResult.050"),
    MSG_109("fx.EditComparisonPane.010"),
    MSG_116("fx.GoogleFilePickerDialog.010"),
    MSG_113("fx.GoogleFilePickerDialog.020"),
    MSG_110("fx.GoogleFilePickerDialogPane.070"),
    MSG_111("fx.GoogleFilePickerDialogPane.080"),
    MSG_112("fx.GoogleFilePickerDialogPane.090"),
    MSG_117("fx.GoogleRevisionSelectorDialog.010"),
    MSG_073("google.GoogleFileFetcher.010"),
    MSG_074("google.GoogleFileFetcher.020"),
    MSG_088("gui.component.GooglePane.010"),
    MSG_091("gui.component.GooglePane.020"),
    MSG_089("gui.component.GooglePane.030"),
    MSG_094("gui.component.GooglePane.040"),
    MSG_092("gui.component.GooglePane.050"),
    MSG_096("gui.component.GooglePane.060"),
    MSG_097("gui.component.GooglePane.070"),
    MSG_095("gui.component.GooglePane.080"),
    MSG_115("gui.component.GooglePane.090"),
    MSG_082("gui.component.LinkPane.010"),
    MSG_099("gui.component.SettingsPane2.010"),
    MSG_100("gui.component.SettingsPane2.020"),
    MSG_101("gui.component.SettingsPane2.030"),
    MSG_102("gui.component.SettingsPane2.040"),
    MSG_120("gui.component.SettingsPane2.051"),
    MSG_121("gui.component.SettingsPane2.052"),
    MSG_122("gui.component.SettingsPane2.053"),
    MSG_098("gui.component.SettingsPane2.060"),
    MSG_103("gui.component.TargetSelectionPane.010"),
    MSG_104("gui.component.TargetSelectionPane.020"),
    MSG_105("gui.component.TargetSelectionPane.030"),
    MSG_107("gui.component.TargetSelectionPane.040"),
    MSG_108("gui.component.TargetSelectionPane.050"),
    MSG_106("gui.component.TargetSelectionPane.060"),
    MSG_123("gui.dialogs.SettingDetailsDialogPane.010"),
    MSG_076("gui.MainController.010"),
    MSG_075("gui.MainController.020"),
    MSG_078("gui.MainController.030"),
    MSG_079("gui.MainController.040"),
    MSG_080("gui.MainController.050"),
    MSG_081("gui.MainController.060"),
    MSG_077("gui.MainController.070"),
    MSG_118("gui.PasswordDialog.010"),
    MSG_119("gui.PasswordDialogPane.010"),
    MSG_085("gui.UpdateChecker.010"),
    MSG_083("gui.UpdateChecker.020"),
    MSG_086("gui.UpdateChecker.030");
    
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
