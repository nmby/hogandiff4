package xyz.hotchpotch.hogandiff.excel.sax;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import xyz.hotchpotch.hogandiff.excel.BookType;
import xyz.hotchpotch.hogandiff.excel.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.excel.PasswordHandlingException;
import xyz.hotchpotch.hogandiff.excel.SheetType;
import xyz.hotchpotch.hogandiff.excel.common.BookHandler;
import xyz.hotchpotch.hogandiff.excel.common.CommonUtil;
import xyz.hotchpotch.hogandiff.util.function.UnsafeFunction;

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
     * @param sheetName シート名（例：{@code "シート1"}）
     * @param id シートId(relId)（例：{@code "rId1"}）
     * @param type シート形式（例：{@link SheetType#WORKSHEET}）
     * @param source ソースエントリのパス文字列（例：{@code "xl/worksheets/sheet1.xml"}）
     * @param commentSource セルコメントのソースエントリのパス文字列（例：{@code "xl/comments1.xml"}）
     * @param vmlDrawingSource セルコメントの図としての情報を保持するソースエントリのパス（例：{@code "xl/drawings/vmlDrawing1.vml"}）
     */
    public static record SheetInfo(
            String sheetName,
            String id,
            SheetType type,
            String source,
            String commentSource,
            String vmlDrawingSource) {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
    }
    
    /**
     * zipファイルとしての.xlsx/.xlsmファイルから次のエントリを読み込み、
     * シート名に対するシートId（relId）を抽出します。<br>
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
        
        private static record SheetNameAndId(String sheetName, String id) {
        };
        
        private static boolean isTarget(String entryName) {
            return "xl/workbook.xml".equals(entryName);
        }
        
        // [instance members] --------------------------------------------------
        
        private final List<SheetNameAndId> sheetNameAndId = new ArrayList<>();
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("sheet".equals(qName)) {
                sheetNameAndId.add(new SheetNameAndId(
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
        
        private static boolean isTarget(String entryName) {
            return "xl/_rels/workbook.xml.rels".equals(entryName);
        }
        
        // [instance members] --------------------------------------------------
        
        private final Map<String, SheetType> idToType = new HashMap<>();
        private final Map<String, String> idToSource = new HashMap<>();
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("Relationship".equals(qName)) {
                String id = attributes.getValue("Id");
                SheetType type = switch (attributes.getValue("Type")) {
                    case "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" -> SheetType.WORKSHEET;
                    case "http://schemas.openxmlformats.org/officeDocument/2006/relationships/chartsheet" -> SheetType.CHART_SHEET;
                    case "http://schemas.openxmlformats.org/officeDocument/2006/relationships/dialogsheet" -> SheetType.DIALOG_SHEET;
                    case "http://schemas.microsoft.com/office/2006/relationships/xlMacrosheet" -> SheetType.MACRO_SHEET;
                    default -> null;
                };
                String source = "xl/" + attributes.getValue("Target");
                
                idToType.put(id, type);
                idToSource.put(id, source);
            }
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
    private static class Handler3 extends DefaultHandler {
        
        // [static members] ----------------------------------------------------
        
        private static final String commentRelType = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/comments";
        private static final String vmlDrawingRelType = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/vmlDrawing";
        
        private static boolean isTarget(String entryName) {
            assert entryName != null;
            return entryName.startsWith("xl/worksheets/_rels/") && entryName.endsWith(".rels");
        }
        
        private static String entryFor(String sheetSource) {
            return sheetSource.replace("xl/worksheets/", "xl/worksheets/_rels/") + ".rels";
        }
        
        // [instance members] --------------------------------------------------
        
        private String commentSource = null;
        private String vmlDrawingSource = null;
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if ("Relationship".equals(qName)) {
                switch (attributes.getValue("Type")) {
                    case commentRelType:
                        commentSource = attributes.getValue("Target").replace("../", "xl/");
                        break;
                    case vmlDrawingRelType:
                        vmlDrawingSource = attributes.getValue("Target").replace("../", "xl/");
                        break;
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
    private static class Handler4 extends DefaultHandler {
        
        // [static members] ----------------------------------------------------
        
        private static boolean isTarget(String entryName) {
            return "xl/sharedStrings.xml".equals(entryName);
        }
        
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
     * {@link InputStream#close()} を無視する {@link InputStream} のラッパーです。<br>
     */
    public static class IgnoreCloseInputStream extends InputStream {
        
        // [static members] ----------------------------------------------------
        
        // [instance members] --------------------------------------------------
        
        private final InputStream delegate;
        
        /**
         * コンストラクタ<br>
         * 
         * @param delegate ソースインプットストリーム
         * @throws NullPointerException パラメータが {@code null} の場合
         */
        public IgnoreCloseInputStream(InputStream delegate) {
            Objects.requireNonNull(delegate);
            this.delegate = delegate;
        }
        
        @Override
        public void close() {
            // ignore
        }
        
        @Override
        public int read() throws IOException {
            return delegate.read();
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            return delegate.read(b);
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegate.read(b, off, len);
        }
        
        @Override
        public byte[] readAllBytes() throws IOException {
            return delegate.readAllBytes();
        }
        
        @Override
        public byte[] readNBytes(int len) throws IOException {
            return delegate.readNBytes(len);
        }
        
        @Override
        public int readNBytes(byte[] b, int off, int len) throws IOException {
            return delegate.readNBytes(b, off, len);
        }
        
        @Override
        public long skip(long n) throws IOException {
            return delegate.skip(n);
        }
        
        @Override
        public void skipNBytes(long n) throws IOException {
            delegate.skipNBytes(n);
        }
        
        @Override
        public int available() throws IOException {
            return delegate.available();
        }
        
        @Override
        public void mark(int readlimit) {
            delegate.mark(readlimit);
        }
        
        @Override
        public void reset() throws IOException {
            delegate.reset();
        }
        
        @Override
        public boolean markSupported() {
            return delegate.markSupported();
        }
        
        @Override
        public long transferTo(OutputStream out) throws IOException {
            return delegate.transferTo(out);
        }
        
    }
    
    /**
     * *.xlsx/*.xlsm 形式のExcelファイルをZipファイルとして処理するためのユーティリティメソッドです。<br>
     * {@code processor} が例外をスローした場合、このメソッドは
     * スローされた例外が {@link ExcelHandlingException} とそのサブタイプの場合はそのままスローし、
     * それ以外の例外の場合は {@link ExcelHandlingException} でラップしてスローします。<br>
     * 
     * @param <T> 戻り値の型
     * @param bookPath Excelブックのパス
     * @param readPassword 読取パスワード（読取パスワード無しの場合は {@code null}）
     * @param processor Zip処理プロセッサ
     * @return 処理結果
     * @throws NullPointerException {@code bookPath}, {@code processor} のいずれかが {@code null} の場合
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    public static <T> T processExcelAsZip(
            Path bookPath,
            String readPassword,
            UnsafeFunction<ZipInputStream, T, Exception> processor)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookPath);
        // readPassword may be null
        Objects.requireNonNull(processor);
        
        if (readPassword == null) {
            try (InputStream is = Files.newInputStream(bookPath);
                    ZipInputStream zis = new ZipInputStream(is)) {
                
                return processor.apply(zis);
                
            } catch (ExcelHandlingException e) {
                throw e;
            } catch (Exception e) {
                throw new ExcelHandlingException(
                        "failed to load the book : %s".formatted(bookPath), e);
            }
            
        } else {
            try (InputStream is = Files.newInputStream(bookPath);
                    POIFSFileSystem poifs = new POIFSFileSystem(is)) {
                
                EncryptionInfo encInfo = new EncryptionInfo(poifs);
                Decryptor decryptor = Decryptor.getInstance(encInfo);
                if (!decryptor.verifyPassword(readPassword)) {
                    throw new PasswordHandlingException();
                }
                
                try (InputStream decryptedIs = decryptor.getDataStream(poifs);
                        ZipInputStream zis = new ZipInputStream(decryptedIs)) {
                    
                    return processor.apply(zis);
                }
                
            } catch (ExcelHandlingException e) {
                throw e;
            } catch (Exception e) {
                throw new ExcelHandlingException(
                        "failed to load the book : %s".formatted(bookPath), e);
            }
        }
    }
    
    /**
     * .xlsx/.xlsm 形式のExcelブックからシート情報の一覧を読み取ります。<br>
     * 
     * @param bookPath Excelブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @return シート情報の一覧
     * @throws NullPointerException {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException {@code bookPath} がサポート対象外の形式の場合
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    public static List<SheetInfo> loadSheetInfos(
            Path bookPath,
            String readPassword)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookPath);
        // readPassword may be null.
        CommonUtil.ifNotSupportedBookTypeThenThrow(SaxUtil.class, BookType.of(bookPath));
        
        UnsafeFunction<ZipInputStream, List<SheetInfo>, Exception> processor = zis -> {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            Handler1 handler1 = new Handler1();
            Handler2 handler2 = new Handler2();
            Map<String, Handler3> handler3s = new HashMap<>();
            
            // SAXParser#parseが勝手にソースzisを閉じてしまうっぽいので、
            // IgnoreCloseInputStream なるラッパーを導入した。
            // 糞がッッッ
            // see: https://stackoverflow.com/questions/53690819/java-io-ioexception-stream-closed-zipinputstream
            InputStream ignoreCloseZis = new IgnoreCloseInputStream(zis);
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                if (Handler1.isTarget(entry.getName())) {
                    parser.parse(ignoreCloseZis, handler1);
                }
                if (Handler2.isTarget(entry.getName())) {
                    parser.parse(ignoreCloseZis, handler2);
                }
                if (Handler3.isTarget(entry.getName())) {
                    Handler3 handler3 = new Handler3();
                    parser.parse(ignoreCloseZis, handler3);
                    handler3s.put(entry.getName(), handler3);
                }
            }
            
            if (readPassword == null && handler1.sheetNameAndId.size() == 0) {
                // 大変雑ではあるが、例外がスローされずしかしシート情報を読み取れなかった場合は
                // 読取パスワードでロックされていると見做してしまう。
                throw new PasswordHandlingException();
            }
            
            return handler1.sheetNameAndId.stream()
                    .map(sheetNameAndId -> {
                        String sheetName = sheetNameAndId.sheetName;
                        String id = sheetNameAndId.id;
                        SheetType type = handler2.idToType.get(id);
                        String source = handler2.idToSource.get(id);
                        Handler3 handler3 = handler3s.get(Handler3.entryFor(source));
                        String commentSource = handler3 != null ? handler3.commentSource : null;
                        String vmlDrawingSource = handler3 != null ? handler3.vmlDrawingSource : null;
                        
                        return new SheetInfo(
                                sheetName,
                                id,
                                type,
                                source,
                                commentSource,
                                vmlDrawingSource);
                    })
                    .toList();
        };
        
        return processExcelAsZip(bookPath, readPassword, processor);
    }
    
    /**
     * .xlsx/.xlsm 形式のExcelブックから Shared Strings を読み取ります。<br>
     * 
     * @param bookPath Excelブックのパス
     * @param readPassword Excelブックの読み取りパスワード
     * @return Shared Strings
     * @throws NullPointerException {@code bookPath} が {@code null} の場合
     * @throws IllegalArgumentException {@code bookPath} がサポート対象外の形式の場合
     * @throws ExcelHandlingException 処理に失敗した場合
     */
    public static List<String> loadSharedStrings(
            Path bookPath,
            String readPassword)
            throws ExcelHandlingException {
        
        Objects.requireNonNull(bookPath);
        // readPassword may be null.
        CommonUtil.ifNotSupportedBookTypeThenThrow(SaxUtil.class, BookType.of(bookPath));
        
        UnsafeFunction<ZipInputStream, List<String>, Exception> processor = zis -> {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            Handler4 handler4 = new Handler4();
            ZipEntry entry;
            
            while ((entry = zis.getNextEntry()) != null) {
                if (Handler4.isTarget(entry.getName())) {
                    parser.parse(zis, handler4);
                    return handler4.sst;
                }
            }
            return List.of();
        };
        
        return processExcelAsZip(bookPath, readPassword, processor);
    }
    
    // [instance members] ******************************************************
    
    private SaxUtil() {
    }
}
