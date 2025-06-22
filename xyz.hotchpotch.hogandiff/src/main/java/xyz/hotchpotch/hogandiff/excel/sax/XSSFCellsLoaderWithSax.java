package xyz.hotchpotch.hogandiff.excel.sax;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.CellData;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.excel.common.SheetHandler;
import xyz.hotchpotch.hogandiff.excel.sax.SaxUtil.IgnoreCloseInputStream;
import xyz.hotchpotch.hogandiff.excel.sax.SaxUtil.SheetInfo;
import xyz.hotchpotch.hogandiff.task.LoaderForCells;
import xyz.hotchpotch.hogandiff.util.function.UnsafeFunction;

/**
 * SAX (Simple API for XML) を利用して、
 * .xlsx/.xlsm 形式のExcelブックのワークシートから
 * セルデータを抽出する {@link LoaderForCells} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLSX, BookType.XLSM })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class XSSFCellsLoaderWithSax implements LoaderForCells {

    // [static members] ********************************************************

    /**
     * セルのタイプ、具体的には c 要素の t 属性の種類を表す列挙型です。<br>
     *
     * @author nmby
     * @see <a href="http://officeopenxml.com/SScontentOverview.php">
     *      http://officeopenxml.com/SScontentOverview.php</a>
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
        private final Map<String, String> addressToContent = new HashMap<>();

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
                    addressToContent.put(address, value);
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

        private final Map<String, String> addressToComment = new HashMap<>();

        private String address;
        private StringBuilder comment;

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
                addressToComment.put(address, comment.toString());
                address = null;
                comment = null;
            }
        }
    }

    // [instance members] ******************************************************

    private final boolean extractCachedValue;
    private final Path bookPath;
    // private final String readPassword;

    private Map<String, SheetInfo> nameToInfo;
    private List<String> sst;

    /**
     * コンストラクタ
     * 
     * @param extractCachedValue
     *                           数式セルからキャッシュされた計算値を抽出する場合は {@code true}、
     *                           数式文字列を抽出する場合は {@code false}
     * @param bookPath           Excepブックのパス
     * @throws NullPointerException     パラメータが {@code null} の場合
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
     *                                  {@code bookPath}, {@code sheetName} のいずれかが
     *                                  {@code null} の場合
     * @throws IllegalArgumentException
     *                                  {@code bookPath} が構成時に指定されたExcelブックと異なる場合
     * @throws IllegalArgumentException
     *                                  {@code bookPath} がサポート対象外の形式の場合
     * @throws ExcelHandlingException
     *                                  処理に失敗した場合
     */
    // 例外カスケードのポリシーについて：
    // ・プログラミングミスに起因するこのメソッドの呼出不正は RuntimeException の派生でレポートする。
    // 例えば null パラメータとか、サポート対象外のブック形式とか。
    // ・それ以外のあらゆる例外は ExcelHandlingException でレポートする。
    // 例えば、ブックやシートが見つからないとか、シート種類がサポート対象外とか。
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
            nameToInfo = SaxUtil.loadSheetInfos(bookPath, readPassword).stream()
                    .collect(Collectors.toMap(
                            SheetInfo::sheetName,
                            Function.identity()));
        }

        if (!nameToInfo.containsKey(sheetName)) {
            throw new ExcelHandlingException(
                    "Processing failed. No such sheet : %s - %s".formatted(bookPath, sheetName));
        }
        SheetInfo info = nameToInfo.get(sheetName);
        if (!CommonUtil.isSupportedSheetType(getClass(), EnumSet.of(info.type()))) {
            throw new ExcelHandlingException(
                    "Processing failed. Unsupported sheet type : %s - %s".formatted(bookPath, sheetName));
        }

        if (sst == null) {
            sst = SaxUtil.loadSharedStrings(bookPath, readPassword);
        }

        UnsafeFunction<ZipInputStream, Set<CellData>, Exception> processor = zis -> {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            Handler1 handler1 = new Handler1(extractCachedValue, sst);
            Handler2 handler2 = new Handler2();
            InputStream ignoreCloseZis = new IgnoreCloseInputStream(zis);
            ZipEntry zipEntry;

            while ((zipEntry = zis.getNextEntry()) != null) {
                if (zipEntry.getName().equals(info.source())) {
                    parser.parse(ignoreCloseZis, handler1);
                }
                if (info.commentSource() != null && zipEntry.getName().equals(info.commentSource())) {
                    parser.parse(ignoreCloseZis, handler2);
                }
            }

            if (info.commentSource() == null || handler2.addressToComment.isEmpty()) {
                return handler1.addressToContent.entrySet().stream()
                        .map(entry -> CellData.of(entry.getKey(), entry.getValue(), null))
                        .collect(Collectors.toSet());
            } else {
                Set<CellData> cells = handler1.addressToContent.entrySet().stream()
                        .map(entry -> {
                            String address = entry.getKey();
                            String content = entry.getValue();
                            String comment = handler2.addressToComment.containsKey(address)
                                    ? handler2.addressToComment.get(address)
                                    : null;
                            return CellData.of(address, content, comment);
                        })
                        .collect(Collectors.toCollection(HashSet::new));

                cells.addAll(
                        handler2.addressToComment.entrySet().stream()
                                .filter(entry -> !handler1.addressToContent.containsKey(entry.getKey()))
                                .map(entry -> CellData.of(entry.getKey(), "", entry.getValue()))
                                .toList());

                return cells;
            }
        };

        return SaxUtil.processExcelAsZip(bookPath, readPassword, processor);
    }
}
