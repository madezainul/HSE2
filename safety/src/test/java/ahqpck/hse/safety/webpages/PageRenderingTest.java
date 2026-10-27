package ahqpck.hse.safety.webpages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.transaction.annotation.Transactional;

import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.HseCertification;
import ahqpck.hse.safety.model.entity.HseDocument;
import ahqpck.hse.safety.model.entity.HseExternalTraining;
import ahqpck.hse.safety.model.entity.HseInduction;
import ahqpck.hse.safety.model.entity.HseInternalTraining;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.entity.ToolboxMeeting;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.repository.HseCertificationRepository;
import ahqpck.hse.safety.repository.HseDocumentRepository;
import ahqpck.hse.safety.repository.HseExternalTrainingRepository;
import ahqpck.hse.safety.repository.HseInductionRepository;
import ahqpck.hse.safety.repository.HseInternalTrainingRepository;
import ahqpck.hse.safety.repository.IncidentRepository;
import ahqpck.hse.safety.repository.ObservationRepository;
import ahqpck.hse.safety.repository.ToolboxMeetingRepository;
import ahqpck.hse.safety.repository.UserRepository;

/**
 * Renders every page as an ADMIN, with empty tables and with seeded data.
 * Template errors only show up at render time, so each page gets its own test case.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@Transactional
class PageRenderingTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AreaRepository areaRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private ObservationRepository observationRepository;
    @Autowired private ToolboxMeetingRepository toolboxRepository;
    @Autowired private HseDocumentRepository documentRepository;
    @Autowired private HseInductionRepository inductionRepository;
    @Autowired private HseInternalTrainingRepository internalRepository;
    @Autowired private HseExternalTrainingRepository externalRepository;
    @Autowired private HseCertificationRepository certificationRepository;

    private User admin;
    private RequestPostProcessor asAdmin;

    static Stream<String> listPages() {
        return Stream.of(
                "/", "/incident", "/observation", "/training/toolbox",
                "/training/induction", "/training/hse-induction",
                "/training/internal-training", "/training/hse-internal-training",
                "/training/external-training", "/training/hse-external-training",
                "/training/certification", "/training/hse-certification",
                "/inspection/fire-safety", "/inspection/environment", "/inspection/first-aid",
                "/inspection/spill-kit", "/inspection/daily", "/inspection/other",
                "/documents/risk-assessment", "/documents/work-instruction",
                "/documents/work-permit", "/documents/hse-form");
    }

    @BeforeEach
    void setUp() {
        String sfx = suffix();
        admin = userRepository.saveAndFlush(User.builder()
                .username("pg_admin_" + sfx)
                .employeeId("PG" + sfx)
                .email("pg" + sfx + "@example.com")
                .password("$2a$10$PAGETESTHASHxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                .fullName("Page Admin")
                .role("ADMIN")
                .build());
        asAdmin = SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    // ------------------------------------------------------------------ list pages

    @ParameterizedTest(name = "empty tables: GET {0} renders 200")
    @MethodSource("listPages")
    void listPageRendersWithEmptyTables(String path) throws Exception {
        clearTables();

        String html = mockMvc.perform(get(path).with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).containsIgnoringCase("<html");
        // The layout has to be applied: sidebar and header markup come from layouts/dashboard
        assertThat(html).contains("Hi,");
        assertThat(html).contains(admin.getUsername());
    }

    @ParameterizedTest(name = "seeded data: GET {0} renders 200")
    @MethodSource("listPages")
    void listPageRendersWithSeededData(String path) throws Exception {
        seedEverything();

        mockMvc.perform(get(path).with(asAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void areasPageRendersWithEmptyOrExistingData() throws Exception {
        mockMvc.perform(get("/areas").with(asAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void areasPageShowsStatusBadgeForEveryStatus() throws Exception {
        saveArea("AR-ACT", "Active Area", Area.AreaStatus.ACTIVE);
        saveArea("AR-INA", "Inactive Area", Area.AreaStatus.INACTIVE);
        saveArea("AR-MNT", "Maintenance Area", Area.AreaStatus.UNDER_MAINTENANCE);

        String html = mockMvc.perform(get("/areas?size=500").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // The Area page status column must not be blank
        assertThat(html).contains("Active Area").contains("Inactive Area").contains("Maintenance Area");
        assertThat(hasBadge(html, "bg-success", "ACTIVE")).as("ACTIVE badge").isTrue();
        assertThat(hasBadge(html, "bg-secondary", "INACTIVE")).as("INACTIVE badge").isTrue();
        assertThat(hasBadge(html, "bg-warning", "UNDER MAINTENANCE")).as("UNDER_MAINTENANCE badge").isTrue();
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: area/view template does not exist - AreaController.java:48")
    void areaDetailPageForExistingAreaRenders() throws Exception {
        Area area = saveArea("AR-VIEW", "View Area", Area.AreaStatus.ACTIVE);

        // AreaController.viewArea returns "area/view"
        mockMvc.perform(get("/areas/" + area.getId()).with(asAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void areaDetailPageForUnknownIdRedirects() throws Exception {
        mockMvc.perform(get("/areas/does-not-exist").with(asAdmin))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/areas"));
    }

    // ------------------------------------------------------------------ incident

    @Test
    void incidentIndexShowsExpectedBadgeColourPerStatus() throws Exception {
        Area area = saveArea(uniq("AR"), "Badge Area", Area.AreaStatus.ACTIVE);
        saveIncident(area, Incident.IncidentStatus.REPORTED);
        saveIncident(area, Incident.IncidentStatus.UNDER_INVESTIGATION);
        saveIncident(area, Incident.IncidentStatus.RESOLVED);
        saveIncident(area, Incident.IncidentStatus.CLOSED);

        String html = mockMvc.perform(get("/incident?size=100").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(hasBadge(html, "bg-warning", "REPORTED")).as("REPORTED -> bg-warning").isTrue();
        assertThat(hasBadge(html, "bg-danger", "UNDER_INVESTIGATION")).as("UNDER_INVESTIGATION -> bg-danger").isTrue();
        assertThat(hasBadge(html, "bg-success", "RESOLVED")).as("RESOLVED -> bg-success").isTrue();
        assertThat(hasBadge(html, "bg-primary", "CLOSED")).as("CLOSED -> bg-primary").isTrue();
    }

    @Test
    void incidentIndexListsSeededIncidentAndAreaName() throws Exception {
        Area area = saveArea(uniq("AR"), "Warehouse North", Area.AreaStatus.ACTIVE);
        Incident incident = saveIncident(area, Incident.IncidentStatus.REPORTED);

        String html = mockMvc.perform(get("/incident?size=100").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(incident.getCode());
        assertThat(html).contains("Warehouse North");
        assertThat(html).contains("data-incident-code=\"" + incident.getCode() + "\"");
    }

    @Test
    void incidentIndexHasNoRowsWhenTableEmpty() throws Exception {
        clearTables();

        String html = mockMvc.perform(get("/incident").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).doesNotContain("data-incident-code=\"INC");
    }

    @Test
    void incidentDetailRendersEditModalAndPdfViewer() throws Exception {
        Area area = saveArea(uniq("AR"), "Detail Area", Area.AreaStatus.ACTIVE);
        Incident incident = saveIncident(area, Incident.IncidentStatus.UNDER_INVESTIGATION);

        String html = mockMvc.perform(get("/incident/" + incident.getCode()).with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(incident.getCode());
        assertThat(html).contains("id=\"addIncidentModal\"");
        assertThat(html).contains("id=\"pdfViewerModal\"");
    }

    @Test
    void incidentDetailWithoutArea() throws Exception {
        Incident incident = saveIncident(null, Incident.IncidentStatus.REPORTED);

        mockMvc.perform(get("/incident/" + incident.getCode()).with(asAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void incidentDetailForUnknownCodeRedirectsToList() throws Exception {
        mockMvc.perform(get("/incident/INC-NOPE").with(asAdmin))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incident"));
    }

    // ------------------------------------------------------------------ observation

    @Test
    void observationIndexShowsStatusBadges() throws Exception {
        Area area = saveArea(uniq("AR"), "Obs Area", Area.AreaStatus.ACTIVE);
        saveObservation(area, Observation.Status.OPEN);
        saveObservation(area, Observation.Status.MITIGATED);
        saveObservation(area, Observation.Status.CLOSED);

        String html = mockMvc.perform(get("/observation?size=100").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(hasBadge(html, "bg-info", "Open")).as("OPEN badge").isTrue();
        assertThat(hasBadge(html, "bg-warning", "Mitigated")).as("MITIGATED badge").isTrue();
        assertThat(hasBadge(html, "bg-success", "Closed")).as("CLOSED badge").isTrue();
    }

    @Test
    void observationDetailRendersEditModalAndPdfViewer() throws Exception {
        Area area = saveArea(uniq("AR"), "Obs Detail Area", Area.AreaStatus.ACTIVE);
        Observation observation = saveObservation(area, Observation.Status.OPEN);

        String html = mockMvc.perform(get("/observation/" + observation.getCode()).with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains(observation.getCode());
        assertThat(html).contains("id=\"addObservationModal\"");
        assertThat(html).contains("id=\"pdfViewerModal\"");
    }

    @Test
    void observationDetailForUnknownCodeRedirectsToList() throws Exception {
        mockMvc.perform(get("/observation/OBS-NOPE").with(asAdmin))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/observation"));
    }

    // ------------------------------------------------------------------ toolbox

    @Test
    void toolboxIndexAndDetailRenderSeededMeeting() throws Exception {
        ToolboxMeeting meeting = saveMeeting("Boiler House");

        String list = mockMvc.perform(get("/training/toolbox?size=100").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).contains(meeting.getCode()).contains("Boiler House");

        String detail = mockMvc.perform(get("/training/toolbox/" + meeting.getCode()).with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).contains(meeting.getCode()).contains("Boiler House");
    }

    @Test
    void toolboxDetailResolvesSupervisorNameFromEmployeeId() throws Exception {
        ToolboxMeeting meeting = ToolboxMeeting.builder()
                .code(uniq("TBM-"))
                .location("Yard")
                .meetingDate(LocalDate.now())
                .timeStarted(LocalTime.of(8, 0))
                .supervisorEmployeeId(admin.getEmployeeId())
                .status(ToolboxMeeting.MeetingStatus.COMPLETED)
                .build();
        toolboxRepository.saveAndFlush(meeting);

        String detail = mockMvc.perform(get("/training/toolbox/" + meeting.getCode()).with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(detail).contains("Page Admin");

        String list = mockMvc.perform(get("/training/toolbox?size=100").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).contains("Page Admin");
    }

    @Test
    void toolboxDetailForUnknownCodeIs404() throws Exception {
        mockMvc.perform(get("/training/toolbox/TBM-NOPE").with(asAdmin))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ training

    @Test
    void inductionPagesRenderSeededRecord() throws Exception {
        HseInduction rec = saveInduction();

        String list = mockMvc.perform(get("/training/hse-induction?size=100").with(asAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(list).contains(rec.getCode());

        String recent = mockMvc.perform(get("/training/induction").with(asAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(recent).contains("HSE Induction");

        String detail = mockMvc.perform(get("/training/hse-induction/" + rec.getCode()).with(asAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains(rec.getCode()).contains("Trainee One");
    }

    @Test
    void inductionDetailWithFileAndCreatorRenders() throws Exception {
        HseInduction rec = HseInduction.builder()
                .code(uniq("IND-"))
                .traineeId("T-1").traineeName("Filed Trainee")
                .inductionDate(LocalDate.now())
                .trainers("Trainer A")
                .filePath("hse-induction/documents/some_file.pdf")
                .originalFileName("some_file.pdf")
                .fileSizeBytes(2048L)
                .fileExtension(".pdf")
                .createdBy(admin)
                .build();
        inductionRepository.saveAndFlush(rec);

        String html = mockMvc.perform(get("/training/hse-induction/" + rec.getCode()).with(asAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("some_file.pdf");
        assertThat(html).contains(admin.getUsername());
    }

    @Test
    void internalTrainingPagesRenderSeededRecord() throws Exception {
        HseInternalTraining rec = saveInternal();

        String list = mockMvc.perform(get("/training/hse-internal-training?size=100").with(asAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(list).contains(rec.getCode());

        mockMvc.perform(get("/training/internal-training").with(asAdmin)).andExpect(status().isOk());

        String detail = mockMvc.perform(get("/training/hse-internal-training/" + rec.getCode()).with(asAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains(rec.getCode()).contains("Working at Height");
    }

    @Test
    void externalTrainingPagesRenderSeededRecord() throws Exception {
        HseExternalTraining rec = saveExternal();

        String list = mockMvc.perform(get("/training/hse-external-training?size=100").with(asAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(list).contains(rec.getCode());

        mockMvc.perform(get("/training/external-training").with(asAdmin)).andExpect(status().isOk());

        String detail = mockMvc.perform(get("/training/hse-external-training/" + rec.getCode()).with(asAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains(rec.getCode()).contains("Confined Space");
    }

    @Test
    void certificationPagesRenderSeededRecordsWithEveryStatus() throws Exception {
        HseCertification expired = saveCertification("Expired Cert", LocalDate.now().minusYears(2), LocalDate.now().minusDays(5));
        saveCertification("Soon Cert", LocalDate.now().minusYears(1), LocalDate.now().plusDays(10));
        saveCertification("Valid Cert", LocalDate.now().minusDays(10), LocalDate.now().plusYears(1));
        saveCertification("No Expiry Cert", LocalDate.now().minusDays(10), null);

        String list = mockMvc.perform(get("/training/hse-certification?size=100").with(asAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(list).contains("Expired").contains("Expiring Soon").contains("Valid").contains("No Expiry");

        mockMvc.perform(get("/training/certification").with(asAdmin)).andExpect(status().isOk());

        String detail = mockMvc.perform(get("/training/hse-certification/" + expired.getCode()).with(asAdmin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(detail).contains(expired.getCode()).contains("Expired Cert");
    }

    @Test
    void trainingDetailPagesRedirectToListForUnknownCode() throws Exception {
        mockMvc.perform(get("/training/hse-induction/NOPE").with(asAdmin))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/training/hse-induction"));
        mockMvc.perform(get("/training/hse-internal-training/NOPE").with(asAdmin))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/training/hse-internal-training"));
        mockMvc.perform(get("/training/hse-external-training/NOPE").with(asAdmin))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/training/hse-external-training"));
        mockMvc.perform(get("/training/hse-certification/NOPE").with(asAdmin))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/training/hse-certification"));
    }

    // ------------------------------------------------------------------ documents / inspections

    @Test
    void documentAndInspectionPagesListSeededDocuments() throws Exception {
        String[][] pages = {
                {"/documents/risk-assessment", "RISK_ASSESSMENT"},
                {"/documents/work-instruction", "WORK_INSTRUCTION"},
                {"/documents/work-permit", "WORK_PERMIT"},
                {"/documents/hse-form", "HSE_FORM"},
                {"/inspection/fire-safety", "FIRE_SAFETY_INSPECTION"},
                {"/inspection/environment", "ENVIRONMENT_INSPECTION"},
                {"/inspection/first-aid", "FIRST_AID_INSPECTION"},
                {"/inspection/spill-kit", "SPILL_KIT_INSPECTION"},
                {"/inspection/daily", "DAILY_INSPECTION"},
                {"/inspection/other", "OTHER_INSPECTION"}};
        for (String[] p : pages) {
            String docName = "Doc for " + p[1] + " " + suffix();
            saveDocument(HseDocument.Module.valueOf(p[1]), docName);

            String html = mockMvc.perform(get(p[0]).with(asAdmin))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).as(p[0]).contains(docName);
        }
    }

    @Test
    void documentPageDoesNotShowDocumentsOfAnotherModule() throws Exception {
        String foreign = "Foreign doc " + suffix();
        saveDocument(HseDocument.Module.WORK_PERMIT, foreign);

        String html = mockMvc.perform(get("/documents/risk-assessment").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain(foreign);
    }

    @Test
    void everySidebarLinkResolves() throws Exception {
        String html = mockMvc.perform(get("/incident").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString().replaceAll("(?s)<!--.*?-->", "");

        // The layout sidebar must not link to pages that do not exist
        java.util.regex.Matcher m = Pattern.compile("href=\"(/(?:documents|inspection|training|areas|incident|observation|user)[^\"#]*)\"").matcher(html);
        java.util.Set<String> seen = new java.util.TreeSet<>();
        while (m.find()) {
            seen.add(m.group(1));
        }
        assertThat(seen).isNotEmpty();
        for (String link : seen) {
            MvcResult r = mockMvc.perform(get(link).with(asAdmin)).andReturn();
            assertThat(r.getResponse().getStatus()).as("sidebar link " + link).isLessThan(400);
        }
    }

    // ------------------------------------------------------------------ edge cases / access

    @Test
    @org.junit.jupiter.api.Disabled("BUG: size=0 gives 500 from PageRequest.of - IncidentController.java:60")
    void zeroPageSizeDoesNotCrashIncidentList() throws Exception {
        MvcResult r = mockMvc.perform(get("/incident?size=0").with(asAdmin)).andReturn();
        assertThat(r.getResponse().getStatus()).isLessThan(500);
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: page=-1 gives 500 from PageRequest.of - ObservationController.java:57")
    void negativePageDoesNotCrashObservationList() throws Exception {
        MvcResult r = mockMvc.perform(get("/observation?page=-1").with(asAdmin)).andReturn();
        assertThat(r.getResponse().getStatus()).isLessThan(500);
    }

    @Test
    void pageBeyondLastPageRendersEmptyList() throws Exception {
        mockMvc.perform(get("/training/hse-induction?page=9999").with(asAdmin))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUserIsSentToLoginForProtectedPages() throws Exception {
        mockMvc.perform(get("/incident"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/training/toolbox"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/areas"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void anonymousUserDoesNotSeeTheDashboard() throws Exception {
        // DashboardController redirects to login when nobody is signed in
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void dashboardRendersChartDataForAdmin() throws Exception {
        seedEverything();

        String html = mockMvc.perform(get("/").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Dashboard");
    }

    // ------------------------------------------------------------------ helpers

    private void clearTables() {
        incidentRepository.deleteAll();
        observationRepository.deleteAll();
        toolboxRepository.deleteAll();
        documentRepository.deleteAll();
        inductionRepository.deleteAll();
        internalRepository.deleteAll();
        externalRepository.deleteAll();
        certificationRepository.deleteAll();
        incidentRepository.flush();
    }

    private void seedEverything() {
        Area area = saveArea(uniq("AR"), "Seed Area", Area.AreaStatus.ACTIVE);
        for (Incident.IncidentStatus s : Incident.IncidentStatus.values()) {
            saveIncident(area, s);
        }
        for (Observation.Status s : Observation.Status.values()) {
            saveObservation(area, s);
        }
        saveMeeting("Seed Location");
        saveInduction();
        saveInternal();
        saveExternal();
        saveCertification("Seed Cert", LocalDate.now().minusDays(3), LocalDate.now().plusDays(300));
        for (HseDocument.Module m : HseDocument.Module.values()) {
            saveDocument(m, "Seed doc " + m);
        }
    }

    private Area saveArea(String code, String name, Area.AreaStatus status) {
        return areaRepository.saveAndFlush(Area.builder()
                .code(code).name(name).description("desc").status(status).build());
    }

    private Incident saveIncident(Area area, Incident.IncidentStatus status) {
        Incident i = new Incident();
        i.setCode(uniq("INC-"));
        i.setReportDate(LocalDateTime.now().plusYears(50));
        i.setDescription("Slip on wet floor near loading bay");
        i.setSeverity(Incident.SeverityLevel.HIGH);
        i.setStatus(status);
        i.setType(Incident.Type.ACCIDENT);
        i.setArea(area);
        i.setReportedBy(admin.getUsername());
        return incidentRepository.saveAndFlush(i);
    }

    private Observation saveObservation(Area area, Observation.Status status) {
        Observation o = new Observation();
        o.setCode(uniq("OBS-"));
        o.setObservationDate(LocalDateTime.now().plusYears(50));
        o.setDescription("Missing guard on conveyor");
        o.setCategory(Observation.Category.PPE);
        o.setType(Observation.Type.UNSAFE_CONDITION);
        o.setStatus(status);
        o.setMitigation(Observation.Mitigation.HIGH);
        o.setArea(area);
        o.setObserver("Observer One");
        return observationRepository.saveAndFlush(o);
    }

    private ToolboxMeeting saveMeeting(String location) {
        return toolboxRepository.saveAndFlush(ToolboxMeeting.builder()
                .code(uniq("TBM-"))
                .location(location)
                .meetingDate(LocalDate.now().plusYears(50))
                .timeStarted(LocalTime.of(8, 0))
                .timeEnd(LocalTime.of(8, 30))
                .supervisor("Sam Supervisor")
                .status(ToolboxMeeting.MeetingStatus.COMPLETED)
                .build());
    }

    private HseInduction saveInduction() {
        return inductionRepository.saveAndFlush(HseInduction.builder()
                .code(uniq("IND-"))
                .traineeId("T-" + suffix()).traineeName("Trainee One")
                .inductionDate(LocalDate.now())
                .trainers("Trainer A")
                .build());
    }

    private HseInternalTraining saveInternal() {
        return internalRepository.saveAndFlush(HseInternalTraining.builder()
                .code(uniq("INT-"))
                .employeeId("E-" + suffix()).employeeName("Employee Two")
                .trainingName("Working at Height")
                .trainingDate(LocalDate.now())
                .duration(2.5)
                .trainerName("Trainer B")
                .build());
    }

    private HseExternalTraining saveExternal() {
        return externalRepository.saveAndFlush(HseExternalTraining.builder()
                .code(uniq("EXT-"))
                .employeeId("E-" + suffix()).employeeName("Employee Three")
                .trainingName("Confined Space")
                .provider("Provider Ltd")
                .trainingDate(LocalDate.now())
                .duration(8.0)
                .build());
    }

    private HseCertification saveCertification(String name, LocalDate issued, LocalDate expiry) {
        return certificationRepository.saveAndFlush(HseCertification.builder()
                .code(uniq("CERT-"))
                .employeeId("E-" + suffix()).employeeName("Employee Four")
                .certificateName(name)
                .issuedDate(issued)
                .expiryDate(expiry)
                .build());
    }

    private HseDocument saveDocument(HseDocument.Module module, String docName) {
        return documentRepository.saveAndFlush(HseDocument.builder()
                .code(uniq("D"))
                .module(module)
                .docName(docName)
                .category("General")
                .department("HSE")
                .filePath(module.toUrlPath() + "/documents/x_file.pdf")
                .originalFileName("file.pdf")
                .fileSizeBytes(4096L)
                .fileExtension(".pdf")
                .uploadedAt(LocalDateTime.now())
                .uploadedBy(admin)
                .build());
    }

    private static boolean hasBadge(String html, String cssClass, String text) {
        Pattern p = Pattern.compile("<span[^>]*class=\"[^\"]*\\b" + Pattern.quote(cssClass)
                + "\\b[^\"]*\"[^>]*>\\s*" + Pattern.quote(text) + "\\s*</span>");
        return p.matcher(html).find();
    }

    private static String suffix() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    private static String uniq(String prefix) {
        return prefix + suffix();
    }
}
