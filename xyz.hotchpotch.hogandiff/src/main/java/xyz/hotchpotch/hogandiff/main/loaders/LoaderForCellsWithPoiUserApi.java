package xyz.hotchpotch.hogandiff.main.loaders;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import xyz.hotchpotch.hogandiff.main.BookHandler;
import xyz.hotchpotch.hogandiff.main.CommonUtil;
import xyz.hotchpotch.hogandiff.main.ExcelHandlingException;
import xyz.hotchpotch.hogandiff.main.PoiUtil;
import xyz.hotchpotch.hogandiff.main.SheetHandler;
import xyz.hotchpotch.hogandiff.main.models.BookType;
import xyz.hotchpotch.hogandiff.main.models.CellData;
import xyz.hotchpotch.hogandiff.main.models.SheetType;

/**
 * Apache POI のユーザーモデル API を利用して
 * .xlsx/.xlsm/.xls 形式のExcelブックのワークシートから
 * セルデータを抽出する {@link LoaderForCells} の実装です。<br>
 *
 * @author nmby
 */
@BookHandler(targetTypes = { BookType.XLS, BookType.XLSX, BookType.XLSM })
@SheetHandler(targetTypes = { SheetType.WORKSHEET })
public class LoaderForCellsWithPoiUserApi implements LoaderForCells {

    // [static members] ********************************************************

    // [instance members] ******************************************************

    private final Function<Cell, CellData> converter;

    /**
     * コンストラクタ
     * 
     * @param converter セル変換関数
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public LoaderForCellsWithPoiUserApi(Function<Cell, CellData> converter) {
        Objects.requireNonNull(converter);

        this.converter = converter;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws NullPointerException
     *                                  {@code bookPath}, {@code sheetName} のいずれかが
     *                                  {@code null} の場合
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
        CommonUtil.ifNotSupportedBookTypeThenThrow(getClass(), BookType.of(bookPath));

        try (Workbook wb = WorkbookFactory.create(
                bookPath.toFile(),
                readPassword,
                true)) {

            Sheet sheet = wb.getSheet(sheetName);
            if (sheet == null) {
                // 例外カスケードポリシーに従い、
                // 後続の catch でさらに ExcelHandlingException にラップする。
                // ちょっと気持ち悪い気もするけど。
                throw new NoSuchElementException(
                        "no such sheet : %s - %s".formatted(bookPath, sheetName));
            }

            Set<SheetType> possibleTypes = PoiUtil.possibleTypes(sheet);
            // 同じく、後続の catch でさらに ExcelHandlingException にラップする。
            CommonUtil.ifNotSupportedSheetTypeThenThrow(getClass(), possibleTypes);

            Set<CellData> cells = StreamSupport.stream(sheet.spliterator(), true)
                    .flatMap(row -> StreamSupport.stream(row.spliterator(), false))
                    .map(converter::apply)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));

            Map<String, CellData> cellsMap = cells.parallelStream()
                    .collect(Collectors.toMap(
                            CellData::address,
                            Function.identity()));

            sheet.getCellComments().forEach((addr, comm) -> {
                String address = addr.formatAsString();
                // xlsx/xlsm 形式の場合、空コメントから null が返されるため、空文字列に標準化する。
                String comment = Optional.ofNullable(comm.getString().getString()).orElse("");

                if (cellsMap.containsKey(address)) {
                    CellData original = cellsMap.get(address);
                    cells.remove(original);
                    cells.add(original.withComment(comment));
                } else {
                    cells.add(CellData.of(address, "", comment));
                }
            });

            return cells;

        } catch (Exception e) {
            throw new ExcelHandlingException(
                    "processing failed : %s - %s".formatted(bookPath, sheetName),
                    e);
        }
    }
}
