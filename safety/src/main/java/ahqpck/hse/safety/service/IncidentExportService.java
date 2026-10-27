package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.dto.ExportTable;
import ahqpck.hse.safety.model.dto.IncidentSearchCriteria;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports all incident reports, newest first, to Excel or PDF.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentExportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Excel gets all of these columns. PDF gets the first PDF_COLUMN_WIDTHS.length of them.
    private static final List<String> HEADERS = List.of(
            "Code", "Report Date", "Area", "Type", "Severity", "Status", "Reported By", "Description",
            "Involved Person", "Involved Employee ID", "Involved Position", "Witnesses", "Witnesses Employee ID",
            "Immediate Action", "Corrective Action", "Medical Attention Required");

    private static final float[] PDF_COLUMN_WIDTHS = {1.3f, 1.6f, 1.6f, 1.6f, 1.2f, 1.6f, 1.5f, 5f};

    private final IncidentRepository incidentRepository;
    private final ExportService exportService;

    // Read inside one transaction because Area is loaded lazily
    @Transactional(readOnly = true)
    public byte[] exportExcel() throws IOException {
        return exportService.toExcel(buildTable());
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf() {
        return exportService.toPdf(buildTable());
    }

    /**
     * Export only the incidents that match the list filters, in the list's sort order.
     */
    @Transactional(readOnly = true)
    public byte[] exportExcel(IncidentSearchCriteria criteria, String sortBy, boolean asc) throws IOException {
        return exportService.toExcel(buildTable(findMatching(criteria, sortBy, asc)));
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(IncidentSearchCriteria criteria, String sortBy, boolean asc) {
        return exportService.toPdf(buildTable(findMatching(criteria, sortBy, asc)));
    }

    private List<Incident> findMatching(IncidentSearchCriteria criteria, String sortBy, boolean asc) {
        return incidentRepository.findAll(IncidentSpecifications.matching(criteria), IncidentService.resolveSort(sortBy, asc));
    }

    private ExportTable buildTable() {
        return buildTable(incidentRepository.findAll(Sort.by(Sort.Direction.DESC, "reportDate")));
    }

    private ExportTable buildTable(List<Incident> incidents) {
        log.info("Building incident export for {} records", incidents.size());

        List<List<String>> rows = new ArrayList<>(incidents.size());
        for (Incident incident : incidents) {
            rows.add(List.of(
                    text(incident.getCode()),
                    incident.getReportDate() != null ? incident.getReportDate().format(DATE_TIME) : "",
                    incident.getArea() != null ? text(incident.getArea().getName()) : "",
                    label(incident.getType()),
                    label(incident.getSeverity()),
                    label(incident.getStatus()),
                    text(incident.getReportedBy()),
                    text(incident.getDescription()),
                    text(incident.getInvolvedPersonName()),
                    text(incident.getInvolvedPersonEmployeeId()),
                    text(incident.getInvolvedPersonPosition()),
                    text(incident.getWitnesses()),
                    text(incident.getWitnessesEmployeeId()),
                    text(incident.getImmediateAction()),
                    text(incident.getCorrectiveAction()),
                    Boolean.TRUE.equals(incident.getMedicalAttentionRequired()) ? "Yes" : "No"));
        }

        return new ExportTable("Incident Report", HEADERS, rows, PDF_COLUMN_WIDTHS);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    /**
     * UNDER_INVESTIGATION -> "Under Investigation"
     */
    private String label(Enum<?> value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String word : value.name().split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
