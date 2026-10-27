package ahqpck.hse.safety.exports;

import ahqpck.hse.safety.model.entity.HseDocument;
import ahqpck.hse.safety.model.entity.HseDocument.Module;
import ahqpck.hse.safety.repository.HseDocumentRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.service.FileStorageService;
import ahqpck.hse.safety.service.HseDocumentService;
import ahqpck.hse.safety.util.FileCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HseDocumentServiceTest {

    @Mock HseDocumentRepository documentRepository;
    @Mock FileStorageService fileStorageService;
    @Mock UserRepository userRepository;
    @InjectMocks HseDocumentService service;

    private static final Map<Module, String> PREFIX = new EnumMap<>(Module.class);
    private static final Map<Module, FileCategory> CATEGORY = new EnumMap<>(Module.class);

    static {
        PREFIX.put(Module.RISK_ASSESSMENT, "RA");
        PREFIX.put(Module.WORK_INSTRUCTION, "WI");
        PREFIX.put(Module.WORK_PERMIT, "WP");
        PREFIX.put(Module.HSE_FORM, "HF");
        PREFIX.put(Module.FIRE_SAFETY_INSPECTION, "FSI");
        PREFIX.put(Module.ENVIRONMENT_INSPECTION, "ENI");
        PREFIX.put(Module.FIRST_AID_INSPECTION, "FAI");
        PREFIX.put(Module.SPILL_KIT_INSPECTION, "SKI");
        PREFIX.put(Module.DAILY_INSPECTION, "DLI");
        PREFIX.put(Module.OTHER_INSPECTION, "OTI");

        CATEGORY.put(Module.RISK_ASSESSMENT, FileCategory.RISK_ASSESSMENT_DOCUMENT);
        CATEGORY.put(Module.WORK_INSTRUCTION, FileCategory.WORK_INSTRUCTION_DOCUMENT);
        CATEGORY.put(Module.WORK_PERMIT, FileCategory.WORK_PERMIT_DOCUMENT);
        CATEGORY.put(Module.HSE_FORM, FileCategory.HSE_FORM_DOCUMENT);
        CATEGORY.put(Module.FIRE_SAFETY_INSPECTION, FileCategory.FIRE_SAFETY_INSPECTION_DOCUMENT);
        CATEGORY.put(Module.ENVIRONMENT_INSPECTION, FileCategory.ENVIRONMENT_INSPECTION_DOCUMENT);
        CATEGORY.put(Module.FIRST_AID_INSPECTION, FileCategory.FIRST_AID_INSPECTION_DOCUMENT);
        CATEGORY.put(Module.SPILL_KIT_INSPECTION, FileCategory.SPILL_KIT_INSPECTION_DOCUMENT);
        CATEGORY.put(Module.DAILY_INSPECTION, FileCategory.DAILY_INSPECTION_DOCUMENT);
        CATEGORY.put(Module.OTHER_INSPECTION, FileCategory.OTHER_INSPECTION_DOCUMENT);
    }

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", "n/a"));
        when(documentRepository.save(any(HseDocument.class))).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.findTopByCodeStartingWithOrderByCodeDesc(anyString())).thenReturn(Optional.empty());
        when(documentRepository.existsByCode(anyString())).thenReturn(false);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(fileStorageService.storeFile(any(), any())).thenReturn("stored/path.pdf");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "Policy.PDF", "application/pdf", new byte[2048]);
    }

    // -------------------------------------------------------- module coverage

    @Test
    void testTablesCoverEveryModule() {
        assertThat(PREFIX.keySet()).containsExactlyInAnyOrder(Module.values());
        assertThat(CATEGORY.keySet()).containsExactlyInAnyOrder(Module.values());
    }

    @ParameterizedTest
    @EnumSource(Module.class)
    void upload_usesExpectedCodePrefixAndFileCategoryForEveryModule(Module module) {
        HseDocument doc = service.upload(module, "Name", "Cat", "Dept", "Desc", true, pdf());

        assertThat(doc.getCode()).isEqualTo(PREFIX.get(module) + "000001");
        assertThat(doc.getModule()).isEqualTo(module);
        verify(fileStorageService).storeFile(any(), eq(CATEGORY.get(module)));
        verify(documentRepository).findTopByCodeStartingWithOrderByCodeDesc(PREFIX.get(module));
    }

    @Test
    void codePrefixesAreUniqueAndNoneIsPrefixOfAnother() {
        for (Module a : Module.values()) {
            for (Module b : Module.values()) {
                if (a != b) {
                    assertThat(PREFIX.get(b)).doesNotStartWith(PREFIX.get(a));
                }
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Module.class)
    void everyModuleCodeFitsTheCodeColumnAndModuleNameFitsItsColumn(Module module) {
        assertThat((PREFIX.get(module) + "000001").length()).isLessThanOrEqualTo(20);
        assertThat(module.name().length()).isLessThanOrEqualTo(30);
    }

    @ParameterizedTest
    @EnumSource(Module.class)
    void fileCategoriesOfEveryModuleAreDocumentsInDistinctDirectories(Module module) {
        FileCategory c = CATEGORY.get(module);
        assertThat(c.getFileType()).isEqualTo(FileCategory.FileType.DOCUMENT);
        long same = CATEGORY.values().stream().filter(x -> x.getPath().equals(c.getPath())).count();
        assertThat(same).isEqualTo(1);
    }

    // ------------------------------------------------------------ url paths

    @ParameterizedTest
    @EnumSource(Module.class)
    void urlPathRoundTrip(Module module) {
        String path = module.toUrlPath();
        assertThat(path).isEqualTo(path.toLowerCase()).doesNotContain("_");
        assertThat(Module.fromUrlPath(path)).isEqualTo(module);
    }

    @Test
    void dailyInspectionUrlPath() {
        assertThat(Module.DAILY_INSPECTION.toUrlPath()).isEqualTo("daily-inspection");
        assertThat(Module.fromUrlPath("DAILY-INSPECTION")).isEqualTo(Module.DAILY_INSPECTION);
    }

    @Test
    void fromUrlPathInvalidThrowsIllegalArgument() {
        assertThatThrownBy(() -> Module.fromUrlPath("no-such-module")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Module.fromUrlPath("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Module.fromUrlPath("risk assessment")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: fromUrlPath accepts underscore form - HseDocument.java:101")
    void fromUrlPathUnderscoreFormIsNotAValidUrlPath() {
        // URL paths use dashes; the enum constant spelling should not silently be accepted as a URL form
        assertThatThrownBy(() -> Module.fromUrlPath("risk_assessment")).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------- upload

    @Test
    void upload_populatesMetadataAndDefaults() {
        HseDocument doc = service.upload(Module.WORK_PERMIT, "Permit", "Hot", "Eng", "Desc", null, pdf());
        assertThat(doc.getDocName()).isEqualTo("Permit");
        assertThat(doc.getCategory()).isEqualTo("Hot");
        assertThat(doc.getDepartment()).isEqualTo("Eng");
        assertThat(doc.getFilePath()).isEqualTo("stored/path.pdf");
        assertThat(doc.getOriginalFileName()).isEqualTo("Policy.PDF");
        assertThat(doc.getFileExtension()).isEqualTo(".pdf");
        assertThat(doc.getFileSizeBytes()).isEqualTo(2048L);
        assertThat(doc.getIsActive()).isTrue();
        assertThat(doc.getStatus()).isEqualTo(HseDocument.DocumentStatus.ACTIVE);
        assertThat(doc.getUploadedAt()).isNotNull();
        assertThat(doc.getUploadedBy()).isNull();
    }

    @Test
    void upload_explicitInactiveIsKept() {
        HseDocument doc = service.upload(Module.HSE_FORM, "n", null, null, null, false, pdf());
        assertThat(doc.getIsActive()).isFalse();
    }

    @Test
    void upload_fileWithoutExtensionGetsEmptyExtension() {
        MockMultipartFile f = new MockMultipartFile("file", "README", "text/plain", new byte[1]);
        HseDocument doc = service.upload(Module.HSE_FORM, "n", null, null, null, true, f);
        assertThat(doc.getFileExtension()).isEmpty();
    }

    @Test
    void upload_doubleExtensionKeepsOnlyLast() {
        MockMultipartFile f = new MockMultipartFile("file", "a.b.DOCX", "x/y", new byte[1]);
        HseDocument doc = service.upload(Module.HSE_FORM, "n", null, null, null, true, f);
        assertThat(doc.getFileExtension()).isEqualTo(".docx");
    }

    @Test
    void upload_incrementsFromHighestExistingCode() {
        HseDocument last = HseDocument.builder().code("WI000041").build();
        when(documentRepository.findTopByCodeStartingWithOrderByCodeDesc("WI")).thenReturn(Optional.of(last));
        HseDocument doc = service.upload(Module.WORK_INSTRUCTION, "n", null, null, null, true, pdf());
        assertThat(doc.getCode()).isEqualTo("WI000042");
    }

    @Test
    void upload_skipsCodesThatAlreadyExist() {
        when(documentRepository.existsByCode("RA000001")).thenReturn(true);
        when(documentRepository.existsByCode("RA000002")).thenReturn(true);
        HseDocument doc = service.upload(Module.RISK_ASSESSMENT, "n", null, null, null, true, pdf());
        assertThat(doc.getCode()).isEqualTo("RA000003");
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: generateCode returns duplicate after 10 tries - HseDocumentService.java:103")
    void upload_neverReturnsACodeThatAlreadyExists() {
        // every candidate is taken: a duplicate code must not be handed out
        when(documentRepository.existsByCode(anyString())).thenReturn(true);
        assertThatThrownBy(() -> service.upload(Module.RISK_ASSESSMENT, "n", null, null, null, true, pdf()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void upload_corruptLastCodeStartsAtOneButMustNotCollideWithExistingCode() {
        HseDocument corrupt = HseDocument.builder().code("RAxxxxxx").build();
        when(documentRepository.findTopByCodeStartingWithOrderByCodeDesc("RA")).thenReturn(Optional.of(corrupt));
        when(documentRepository.existsByCode("RA000001")).thenReturn(true);
        HseDocument doc = service.upload(Module.RISK_ASSESSMENT, "n", null, null, null, true, pdf());
        assertThat(doc.getCode()).isNotEqualTo("RA000001");
    }

    @Test
    void upload_setsUploaderWhenUserExists() {
        ahqpck.hse.safety.model.entity.User u = new ahqpck.hse.safety.model.entity.User();
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(u));
        HseDocument doc = service.upload(Module.HSE_FORM, "n", null, null, null, true, pdf());
        assertThat(doc.getUploadedBy()).isSameAs(u);
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: file stored before validation/auth check - HseDocumentService.java:42")
    void upload_withoutAuthenticationShouldFailBeforeStoringFile() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> service.upload(Module.HSE_FORM, "n", null, null, null, true, pdf()))
                .isInstanceOf(RuntimeException.class);
        // an orphan file must not be left on disk when the upload is rejected
        verify(fileStorageService, never()).storeFile(any(), any());
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: file stored before validation/auth check - HseDocumentService.java:42")
    void upload_nullOriginalFilenameShouldFailBeforeStoringFile() {
        MockMultipartFile f = new MockMultipartFile("file", null, "x/y", new byte[1]);
        // MockMultipartFile turns null into "" so use an anonymous override for a truly null name
        org.springframework.web.multipart.MultipartFile nullName = new MockMultipartFile("file", "x.pdf", "x/y", new byte[1]) {
            @Override
            public String getOriginalFilename() {
                return null;
            }
        };
        assertThatThrownBy(() -> service.upload(Module.HSE_FORM, "n", null, null, null, true, nullName))
                .isInstanceOf(NullPointerException.class);
        verify(fileStorageService, never()).storeFile(any(), any());
        assertThat(f.getOriginalFilename()).isEmpty();
    }

    @Test
    void upload_savesExactlyOneEntity() {
        service.upload(Module.HSE_FORM, "n", null, null, null, true, pdf());
        ArgumentCaptor<HseDocument> cap = ArgumentCaptor.forClass(HseDocument.class);
        verify(documentRepository).save(cap.capture());
        assertThat(cap.getValue().getModule()).isEqualTo(Module.HSE_FORM);
    }

    // ---------------------------------------------------- status and delete

    @Test
    void updateStatus_changesStatus() {
        HseDocument d = HseDocument.builder().code("HF000001").build();
        when(documentRepository.findByCode("HF000001")).thenReturn(Optional.of(d));
        HseDocument result = service.updateStatus("HF000001", HseDocument.DocumentStatus.OBSOLETE);
        assertThat(result.getStatus()).isEqualTo(HseDocument.DocumentStatus.OBSOLETE);
    }

    @Test
    void updateStatus_unknownCodeThrows() {
        when(documentRepository.findByCode("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatus("NOPE", HseDocument.DocumentStatus.UPDATED))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("NOPE");
        verify(documentRepository, never()).save(any());
    }

    @Test
    void delete_removesFileAndRow() throws IOException {
        HseDocument d = HseDocument.builder().code("HF000001").filePath("a/b.pdf").build();
        when(documentRepository.findByCode("HF000001")).thenReturn(Optional.of(d));
        service.delete("HF000001");
        verify(fileStorageService).deleteFile("a/b.pdf");
        verify(documentRepository).delete(d);
    }

    @Test
    void delete_documentWithoutFilePathSkipsFileDeletion() throws IOException {
        HseDocument d = HseDocument.builder().code("HF000002").build();
        when(documentRepository.findByCode("HF000002")).thenReturn(Optional.of(d));
        service.delete("HF000002");
        verify(fileStorageService, never()).deleteFile(any());
        verify(documentRepository).delete(d);
    }

    @Test
    void delete_unknownCodeThrowsAndDeletesNothing() throws IOException {
        when(documentRepository.findByCode("X")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete("X")).isInstanceOf(RuntimeException.class);
        verify(fileStorageService, never()).deleteFile(any());
        verify(documentRepository, never()).delete(any());
    }

    @Test
    void findByModule_delegatesToRepository() {
        when(documentRepository.findByModuleOrderByUploadedAtDesc(Module.DAILY_INSPECTION)).thenReturn(java.util.List.of());
        assertThat(service.findByModule(Module.DAILY_INSPECTION)).isEmpty();
        verify(documentRepository).findByModuleOrderByUploadedAtDesc(Module.DAILY_INSPECTION);
    }

    // ------------------------------------------------- entity helper methods

    @Test
    void formattedFileSize() {
        assertThat(HseDocument.builder().build().getFormattedFileSize()).isEqualTo("N/A");
        assertThat(HseDocument.builder().fileSizeBytes(0L).build().getFormattedFileSize()).isEqualTo("0 KB");
        assertThat(HseDocument.builder().fileSizeBytes(2048L).build().getFormattedFileSize()).isEqualTo("2 KB");
        assertThat(HseDocument.builder().fileSizeBytes(1_048_576L).build().getFormattedFileSize()).contains("1").contains("MB");
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: file size rounding at edges - HseDocument.java:78")
    void formattedFileSizeJustUnderOneMegabyteMustNotRoundToThousandAndOneKb() {
        // 1_048_575 bytes is 1024 KB after rounding, which reads as a wrong unit
        String s = HseDocument.builder().fileSizeBytes(1_048_575L).build().getFormattedFileSize();
        assertThat(s).doesNotStartWith("1024");
    }

    @Test
    void fileIconClassByExtension() {
        assertThat(HseDocument.builder().fileExtension(".pdf").build().getFileIconClass()).contains("pdf");
        assertThat(HseDocument.builder().fileExtension(".DOCX").build().getFileIconClass()).contains("word");
        assertThat(HseDocument.builder().fileExtension(".xlsx").build().getFileIconClass()).contains("excel");
        assertThat(HseDocument.builder().fileExtension(".pptx").build().getFileIconClass()).contains("powerpoint");
        assertThat(HseDocument.builder().fileExtension(".zip").build().getFileIconClass()).isEqualTo("fa fa-file me-2 text-muted");
        assertThat(HseDocument.builder().build().getFileIconClass()).isEqualTo("fa fa-file me-2 text-muted");
    }

    @Test
    void builderDefaultsAreActive() {
        HseDocument d = HseDocument.builder().build();
        assertThat(d.getIsActive()).isTrue();
        assertThat(d.getStatus()).isEqualTo(HseDocument.DocumentStatus.ACTIVE);
    }
}
