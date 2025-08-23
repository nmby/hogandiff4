/**
 * このアプリケーションのメインモジュール
 * 
 * @author nmby
 */
module xyz.hotchpotch.hogandiff {
    requires java.desktop;
    requires java.xml;
    
    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    
    requires jdk.charsets;
    requires jdk.httpserver;
    requires jdk.zipfs;
    
    requires org.apache.commons.codec;
    requires org.apache.poi.ooxml;
    requires org.apache.poi.poi;
    
    requires org.json;
    
    requires com.google.api.client.auth;
    requires com.google.api.client;
    requires com.google.api.services.drive;
}
