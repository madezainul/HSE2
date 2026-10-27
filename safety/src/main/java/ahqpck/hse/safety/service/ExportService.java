package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.dto.ExportTable;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes an {@link ExportTable} to an Excel (.xlsx) or PDF file.
 */
@Service
@Slf4j
public class ExportService {

    private static final int EXCEL_MAX_CELL_LENGTH = 32767; // Excel's hard limit per cell
    private static final int EXCEL_MAX_COLUMN_CHARS = 60;
    private static final int EXCEL_MIN_COLUMN_CHARS = 10;
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] toExcel(ExportTable table) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(table.title()));

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle bodyStyle = workbook.createCellStyle();
            bodyStyle.setWrapText(true);
            bodyStyle.setVerticalAlignment(VerticalAlignment.TOP);

            int columnCount = table.headers().size();
            int[] maxChars = new int[columnCount];

            Row headerRow = sheet.createRow(0);
            for (int c = 0; c < columnCount; c++) {
                String header = table.headers().get(c);
                headerRow.createCell(c).setCellValue(header);
                headerRow.getCell(c).setCellStyle(headerStyle);
                maxChars[c] = header.length();
            }

            int rowIndex = 1;
            for (List<String> values : table.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int c = 0; c < columnCount; c++) {
                    String value = c < values.size() ? values.get(c) : "";
                    if (value.length() > EXCEL_MAX_CELL_LENGTH) {
                        value = value.substring(0, EXCEL_MAX_CELL_LENGTH);
                    }
                    row.createCell(c).setCellValue(value);
                    row.getCell(c).setCellStyle(bodyStyle);
                    maxChars[c] = Math.max(maxChars[c], longestLine(value));
                }
            }

            for (int c = 0; c < columnCount; c++) {
                int chars = Math.min(Math.max(maxChars[c], EXCEL_MIN_COLUMN_CHARS), EXCEL_MAX_COLUMN_CHARS);
                sheet.setColumnWidth(c, (chars + 2) * 256);
            }

            sheet.createFreezePane(0, 1);
            if (!table.rows().isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(0, table.rows().size(), 0, columnCount - 1));
            }

            workbook.write(out);
            log.info("Excel export '{}' created with {} rows", table.title(), table.rows().size());
            return out.toByteArray();
        }
    }

    public byte[] toPdf(ExportTable table) {
        float[] widths = table.pdfColumnWidths();
        int columnCount = widths.length;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 30, 30);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            document.add(new Paragraph(table.title(), titleFont));
            Paragraph meta = new Paragraph(
                    "Generated " + LocalDateTime.now().format(GENERATED_AT) + "  |  " + table.rows().size() + " records",
                    metaFont);
            meta.setSpacingAfter(8);
            document.add(meta);

            PdfPTable pdfTable = new PdfPTable(columnCount);
            pdfTable.setWidthPercentage(100);
            pdfTable.setWidths(widths);
            pdfTable.setHeaderRows(1);

            for (int c = 0; c < columnCount; c++) {
                PdfPCell cell = new PdfPCell(new Phrase(table.headers().get(c), headerFont));
                cell.setBackgroundColor(new Color(0, 0, 139));
                cell.setHorizontalAlignment(Element.ALIGN_LEFT);
                cell.setPadding(4);
                pdfTable.addCell(cell);
            }

            for (List<String> values : table.rows()) {
                for (int c = 0; c < columnCount; c++) {
                    String value = c < values.size() ? values.get(c) : "";
                    PdfPCell cell = new PdfPCell(new Phrase(value, bodyFont));
                    cell.setPadding(3);
                    pdfTable.addCell(cell);
                }
            }

            document.add(pdfTable);
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not create PDF export '" + table.title() + "'", e);
        } finally {
            document.close();
        }

        log.info("PDF export '{}' created with {} rows", table.title(), table.rows().size());
        return out.toByteArray();
    }

    /**
     * Excel sheet names are limited to 31 characters and cannot contain : \ / ? * [ ]
     */
    private String safeSheetName(String title) {
        String cleaned = title.replaceAll("[:\\\\/?*\\[\\]]", " ");
        return cleaned.length() > 31 ? cleaned.substring(0, 31) : cleaned;
    }

    private int longestLine(String value) {
        int longest = 0;
        for (String line : value.split("\\R", -1)) {
            longest = Math.max(longest, line.length());
        }
        return longest;
    }
}
