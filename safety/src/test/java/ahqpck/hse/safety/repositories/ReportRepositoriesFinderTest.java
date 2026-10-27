package ahqpck.hse.safety.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.entity.ToolboxMeeting;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.repository.IncidentRepository;
import ahqpck.hse.safety.repository.ObservationRepository;
import ahqpck.hse.safety.repository.ToolboxMeetingRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("h2test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReportRepositoriesFinderTest {

    @Autowired private IncidentRepository incidentRepo;
    @Autowired private ObservationRepository observationRepo;
    @Autowired private ToolboxMeetingRepository toolboxRepo;
    @Autowired private AreaRepository areaRepo;

    private Incident incident(String code, Incident.IncidentStatus status, Incident.Type type,
                              Incident.SeverityLevel sev, LocalDateTime date) {
        return incidentRepo.saveAndFlush(Incident.builder()
                .code(code).reportDate(date).description("d").severity(sev)
                .status(status).type(type).reportedBy("tester").build());
    }

    private Area area() {
        return areaRepo.saveAndFlush(Area.builder()
                .code("AR-" + System.nanoTime()).name("Plant").status(Area.AreaStatus.ACTIVE).build());
    }

    private Observation observation(String code, Observation.Status status, Area area, LocalDateTime date) {
        return observationRepo.saveAndFlush(Observation.builder()
                .code(code).observationDate(date).description("d")
                .category(Observation.Category.PPE).type(Observation.Type.UNSAFE_ACT)
                .status(status).mitigation(Observation.Mitigation.HIGH).area(area).build());
    }

    private ToolboxMeeting meeting(String code, ToolboxMeeting.MeetingStatus status, LocalDate date, String supervisorId) {
        return toolboxRepo.saveAndFlush(ToolboxMeeting.builder()
                .code(code).location("Yard").meetingDate(date).timeStarted(LocalTime.of(8, 0))
                .status(status).supervisorEmployeeId(supervisorId).build());
    }

    // ---- Incident ----

    @Test
    void incident_findByCode_hitAndMiss() {
        incident("INC-000001", Incident.IncidentStatus.REPORTED, Incident.Type.ACCIDENT,
                Incident.SeverityLevel.HIGH, LocalDateTime.of(2025, 3, 1, 10, 0));
        assertThat(incidentRepo.findByCode("INC-000001")).isNotNull();
        assertThat(incidentRepo.findByCode("INC-000002")).isNull();
    }

    @Test
    void incident_findByCodeWithFiles_returnsIncidentWithoutFiles() {
        incident("INC-000001", Incident.IncidentStatus.REPORTED, Incident.Type.ACCIDENT,
                Incident.SeverityLevel.HIGH, LocalDateTime.of(2025, 3, 1, 10, 0));
        assertThat(incidentRepo.findByCodeWithFiles("INC-000001")).isNotNull();
        assertThat(incidentRepo.findByCodeWithFiles("nope")).isNull();
    }

    @Test
    void incident_countByStatus() {
        assertThat(incidentRepo.countByStatus(Incident.IncidentStatus.CLOSED)).isZero();
        incident("INC-000001", Incident.IncidentStatus.CLOSED, Incident.Type.ACCIDENT, Incident.SeverityLevel.LOW, LocalDateTime.of(2025, 1, 1, 0, 0));
        incident("INC-000002", Incident.IncidentStatus.CLOSED, Incident.Type.NEAR_MISS, Incident.SeverityLevel.LOW, LocalDateTime.of(2025, 1, 2, 0, 0));
        incident("INC-000003", Incident.IncidentStatus.REPORTED, Incident.Type.NEAR_MISS, Incident.SeverityLevel.LOW, LocalDateTime.of(2025, 1, 3, 0, 0));
        assertThat(incidentRepo.countByStatus(Incident.IncidentStatus.CLOSED)).isEqualTo(2);
        assertThat(incidentRepo.countByStatus(Incident.IncidentStatus.REPORTED)).isEqualTo(1);
        assertThat(incidentRepo.countByStatus(Incident.IncidentStatus.RESOLVED)).isZero();
    }

    @Test
    void incident_statusSeverityTypeFinders_andPagedVariants() {
        incident("INC-000001", Incident.IncidentStatus.REPORTED, Incident.Type.ACCIDENT, Incident.SeverityLevel.CRITICAL, LocalDateTime.of(2025, 1, 1, 0, 0));
        incident("INC-000002", Incident.IncidentStatus.REPORTED, Incident.Type.NEAR_MISS, Incident.SeverityLevel.LOW, LocalDateTime.of(2025, 1, 2, 0, 0));
        incident("INC-000003", Incident.IncidentStatus.CLOSED, Incident.Type.NEAR_MISS, Incident.SeverityLevel.LOW, LocalDateTime.of(2025, 1, 3, 0, 0));

        assertThat(incidentRepo.findByStatus(Incident.IncidentStatus.REPORTED)).hasSize(2);
        assertThat(incidentRepo.findBySeverity(Incident.SeverityLevel.LOW)).hasSize(2);
        assertThat(incidentRepo.findByType(Incident.Type.NEAR_MISS)).hasSize(2);
        assertThat(incidentRepo.findByType(Incident.Type.NEAR_MISS, PageRequest.of(0, 1)).getTotalElements()).isEqualTo(2);
        assertThat(incidentRepo.findByType(Incident.Type.NEAR_MISS, PageRequest.of(0, 1)).getContent()).hasSize(1);
        assertThat(incidentRepo.findByStatus(Incident.IncidentStatus.CLOSED, PageRequest.of(0, 10)).getContent()).hasSize(1);
        assertThat(incidentRepo.findBySeverity(Incident.SeverityLevel.CRITICAL, PageRequest.of(0, 10)).getContent()).hasSize(1);
    }

    @Test
    void incident_countByYearAndType_groupsByMonthAndFiltersYearAndType() {
        incident("INC-000001", Incident.IncidentStatus.REPORTED, Incident.Type.ACCIDENT, Incident.SeverityLevel.LOW, LocalDateTime.of(2025, 3, 5, 9, 0));
        incident("INC-000002", Incident.IncidentStatus.REPORTED, Incident.Type.ACCIDENT, Incident.SeverityLevel.LOW, LocalDateTime.of(2025, 3, 20, 9, 0));
        incident("INC-000003", Incident.IncidentStatus.REPORTED, Incident.Type.ACCIDENT, Incident.SeverityLevel.LOW, LocalDateTime.of(2025, 7, 1, 9, 0));
        incident("INC-000004", Incident.IncidentStatus.REPORTED, Incident.Type.ACCIDENT, Incident.SeverityLevel.LOW, LocalDateTime.of(2024, 3, 1, 9, 0));  // other year
        incident("INC-000005", Incident.IncidentStatus.REPORTED, Incident.Type.NEAR_MISS, Incident.SeverityLevel.LOW, LocalDateTime.of(2025, 3, 1, 9, 0));  // other type

        List<Object[]> rows = incidentRepo.countByYearAndType(2025, Incident.Type.ACCIDENT);
        assertThat(rows).hasSize(2);
        for (Object[] r : rows) {
            int month = ((Number) r[0]).intValue();
            long count = ((Number) r[1]).longValue();
            if (month == 3) assertThat(count).isEqualTo(2);
            else if (month == 7) assertThat(count).isEqualTo(1);
            else throw new AssertionError("unexpected month " + month);
        }
        assertThat(incidentRepo.countByYearAndType(2023, Incident.Type.ACCIDENT)).isEmpty();
    }

    // ---- Observation ----

    @Test
    void observation_findByCode_hitAndMiss() {
        observation("OBS-000001", Observation.Status.OPEN, area(), LocalDateTime.of(2025, 2, 1, 8, 0));
        assertThat(observationRepo.findByCode("OBS-000001")).isNotNull();
        assertThat(observationRepo.findByCode("OBS-000009")).isNull();
        assertThat(observationRepo.findByCodeWithFiles("OBS-000001")).isNotNull();
    }

    @Test
    void observation_countByStatus() {
        Area a = area();
        observation("OBS-000001", Observation.Status.OPEN, a, LocalDateTime.of(2025, 2, 1, 8, 0));
        observation("OBS-000002", Observation.Status.OPEN, a, LocalDateTime.of(2025, 2, 2, 8, 0));
        observation("OBS-000003", Observation.Status.CLOSED, a, LocalDateTime.of(2025, 2, 3, 8, 0));
        assertThat(observationRepo.countByStatus(Observation.Status.OPEN)).isEqualTo(2);
        assertThat(observationRepo.countByStatus(Observation.Status.CLOSED)).isEqualTo(1);
        assertThat(observationRepo.countByStatus(Observation.Status.MITIGATED)).isZero();
    }

    @Test
    void observation_findByStatus_andType() {
        Area a = area();
        observation("OBS-000001", Observation.Status.OPEN, a, LocalDateTime.of(2025, 2, 1, 8, 0));
        observation("OBS-000002", Observation.Status.CLOSED, a, LocalDateTime.of(2025, 2, 2, 8, 0));
        assertThat(observationRepo.findByStatus(Observation.Status.OPEN)).hasSize(1);
        assertThat(observationRepo.findByStatus(Observation.Status.OPEN, PageRequest.of(0, 5)).getTotalElements()).isEqualTo(1);
        assertThat(observationRepo.findByType(Observation.Type.UNSAFE_ACT)).hasSize(2);
        assertThat(observationRepo.findByType(Observation.Type.NEAR_MISS)).isEmpty();
    }

    /**
     * ObservationRepository declares findByCategory(String) / findByMitigation(String) although
     * Observation.category and Observation.mitigation are enums. A valid enum name passed as a String
     * should still return rows; this checks whether the derived query really works.
     */
    @Test
    @org.junit.jupiter.api.Disabled("BUG: derived finder takes String for enum property - ObservationRepository.java:27")
    void observation_findByCategoryString_returnsMatchingRows() {
        observation("OBS-000001", Observation.Status.OPEN, area(), LocalDateTime.of(2025, 2, 1, 8, 0));
        assertThat(observationRepo.findByCategory("PPE")).hasSize(1);
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: derived finder takes String for enum property - ObservationRepository.java:27")
    void observation_findByMitigationString_returnsMatchingRows() {
        observation("OBS-000001", Observation.Status.OPEN, area(), LocalDateTime.of(2025, 2, 1, 8, 0));
        assertThat(observationRepo.findByMitigation("HIGH")).hasSize(1);
    }

    @Test
    void observation_countByYearAndStatus() {
        Area a = area();
        observation("OBS-000001", Observation.Status.OPEN, a, LocalDateTime.of(2025, 5, 1, 8, 0));
        observation("OBS-000002", Observation.Status.OPEN, a, LocalDateTime.of(2025, 5, 9, 8, 0));
        observation("OBS-000003", Observation.Status.OPEN, a, LocalDateTime.of(2024, 5, 9, 8, 0));
        observation("OBS-000004", Observation.Status.CLOSED, a, LocalDateTime.of(2025, 6, 9, 8, 0));

        List<Object[]> rows = observationRepo.countByYearAndStatus(2025, Observation.Status.OPEN);
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0)[0]).intValue()).isEqualTo(5);
        assertThat(((Number) rows.get(0)[1]).longValue()).isEqualTo(2);
    }

    // ---- Toolbox ----

    @Test
    void toolbox_findByCode_andExistsByCode() {
        meeting("TBM-000001", ToolboxMeeting.MeetingStatus.COMPLETED, LocalDate.of(2025, 4, 1), "E100");
        assertThat(toolboxRepo.findByCode("TBM-000001")).isPresent();
        assertThat(toolboxRepo.findByCode("TBM-000002")).isEmpty();
        assertThat(toolboxRepo.existsByCode("TBM-000001")).isTrue();
        assertThat(toolboxRepo.existsByCode("TBM-000002")).isFalse();
    }

    @Test
    void toolbox_findByStatus() {
        meeting("TBM-000001", ToolboxMeeting.MeetingStatus.COMPLETED, LocalDate.of(2025, 4, 1), "E100");
        meeting("TBM-000002", ToolboxMeeting.MeetingStatus.DRAFT, LocalDate.of(2025, 4, 2), "E100");
        assertThat(toolboxRepo.findByStatus(ToolboxMeeting.MeetingStatus.DRAFT)).hasSize(1);
        assertThat(toolboxRepo.findByStatus(ToolboxMeeting.MeetingStatus.CANCELLED)).isEmpty();
    }

    @Test
    void toolbox_prePersistDefaultsNullStatusToDraft() {
        ToolboxMeeting saved = toolboxRepo.saveAndFlush(ToolboxMeeting.builder()
                .code("TBM-000001").location("Yard").meetingDate(LocalDate.of(2025, 4, 1))
                .timeStarted(LocalTime.of(8, 0)).build());
        assertThat(saved.getStatus()).isEqualTo(ToolboxMeeting.MeetingStatus.DRAFT);
    }

    @Test
    void toolbox_findByMeetingDateBetween_isInclusiveOnBothEnds() {
        meeting("TBM-000001", ToolboxMeeting.MeetingStatus.COMPLETED, LocalDate.of(2025, 4, 1), "E1");
        meeting("TBM-000002", ToolboxMeeting.MeetingStatus.COMPLETED, LocalDate.of(2025, 4, 15), "E1");
        meeting("TBM-000003", ToolboxMeeting.MeetingStatus.COMPLETED, LocalDate.of(2025, 4, 30), "E1");
        meeting("TBM-000004", ToolboxMeeting.MeetingStatus.COMPLETED, LocalDate.of(2025, 5, 1), "E1");
        List<ToolboxMeeting> found = toolboxRepo.findByMeetingDateBetween(LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 30));
        assertThat(found).extracting(ToolboxMeeting::getCode)
                .containsExactlyInAnyOrder("TBM-000001", "TBM-000002", "TBM-000003");
    }

    @Test
    void toolbox_findBySupervisorEmployeeId() {
        meeting("TBM-000001", ToolboxMeeting.MeetingStatus.COMPLETED, LocalDate.of(2025, 4, 1), "E100");
        meeting("TBM-000002", ToolboxMeeting.MeetingStatus.COMPLETED, LocalDate.of(2025, 4, 2), "E200");
        meeting("TBM-000003", ToolboxMeeting.MeetingStatus.COMPLETED, LocalDate.of(2025, 4, 3), null);
        assertThat(toolboxRepo.findBySupervisorEmployeeId("E100")).extracting(ToolboxMeeting::getCode).containsExactly("TBM-000001");
        assertThat(toolboxRepo.findBySupervisorEmployeeId("E999")).isEmpty();
    }
}
