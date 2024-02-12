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

import xyz.hotchpotch.hogandiff.excel.BookOpenInfo;
import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.CellsLoader;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.common.SheetHandler;
import xyz.hotchpotch.hogandiff.excel.sax.SaxUtil.SheetInfo;

/**
 * SAX (Simple API for XML) を利用して、
 * .xlsx/.xlsm 形式のExcelブックのワークシートから
 * セルデータを抽出する {@link CellsLoader} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class XSSFCellsLoaderWithSax implements CellsLoader {
    
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
                    cells.add(CellData.of(address, value, null));
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
                    cells.add(CellData.of(address, "", comment.toString()));
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
     * @param bookOpenInfo Excelブックの情報
     * @return 新しいローダー
     * @throws NullPointerException
     *              {@code bookOpenInfo} が {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookOpenInfo} がサポート対象外の形式の場合
     * @throws ExcelHandlingException
     *              ローダーの構成に失敗した場合。
     *              具体的には、Excelブックから共通情報の取得に失敗した場合
     */
    public static CellsLoader of(
            boolean extractCachedValue,
            boolean saveMemory,
            BookOpenInfo bookOpenInfo)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        CommonUtil.ifNotSupportedBookTypeThenThrow(
                XSSFCellsLoaderWithSax.class,
                bookOpenInfo.bookType());
        
        return new XSSFCellsLoaderWithSax(
                extractCachedValue,
                saveMemory,
                bookOpenInfo);
    }
    
    // [instance members] ******************************************************
    
    private final boolean extractCachedValue;
    private final boolean saveMemory;
    private final BookOpenInfo bookOpenInfo;
    private final Map<String, SheetInfo> nameToInfo;
    private final List<String> sst;
    
    private XSSFCellsLoaderWithSax(
            boolean extractCachedValue,
            boolean saveMemory,
            BookOpenInfo bookOpenInfo)
            throws ExcelHandlingException {
        
        assert bookOpenInfo != null;
        assert CommonUtil.isSupportedBookType(getClass(), bookOpenInfo.bookType());
        
        this.extractCachedValue = extractCachedValue;
        this.saveMemory = saveMemory;
        this.bookOpenInfo = bookOpenInfo;
        this.nameToInfo = SaxUtil.loadSheetInfo(bookOpenInfo).stream()
                .collect(Collectors.toMap(
                        SheetInfo::name,
                        Function.identity()));
        this.sst = SaxUtil.loadSharedStrings(bookOpenInfo);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code bookOpenInfo}, {@code sheetName} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookOpenInfo} が構成時に指定されたExcelブックと異なる場合
     * @throws IllegalArgumentException
     *              {@code bookOpenInfo} がサポート対象外の形式の場合
     * @throws ExcelHandlingException
     *              処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    //      例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    //      例えば、ブックやシートが見つからないとか、シート種類がサポート対象外とか。
    @Override
    public Set<CellData> loadCells(
            BookOpenInfo bookOpenInfo,
            String sheetName)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookOpenInfo, "bookOpenInfo");
        Objects.requireNonNull(sheetName, "sheetName");
        if (!Objects.equals(this.bookOpenInfo.bookPath(), bookOpenInfo.bookPath())) {
            throw new IllegalArgumentException(
                    "This loader is configured for %s. Not available for another book (%s)."
                            .formatted(this.bookOpenInfo, bookOpenInfo));
        }
        
        try (FileSystem fs = FileSystems.newFileSystem(bookOpenInfo.bookPath())) {
            
            if (!nameToInfo.containsKey(sheetName)) {
                // 例外カスケードポリシーに従い、
                // 後続の catch でさらに ExcelHandlingException にラップする。
                // ちょっと気持ち悪い気もするけど。
                throw new NoSuchElementException("no such sheet : " + sheetName);
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
                    "processing failed : %s - %s".formatted(bookOpenInfo, sheetName), e);
        }
    }
}
