package ahqpck.hse.safety.exports;

import ahqpck.hse.safety.model.dto.ExportTable;
import ahqpck.hse.safety.service.ExportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ExportServiceTest {

    private final ExportService service = new ExportService();

    private static ExportTable table(String title, List<String> headers, List<List<String>> rows, float[] widths) {
        return new ExportTable(title, headers, rows, widths);
    }

    private static XSSFWorkbook open(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    // ---------------------------------------------------------------- Excel

    @Test
    void excel_basicSheetHeaderRowsAndValues() throws IOException {
        ExportTable t = table("Incident Report", List.of("Code", "Area", "Desc"),
                List.of(List.of("INC-1", "Plant A", "Spill"), List.of("INC-2", "Plant B", "Fall")),
                new float[]{1, 1, 1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
            Sheet s = wb.getSheetAt(0);
            assertThat(s.getSheetName()).isEqualTo("Incident Report");
            assertThat(s.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Code");
            assertThat(s.getRow(0).getCell(2).getStringCellValue()).isEqualTo("Desc");
            assertThat(s.getLastRowNum()).isEqualTo(2);
            assertThat(s.getRow(1).getCell(0).getStringCellValue()).isEqualTo("INC-1");
            assertThat(s.getRow(2).getCell(1).getStringCellValue()).isEqualTo("Plant B");
            assertThat(s.getRow(2).getCell(2).getStringCellValue()).isEqualTo("Fall");
        }
    }

    @Test
    void excel_headerRowIsFrozen() throws IOException {
        ExportTable t = table("T", List.of("A"), List.of(List.of("x")), new float[]{1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            assertThat(wb.getSheetAt(0).getPaneInformation()).isNotNull();
            assertThat(wb.getSheetAt(0).getPaneInformation().isFreezePane()).isTrue();
            assertThat(wb.getSheetAt(0).getPaneInformation().getHorizontalSplitTopRow()).isEqualTo((short) 1);
        }
    }

    @Test
    void excel_emptyTableHasOnlyHeader() throws IOException {
        ExportTable t = table("Empty", List.of("A", "B"), List.of(), new float[]{1, 1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            Sheet s = wb.getSheetAt(0);
            assertThat(s.getLastRowNum()).isZero();
            assertThat(s.getRow(0).getLastCellNum()).isEqualTo((short) 2);
        }
    }

    @Test
    void excel_autoFilterCoversHeaderAndAllRows() throws IOException {
        ExportTable t = table("T", List.of("A", "B"),
                List.of(List.of("1", "2"), List.of("3", "4"), List.of("5", "6")), new float[]{1, 1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            assertThat(wb.getSheetAt(0).getCTWorksheet().getAutoFilter().getRef()).isEqualTo("A1:B4");
        }
    }

    @Test
    void excel_textOver32767CharsIsTruncated() throws IOException {
        String big = "x".repeat(40_000);
        ExportTable t = table("T", List.of("A"), List.of(List.of(big)), new float[]{1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            assertThat(wb.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).hasSize(32767);
        }
    }

    @Test
    void excel_textExactly32767CharsIsKept() throws IOException {
        String big = "y".repeat(32767);
        ExportTable t = table("T", List.of("A"), List.of(List.of(big)), new float[]{1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            assertThat(wb.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).isEqualTo(big);
        }
    }

    @Test
    void excel_forbiddenSheetNameCharactersAreReplaced() throws IOException {
        ExportTable t = table("a:b\\c/d?e*f[g]h", List.of("A"), List.of(), new float[]{1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            String name = wb.getSheetAt(0).getSheetName();
            assertThat(name).doesNotContainPattern("[:\\\\/?*\\[\\]]");
            assertThat(name).startsWith("a").endsWith("h");
        }
    }

    @Test
    void excel_sheetNameLongerThan31IsCut() throws IOException {
        ExportTable t = table("This is a very long report title that exceeds", List.of("A"), List.of(), new float[]{1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            assertThat(wb.getSheetAt(0).getSheetName()).hasSize(31);
        }
    }

    @Test
    void excel_shortRowIsPaddedWithEmptyCells() throws IOException {
        ExportTable t = table("T", List.of("A", "B", "C"), List.of(List.of("only")), new float[]{1, 1, 1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            Row r = wb.getSheetAt(0).getRow(1);
            assertThat(r.getCell(0).getStringCellValue()).isEqualTo("only");
            assertThat(r.getCell(1).getStringCellValue()).isEmpty();
            assertThat(r.getCell(2).getStringCellValue()).isEmpty();
        }
    }

    @Test
    void excel_longRowIsCutToHeaderCount() throws IOException {
        ExportTable t = table("T", List.of("A"), List.of(List.of("a", "b", "c")), new float[]{1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            assertThat(wb.getSheetAt(0).getRow(1).getLastCellNum()).isEqualTo((short) 1);
        }
    }

    @Test
    void excel_multilineAndUnicodeValuesRoundTrip() throws IOException {
        String v = "line1\nline2 é中文";
        ExportTable t = table("T", List.of("A"), List.of(List.of(v)), new float[]{1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            assertThat(wb.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).isEqualTo(v);
        }
    }

    @Test
    void excel_columnWidthIsClamped() throws IOException {
        ExportTable t = table("T", List.of("A", "B"), List.of(List.of("x", "z".repeat(500))), new float[]{1, 1});
        try (XSSFWorkbook wb = open(service.toExcel(t))) {
            Sheet s = wb.getSheetAt(0);
            assertThat(s.getColumnWidth(0)).isEqualTo(12 * 256);
            assertThat(s.getColumnWidth(1)).isEqualTo(62 * 256);
        }
    }

    @Test
    void excel_manyRows() throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            rows.add(List.of("r" + i, "v"));
        }
        try (XSSFWorkbook wb = open(service.toExcel(table("T", List.of("A", "B"), rows, new float[]{1, 1})))) {
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(5000);
            assertThat(wb.getSheetAt(0).getRow(5000).getCell(0).getStringCellValue()).isEqualTo("r4999");
        }
    }





    @Test
    void excel_titleMadeOnlyOfForbiddenCharsShouldNotCrash() {
        ExportTable t = table("///", List.of("A"), List.of(), new float[]{1});
        assertThatCode(() -> service.toExcel(t)).doesNotThrowAnyException();
    }

    // ------------------------------------------------------------------ PDF

    private static boolean startsWithPdf(byte[] b) {
        return b.length > 5 && new String(b, 0, 5, StandardCharsets.ISO_8859_1).equals("%PDF-");
    }

    @Test
    void pdf_basicIsValidPdf() {
        ExportTable t = table("Report", List.of("A", "B"), List.of(List.of("1", "2")), new float[]{1, 1});
        byte[] pdf = service.toPdf(t);
        assertThat(startsWithPdf(pdf)).isTrue();
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).contains("%%EOF");
    }

    @Test
    void pdf_emptyTableStillProducesPdf() {
        ExportTable t = table("Empty", List.of("A", "B"), List.of(), new float[]{1, 1});
        assertThat(startsWithPdf(service.toPdf(t))).isTrue();
    }

    @Test
    void pdf_manyRowsProduceLargerPdf() {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            rows.add(List.of("row " + i, "some text " + i));
        }
        byte[] small = service.toPdf(table("T", List.of("A", "B"), rows.subList(0, 1), new float[]{1, 1}));
        byte[] big = service.toPdf(table("T", List.of("A", "B"), rows, new float[]{1, 1}));
        assertThat(startsWithPdf(big)).isTrue();
        assertThat(big.length).isGreaterThan(small.length);
    }

    @Test
    void pdf_fewerWidthsThanHeadersUsesOnlyFirstColumns() {
        ExportTable t = table("T", List.of("A", "B", "C", "D"), List.of(List.of("1", "2", "3", "4")), new float[]{1, 1});
        assertThat(startsWithPdf(service.toPdf(t))).isTrue();
    }

    @Test
    void pdf_shortRowIsPadded() {
        ExportTable t = table("T", List.of("A", "B", "C"), List.of(List.of("1")), new float[]{1, 1, 1});
        assertThat(startsWithPdf(service.toPdf(t))).isTrue();
    }


    @Test
    void pdf_veryLongSingleCellDoesNotFail() {
        ExportTable t = table("T", List.of("A"), List.of(List.of("word ".repeat(20_000))), new float[]{1});
        assertThat(startsWithPdf(service.toPdf(t))).isTrue();
    }

    @Test
    void pdf_nullCellValueShouldBeTreatedAsEmpty() {
        List<String> row = Arrays.asList("a", null);
        ExportTable t = table("T", List.of("A", "B"), List.of(row), new float[]{1, 1});
        assertThatCode(() -> service.toPdf(t)).doesNotThrowAnyException();
    }

    @Test
    void pdf_unicodeBeyondLatin1DoesNotFail() {
        ExportTable t = table("T", List.of("A"), List.of(List.of("中文 АБ")), new float[]{1});
        assertThat(startsWithPdf(service.toPdf(t))).isTrue();
    }
}
