package ahqpck.hse.safety.incidentlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import ahqpck.hse.safety.model.dto.IncidentSearchCriteria;
import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.repository.IncidentRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.service.IncidentService;

/**
 * The Incident list: filters, sort, real pagination and the export that follows them.
 * Runs on the in-memory database (profile "h2test"); every test rolls back.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@Transactional
class IncidentListFilterSortTest {

    private static final Pattern ROW_CODE = Pattern.compile("data-incident-code=\"([^\"]+)\"");

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AreaRepository areaRepository;
    @Autowired private IncidentRepository incidentRepository;

    private RequestPostProcessor asAdmin;

    @BeforeEach
    void seed() {
        long n = ThreadLocalRandom.current().nextLong(100000, 999999);
        User admin = userRepository.saveAndFlush(User.builder()
                .username("il_admin_" + n).employeeId("IL" + n).email("il" + n + "@example.com")
                .password("$2a$10$LISTTESTHASHxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                .fullName("List Admin").role("ADMIN").build());
        asAdmin = SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));

        incidentRepository.deleteAll();
        areaRepository.deleteAll();
        incidentRepository.flush();

        Area plant = area("PLANT-" + n, "Plant A");
        Area warehouse = area("WH-" + n, "Warehouse B");

        incident("INC-T01", LocalDateTime.of(2026, 3, 10, 23, 30), plant, Incident.Type.ACCIDENT,
                Incident.SeverityLevel.CRITICAL, Incident.IncidentStatus.REPORTED, "Forklift hit the rack", "Budi");
        incident("INC-T02", LocalDateTime.of(2026, 3, 11, 8, 0), warehouse, Incident.Type.NEAR_MISS,
                Incident.SeverityLevel.LOW, Incident.IncidentStatus.CLOSED, "Wet floor near the dock", null);
        incident("INC-T03", LocalDateTime.of(2026, 3, 12, 10, 0), plant, Incident.Type.UNSAFE_ACT,
                Incident.SeverityLevel.HIGH, Incident.IncidentStatus.UNDER_INVESTIGATION, "Worker without helmet", null);
        incident("INC-T04", LocalDateTime.of(2026, 3, 13, 9, 0), warehouse, Incident.Type.ACCIDENT,
                Incident.SeverityLevel.MEDIUM, Incident.IncidentStatus.RESOLVED, "Battery at 100% charge a_b", null);
        incident("INC-T05", LocalDateTime.of(2026, 3, 14, 15, 0), plant, Incident.Type.UNSAFE_CONDITION,
                Incident.SeverityLevel.LOW, Incident.IncidentStatus.CLOSED, "Cable across the floor", null);
        incidentRepository.flush();
    }

    // ------------------------------------------------------------------ helpers

    private Area area(String code, String name) {
        return areaRepository.saveAndFlush(Area.builder().code(code).name(name).status(Area.AreaStatus.ACTIVE).build());
    }

    private void incident(String code, LocalDateTime date, Area area, Incident.Type type,
                          Incident.SeverityLevel severity, Incident.IncidentStatus status, String description, String involved) {
        incidentRepository.save(Incident.builder()
                .code(code).reportDate(date).area(area).type(type).severity(severity).status(status)
                .description(description).reportedBy("tester").involvedPersonName(involved).build());
    }

    private String page(String query) throws Exception {
        return mockMvc.perform(get("/incident" + query).with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String pageParams(String... pairs) throws Exception {
        var request = get("/incident").with(asAdmin);
        for (int i = 0; i < pairs.length; i += 2) {
            request = request.param(pairs[i], pairs[i + 1]);
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static List<String> codes(String html) {
        List<String> found = new ArrayList<>();
        Matcher m = ROW_CODE.matcher(html);
        while (m.find()) found.add(m.group(1));
        return found;
    }

    private String areaCodeOf(String name) {
        return areaRepository.findAll().stream().filter(a -> name.equals(a.getName())).findFirst().orElseThrow().getCode();
    }

    // ------------------------------------------------------------------ default list

    @Test
    void defaultListShowsEverythingNewestFirst() throws Exception {
        assertThat(codes(page(""))).containsExactly("INC-T05", "INC-T04", "INC-T03", "INC-T02", "INC-T01");
    }

    @Test
    void emptyResultShowsTheNoIncidentsMessage() throws Exception {
        String html = page("?keyword=nothing-matches-this");
        assertThat(codes(html)).isEmpty();
        assertThat(html).contains("No incidents found.");
    }

    // ------------------------------------------------------------------ filters

    @Test
    void keywordSearchesDescriptionAreaAndInvolvedPersonIgnoringCase() throws Exception {
        assertThat(codes(page("?keyword=FORKLIFT"))).containsExactly("INC-T01");
        assertThat(codes(page("?keyword=warehouse"))).containsExactlyInAnyOrder("INC-T02", "INC-T04");
        assertThat(codes(page("?keyword=budi"))).containsExactly("INC-T01");
        assertThat(codes(page("?keyword=inc-t03"))).containsExactly("INC-T03");
    }

    @Test
    void percentAndUnderscoreInTheSearchAreOrdinaryCharacters() throws Exception {
        // sent as real parameters: putting %25 in the URL string would be encoded a second time by MockMvc
        assertThat(codes(pageParams("keyword", "100%"))).containsExactly("INC-T04");
        assertThat(codes(pageParams("keyword", "a_b"))).containsExactly("INC-T04");
        assertThat(codes(pageParams("keyword", "%"))).containsExactly("INC-T04");   // a lone % must not match everything
        assertThat(codes(pageParams("keyword", "_"))).containsExactly("INC-T04");
    }

    @Test
    void statusTypeAndSeverityFilters() throws Exception {
        assertThat(codes(page("?status=CLOSED"))).containsExactlyInAnyOrder("INC-T02", "INC-T05");
        assertThat(codes(page("?type=ACCIDENT"))).containsExactlyInAnyOrder("INC-T01", "INC-T04");
        assertThat(codes(page("?severity=LOW"))).containsExactlyInAnyOrder("INC-T02", "INC-T05");
        assertThat(codes(page("?status=closed&severity=low&type=near_miss"))).containsExactly("INC-T02");
    }

    @Test
    void areaFilter() throws Exception {
        assertThat(codes(page("?areaCode=" + areaCodeOf("Plant A"))))
                .containsExactlyInAnyOrder("INC-T01", "INC-T03", "INC-T05");
    }

    @Test
    void dateRangeIncludesTheWholeEndDay() throws Exception {
        // INC-T01 was reported at 23:30 on 10 March: it must be inside "to 2026-03-10"
        assertThat(codes(page("?reportDateFrom=2026-03-10&reportDateTo=2026-03-10"))).containsExactly("INC-T01");
        assertThat(codes(page("?reportDateFrom=2026-03-11&reportDateTo=2026-03-12")))
                .containsExactlyInAnyOrder("INC-T02", "INC-T03");
        assertThat(codes(page("?reportDateFrom=2026-03-14"))).containsExactly("INC-T05");
        assertThat(codes(page("?reportDateTo=2026-03-10"))).containsExactly("INC-T01");
    }

    @Test
    void filtersCombine() throws Exception {
        assertThat(codes(page("?type=ACCIDENT&areaCode=" + areaCodeOf("Warehouse B")))).containsExactly("INC-T04");
    }

    @Test
    void badFilterValuesAreIgnoredInsteadOfFailing() throws Exception {
        assertThat(codes(pageParams("status", "NOT_A_STATUS", "type", "", "severity", "<b>",
                "reportDateFrom", "abc", "reportDateTo", "2026-13-45"))).hasSize(5);
    }

    @Test
    void filterFormKeepsTheChosenValues() throws Exception {
        String html = page("?keyword=forklift&status=CLOSED&reportDateFrom=2026-03-01");
        assertThat(html).contains("value=\"forklift\"");
        assertThat(html).contains("value=\"2026-03-01\"");
        assertThat(html).containsPattern("<option value=\"CLOSED\"[^>]*selected");
    }

    // ------------------------------------------------------------------ sorting

    @Test
    void sortByCodeAscendingAndDescending() throws Exception {
        assertThat(codes(page("?sortBy=code&asc=true"))).containsExactly("INC-T01", "INC-T02", "INC-T03", "INC-T04", "INC-T05");
        assertThat(codes(page("?sortBy=code&asc=false"))).containsExactly("INC-T05", "INC-T04", "INC-T03", "INC-T02", "INC-T01");
    }

    @Test
    void sortByAreaNameThenStableOrder() throws Exception {
        List<String> byArea = codes(page("?sortBy=area&asc=true"));
        // "Plant A" rows first, then "Warehouse B" rows
        assertThat(byArea.subList(0, 3)).containsExactlyInAnyOrder("INC-T01", "INC-T03", "INC-T05");
        assertThat(byArea.subList(3, 5)).containsExactlyInAnyOrder("INC-T02", "INC-T04");
    }

    @Test
    void sortByStatusAndKeepsFilters() throws Exception {
        assertThat(codes(page("?sortBy=code&asc=true&type=ACCIDENT"))).containsExactly("INC-T01", "INC-T04");
    }

    @Test
    void unknownSortColumnFallsBackToNewestFirst() throws Exception {
        assertThat(codes(page("?sortBy=password&asc=true"))).containsExactly("INC-T05", "INC-T04", "INC-T03", "INC-T02", "INC-T01");
        assertThat(codes(pageParams("sortBy", "code; drop table incidents", "asc", "true")))
                .containsExactly("INC-T05", "INC-T04", "INC-T03", "INC-T02", "INC-T01");
    }

    @Test
    void sortChoiceIsShownInTheSortMenu() throws Exception {
        String html = page("?sortBy=severity&asc=true");
        assertThat(html).containsPattern("id=\"sortByInput\"[^>]*value=\"severity\"|value=\"severity\"[^>]*id=\"sortByInput\"");
        assertThat(html).containsPattern("id=\"ascInput\"[^>]*value=\"true\"|value=\"true\"[^>]*id=\"ascInput\"");
    }

    // ------------------------------------------------------------------ paging

    @Test
    void realPaginationWalksThroughThePages() throws Exception {
        assertThat(codes(page("?size=2&page=0"))).containsExactly("INC-T05", "INC-T04");
        assertThat(codes(page("?size=2&page=1"))).containsExactly("INC-T03", "INC-T02");
        assertThat(codes(page("?size=2&page=2"))).containsExactly("INC-T01");
        assertThat(codes(page("?size=2&page=9"))).isEmpty();
    }

    @Test
    void pageLinksKeepFiltersSortAndSize() throws Exception {
        String html = page("?size=1&keyword=o&status=&type=ACCIDENT&sortBy=code&asc=true");
        // two accidents, one per page: there must be a Next link carrying everything
        assertThat(html).containsPattern("href=\"[^\"]*page=1[^\"]*\"");
        assertThat(html).containsPattern("href=\"[^\"]*keyword=o[^\"]*\"");
        assertThat(html).containsPattern("href=\"[^\"]*type=ACCIDENT[^\"]*\"");
        assertThat(html).containsPattern("href=\"[^\"]*sortBy=code[^\"]*\"");
        assertThat(html).containsPattern("href=\"[^\"]*asc=true[^\"]*\"");
        assertThat(html).containsPattern("href=\"[^\"]*size=1[^\"]*\"");
    }

    @Test
    void absurdPagingValuesDoNotCauseAServerError() throws Exception {
        assertThat(codes(page("?size=0"))).hasSize(5);
        assertThat(codes(page("?size=-5"))).hasSize(5);
        assertThat(codes(page("?page=-1"))).hasSize(5);
        assertThat(codes(page("?size=100000"))).hasSize(5);
    }

    // ------------------------------------------------------------------ table and column list stay in step

    @Test
    void everyColumnIsSortableAndEveryRowHasAMatchingCell() throws Exception {
        String html = page("");
        int headers = count(html, "data-sort=\"");
        assertThat(headers).isEqualTo(15);                                   // incident-list.js COLUMN_SETTINGS has 15 entries
        assertThat(html).contains("id=\"incident-table\"");
        assertThat(html).contains("id=\"column-toggle-list\"");

        // each seeded row has one data-field cell per column
        String firstRow = html.substring(html.indexOf("data-incident-code=\"INC-T05\""));
        firstRow = firstRow.substring(0, firstRow.indexOf("</tr>"));
        assertThat(count(firstRow, "data-field=\"")).isEqualTo(15);
    }

    @Test
    void sortableColumnsMatchWhatTheServerAccepts() throws Exception {
        // every sort name in incident-list.js must change the order or at least be accepted (200)
        for (String field : List.of("code", "reportDate", "area", "description", "type", "severity", "status",
                "reportedBy", "involvedPersonName", "createdAt", "updatedAt")) {
            assertThat(codes(page("?sortBy=" + field + "&asc=true"))).as("sortBy=" + field).hasSize(5);
        }
    }

    // ------------------------------------------------------------------ export follows the filters

    @Test
    void excelExportContainsOnlyTheFilteredRowsInTheChosenOrder() throws Exception {
        byte[] bytes = mockMvc.perform(get("/incident/export/excel?status=CLOSED&sortBy=code&asc=true").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(2);                   // header + 2 rows
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("INC-T02");
            assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("INC-T05");
        }
    }

    @Test
    void exportWithoutFiltersContainsEverythingNewestFirst() throws Exception {
        byte[] bytes = mockMvc.perform(get("/incident/export/excel").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(5);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("INC-T05");
        }
    }

    @Test
    void pdfExportAcceptsFiltersToo() throws Exception {
        byte[] bytes = mockMvc.perform(get("/incident/export/pdf?type=ACCIDENT&keyword=forklift").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void exportLinksOnThePageExist() throws Exception {
        String html = page("?status=CLOSED");
        assertThat(html).contains("id=\"exportExcel\"").contains("id=\"exportPdf\"");
        assertThat(html).contains("/incident/export/excel").contains("/incident/export/pdf");
    }

    // ------------------------------------------------------------------ plain unit tests of the helpers

    @Test
    void resolveSortOnlyAcceptsKnownColumns() {
        assertThat(IncidentService.resolveSort("area", true).getOrderFor("area.name")).isNotNull();
        assertThat(IncidentService.resolveSort("code", false).getOrderFor("code").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(IncidentService.resolveSort("code", true).getOrderFor("code").getDirection()).isEqualTo(Sort.Direction.ASC);
        // unknown / hostile names become "newest first", and the id is always the tie-breaker
        Sort fallback = IncidentService.resolveSort("1=1; --", true);
        assertThat(fallback.getOrderFor("reportDate").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(fallback.getOrderFor("id")).isNotNull();
        assertThat(IncidentService.resolveSort(null, true).getOrderFor("reportDate")).isNotNull();
    }

    @Test
    void safePageAndSizeAreClamped() {
        assertThat(IncidentService.safePage(-3)).isZero();
        assertThat(IncidentService.safePage(4)).isEqualTo(4);
        assertThat(IncidentService.safeSize(0)).isEqualTo(10);
        assertThat(IncidentService.safeSize(-1)).isEqualTo(10);
        assertThat(IncidentService.safeSize(30)).isEqualTo(30);
        assertThat(IncidentService.safeSize(100000)).isEqualTo(500);
    }

    @Test
    void criteriaReadsValuesLeniently() {
        IncidentSearchCriteria c = IncidentSearchCriteria.of(" fire ", "2026-01-31", "bad", "accident", "Nope", "", "  ");
        assertThat(c.keyword()).isEqualTo("fire");
        assertThat(c.reportDateFrom()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(c.reportDateTo()).isNull();
        assertThat(c.type()).isEqualTo(Incident.Type.ACCIDENT);
        assertThat(c.severity()).isNull();
        assertThat(c.status()).isNull();
        assertThat(c.areaCode()).isNull();
        assertThat(c.isEmpty()).isFalse();
        assertThat(IncidentSearchCriteria.of(null, null, null, null, null, null, null).isEmpty()).isTrue();
        assertThat(IncidentSearchCriteria.none().isEmpty()).isTrue();
    }

    private static int count(String text, String needle) {
        int count = 0, from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) { count++; from += needle.length(); }
        return count;
    }
}
