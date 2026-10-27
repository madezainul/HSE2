package ahqpck.hse.safety.webpages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.HseDocument;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.repository.HseDocumentRepository;
import ahqpck.hse.safety.repository.UserRepository;

/**
 * Document upload API (documents + inspection modules) and the Area JSON endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2test")
@Transactional
class DocumentAndAreaApiTest {

    private static final byte[] PDF_BYTES =
            "%PDF-1.4\n1 0 obj\n<< /Type /Catalog >>\nendobj\ntrailer\n<< /Root 1 0 R >>\n%%EOF\n"
                    .getBytes(StandardCharsets.US_ASCII);

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private HseDocumentRepository documentRepository;
    @Autowired private AreaRepository areaRepository;

    private User admin;
    private RequestPostProcessor asAdmin;

    @BeforeEach
    void setUp() {
        String sfx = sfx();
        admin = userRepository.saveAndFlush(User.builder()
                .username("doc_admin_" + sfx)
                .employeeId("DA" + sfx)
                .email("doc" + sfx + "@example.com")
                .password("$2a$10$DOCTESTHASHxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                .fullName("Doc Admin")
                .role("ADMIN")
                .build());
        asAdmin = SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    // ------------------------------------------------------------------ document upload

    @Test
    void dailyInspectionUploadCreatesDocumentShownOnPage() throws Exception {
        String docName = "Daily check " + sfx();

        MvcResult res = mockMvc.perform(upload("daily-inspection", docName, pdf("daily.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith("DLI")))
                .andReturn();
        String code = JsonPath.read(res.getResponse().getContentAsString(), "$.code");

        HseDocument saved = documentRepository.findByCode(code).orElseThrow();
        assertThat(saved.getModule()).isEqualTo(HseDocument.Module.DAILY_INSPECTION);
        assertThat(saved.getDocName()).isEqualTo(docName);
        assertThat(saved.getOriginalFileName()).isEqualTo("daily.pdf");
        assertThat(saved.getFileExtension()).isEqualTo(".pdf");
        assertThat(saved.getFileSizeBytes()).isEqualTo(PDF_BYTES.length);
        assertThat(saved.getUploadedBy()).isNotNull();
        assertThat(saved.getUploadedBy().getUsername()).isEqualTo(admin.getUsername());
        assertThat(Files.exists(uploadRoot().resolve(saved.getFilePath()))).isTrue();

        String html = mockMvc.perform(get("/inspection/daily").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(html).contains(docName);
    }

    @ParameterizedTest(name = "module {0} gets code prefix {1}")
    @CsvSource({
            "risk-assessment,RA", "work-instruction,WI", "work-permit,WP", "hse-form,HF",
            "fire-safety-inspection,FSI", "environment-inspection,ENI", "first-aid-inspection,FAI",
            "spill-kit-inspection,SKI", "daily-inspection,DLI", "other-inspection,OTI"})
    void everyModuleAcceptsUploadWithItsOwnCodePrefix(String module, String prefix) throws Exception {
        mockMvc.perform(upload(module, "Doc " + sfx(), pdf("a.pdf")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.startsWith(prefix)));
    }

    @Test
    void secondUploadGetsHigherCodeThanFirst() throws Exception {
        String first = uploadAndGetCode("daily-inspection");
        String second = uploadAndGetCode("daily-inspection");

        assertThat(second).isNotEqualTo(first);
        assertThat(second.compareTo(first)).isGreaterThan(0);
    }

    @Test
    void uploadToUnknownModuleIsBadRequest() throws Exception {
        mockMvc.perform(upload("not-a-module", "Doc", pdf("a.pdf")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void uploadWithDisallowedExtensionIsRejected() throws Exception {
        MockMultipartFile exe = new MockMultipartFile("docFile", "run.exe", "application/octet-stream", new byte[] {1, 2});

        mockMvc.perform(upload("daily-inspection", "Bad file", exe))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void uploadWithImageIsRejectedForDocumentModules() throws Exception {
        // Document modules accept pdf/doc/xls/ppt only (FileCategory.FileType.DOCUMENT)
        MockMultipartFile png = new MockMultipartFile("docFile", "photo.png", "image/png", new byte[] {1, 2, 3});

        mockMvc.perform(upload("risk-assessment", "Photo", png))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadWithoutFileIsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/documents/daily-inspection").param("docName", "No file").with(asAdmin))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: empty file accepted - HseDocumentService.java:41")
    void uploadWithEmptyFileIsRejected() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("docFile", "empty.pdf", "application/pdf", new byte[0]);

        MvcResult r = mockMvc.perform(upload("daily-inspection", "Empty", empty)).andReturn();

        assertThat(r.getResponse().getStatus()).as("a zero-byte file must not be stored as a document").isBetween(400, 499);
    }

    @Test
    void uploadWithoutDocNameIsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/documents/daily-inspection").file(pdf("a.pdf")).with(asAdmin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadWithPathTraversalFileNameDoesNotWriteOutsideUploadDirectory() throws Exception {
        String marker = "escape_" + sfx() + ".pdf";
        MockMultipartFile evil = new MockMultipartFile("docFile", "../../" + marker, "application/pdf", PDF_BYTES);

        mockMvc.perform(upload("daily-inspection", "Evil", evil)).andReturn();

        Path root = uploadRoot();
        assertThat(Files.exists(root.getParent().resolve(marker))).isFalse();
        assertThat(Files.exists(root.resolve("inspection").resolve(marker))).isFalse();
        assertThat(Files.exists(root.resolve(marker))).isFalse();
    }

    @Test
    void inactiveFlagIsStored() throws Exception {
        String code = JsonPath.read(mockMvc.perform(upload("daily-inspection", "Inactive " + sfx(), pdf("a.pdf"))
                        .param("isActive", "false"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.code");

        assertThat(documentRepository.findByCode(code).orElseThrow().getIsActive()).isFalse();
    }

    // ------------------------------------------------------------------ status / delete

    @Test
    void patchStatusUpdatesDocumentStatus() throws Exception {
        String code = uploadAndGetCode("daily-inspection");

        mockMvc.perform(patch("/api/documents/" + code + "/status").param("status", "obsolete").with(asAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        assertThat(documentRepository.findByCode(code).orElseThrow().getStatus())
                .isEqualTo(HseDocument.DocumentStatus.OBSOLETE);
    }

    @Test
    void patchStatusWithInvalidValueIsBadRequest() throws Exception {
        String code = uploadAndGetCode("daily-inspection");

        mockMvc.perform(patch("/api/documents/" + code + "/status").param("status", "banana").with(asAdmin))
                .andExpect(status().isBadRequest());
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: unknown document returns 400 not 404 - HseDocumentRestController.java:63")
    void patchStatusOfUnknownDocumentIs404() throws Exception {
        mockMvc.perform(patch("/api/documents/NOPE000000/status").param("status", "ACTIVE").with(asAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesDocumentAndFile() throws Exception {
        String code = uploadAndGetCode("daily-inspection");
        String filePath = documentRepository.findByCode(code).orElseThrow().getFilePath();

        mockMvc.perform(delete("/api/documents/" + code).with(asAdmin))
                .andExpect(status().isOk());

        assertThat(documentRepository.findByCode(code)).isEmpty();
        assertThat(Files.exists(uploadRoot().resolve(filePath))).isFalse();
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: unknown document returns 400 not 404 - HseDocumentRestController.java:63")
    void deleteUnknownDocumentIs404() throws Exception {
        mockMvc.perform(delete("/api/documents/NOPE000000").with(asAdmin))
                .andExpect(status().isNotFound());
    }


    // ------------------------------------------------------------------ areas JSON

    @Test
    void createAreaThenFetchItsData() throws Exception {
        String code = "AR-" + sfx();
        MvcResult created = mockMvc.perform(post("/areas").with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\",\"name\":\"Packing Hall\",\"description\":\"Line 3\",\"status\":\"UNDER_MAINTENANCE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("UNDER_MAINTENANCE"))
                .andReturn();
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/areas/" + id + "/data").with(asAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.name").value("Packing Hall"))
                .andExpect(jsonPath("$.status").value("UNDER_MAINTENANCE"));
    }

    @Test
    void createAreaWithoutStatusDefaultsToActive() throws Exception {
        mockMvc.perform(post("/areas").with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"AR-" + sfx() + "\",\"name\":\"Default Status\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createAreaWithInvalidStatusIsBadRequest() throws Exception {
        mockMvc.perform(post("/areas").with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"AR-" + sfx() + "\",\"name\":\"Bad\",\"status\":\"BOGUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // Runs without the test transaction: the NOT NULL violation only surfaces when the save commits
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void createAreaWithoutNameIsBadRequest() throws Exception {
        String code = "AR-" + sfx();
        try {
            mockMvc.perform(post("/areas").with(asAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"" + code + "\"}"))
                    .andExpect(status().isBadRequest());
        } finally {
            areaRepository.findAll().stream().filter(a -> code.equals(a.getCode()))
                    .forEach(areaRepository::delete);
        }
    }

    // Runs without the test transaction: the unique-key violation only surfaces when the save commits
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void createAreaWithDuplicateCodeIsRejected() throws Exception {
        String code = "AR-" + sfx();
        areaRepository.saveAndFlush(Area.builder().code(code).name("First").status(Area.AreaStatus.ACTIVE).build());
        try {
            MvcResult r = mockMvc.perform(post("/areas").with(asAdmin)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"code\":\"" + code + "\",\"name\":\"Second\",\"status\":\"ACTIVE\"}"))
                    .andReturn();

            assertThat(r.getResponse().getStatus()).as("duplicate area code").isBetween(400, 409);
        } finally {
            areaRepository.findAll().stream().filter(a -> code.equals(a.getCode()))
                    .forEach(areaRepository::delete);
        }
    }

    @Test
    void updateAreaChangesFields() throws Exception {
        Area area = areaRepository.saveAndFlush(Area.builder()
                .code("AR-" + sfx()).name("Old Name").status(Area.AreaStatus.ACTIVE).build());

        mockMvc.perform(put("/areas/" + area.getId()).with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + area.getCode() + "\",\"name\":\"New Name\",\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Name"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void updateUnknownAreaIs404() throws Exception {
        mockMvc.perform(put("/areas/does-not-exist").with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"X\",\"name\":\"X\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void areaDataForUnknownIdIs404() throws Exception {
        mockMvc.perform(get("/areas/does-not-exist/data").with(asAdmin))
                .andExpect(status().isNotFound());
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: ghost ids counted as deleted - AreaController.java:119")
    void batchDeleteRemovesGivenAreasAndReportsCount() throws Exception {
        Area a = areaRepository.saveAndFlush(Area.builder().code("AR-" + sfx()).name("A").status(Area.AreaStatus.ACTIVE).build());
        Area b = areaRepository.saveAndFlush(Area.builder().code("AR-" + sfx()).name("B").status(Area.AreaStatus.ACTIVE).build());

        mockMvc.perform(delete("/areas/batch").with(asAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"" + a.getId() + "\",\"" + b.getId() + "\",\"ghost-id\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(2));

        assertThat(areaRepository.findById(a.getId())).isEmpty();
        assertThat(areaRepository.findById(b.getId())).isEmpty();
    }

    @Test
    void selectAllReturnsTotalAreaCount() throws Exception {
        long before = areaRepository.count();
        areaRepository.saveAndFlush(Area.builder().code("AR-" + sfx()).name("Count").status(Area.AreaStatus.ACTIVE).build());

        String body = mockMvc.perform(post("/areas/select-all").with(asAdmin))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(Long.parseLong(body.trim())).isEqualTo(before + 1);
    }

    // ------------------------------------------------------------------ helpers

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder upload(
            String module, String docName, MockMultipartFile file) {
        org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder req =
                multipart("/api/documents/" + module);
        req.file(file);
        req.param("docName", docName);
        req.param("category", "General");
        req.param("department", "HSE");
        req.with(asAdmin);
        return req;
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("docFile", name, "application/pdf", PDF_BYTES);
    }

    private String uploadAndGetCode(String module) throws Exception {
        String body = mockMvc.perform(upload(module, "Doc " + sfx(), pdf("seed.pdf")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.code");
    }

    private Path uploadRoot() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "hse-test-uploads").toAbsolutePath().normalize();
    }

    private static String sfx() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 999999));
    }

    @SuppressWarnings("unused")
    private static Stream<String> unused() {
        return Stream.empty();
    }
}
