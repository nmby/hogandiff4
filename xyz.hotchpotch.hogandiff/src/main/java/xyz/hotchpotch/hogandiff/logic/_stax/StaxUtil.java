package xyz.hotchpotch.hogandiff.logic._stax;

import java.util.Objects;

import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;

/**
 * StAX (Streaming API for XML) と組み合わせて利用すると便利な機能を集めた
 * ユーティリティクラスです。<br>
 *
 * @author nmby
 */
public class StaxUtil {

    // [static members] ********************************************************

    /**
     * xmlns = {@code "http://schemas.openxmlformats.org/spreadsheetml/2006/main"}
     * の各種QNAMEを提供します。<br>
     *
     * @author nmby
     */
    public static class QNAME {

        // [static members] ----------------------------------------------------

        /** xmlns */
        public static final String XMLNS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";

        /** bgColor */
        public static final QName BG_COLOR = new QName(XMLNS, "bgColor");

        /** border */
        public static final QName BORDER = new QName(XMLNS, "border");

        /** borders */
        public static final QName BORDERS = new QName(XMLNS, "borders");

        /** bottom */
        public static final QName BOTTOM = new QName(XMLNS, "bottom");

        /** c */
        public static final QName C = new QName(XMLNS, "c");

        /** col */
        public static final QName COL = new QName(XMLNS, "col");

        /** cols */
        public static final QName COLS = new QName(XMLNS, "cols");

        /** color */
        public static final QName COLOR = new QName(XMLNS, "color");

        /** comment */
        public static final QName COMMENT = new QName(XMLNS, "comment");

        /** conditionalFormatting */
        public static final QName CONDITIONAL_FORMATTING = new QName(XMLNS, "conditionalFormatting");

        /** diagonal */
        public static final QName DIAGONAL = new QName(XMLNS, "diagonal");

        /** fgColor */
        public static final QName FG_COLOR = new QName(XMLNS, "fgColor");

        /** fill */
        public static final QName FILL = new QName(XMLNS, "fill");

        /** fills */
        public static final QName FILLS = new QName(XMLNS, "fills");

        /** font */
        public static final QName FONT = new QName(XMLNS, "font");

        /** fonts */
        public static final QName FONTS = new QName(XMLNS, "fonts");

        /** gradientFill */
        public static final QName GRADIENT_FILL = new QName(XMLNS, "gradientFill");

        /** left */
        public static final QName LEFT = new QName(XMLNS, "left");

        /** patternFill */
        public static final QName PATTERN_FILL = new QName(XMLNS, "patternFill");

        /** r */
        public static final QName R = new QName(XMLNS, "r");

        /** right */
        public static final QName RIGHT = new QName(XMLNS, "right");

        /** row */
        public static final QName ROW = new QName(XMLNS, "row");

        /** rPr */
        public static final QName RPR = new QName(XMLNS, "rPr");

        /** sheetData */
        public static final QName SHEET_DATA = new QName(XMLNS, "sheetData");

        /** sheetPr */
        public static final QName SHEET_PR = new QName(XMLNS, "sheetPr");

        /** tabColor */
        public static final QName TAB_COLOR = new QName(XMLNS, "tabColor");

        /** text */
        public static final QName TEXT = new QName(XMLNS, "text");

        /** top */
        public static final QName TOP = new QName(XMLNS, "top");

        /** worksheet */
        public static final QName WORKSHEET = new QName(XMLNS, "worksheet");

        // [instance members] --------------------------------------------------

        private QNAME() {
        }
    }

    /**
     * xmlns = {@code "urn:schemas-microsoft-com:office:excel"}
     * の各種QNAMEを提供します。<br>
     *
     * @author nmby
     */
    public static class X_QNAME {

        // [static members] ----------------------------------------------------

        /** xmlns */
        public static final String XMLNS = "urn:schemas-microsoft-com:office:excel";

        /** ClientData */
        public static final QName CLIENT_DATA = new QName(XMLNS, "ClientData", "x");

        /** Column */
        public static final QName COLUMN = new QName(XMLNS, "Column", "x");

        /** Row */
        public static final QName ROW = new QName(XMLNS, "Row", "x");

        /** Visible */
        public static final QName VISIBLE = new QName(XMLNS, "Visible", "x");

        // [instance members] --------------------------------------------------

        private X_QNAME() {
        }
    }

    /**
     * xmlns = {@code "urn:schemas-microsoft-com:vml"}
     * の各種QNAMEを提供します。<br>
     *
     * @author nmby
     */
    public static class V_QNAME {

        // [static members] ----------------------------------------------------

        /** xmlns */
        public static final String XMLNS = "urn:schemas-microsoft-com:vml";

        /** fill */
        public static final QName FILL = new QName(XMLNS, "fill", "v");

        /** shape */
        public static final QName SHAPE = new QName(XMLNS, "shape", "v");

        // [instance members] --------------------------------------------------

        private V_QNAME() {
        }
    }

    /**
     * それ自体は xmlns の定義されない各種QNAMEを提供します。<br>
     * 
     * @author nmby
     */
    public static class NONS_QNAME {

        // [static members] ----------------------------------------------------

        /** customFormat */
        public static final QName CUSTOM_FORMAT = new QName(null, "customFormat");

        /** fillcolor */
        public static final QName FILL_COLOR = new QName(null, "fillcolor");

        /** max */
        public static final QName MAX = new QName(null, "max");

        /** min */
        public static final QName MIN = new QName(null, "min");

        /** ObjectType */
        public static final QName OBJECT_TYPE = new QName(null, "ObjectType");

        /** patternFill */
        public static final QName PATTERN_TYPE = new QName(null, "patternType");

        /** r */
        public static final QName R = new QName(null, "r");

        /** rgb */
        public static final QName RGB = new QName(null, "rgb");

        /** s */
        public static final QName S = new QName(null, "s");

        /** spans */
        public static final QName SPANS = new QName(null, "spans");

        /** strokecolor */
        public static final QName STROKE_COLOR = new QName(null, "strokecolor");

        /** style */
        public static final QName STYLE = new QName(null, "style");

        /** type */
        public static final QName TYPE = new QName(null, "type");

        /** width */
        public static final QName WIDTH = new QName(null, "width");

        // [instance members] --------------------------------------------------

        private NONS_QNAME() {
        }
    }

    /**
     * 指定されたイベントが指定されたQNAMEの開始要素であるかを返します。<br>
     * 
     * @param event XMLイベント
     * @param qName QNAME
     * @return 指定されたイベントが指定されたQNAMEの開始要素である場合は {@code true}
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static boolean isStart(XMLEvent event, QName qName) {
        Objects.requireNonNull(event);
        Objects.requireNonNull(qName);

        return event.isStartElement() && qName.equals(event.asStartElement().getName());
    }

    /**
     * 指定されたイベントが指定されたQNAMEの終了要素であるかを返します。<br>
     * 
     * @param event XMLイベント
     * @param qName QNAME
     * @return 指定されたイベントが指定されたQNAMEの終了要素である場合は {@code true}
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static boolean isEnd(XMLEvent event, QName qName) {
        Objects.requireNonNull(event);
        Objects.requireNonNull(qName);

        return event.isEndElement() && qName.equals(event.asEndElement().getName());
    }

    // [instance members] ******************************************************

    private StaxUtil() {
    }
}
