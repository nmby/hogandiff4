package xyz.hotchpotch.hogandiff.excel.sax;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import xyz.hotchpotch.hogandiff.excel.BookInfo;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetLoader;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.common.SheetHandler;
import xyz.hotchpotch.hogandiff.excel.sax.SaxUtil.SheetInfo;

/**
 * SAX (Simple API for XML) を利用して、
 * .xlsx/.xlsm 形式のExcelブックのワークシートから
 * セルデータを抽出する {@link SheetLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class XSSFSheetLoaderWithSax implements SheetLoader {
    
    // [static members] ********************************************************
    
    /**
     * セルのタイプ、具体的には c 要素の t 属性の種類を表す列挙型です。<br>
     *
     * @author nmby
     * @see <a href="http://officeopenxml.com/SScontentOverview.php">
     *               http://officeopenxml.com/SScontentOverview.php</a>
     */
    private static enum XSSFCellType {
        
        // [static members] ----------------------------------------------------
        
        /** boolean */
        b,
        
        /** date */
        d,
        
        /** error */
        e,
        
        /** inline string */
        inlineStr,
        
        /** number */
        n,
        
        /** shared string */
        s,
        
        /** formula */
        str;
        
        private static XSSFCellType of(String t) {
            return t == null ? n : valueOf(t);
        }
        
        // [instance members] --------------------------------------------------
    }
    
    private static class Handler1 extends DefaultHandler {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final boolean extractCachedValue;
        private final boolean saveMemory;
        private final List<String> sst;
        
        private final Deque<String> qNames = new ArrayDeque<>();
        private final Map<String, StringBuilder> texts = new HashMap<>();
        private final Set<CellData> cells = new HashSet<>();
        
        private XSSFCellType type;
        private String address;
        
        private Handler1(
                boolean extractCachedValue,
                boolean saveMemory,
                List<String> sst) {
            
            assert sst != null;
            
            this.extractCachedValue = extractCachedValue;
            this.saveMemory = saveMemory;
            this.sst = sst;
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            
            qNames.addFirst(qName);
            
            if ("c".equals(qName)) {
                type = XSSFCellType.of(attributes.getValue("t"));
                address = attributes.getValue("r");
                texts.clear();
            }
        }
        
        @Override
        public void characters(char ch[], int start, int length) {
            String qName = qNames.getFirst();
            texts.putIfAbsent(qName, new StringBuilder());
            texts.get(qName).append(ch, start, length);
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("c".equals(qName)) {
                StringBuilder vText = texts.get("v");
                StringBuilder fText = texts.get("f");
                StringBuilder tText = texts.get("t");
                String value = null;
                
                if (!extractCachedValue && fText != null) {
                    value = fText.toString();
                } else {
                    switch (type) {
                    case b:
                        if (vText != null) {
                            value = Boolean.toString("1".equals(vText.toString()));
                        }
                        break;
                    
                    case n:
                    case d:
                    case e:
                    case str:
                        if (vText != null) {
                            value = vText.toString();
                        }
                        break;
                    
                    case inlineStr:
                        if (tText != null) {
                            value = tText.toString();
                        }
                        break;
                    
                    case s:
                        if (vText != null) {
                            int idx = Integer.parseInt(vText.toString());
                            value = sst.get(idx);
                        }
                        break;
                    
                    default:
                        throw new AssertionError(type);
                    }
                }
                if (value != null && !"".equals(value)) {
                    cells.add(CellData.of(address, value, saveMemory));
                }
                
                qNames.removeFirst();
                type = null;
                address = null;
                texts.clear();
            }
        }
    }
    
    private static class Handler2 extends DefaultHandler {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final Set<CellData> cells;
        private final Map<String, CellData> cellsMap;
        private final boolean saveMemory;
        
        private String address;
        private StringBuilder comment;
        
        private Handler2(Set<CellData> cells, boolean saveMemory) {
            assert cells != null;
            
            this.cells = cells;
            this.cellsMap = cells.parallelStream()
                    .collect(Collectors.toMap(CellData::address, Function.identity()));
            this.saveMemory = saveMemory;
        }
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            
            if ("comment".equals(qName)) {
                address = attributes.getValue("ref");
                comment = new StringBuilder();
            }
        }
        
        @Override
        public void characters(char ch[], int start, int length) {
            if (comment != null) {
                comment.append(ch, start, length);
            }
        }
        
        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("comment".equals(qName)) {
                if (cellsMap.containsKey(address)) {
                    CellData original = cellsMap.get(address);
                    cells.remove(original);
                    cells.add(original.withComment(comment.toString()));
                } else {
                    cells.add(CellData.of(address, "", saveMemory).withComment(comment.toString()));
                }
                
                address = null;
                comment = null;
            }
        }
    }
    
    /**
     * 新しいローダーを構成します。<br>
     * 
     * @param extractCachedValue
     *              数式セルからキャッシュされた計算値を抽出する場合は {@code true}、
     *              数式文字列を抽出する場合は {@code false}
     * @param saveMemory 省メモリモードの場合は {@code true}
     * @param bookInfo Excelブックの情報
     * @return 新しいローダー
     * @throws NullPointerException
     *              {@code bookInfo} が {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookInfo} がサポート対象外の形式もしくは不明な形式の場合
     * @throws ExcelHandlingException
     *              ローダーの構成に失敗した場合。
     *              具体的には、Excelブックから共通情報の取得に失敗した場合
     */
    public static SheetLoader of(
            boolean extractCachedValue,
            boolean saveMemory,
            BookInfo bookInfo)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookInfo, "bookInfo");
        CommonUtil.ifNotSupportedBookTypeThenThrow(
                XSSFSheetLoaderWithSax.class,
                bookInfo.bookType());
        
        return new XSSFSheetLoaderWithSax(
                extractCachedValue,
                saveMemory,
                bookInfo);
    }
    
    // [instance members] ******************************************************
    
    private final boolean extractCachedValue;
    private final boolean saveMemory;
    private final BookInfo bookInfo;
    private final Map<String, SheetInfo> nameToInfo;
    private final List<String> sst;
    
    private XSSFSheetLoaderWithSax(
            boolean extractCachedValue,
            boolean saveMemory,
            BookInfo bookInfo)
            throws ExcelHandlingException {
        
        assert bookInfo != null;
        assert CommonUtil.isSupportedBookType(getClass(), bookInfo.bookType());
        
        this.extractCachedValue = extractCachedValue;
        this.saveMemory = saveMemory;
        this.bookInfo = bookInfo;
        this.nameToInfo = SaxUtil.loadSheetInfo(bookInfo).stream()
                .collect(Collectors.toMap(
                        SheetInfo::name,
                        Function.identity()));
        this.sst = SaxUtil.loadSharedStrings(bookInfo);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code bookInfo}, {@code sheetName} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookInfo} が構成時に指定されたExcelブックと異なる場合
     * @throws IllegalArgumentException
     *              {@code bookInfo} がサポート対象外の形式もしくは不明な形式の場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックやシートが見つからないとか、シート種類がサポート対象外とか。
    @Override
    public Set<CellData> loadCells(BookInfo bookInfo, String sheetName)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookInfo, "bookInfo");
        Objects.requireNonNull(sheetName, "sheetName");
        if (!this.bookInfo.bookPath().equals(bookInfo.bookPath())) {
            throw new IllegalArgumentException(
                    "このローダーは %s 用に構成されています。別ブック（%s）には利用できません。"
                            .formatted(this.bookInfo.bookPath(), bookInfo.bookPath()));
        }
        
        try (FileSystem fs = FileSystems.newFileSystem(bookInfo.bookPath())) {
            
            if (!nameToInfo.containsKey(sheetName)) {
                // 例外カスケードポリシーに従い、
                // 後続の catch でさらに ExcelHandlingException にラップする。
                // ちょっと気持ち悪い気もするけど。
                throw new NoSuchElementException("シートが存在しません：" + sheetName);
            }
            SheetInfo info = nameToInfo.get(sheetName);
            // 同じく、後続の catch でさらに ExcelHandlingException にラップする。
            CommonUtil.ifNotSupportedSheetTypeThenThrow(getClass(), EnumSet.of(info.type()));
            
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            Set<CellData> cells = null;
            
            Handler1 handler1 = new Handler1(extractCachedValue, saveMemory, sst);
            try (InputStream is = Files.newInputStream(fs.getPath(info.source()))) {
                parser.parse(is, handler1);
            }
            cells = handler1.cells;
            
            if (info.commentSource() != null) {
                Handler2 handler2 = new Handler2(cells, saveMemory);
                try (InputStream is = Files.newInputStream(fs.getPath(info.commentSource()))) {
                    parser.parse(is, handler2);
                }
            }
            
            return Set.copyOf(cells);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "処理に失敗しました：%s - %s".formatted(bookInfo.bookPath(), sheetName),
                    e);
        }
    }
}
