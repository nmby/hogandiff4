package xyz.hotchpotch.hogandiff.excel.poi.usermodel;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.apache.poi.hssf.usermodel.HSSFComment;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFSheetConditionalFormatting;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.eval.ErrorEval;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFChartSheet;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFDialogsheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import xyz.hotchpotch.hogandiff.excel.SheetType;

/**
 * Apache POI と組み合わせて利用すると便利な機能を集めたユーティリティクラスです。<br>
 *
 * @author nmby
 */
public class PoiUtil {
    
    // [static members] ********************************************************
    
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
    
    /**
     * セルの形式が何であれ、セルの格納値を表す文字列を返します。<br>
     * セルの形式が数式であり {@code useCachedValue} が {@code true} の場合は、
     * ブックにキャッシュされている計算結果の文字列表現を返します。
     * そうでない場合は、数式の文字列（例えば {@code "SUM(A4:B4)"}）を返します。<br>
     * セルが空の場合は空文字列（{@code ""}）を返します。
     * このメソッドが {@code null} を返すことはありません。<br>
     * <br>
     * このメソッドは、セルの表示形式を無視します。
     * 例えば、セルの保持する値が {@code 3.14159} でありセルの表示形式が {@code "0.00"} のとき、
     * このメソッドは {@code "3.14"} ではなく {@code "3.141592"} をクライアントに返します。<br>
     * <br>
     * 日付の扱いに関する補足：<br>
     * このメソッドは、日付または時刻のフォーマットが指定されているセルの値を
     * {@code "yyyy/MM/dd HH:mm:ss.SSS"} 形式の文字列に変換して返します。<br>
     * Excelにおける日付・時刻の扱いは独特です。<br>
     * 例えばセルに「{@code 10:27}」と入力したとき、
     * Excel内部ではセルの値は「{@code 0.435417}」として管理されます。
     * 一方で、Excelでは {@code 1900/1/1 00:00} が「{@code 1}」で表されます。<br>
     * 従って、「{@code 0.435417}」というセル値を {@code "yyyy/MM/dd HH:mm:ss.SSS"}
     * という形式で評価すると、{@code "1899/12/31 10:27:00.000"} という文字列になります。<br>
     * 以上の理由により、このメソッドは「{@code 10:27}」と入力されたセルの値を
     * {@code "1899/12/31 10:27:00.000"} という文字列で返します。<br>
     * 
     * @param cell 対象のセル
     * @param useCachedValue 対象のセルの形式が数式の場合に、
     *              数式ではなくキャッシュされた算出値を返す場合は {@code true}
     * @return セルの格納値を表す文字列
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static String getCellContentAsString(Cell cell, boolean useCachedValue) {
        Objects.requireNonNull(cell);
        
        CellType type = useCachedValue && cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();
        
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case FORMULA -> cell.getCellFormula();
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
        
            case NUMERIC -> {
                // 日付セルや独自書式セルの値の扱いは甚だ不完全なものの、
                // diffツールとしては内容の比較を行えればよいのだと割り切り、
                // これ以上に凝ったコーディングは行わないこととする。
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    LocalDateTime localDateTime = LocalDateTime
                            .ofInstant(date.toInstant(), ZoneId.systemDefault());
                    yield dateTimeFormatter.format(localDateTime);
                    
                } else {
                    String val = BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                    if (val.endsWith(".0")) {
                        val = val.substring(0, val.length() - 2);
                    }
                    yield val;
                }
            }
            
            case ERROR -> ErrorEval.getText(cell.getErrorCellValue());
            case BLANK -> "";
            default -> throw new AssertionError("unknown cell type: " + type);
        };
    }
    
    /**
     * Excelシートの種類を推定して、可能性のある種類を返します。<br>
     * 
     * @param sheet Excelシート
     * @return 可能性のある種類
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    // このロジックで合ってるのかはさっぱり分からん
    // FIXME: [No.1 シート識別不正 - usermodel] 識別精度を上げたい...
    public static Set<SheetType> possibleTypes(Sheet sheet) {
        Objects.requireNonNull(sheet);
        
        return switch (sheet) {
            case XSSFChartSheet xcs -> EnumSet.of(SheetType.CHART_SHEET);
            case XSSFDialogsheet xds -> EnumSet.of(SheetType.DIALOG_SHEET);
            case XSSFSheet xs -> EnumSet.of(SheetType.WORKSHEET, SheetType.MACRO_SHEET);
        
            case HSSFSheet hSheet -> {
                try {
                    if (hSheet.getDialog()) {
                        // FIXME: [No.1 シート識別不正 - usermodel] ダイアログシートであっても、どういう訳かここに入らない
                        yield EnumSet.of(SheetType.DIALOG_SHEET);
                    }
                } catch (NullPointerException e) {
                    // HSSFSheet#getDialog() はたまにヌルポを吐くので受け止める。
                }
                // FIXME: [No.1 シート識別不正 - usermodel] ダイアログシートの場合もここに到達してしまうので、やむを得ず含めることにする。
                yield EnumSet.of(
                        SheetType.WORKSHEET,
                        SheetType.CHART_SHEET,
                        SheetType.MACRO_SHEET,
                        SheetType.DIALOG_SHEET);
            }
            
            default -> throw new AssertionError("unknown sheet type: " + sheet.getClass().getName());
        };
    }
    
    /**
     * 指定されたExcelブックに設定されているあらゆる色をクリアし、
     * セルコメントを非表示にします。<br>
     * 
     * @param book Excelブック
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static void clearAllColors(Workbook book) {
        Objects.requireNonNull(book);
        
        switch (book) {
            case XSSFWorkbook xBook -> clearAllColors(xBook);
            case HSSFWorkbook hBook -> clearAllColors(hBook);
            default -> throw new AssertionError("unknown book type: " + book.getClass().getName());
        }
    }
    
    private static void clearAllColors(XSSFWorkbook book) {
        assert book != null;
        
        short automatic = IndexedColors.AUTOMATIC.getIndex();
        
        // セルスタイルに対する処理
        IntStream.range(0, book.getNumCellStyles()).mapToObj(book::getCellStyleAt).forEach(style -> {
            
            // 罫線の色
            style.setTopBorderColor(automatic);
            style.setBottomBorderColor(automatic);
            style.setLeftBorderColor(automatic);
            style.setRightBorderColor(automatic);
            // FIXME: [No.3 着色関連] 斜めの罫線に対する処理が必要
            // 参考：http://higehige0.blog.fc2.com/blog-entry-65.html
            
            // パターンは残したまま、背景色＝白、前景色＝黒にする
            if (style.getFillPattern() == FillPatternType.SOLID_FOREGROUND) {
                style.setFillPattern(FillPatternType.NO_FILL);
                
            } else if (style.getFillPattern() != FillPatternType.NO_FILL) {
                style.setFillForegroundColor(null);
                style.setFillBackgroundColor(null);
            }
            // FIXME: [No.3 着色関連] グラデーション背景色の消し方が分からない
        });
        
        // フォントに対する処理
        IntStream.range(0, book.getNumberOfFonts()).mapToObj(book::getFontAt).forEach(font -> {
            font.setColor(null);
            // FIXME: [No.3 着色関連] 文字列内の部分着色の消し方が分からない
        });
        
        // 条件付き書式
        // 面倒なので、条件付き書式の色を消すのではなく条件付き書式そのものを消してしまうことにする。
        // FIXME: [No.3 着色関連] 条件付き書式の色を消す方式に変える
        IntStream.range(0, book.getNumberOfSheets()).mapToObj(book::getSheetAt).forEach(sheet -> {
            XSSFSheetConditionalFormatting cfs = sheet.getSheetConditionalFormatting();
            while (0 < cfs.getNumConditionalFormattings()) {
                cfs.removeConditionalFormatting(0);
            }
        });
        
        // シート見出し
        // FIXME: [No.3 着色関連] この実装で正しいのかさっぱり分からない
        // が事実としてシート見出し色が消えるのできっと良いのだろう・・
        book.forEach(sheet -> ((XSSFSheet) sheet).setTabColor(new XSSFColor(new DefaultIndexedColorMap())));
        
        // セルコメントに対する処理
        book.forEach(sheet -> ((XSSFSheet) sheet).getCellComments().values().forEach(comment -> {
            // FIXME: [No.7 POI関連] XSSFComment#setVisible(boolean)が機能しない
            comment.setVisible(false);
            // FIXME: [No.3 着色関連] セルコメントのスタイル変更方法が分からない
        }));
    }
    
    private static void clearAllColors(HSSFWorkbook book) {
        assert book != null;
        
        short automatic = IndexedColors.AUTOMATIC.getIndex();
        
        // セルスタイルに対する処理
        IntStream.range(0, book.getNumCellStyles()).mapToObj(book::getCellStyleAt).forEach(style -> {
            
            // 罫線の色
            style.setTopBorderColor(automatic);
            style.setBottomBorderColor(automatic);
            style.setLeftBorderColor(automatic);
            style.setRightBorderColor(automatic);
            // FIXME: [No.3 着色関連] 斜めの罫線に対する処理が必要
            // 参考：http://higehige0.blog.fc2.com/blog-entry-65.html
            
            // パターンは残したまま、背景色＝白、前景色＝黒にする
            if (style.getFillPattern() == FillPatternType.SOLID_FOREGROUND) {
                style.setFillPattern(FillPatternType.NO_FILL);
                
            } else if (style.getFillPattern() != FillPatternType.NO_FILL) {
                style.setFillForegroundColor(automatic);
                style.setFillBackgroundColor(automatic);
            }
            // FIXME: [No.3 着色関連] グラデーション背景色の消し方が分からない
        });
        
        // フォントに対する処理
        IntStream.range(0, book.getNumberOfFonts()).mapToObj(book::getFontAt).forEach(font -> {
            font.setColor(HSSFFont.COLOR_NORMAL);
            // FIXME: [No.3 着色関連] 非インデックスフォント色の消し方が分からない
        });
        
        // 条件付き書式
        // 面倒なので、条件付き書式の色を消すのではなく条件付き書式そのものを消してしまうことにする。
        // FIXME: [No.3 着色関連] 条件付き書式の色を消す方式に変える
        IntStream.range(0, book.getNumberOfSheets()).mapToObj(book::getSheetAt).forEach(sheet -> {
            HSSFSheetConditionalFormatting cfs = sheet.getSheetConditionalFormatting();
            while (0 < cfs.getNumConditionalFormattings()) {
                cfs.removeConditionalFormatting(0);
            }
        });
        
        // FIXME: [No.3 着色関連] シート見出しの色の消し方が分からない
        
        // セルコメントに対する処理
        book.forEach(sheet -> ((HSSFSheet) sheet).getCellComments().values().forEach(comment -> {
            comment.setVisible(false);
            comment.resetBackgroundImage();
            comment.setFillColor(HSSFComment.FILL__FILLCOLOR_DEFAULT);
            comment.setLineStyle(HSSFComment.LINESTYLE_SOLID);
            comment.setLineStyleColor(HSSFComment.LINESTYLE__COLOR_DEFAULT);
        }));
    }
    
    /**
     * 指定されたExcelシート上の指定された行に指定された色を付けます。<br>
     * 
     * @param sheet Excelシート
     * @param rowIdxs 色を付ける行のインデックス値
     * @param color 着色する色のインデックス値
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static void paintRows(Sheet sheet, int[] rowIdxs, short color) {
        Objects.requireNonNull(sheet);
        Objects.requireNonNull(rowIdxs);
        
        if (rowIdxs.length == 0) {
            return;
        }
        
        // まず、存在しない行を作成する。
        Set<Row> rows = new HashSet<>();
        for (int i : rowIdxs) {
            if (sheet.getRow(i) == null) {
                sheet.createRow(i);
            }
            rows.add(sheet.getRow(i));
        }
        
        // 次に、着色前の現在のスタイルで行をグルーピングする。
        Map<CellStyle, Set<Row>> currStyles = rows.stream()
                .filter(row -> row.getRowStyle() != null)
                .collect(Collectors.groupingBy(
                        Row::getRowStyle,
                        HashMap::new,
                        Collectors.toSet()));
        currStyles.put(null, rows.stream()
                .filter(row -> row.getRowStyle() == null)
                .collect(Collectors.toSet()));
        
        // 最後に、現行スタイルごとに着色スタイルを用意し、対象行に適用する。
        currStyles.forEach((currStyle, rs) -> {
            CellStyle newStyle = sheet.getWorkbook().createCellStyle();
            if (currStyle != null) {
                newStyle.cloneStyleFrom(currStyle);
            }
            newStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            newStyle.setFillForegroundColor(color);
            rs.forEach(r -> r.setRowStyle(newStyle));
        });
        
        // 対象行上のセルのアドレスを集め、着色する。
        Set<CellAddress> addresses = rows.stream()
                .flatMap(row -> StreamSupport.stream(row.spliterator(), false))
                .map(Cell::getAddress)
                .collect(Collectors.toSet());
        paintCells(sheet, addresses, color);
    }
    
    /**
     * 指定されたExcelシートの指定された列に指定された色を付けます。<br>
     * 
     * @param sheet Excelシート
     * @param columnIdxs 色を付ける行のインデックス値
     * @param color 着色する色のインデックス値
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static void paintColumns(Sheet sheet, int[] columnIdxs, short color) {
        Objects.requireNonNull(sheet);
        Objects.requireNonNull(columnIdxs);
        
        if (columnIdxs.length == 0) {
            return;
        }
        
        // まず、着色前の現行スタイルで列をグルーピングする。
        Map<CellStyle, Set<Integer>> currStyles = Arrays.stream(columnIdxs)
                .filter(i -> sheet.getColumnStyle(i) != null)
                .boxed()
                .collect(Collectors.groupingBy(
                        sheet::getColumnStyle,
                        HashMap::new,
                        Collectors.toSet()));
        currStyles.put(null, Arrays.stream(columnIdxs)
                .filter(i -> sheet.getColumnStyle(i) == null)
                .boxed()
                .collect(Collectors.toSet()));
        
        // そして、現行スタイルごとに着色スタイルを用意し、対象列に適用する。
        currStyles.forEach((currStyle, idxs) -> {
            CellStyle newStyle = sheet.getWorkbook().createCellStyle();
            if (currStyle != null) {
                newStyle.cloneStyleFrom(currStyle);
            }
            newStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            newStyle.setFillForegroundColor(color);
            idxs.forEach(i -> sheet.setDefaultColumnStyle(i, newStyle));
        });
        
        // 対象列上のセルのアドレスを集め、着色する。
        Set<Integer> idxs = Arrays.stream(columnIdxs).boxed().collect(Collectors.toSet());
        Set<CellAddress> addresses = StreamSupport.stream(sheet.spliterator(), true)
                .flatMap(row -> StreamSupport.stream(row.spliterator(), false))
                .filter(cell -> idxs.contains(cell.getColumnIndex()))
                .map(Cell::getAddress)
                .collect(Collectors.toSet());
        paintCells(sheet, addresses, color);
    }
    
    /**
     * 指定されたExcelシート上の指定された位置のセルに指定された色を付けます。<br>
     * 
     * @param sheet Excelシート
     * @param addresses 色を付けるセルの位置
     * @param color 着色する色のインデックス値
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static void paintCells(
            Sheet sheet,
            Set<CellAddress> addresses,
            short color) {
        
        Objects.requireNonNull(sheet);
        Objects.requireNonNull(addresses);
        
        // まず、存在しないセルを作成する。
        Set<Cell> cells = new HashSet<>();
        Map<Integer, List<CellAddress>> grouped = addresses.stream()
                .collect(Collectors.groupingBy(CellAddress::getRow));
        
        grouped.forEach((rowIdx, as) -> {
            Row row = sheet.getRow(rowIdx);
            if (row == null) {
                row = sheet.createRow(rowIdx);
            }
            Row row1 = row;
            as.forEach(a -> {
                Cell cell = row1.getCell(a.getColumn());
                if (cell == null) {
                    cell = row1.createCell(a.getColumn());
                }
                cells.add(cell);
            });
        });
        
        // 次に、着色前の現行スタイルでセルをグルーピングする。
        Map<CellStyle, Set<Cell>> currStyles = cells.stream()
                .filter(cell -> cell.getCellStyle() != null)
                .collect(Collectors.groupingBy(
                        Cell::getCellStyle,
                        HashMap::new,
                        Collectors.toSet()));
        currStyles.put(null, cells.stream()
                .filter(cell -> cell.getCellStyle() == null)
                .collect(Collectors.toSet()));
        
        // 最後に、現行スタイルごとに着色スタイルを用意し、対象セルに適用する。
        Map<String, Object> newProperties = Map.of(
                CellUtil.FILL_PATTERN, FillPatternType.SOLID_FOREGROUND,
                CellUtil.FILL_FOREGROUND_COLOR, color);
        
        currStyles.forEach((currStyle, cs) -> {
            Iterator<Cell> itr = cs.iterator();
            if (itr.hasNext()) {
                Cell first = itr.next();
                CellUtil.setCellStyleProperties(first, newProperties);
                CellStyle newStyle = first.getCellStyle();
                itr.forEachRemaining(cell -> cell.setCellStyle(newStyle));
            }
        });
    }
    
    /**
     * 指定されたExcelシート上の指定された位置のセルに付されているコメントに
     * 指定された色を付け、表示状態にします。<br>
     * 
     * @param sheet Excelシート
     * @param addresses 色を付けるセルコメントの位置
     * @param color 着色する色
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static void paintComments(
            Sheet sheet,
            Set<CellAddress> addresses,
            Color color) {
        
        Objects.requireNonNull(sheet);
        Objects.requireNonNull(addresses);
        Objects.requireNonNull(color);
        
        Map<CellAddress, ? extends Comment> comments = sheet.getCellComments();
        
        addresses.forEach(addr -> {
            Comment c = comments.get(addr);
            
            switch (c) {
                case XSSFComment comment:
                    // FIXME: [No.7 POI関連] XSSFComment#setVisible(boolean)が機能しない
                    comment.setVisible(true);
                    // FIXME: [No.3 着色関連] セルコメントのスタイル変更方法が分からない
                    break;
                
                case HSSFComment comment:
                    comment.setVisible(true);
                    comment.setFillColor(color.getRed(), color.getGreen(), color.getBlue());
                    break;
                
                default:
                    throw new AssertionError("unknown comment type: " + c.getClass().getName());
            }
        });
    }
    
    /**
     * 指定されたExcelシートの見出しに指定された色を付けます。<br>
     * 
     * @param sheet Excelシート
     * @param color 着色する色
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static void paintSheetTab(
            Sheet sheet,
            Color color) {
        
        Objects.requireNonNull(sheet);
        Objects.requireNonNull(color);
        
        switch (sheet) {
            case XSSFSheet xSheet:
                xSheet.setTabColor(new XSSFColor(
                        new byte[] { (byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue() },
                        new DefaultIndexedColorMap()));
                break;
            
            case HSSFSheet hSheet:
                // FIXME: [No.3 着色関連] シート見出しの色の設定方法が分からない
                break;
            
            default:
                // nop
        }
    }
    
    /**
     * シート上の指定した位置の {@link Cell} オブジェクトを返します。<br>
     * 指定した位置のセルオブジェクトが存在しない場合は作成して返します。<br>
     * 
     * @param sheet シート
     * @param r 行インデックス（0開始）
     * @param c 列インデックス（0開始）
     * @return セルオブジェクト
     * @throws NullPointerException パラメータが {@code null} の場合
     * @throws IllegalArgumentException {@code r}, {@code c} のいずれかが負数の場合
     */
    public static Cell getCell(
            Sheet sheet,
            int r,
            int c) {
        
        Objects.requireNonNull(sheet);
        if (r < 0 || c < 0) {
            throw new IllegalArgumentException("(row:%d, column:%d)".formatted(r, c));
        }
        
        Row row = CellUtil.getRow(r, sheet);
        return CellUtil.getCell(row, c);
    }
    
