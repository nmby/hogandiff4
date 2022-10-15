package xyz.hotchpotch.hogandiff.excel.sax;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;

/**
 * SAX (Simple API for XML) と組み合わせて利用すると便利な機能を集めた
 * ユーティリティクラスです。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM })
public class SaxUtil {
    
    // [static members] ********************************************************
    
    /**
     * .xlsx/.xlsm 形式のExcelブックに含まれるシートエントリの情報を保持する
     * 不変クラスです。<br>
     *
     * @author nmby
     */
    // 技術メモ：
    // 厳密に言うと不変クラスではないのだけれど、
    // SaxUtil クラス内部でしか可変でなく外部からは不変だし、
    // 変更中のオブジェクトを決して外部に公開しないので、
    // 「不変クラスです。」と言っちゃって良いよね？！だめかな？！
    public static class SheetInfo {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final String name;
        private final String id;
        private SheetType type;
        private String source;
        private String commentSource;
        private String vmlDrawingSource;
        
        private SheetInfo(String name, String id) {
            this.name = name;
            this.id = id;
        }
        
        /**
         * シート名を返します。<br>
         * 例）{@code "シート1"}
         * 
         * @return シート名
         */
        public String name() {
            return name;
        }
        
        /**
         * シートId(relId)を返します。<br>
         * 例）{@code "rId1"}
         * 
         * @return シートId(relId)
         */
        public String id() {
            return id;
        }
        
        /**
         * シート形式を返します。<br>
         * 例）{@link SheetType#WORKSHEET}
         * 
         * @return シート形式
         */
        public SheetType type() {
            return type;
        }
        
        /**
         * zipファイルとしてのExcelファイル内における
         * ソースエントリのパス文字列を返します。<br>
         * 例）{@code "xl/worksheets/sheet1.xml"}
         * 
         * @return ソースエントリのパス文字列
         */
        public String source() {
            return source;
        }
        
        /**
         * zipファイルとしてのExcelファイル内における
         * セルコメントのソースエントリのパス文字列を返します。<br>
         * 例）{@code "xl/comments1.xml"}
         * 
         * @return セルコメントのソースエントリのパス文字列
         */
        public String commentSource() {
            return commentSource;
        }
        
        /**
         * zipファイルとしてのExcelファイル内における
         * セルコメントの図としての情報を保持するソースエントリのパス文字列を返します。<br>
         * 例）{@code "xl/drawings/vmlDrawing1.vml"}
         * 
         * @return セルコメントの図としての情報を保持するソースエントリのパス文字列
         */
        public String vmlDrawingSource() {
            return vmlDrawingSource;
        }
    }
    
    /**
     * zipファイルとしての.xlsx/.xlsmファイルから次のエントリを読み込み、
     * シート名とシートId（relId）を抽出します。<br>
     * <pre>
     * *.xlsx
     *   +-xl
     *     +-workbook.xml
     * </pre>
     * 
     * @author nmby
     */
    private static class Handler1 extends DefaultHandler {
        
        // [static members] ----------------------------------------------------
        
        private static final String targetEntry = "xl/workbook.xml";
        
        // [instance members] --------------------------------------------------
        
        private final List<SheetInfo> sheets = new ArrayList<>();
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("sheet".equals(qName)) {
                sheets.add(new SheetInfo(
                        attributes.getValue("name"),
                        attributes.getValue("r:id")));
            }
        }
    }
    
    /**
     * zipファイルとしての.xlsx/.xlsmファイルから次のエントリを読み込み、
     * シートId(relId)に対するシート形式とソースパスを抽出します。<br>
     * <pre>
     * *.xlsx
     *   +-xl
     *     +-_rels
     *       +-workbook.xml.rels
     * </pre>
     * 
     * @author nmby
     */
    private static class Handler2 extends DefaultHandler {
        
        // [static members] ----------------------------------------------------
        
        private static final String targetEntry = "xl/_rels/workbook.xml.rels";
        
        // [instance members] --------------------------------------------------
        
        private final Map<String, SheetInfo> sheets;
        
        private Handler2(List<SheetInfo> sheets) {
            assert sheets != null;
            
            this.sheets = sheets.stream()
                    .collect(Collectors.toMap(
                            info -> info.id,
                            Function.identity()));
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("Relationship".equals(qName)) {
                SheetInfo info = sheets.get(attributes.getValue("Id"));
                if (info != null) {
                    info.type = switch (attributes.getValue("Type")) {
                    case "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" -> SheetType.WORKSHEET;
                    case "http://schemas.openxmlformats.org/officeDocument/2006/relationships/chartsheet" -> SheetType.CHART_SHEET;
                    case "http://schemas.openxmlformats.org/officeDocument/2006/relationships/dialogsheet" -> SheetType.DIALOG_SHEET;
                    case "http://schemas.microsoft.com/office/2006/relationships/xlMacrosheet" -> SheetType.MACRO_SHEET;
                    default -> null;
                    };
                    info.source = "xl/" + attributes.getValue("Target");
                }
            }
        }
    }
    
    /**
     * zipファイルとしての.xlsx/.xlsmファイルから次のエントリを読み込み、
     * いわゆる SharedStringsTable のデータを抽出します。<br>
     * <pre>
     * *.xlsx
     *   +-xl
     *     +-sharedStrings.xml
     * </pre>
     * 
     * @author nmby
     */
    private static class Handler3 extends DefaultHandler {
        
        // [static members] ----------------------------------------------------
        
        private static final String targetEntry = "xl/sharedStrings.xml";
        
        // [instance members] --------------------------------------------------
        
        private final Deque<String> qNames = new ArrayDeque<>();
        private final List<String> sst = new ArrayList<>();
        private StringBuilder text;
        private boolean waitingText;
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("si".equals(qName)) {
                text = new StringBuilder();
            } else if ("t".equals(qName)) {
                String parent = qNames.getFirst();
                if ("si".equals(parent) || "r".equals(parent)) {
                    waitingText = true;
                }
            }
            qNames.addFirst(qName);
        }
        
        @Override
        public void characters(char ch[], int start, int length) {
            if (waitingText) {
                text.append(ch, start, length);
            }
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("si".equals(qName)) {
                sst.add(text.toString());
                text = null;
            } else if ("t".equals(qName)) {
                waitingText = false;
            }
            qNames.removeFirst();
        }
    }
    
    /**
     * zipファイルとしての.xlsx/.xlsmファイルから次のエントリを読み込み、
     * 指定されたシートに対するセルコメントのソースパス
     * およびセルコメントの図としての情報を保持するソースパスを抽出します。<br>
     * <pre>
     * *.xlsx
     *   +-xl
     *     +-worksheets
     *       +-_rels
     *         +-sheet?.xml.rels
     * </pre>
     * 
     * @author nmby
     */
    private static class Handler4 extends DefaultHandler {
        
        // [static members] ----------------------------------------------------
        
        private static final String commentRelType = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/comments";
        private static final String vmlDrawingRelType = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/vmlDrawing";
        
        // [instance members] --------------------------------------------------
        
        private final SheetInfo info;
        private final String relEntry;
        
        private Handler4(SheetInfo info) {
            assert info != null;
            
            this.info = info;
            this.relEntry = info.source.replace("xl/worksheets/", "xl/worksheets/_rels/") + ".rels";
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("Relationship".equals(qName)) {
                if (commentRelType.equals(attributes.getValue("Type"))) {
                    info.commentSource = attributes.getValue("Target").replace("../", "xl/");
                } else if (vmlDrawingRelType.equals(attributes.getValue("Type"))) {
                    info.vmlDrawingSource = attributes.getValue("Target").replace("../", "xl/");
                }
            }
        }
    }
    
    /**
     * .xlsx/.xlsm 形式のExcelブックからシート情報の一覧を読み取ります。<br>
     * 
     * @param bookInfo Excelブックの情報
     * @return シート情報の一覧
     * @throws NullPointerException {@code bookInfo} が {@code null} の場合
     * @throws IllegalArgumentException {@code bookInfo} がサポート対象外の形式の場合
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    public static List<SheetInfo> loadSheetInfo(BookInfo bookInfo) throws ExcelHandlingException {
        Objects.requireNonNull(bookInfo, "bookInfo");
        CommonUtil.ifNotSupportedBookTypeThenThrow(SaxUtil.class, bookInfo.bookType());
        
        try (FileSystem fs = FileSystems.newFileSystem(bookInfo.bookPath())) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            
            Handler1 handler1 = new Handler1();
            try (InputStream is = Files.newInputStream(fs.getPath(Handler1.targetEntry))) {
                parser.parse(is, handler1);
            }
            
            Handler2 handler2 = new Handler2(handler1.sheets);
            try (InputStream is = Files.newInputStream(fs.getPath(Handler2.targetEntry))) {
                parser.parse(is, handler2);
            }
            
            for (SheetInfo info : handler1.sheets) {
                Handler4 handler4 = new Handler4(info);
                if (Files.exists(fs.getPath(handler4.relEntry))) {
                    try (InputStream is = Files.newInputStream(fs.getPath(handler4.relEntry))) {
                        parser.parse(is, handler4);
                    }
                }
            }
            
            return List.copyOf(handler1.sheets);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "failed to load the book : %s".formatted(bookInfo), e);
        }
    }
    
    /**
     * .xlsx/.xlsm 形式のExcelブックから Shared Strings を読み取ります。<br>
     * 
     * @param bookInfo Excelブックの情報
     * @return Shared Strings
     * @throws NullPointerException {@code bookInfo} が {@code null} の場合
     * @throws IllegalArgumentException {@code bookInfo} がサポート対象外の形式の場合
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    public static List<String> loadSharedStrings(BookInfo bookInfo) throws ExcelHandlingException {
        Objects.requireNonNull(bookInfo, "bookInfo");
        CommonUtil.ifNotSupportedBookTypeThenThrow(SaxUtil.class, bookInfo.bookType());
        
        try (FileSystem fs = FileSystems.newFileSystem(bookInfo.bookPath())) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            
            if (Files.exists(fs.getPath(Handler3.targetEntry))) {
                Handler3 handler3 = new Handler3();
                try (InputStream is = Files.newInputStream(fs.getPath(Handler3.targetEntry))) {
                    parser.parse(is, handler3);
                }
                return List.copyOf(handler3.sst);
                
            } else {
                return List.of();
            }
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "failed to load the book : %s".formatted(bookInfo), e);
        }
    }
    
    // [instance members] ******************************************************
    
    private SaxUtil() {
    }
}
