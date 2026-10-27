package ahqpck.hse.safety.model.dto;

import java.util.List;

/**
 * A table of text rows ready to be written to Excel or PDF.
 * <p>
 * Excel gets every column. PDF gets only the first {@code pdfColumnWidths.length} columns,
 * with those relative widths, because a landscape page cannot hold them all.
 */
public record ExportTable(
        String title,
        List<String> headers,
        List<List<String>> rows,
        float[] pdfColumnWidths) {
}