    /**
     * シート上の指定した位置の {@link Cell} オブジェクトを返します。<br>
     * 指定した位置のセルオブジェクトが存在しない場合は {@link Optional#empty} を返します。<br>
     * 
     * @param sheet シート
     * @param r 行インデックス（0開始）
     * @param c 列インデックス（0開始）
     * @return セルオブジェクト（存在しない場合は {@link Optional#empty}）
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static Optional<Cell> getCellIfPresent(
            Sheet sheet,
            int r,
            int c) {
        
        Objects.requireNonNull(sheet);
        if (r < 0 || c < 0) {
            throw new IllegalArgumentException("(row:%d, column:%d)".formatted(r, c));
        }
        
        return Optional
                .ofNullable(sheet.getRow(r))
                .map(row -> row.getCell(c));
    }
    
    /**
     * シート上の指定した位置のセルに値を設定します。<br>
     * 指定した位置のセルオブジェクトが存在しない場合は作成して値を設定します。<br>
     * 
     * @param sheet シート
     * @param r 行インデックス（0開始）
     * @param c 列インデックス（0開始）
     * @param value 設定する値
     * @return セルオブジェクト
     */
    public static Cell setCellValue(
            Sheet sheet,
            int r,
            int c,
            String value) {
        
        Cell cell = getCell(sheet, r, c);
        cell.setCellValue(value);
        return cell;
    }
    
    // [instance members] ******************************************************
    
    private PoiUtil() {
    }
}
