package xyz.hotchpotch.hogandiff.excel.sax;

import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
// FIXME: [No.08 読取PW対応] このローダーが読み取りパスワード付きExcelファイルに対応できるようにする
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
        private final List<String> sst;
        
        private final Deque<String> qNames = new ArrayDeque<>();
        private final Map<String, StringBuilder> texts = new HashMap<>();
        private final Set<CellData> cells = new HashSet<>();
        
        private XSSFCellType type;
        private String address;
        
        private Handler1(
                boolean extractCachedValue,
                List<String> sst) {
            
            assert sst != null;
            
            this.extractCachedValue = extractCachedValue;
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
        
        private String address;
        private StringBuilder comment;
        
        private Handler2(Set<CellData> cells) {
            assert cells != null;
            
            this.cells = cells;
            this.cellsMap = cells.parallelStream()
                    .collect(Collectors.toMap(CellData::address, Function.identity()));
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
    
    // [instance members] ******************************************************
    
    private final boolean extractCachedValue;
    private final Path bookPath;
    //private final String readPassword;
    
    private Map<String, SheetInfo> nameToInfo;
    private List<String> sst;
    
    /**
     * コンストラクタ
     * 
     * @param extractCachedValue
     *              数式セルからキャッシュされた計算値を抽出する場合は {@code true}、
     *              数式文字列を抽出する場合は {@code false}
     * @param bookPath Excepブックのパス
     * @throws NullPointerException パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code bookPath} がサポート対象外の形式の場合
     */
    public XSSFCellsLoaderWithSax(
            boolean extractCachedValue,
            Path bookPath) {
        
        Objects.requireNonNull(bookPath);
        CommonUtil.ifNotSupportedBookTypeThenThrow(
                XSSFCellsLoaderWithSax.class,
                BookType.of(bookPath));
        
        this.extractCachedValue = extractCachedValue;
        this.bookPath = bookPath;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *              {@code bookPath}, {@code sheetName} のいずれかが {@code null} の場合
     * @throws IllegalArgumentException
     *              {@code bookPath} が構成時に指定されたExcelブックと異なる場合
     * @throws IllegalArgumentException
     *              {@code bookPath} がサポート対象外の形式の場合
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
            Path bookPath,
            String readPassword,
            String sheetName)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookPath);
        // readPassword may be null.
        Objects.requireNonNull(sheetName);
        if (!Objects.equals(this.bookPath, bookPath)) {
            throw new IllegalArgumentException(
                    "This loader is configured for %s. Not available for another book (%s)."
                            .formatted(this.bookPath, bookPath));
        }
        
        if (nameToInfo == null) {
            nameToInfo = SaxUtil.loadSheetInfo(bookPath, readPassword).stream()
                    .collect(Collectors.toMap(
                            SheetInfo::name,
                            Function.identity()));
        }
        if (sst == null) {
            sst = SaxUtil.loadSharedStrings(bookPath, readPassword);
        }
        
        try (FileSystem fs = FileSystems.newFileSystem(bookPath)) {
            
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
            
            Handler1 handler1 = new Handler1(extractCachedValue, sst);
            try (InputStream is = Files.newInputStream(fs.getPath(info.source()))) {
                parser.parse(is, handler1);
            }
            cells = handler1.cells;
            
            if (info.commentSource() != null) {
                Handler2 handler2 = new Handler2(cells);
                try (InputStream is = Files.newInputStream(fs.getPath(info.commentSource()))) {
                    parser.parse(is, handler2);
                }
            }
            
            return Set.copyOf(cells);
            
        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "processing failed : %s - %s".formatted(bookPath, sheetName), e);
        }
    }
}
