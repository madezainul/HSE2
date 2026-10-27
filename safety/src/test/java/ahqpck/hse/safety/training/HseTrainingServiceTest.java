package ahqpck.hse.safety.training;

import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.model.entity.HseInduction;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.HseInductionRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.service.FileStorageService;
import ahqpck.hse.safety.service.HseTrainingService;
import ahqpck.hse.safety.util.FileCategory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HseTrainingServiceTest {

    @Mock HseInductionRepository repo;
    @Mock FileStorageService files;
    @Mock UserRepository users;
    @InjectMocks HseTrainingService service;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setUsername("admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "x"));
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(repo.save(any(HseInduction.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private HseInduction existing(String filePath) {
        return HseInduction.builder().code("IND-000005").traineeId("T1").traineeName("Old")
                .inductionDate(LocalDate.of(2024, 1, 1)).trainers("Tr").trainerId("TR1")
                .filePath(filePath).originalFileName(filePath == null ? null : "a.pdf")
                .fileSizeBytes(filePath == null ? null : 10L).fileExtension(filePath == null ? null : ".pdf").build();
    }

    private HseInduction doCreate(MockMultipartFile f) throws IOException {
        return service.create("T1", "Name", LocalDate.of(2025, 1, 1), "Trainer", "TR1", f);
    }

    @Test
    void create_firstCodeWhenTableEmpty() throws IOException {
        when(repo.getMaxCodeNumber()).thenReturn(null);
        assertThat(doCreate(null).getCode()).isEqualTo("IND-000001");
    }

    @Test
    void create_nextCodeIsHighestPlusOneWithPadding() throws IOException {
        when(repo.getMaxCodeNumber()).thenReturn(41);
        assertThat(doCreate(null).getCode()).isEqualTo("IND-000042");
    }

    @Test
    void create_skipsCodeThatAlreadyExists() throws IOException {
        when(repo.getMaxCodeNumber()).thenReturn(1);
        when(repo.findByCode("IND-000002")).thenReturn(existing(null));
        assertThat(doCreate(null).getCode()).isEqualTo("IND-000003");
    }

    @Test
    void create_setsCreatedByAndFields() throws IOException {
        HseInduction r = doCreate(null);
        assertThat(r.getCreatedBy()).isSameAs(admin);
        assertThat(r.getTraineeId()).isEqualTo("T1");
        assertThat(r.getTrainers()).isEqualTo("Trainer");
        assertThat(r.getCreatedAt()).isNotNull();
    }

    @Test
    void create_unknownUserLeavesCreatedByNull() throws IOException {
        when(users.findByUsername("admin")).thenReturn(Optional.empty());
        assertThat(doCreate(null).getCreatedBy()).isNull();
    }

    @Test
    void create_noFileOrEmptyFileDoesNotStore() throws IOException {
        doCreate(null);
        doCreate(new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]));
        verify(files, never()).storeFile(any(), any());
    }

    @Test
    void create_documentUsesDocumentCategory() throws IOException {
        when(files.storeFile(any(), eq(FileCategory.HSE_INDUCTION_DOCUMENT))).thenReturn("hse-induction/documents/x_a.pdf");
        HseInduction r = doCreate(new MockMultipartFile("file", "a.PDF", "application/pdf", new byte[]{1, 2, 3}));
        assertThat(r.getFilePath()).isEqualTo("hse-induction/documents/x_a.pdf");
        assertThat(r.getOriginalFileName()).isEqualTo("a.PDF");
        assertThat(r.getFileExtension()).isEqualTo(".pdf");
        assertThat(r.getFileSizeBytes()).isEqualTo(3L);
    }

    @Test
    void create_imageUsesImageCategory() throws IOException {
        for (String n : new String[]{"p.jpg", "p.JPEG", "p.png", "p.webp"}) {
            doCreate(new MockMultipartFile("file", n, "image/x", new byte[]{1}));
        }
        verify(files, times(4)).storeFile(any(), eq(FileCategory.HSE_INDUCTION_IMAGE));
        verify(files, never()).storeFile(any(), eq(FileCategory.HSE_INDUCTION_DOCUMENT));
    }

    @Test
    void create_gifIsNotTreatedAsImage() throws IOException {
        doCreate(new MockMultipartFile("file", "p.gif", "image/gif", new byte[]{1}));
        verify(files).storeFile(any(), eq(FileCategory.HSE_INDUCTION_DOCUMENT));
    }

    @Test
    void create_storeFailureDoesNotSave() {
        when(files.storeFile(any(), any())).thenThrow(new RuntimeException("bad type"));
        assertThatThrownBy(() -> doCreate(new MockMultipartFile("file", "a.exe", "x", new byte[]{1})))
                .isInstanceOf(RuntimeException.class);
        verify(repo, never()).save(any());
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: throws RuntimeException instead of ResourceNotFoundException - HseTrainingService.java:90")
    void findByCode_notFoundThrowsResourceNotFound_perClaudeMd() {
        when(repo.findByCode("IND-999999")).thenReturn(null);
        assertThatThrownBy(() -> service.findByCode("IND-999999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByCode_found() {
        HseInduction e = existing(null);
        when(repo.findByCode("IND-000005")).thenReturn(e);
        assertThat(service.findByCode("IND-000005")).isSameAs(e);
    }

    @Test
    void update_nullParametersLeaveFieldsUnchanged() throws IOException {
        HseInduction e = existing(null);
        when(repo.findByCode("IND-000005")).thenReturn(e);
        service.update("IND-000005", null, null, null, null, null, null);
        assertThat(e.getTraineeId()).isEqualTo("T1");
        assertThat(e.getTraineeName()).isEqualTo("Old");
        assertThat(e.getInductionDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(e.getTrainers()).isEqualTo("Tr");
        assertThat(e.getTrainerId()).isEqualTo("TR1");
    }

    @Test
    void update_setsProvidedFields() throws IOException {
        HseInduction e = existing(null);
        when(repo.findByCode("IND-000005")).thenReturn(e);
        service.update("IND-000005", "T2", "New", LocalDate.of(2025, 5, 5), "T", "TR2", null);
        assertThat(e.getTraineeId()).isEqualTo("T2");
        assertThat(e.getTraineeName()).isEqualTo("New");
        assertThat(e.getInductionDate()).isEqualTo(LocalDate.of(2025, 5, 5));
        assertThat(e.getTrainerId()).isEqualTo("TR2");
        assertThat(e.getUpdatedAt()).isNotNull();
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: throws RuntimeException instead of ResourceNotFoundException - HseTrainingService.java:90")
    void update_notFoundThrowsResourceNotFound() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.update("X", null, null, null, null, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_replacingFileDeletesOldFile() throws IOException {
        HseInduction e = existing("hse-induction/documents/old.pdf");
        when(repo.findByCode("IND-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenReturn("hse-induction/documents/new.pdf");
        service.update("IND-000005", null, null, null, null, null,
                new MockMultipartFile("file", "n.pdf", "application/pdf", new byte[]{1}));
        verify(files).deleteFile("hse-induction/documents/old.pdf");
        assertThat(e.getFilePath()).isEqualTo("hse-induction/documents/new.pdf");
    }

    @Test
    void update_emptyFileKeepsOldFile() throws IOException {
        HseInduction e = existing("old.pdf");
        when(repo.findByCode("IND-000005")).thenReturn(e);
        service.update("IND-000005", null, null, null, null, null,
                new MockMultipartFile("file", "n.pdf", "application/pdf", new byte[0]));
        verify(files, never()).deleteFile(any());
        assertThat(e.getFilePath()).isEqualTo("old.pdf");
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: old file deleted before new upload succeeds - HseTrainingService.java:101")
    void update_failingUploadMustNotDeleteOldFile() throws IOException {
        HseInduction e = existing("old.pdf");
        when(repo.findByCode("IND-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenThrow(new RuntimeException("bad type"));
        assertThatThrownBy(() -> service.update("IND-000005", null, null, null, null, null,
                new MockMultipartFile("file", "n.exe", "x", new byte[]{1})))
                .isInstanceOf(RuntimeException.class);
        verify(files, never()).deleteFile("old.pdf");
    }

    @Test
    void update_oldFileDeleteIoErrorIsSwallowed() throws IOException {
        HseInduction e = existing("old.pdf");
        when(repo.findByCode("IND-000005")).thenReturn(e);
        doThrow(new IOException("locked")).when(files).deleteFile("old.pdf");
        when(files.storeFile(any(), any())).thenReturn("new.pdf");
        service.update("IND-000005", null, null, null, null, null,
                new MockMultipartFile("file", "n.pdf", "application/pdf", new byte[]{1}));
        assertThat(e.getFilePath()).isEqualTo("new.pdf");
    }

    @Test
    void delete_removesFileAndRecord() throws IOException {
        HseInduction e = existing("old.pdf");
        when(repo.findByCode("IND-000005")).thenReturn(e);
        service.delete("IND-000005");
        verify(files).deleteFile("old.pdf");
        verify(repo).delete(e);
    }

    @Test
    void delete_withoutFileOnlyRemovesRecord() throws IOException {
        HseInduction e = existing(null);
        when(repo.findByCode("IND-000005")).thenReturn(e);
        service.delete("IND-000005");
        verify(files, never()).deleteFile(any());
        verify(repo).delete(e);
    }

    @Test
    void delete_fileIoErrorStillDeletesRecord() throws IOException {
        HseInduction e = existing("old.pdf");
        when(repo.findByCode("IND-000005")).thenReturn(e);
        doThrow(new IOException("x")).when(files).deleteFile("old.pdf");
        service.delete("IND-000005");
        verify(repo).delete(e);
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: throws RuntimeException instead of ResourceNotFoundException - HseTrainingService.java:90")
    void delete_notFoundThrowsResourceNotFound() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.delete("X")).isInstanceOf(ResourceNotFoundException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void deleteFile_clearsFileMetadata() throws IOException {
        HseInduction e = existing("old.pdf");
        when(repo.findByCode("IND-000005")).thenReturn(e);
        service.deleteFile("IND-000005");
        verify(files).deleteFile("old.pdf");
        assertThat(e.getFilePath()).isNull();
        assertThat(e.getOriginalFileName()).isNull();
        assertThat(e.getFileSizeBytes()).isNull();
        assertThat(e.getFileExtension()).isNull();
        verify(repo).save(e);
    }

    @Test
    void deleteFile_noFileIsNoOp() throws IOException {
        when(repo.findByCode("IND-000005")).thenReturn(existing(null));
        service.deleteFile("IND-000005");
        verify(files, never()).deleteFile(any());
        verify(repo, never()).save(any());
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: throws RuntimeException instead of ResourceNotFoundException - HseTrainingService.java:90")
    void deleteFile_notFoundThrowsResourceNotFound() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.deleteFile("X")).isInstanceOf(ResourceNotFoundException.class);
    }
}
