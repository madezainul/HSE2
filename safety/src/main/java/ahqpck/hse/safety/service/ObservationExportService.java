package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.dto.ExportTable;
import ahqpck.hse.safety.model.dto.ObservationSearchCriteria;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.repository.ObservationRepository;
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
 * Exports all observation reports, newest first, to Excel or PDF.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ObservationExportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Excel gets all of these columns. PDF gets the first PDF_COLUMN_WIDTHS.length of them.
    private static final List<String> HEADERS = List.of(
            "Code", "Observation Date", "Area", "Category", "Type", "Status", "Observer", "Description",
            "Observer Employee ID", "Responsible", "Responsible Employee ID", "Mitigation",
            "Corrective Action", "Remarks");

    private static final float[] PDF_COLUMN_WIDTHS = {1.3f, 1.6f, 1.6f, 1.8f, 1.5f, 1.2f, 1.5f, 5f};

    private final ObservationRepository observationRepository;
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
     * Export only the observations that match the list filters, in the list's sort order.
     */
    @Transactional(readOnly = true)
    public byte[] exportExcel(ObservationSearchCriteria criteria, String sortBy, boolean asc) throws IOException {
        return exportService.toExcel(buildTable(findMatching(criteria, sortBy, asc)));
    }

    @Transactional(readOnly = true)
    public byte[] exportPdf(ObservationSearchCriteria criteria, String sortBy, boolean asc) {
        return exportService.toPdf(buildTable(findMatching(criteria, sortBy, asc)));
    }

    private List<Observation> findMatching(ObservationSearchCriteria criteria, String sortBy, boolean asc) {
        return observationRepository.findAll(ObservationSpecifications.matching(criteria),
                ObservationService.resolveSort(sortBy, asc));
    }

    private ExportTable buildTable() {
        return buildTable(observationRepository.findAll(Sort.by(Sort.Direction.DESC, "observationDate")));
    }

    private ExportTable buildTable(List<Observation> observations) {
        log.info("Building observation export for {} records", observations.size());

        List<List<String>> rows = new ArrayList<>(observations.size());
        for (Observation observation : observations) {
            rows.add(List.of(
                    text(observation.getCode()),
                    observation.getObservationDate() != null ? observation.getObservationDate().format(DATE_TIME) : "",
                    observation.getArea() != null ? text(observation.getArea().getName()) : "",
                    observation.getCategory() != null ? observation.getCategory().getDisplayName() : "",
                    observation.getType() != null ? observation.getType().getDisplayName() : "",
                    observation.getStatus() != null ? observation.getStatus().getDisplayName() : "",
                    text(observation.getObserver()),
                    text(observation.getDescription()),
                    text(observation.getObserverEmployeeId()),
                    text(observation.getResponsible()),
                    text(observation.getResponsibleEmployeeId()),
                    observation.getMitigation() != null ? observation.getMitigation().getDisplayName() : "",
                    text(observation.getCorrectiveAction()),
                    text(observation.getRemarks())));
        }

        return new ExportTable("Observation Report", HEADERS, rows, PDF_COLUMN_WIDTHS);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
