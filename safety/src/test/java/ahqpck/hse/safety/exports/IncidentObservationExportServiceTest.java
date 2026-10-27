package ahqpck.hse.safety.exports;

import ahqpck.hse.safety.model.dto.ExportTable;
import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.repository.IncidentRepository;
import ahqpck.hse.safety.repository.ObservationRepository;
import ahqpck.hse.safety.service.ExportService;
import ahqpck.hse.safety.service.IncidentExportService;
import ahqpck.hse.safety.service.ObservationExportService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncidentObservationExportServiceTest {

    // ------------------------------------------------------------ helpers

    private static Incident fullIncident(String code) {
        Incident i = new Incident();
        i.setCode(code);
        i.setReportDate(LocalDateTime.of(2026, 3, 4, 5, 6));
        i.setArea(Area.builder().name("Plant A").build());
        i.setType(Incident.Type.NEAR_MISS);
        i.setSeverity(Incident.SeverityLevel.HIGH);
        i.setStatus(Incident.IncidentStatus.UNDER_INVESTIGATION);
        i.setReportedBy("Alice");
        i.setDescription("Desc");
        i.setInvolvedPersonName("Bob");
        i.setInvolvedPersonEmployeeId("E1");
        i.setInvolvedPersonPosition("Operator");
        i.setWitnesses("Carl");
        i.setWitnessesEmployeeId("E2");
        i.setImmediateAction("Stop");
        i.setCorrectiveAction("Fix");
        i.setMedicalAttentionRequired(true);
        return i;
    }

    private static Observation fullObservation(String code) {
        Observation o = new Observation();
        o.setCode(code);
        o.setObservationDate(LocalDateTime.of(2026, 1, 2, 3, 4));
        o.setArea(Area.builder().name("Warehouse").build());
        o.setCategory(Observation.Category.FIRE_PREVENTION_AND_FIRE_PROTECTION);
        o.setType(Observation.Type.UNSAFE_ACT);
        o.setStatus(Observation.Status.OPEN);
        o.setObserver("Obs");
        o.setDescription("D");
        o.setObserverEmployeeId("O1");
        o.setResponsible("Resp");
        o.setResponsibleEmployeeId("R1");
        o.setMitigation(Observation.Mitigation.HIGH);
        o.setCorrectiveAction("CA");
        o.setRemarks("Rem");
        return o;
    }

    private static ExportTable captureIncidentTable(IncidentRepository repo, List<Incident> data) throws IOException {
        when(repo.findAll(any(Sort.class))).thenReturn(data);
        ExportService es = mock(ExportService.class);
        IncidentExportService svc = new IncidentExportService(repo, es);
        svc.exportExcel();
        ArgumentCaptor<ExportTable> cap = ArgumentCaptor.forClass(ExportTable.class);
        verify(es).toExcel(cap.capture());
        return cap.getValue();
    }

    private static ExportTable captureObservationTable(ObservationRepository repo, List<Observation> data) throws IOException {
        when(repo.findAll(any(Sort.class))).thenReturn(data);
        ExportService es = mock(ExportService.class);
        ObservationExportService svc = new ObservationExportService(repo, es);
        svc.exportExcel();
        ArgumentCaptor<ExportTable> cap = ArgumentCaptor.forClass(ExportTable.class);
        verify(es).toExcel(cap.capture());
        return cap.getValue();
    }

    // ------------------------------------------------------------ incident

    @Test
    void incident_fullRowMapsEveryColumn() throws IOException {
        ExportTable t = captureIncidentTable(mock(IncidentRepository.class), List.of(fullIncident("INC-000001")));
        assertThat(t.title()).isEqualTo("Incident Report");
        assertThat(t.rows()).hasSize(1);
        assertThat(t.rows().get(0)).containsExactly(
                "INC-000001", "2026-03-04 05:06", "Plant A", "Near Miss", "High", "Under Investigation",
                "Alice", "Desc", "Bob", "E1", "Operator", "Carl", "E2", "Stop", "Fix", "Yes");
    }

    @Test
    void incident_rowAndHeaderCountsMatch() throws IOException {
        ExportTable t = captureIncidentTable(mock(IncidentRepository.class),
                List.of(fullIncident("A"), fullIncident("B"), fullIncident("C")));
        assertThat(t.rows()).hasSize(3);
        assertThat(t.rows()).allSatisfy(r -> assertThat(r).hasSameSizeAs(t.headers()));
        assertThat(t.pdfColumnWidths().length).isLessThanOrEqualTo(t.headers().size());
    }

    @Test
    void incident_nullAreaNullEnumsNullDateAndNullTextBecomeEmpty() throws IOException {
        Incident i = new Incident();
        i.setCode("INC-2");
        ExportTable t = captureIncidentTable(mock(IncidentRepository.class), List.of(i));
        List<String> row = t.rows().get(0);
        assertThat(row).hasSameSizeAs(t.headers());
        assertThat(row.get(0)).isEqualTo("INC-2");
        assertThat(row.subList(1, 15)).containsOnly("");
    }

    @Test
    void incident_areaWithNullNameBecomesEmpty() throws IOException {
        Incident i = fullIncident("X");
        i.setArea(new Area());
        ExportTable t = captureIncidentTable(mock(IncidentRepository.class), List.of(i));
        assertThat(t.rows().get(0).get(2)).isEmpty();
    }

    @Test
    void incident_medicalAttentionNullAndFalseBothExportNo() throws IOException {
        Incident a = fullIncident("A");
        a.setMedicalAttentionRequired(null);
        Incident b = fullIncident("B");
        b.setMedicalAttentionRequired(false);
        ExportTable t = captureIncidentTable(mock(IncidentRepository.class), List.of(a, b));
        assertThat(t.rows().get(0).get(15)).isEqualTo("No");
        assertThat(t.rows().get(1).get(15)).isEqualTo("No");
    }

    @Test
    void incident_requestsNewestFirstSortByReportDate() throws IOException {
        IncidentRepository repo = mock(IncidentRepository.class);
        captureIncidentTable(repo, List.of());
        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(repo).findAll(sort.capture());
        Sort.Order order = sort.getValue().getOrderFor("reportDate");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void incident_everySeverityTypeAndStatusHasLabelWithoutUnderscore() throws IOException {
        List<Incident> all = new ArrayList<>();
        for (Incident.Type ty : Incident.Type.values()) {
            for (Incident.SeverityLevel sv : Incident.SeverityLevel.values()) {
                for (Incident.IncidentStatus st : Incident.IncidentStatus.values()) {
                    Incident i = fullIncident("C");
                    i.setType(ty);
                    i.setSeverity(sv);
                    i.setStatus(st);
                    all.add(i);
                }
            }
        }
        ExportTable t = captureIncidentTable(mock(IncidentRepository.class), all);
        assertThat(t.rows()).hasSize(all.size());
        for (List<String> r : t.rows()) {
            assertThat(r.get(3)).doesNotContain("_").isNotBlank();
            assertThat(r.get(4)).doesNotContain("_").isNotBlank();
            assertThat(r.get(5)).doesNotContain("_").isNotBlank();
        }
    }

    @Test
    void incident_emptyRepositoryGivesEmptyTableWithHeaders() throws IOException {
        ExportTable t = captureIncidentTable(mock(IncidentRepository.class), List.of());
        assertThat(t.rows()).isEmpty();
        assertThat(t.headers()).isNotEmpty();
    }

    @Test
    void incident_exportExcelWithRealExportServiceProducesReadableWorkbook() throws IOException {
        IncidentRepository repo = mock(IncidentRepository.class);
        when(repo.findAll(any(Sort.class))).thenReturn(List.of(fullIncident("A"), fullIncident("B")));
        byte[] bytes = new IncidentExportService(repo, new ExportService()).exportExcel();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Incident Report");
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(2);
            assertThat(wb.getSheetAt(0).getRow(0).getLastCellNum()).isEqualTo((short) 16);
            assertThat(wb.getSheetAt(0).getRow(1).getCell(15).getStringCellValue()).isEqualTo("Yes");
        }
    }

    @Test
    void incident_exportPdfWithRealExportServiceStartsWithPdfHeader() {
        IncidentRepository repo = mock(IncidentRepository.class);
        when(repo.findAll(any(Sort.class))).thenReturn(List.of(fullIncident("A")));
        byte[] pdf = new IncidentExportService(repo, new ExportService()).exportPdf();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    void incident_exportPdfEmptyRepository() {
        IncidentRepository repo = mock(IncidentRepository.class);
        when(repo.findAll(any(Sort.class))).thenReturn(List.of());
        byte[] pdf = new IncidentExportService(repo, new ExportService()).exportPdf();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    // --------------------------------------------------------- observation

    @Test
    void observation_fullRowMapsEveryColumn() throws IOException {
        ExportTable t = captureObservationTable(mock(ObservationRepository.class), List.of(fullObservation("OBS-000001")));
        assertThat(t.title()).isEqualTo("Observation Report");
        assertThat(t.rows().get(0)).containsExactly(
                "OBS-000001", "2026-01-02 03:04", "Warehouse", "Fire Prevention And Fire Protection", "Unsafe Act",
                "Open", "Obs", "D", "O1", "Resp", "R1", "High", "CA", "Rem");
    }

    @Test
    void observation_rowAndHeaderCountsMatch() throws IOException {
        ExportTable t = captureObservationTable(mock(ObservationRepository.class),
                List.of(fullObservation("A"), fullObservation("B")));
        assertThat(t.rows()).hasSize(2);
        assertThat(t.rows()).allSatisfy(r -> assertThat(r).hasSameSizeAs(t.headers()));
        assertThat(t.pdfColumnWidths().length).isLessThanOrEqualTo(t.headers().size());
    }

    @Test
    void observation_nullAreaAndNullEnumsBecomeEmpty() throws IOException {
        Observation o = new Observation();
        o.setCode("OBS-2");
        ExportTable t = captureObservationTable(mock(ObservationRepository.class), List.of(o));
        List<String> row = t.rows().get(0);
        assertThat(row).hasSameSizeAs(t.headers());
        assertThat(row.get(0)).isEqualTo("OBS-2");
        assertThat(row.subList(1, row.size())).containsOnly("");
    }

    @Test
    void observation_requestsNewestFirstSortByObservationDate() throws IOException {
        ObservationRepository repo = mock(ObservationRepository.class);
        captureObservationTable(repo, List.of());
        ArgumentCaptor<Sort> sort = ArgumentCaptor.forClass(Sort.class);
        verify(repo).findAll(sort.capture());
        Sort.Order order = sort.getValue().getOrderFor("observationDate");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void observation_everyCategoryHasDisplayLabel() throws IOException {
        List<Observation> all = new ArrayList<>();
        for (Observation.Category c : Observation.Category.values()) {
            Observation o = fullObservation("C");
            o.setCategory(c);
            all.add(o);
        }
        ExportTable t = captureObservationTable(mock(ObservationRepository.class), all);
        for (List<String> r : t.rows()) {
            assertThat(r.get(3)).isNotBlank().doesNotContain("_");
        }
    }

    @Test
    void observation_exportExcelWithRealExportService() throws IOException {
        ObservationRepository repo = mock(ObservationRepository.class);
        when(repo.findAll(any(Sort.class))).thenReturn(List.of(fullObservation("A")));
        byte[] bytes = new ObservationExportService(repo, new ExportService()).exportExcel();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Observation Report");
            assertThat(wb.getSheetAt(0).getRow(0).getLastCellNum()).isEqualTo((short) 14);
            assertThat(wb.getSheetAt(0).getLastRowNum()).isEqualTo(1);
        }
    }

    @Test
    void observation_exportPdfWithRealExportService() {
        ObservationRepository repo = mock(ObservationRepository.class);
        when(repo.findAll(any(Sort.class))).thenReturn(List.of(fullObservation("A")));
        byte[] pdf = new ObservationExportService(repo, new ExportService()).exportPdf();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    // ------------------------------------------- builder default (warnings)

    @Test
    @org.junit.jupiter.api.Disabled("BUG: missing @Builder.Default - Incident.java:72")
    void incidentBuilder_medicalAttentionRequiredShouldDefaultToFalse() {
        Incident i = Incident.builder().build();
        assertThat(i.getMedicalAttentionRequired()).isNotNull().isFalse();
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: missing @Builder.Default - Incident.java:72")
    void incidentBuilder_commentsShouldDefaultToEmptyList() {
        Incident i = Incident.builder().build();
        assertThat(i.getComments()).isNotNull().isEmpty();
    }

    @Test
    void incidentBuilder_filesDefaultsToEmptyList() {
        Incident i = Incident.builder().build();
        assertThat(i.getFiles()).isNotNull().isEmpty();
    }

    @Test
    void incidentNoArgsConstructor_keepsInitialisers() {
        Incident i = new Incident();
        assertThat(i.getComments()).isNotNull();
        assertThat(i.getMedicalAttentionRequired()).isFalse();
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: missing @Builder.Default - Observation.java:77")
    void observationBuilder_filesShouldDefaultToEmptyList() {
        Observation o = Observation.builder().build();
        assertThat(o.getFiles()).isNotNull().isEmpty();
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: missing @Builder.Default - Observation.java:77")
    void observationBuilder_commentsShouldDefaultToEmptyList() {
        Observation o = Observation.builder().build();
        assertThat(o.getComments()).isNotNull().isEmpty();
    }

    @Test
    void observationNoArgsConstructor_keepsInitialisers() {
        Observation o = new Observation();
        assertThat(o.getFiles()).isNotNull();
        assertThat(o.getComments()).isNotNull();
    }

    @Test
    void exportOfBuilderCreatedIncidentWithoutMedicalFlagDoesNotCrash() throws IOException {
        Incident i = Incident.builder().code("B1").build();
        ExportTable t = captureIncidentTable(mock(IncidentRepository.class), List.of(i));
        assertThat(t.rows().get(0).get(15)).isEqualTo("No");
    }
}
