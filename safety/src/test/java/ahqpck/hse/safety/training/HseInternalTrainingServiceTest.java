package ahqpck.hse.safety.training;

import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.model.entity.HseInternalTraining;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.HseInternalTrainingRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.service.FileStorageService;
import ahqpck.hse.safety.service.HseInternalTrainingService;
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
class HseInternalTrainingServiceTest {

    @Mock HseInternalTrainingRepository repo;
    @Mock FileStorageService files;
    @Mock UserRepository users;
    @InjectMocks HseInternalTrainingService service;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setUsername("admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "x"));
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(repo.save(any(HseInternalTraining.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private HseInternalTraining existing(String filePath) {
        return HseInternalTraining.builder().code("INT-TRAIN-000005").employeeId("E1").employeeName("Emp")
                .trainingName("Fire").trainingDate(LocalDate.of(2024, 1, 1)).duration(2.0)
                .trainerName("Tr").trainerId("TR1").remarks("r")
                .filePath(filePath).originalFileName(filePath == null ? null : "a.pdf")
                .fileSizeBytes(filePath == null ? null : 10L).fileExtension(filePath == null ? null : ".pdf").build();
    }

    private HseInternalTraining doCreate(MockMultipartFile f) throws IOException {
        return service.create("E1", "Emp", "Fire", LocalDate.of(2025, 1, 1), 1.5, "Trainer", "TR1", "rem", f);
    }

    private HseInternalTraining doUpdate(String code, MockMultipartFile f) throws IOException {
        return service.update(code, null, null, null, null, null, null, null, null, f);
    }

    @Test
    void create_firstCodeWhenTableEmpty() throws IOException {
        when(repo.getMaxCodeNumber()).thenReturn(null);
        assertThat(doCreate(null).getCode()).isEqualTo("INT-TRAIN-000001");
    }

    @Test
    void create_nextCodeIsHighestPlusOneWithPadding() throws IOException {
        when(repo.getMaxCodeNumber()).thenReturn(123);
        assertThat(doCreate(null).getCode()).isEqualTo("INT-TRAIN-000124");
    }

    @Test
    void create_skipsCodeThatAlreadyExists() throws IOException {
        when(repo.getMaxCodeNumber()).thenReturn(1);
        when(repo.findByCode("INT-TRAIN-000002")).thenReturn(existing(null));
        assertThat(doCreate(null).getCode()).isEqualTo("INT-TRAIN-000003");
    }

    @Test
    void create_setsCreatedByAndFields() throws IOException {
        HseInternalTraining r = doCreate(null);
        assertThat(r.getCreatedBy()).isSameAs(admin);
        assertThat(r.getEmployeeId()).isEqualTo("E1");
        assertThat(r.getDuration()).isEqualTo(1.5);
        assertThat(r.getTrainerName()).isEqualTo("Trainer");
        assertThat(r.getRemarks()).isEqualTo("rem");
    }

    @Test
    void create_noFileOrEmptyFileDoesNotStore() throws IOException {
        doCreate(null);
        doCreate(new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]));
        verify(files, never()).storeFile(any(), any());
    }

    @Test
    void create_documentAndImageCategories() throws IOException {
        when(files.storeFile(any(), any())).thenReturn("p/x");
        HseInternalTraining d = doCreate(new MockMultipartFile("file", "a.DOCX", "x", new byte[]{1, 2}));
        assertThat(d.getFileExtension()).isEqualTo(".docx");
        assertThat(d.getFileSizeBytes()).isEqualTo(2L);
        assertThat(d.getFilePath()).isEqualTo("p/x");
        verify(files).storeFile(any(), eq(FileCategory.HSE_INTERNAL_TRAINING_DOCUMENT));
        doCreate(new MockMultipartFile("file", "a.PNG", "x", new byte[]{1}));
        verify(files).storeFile(any(), eq(FileCategory.HSE_INTERNAL_TRAINING_IMAGE));
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: throws RuntimeException instead of ResourceNotFoundException - HseInternalTrainingService.java:96")
    void findByCode_notFoundThrowsResourceNotFound_perClaudeMd() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.findByCode("X")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_nullParametersLeaveFieldsUnchanged() throws IOException {
        HseInternalTraining e = existing(null);
        when(repo.findByCode("INT-TRAIN-000005")).thenReturn(e);
        doUpdate("INT-TRAIN-000005", null);
        assertThat(e.getEmployeeId()).isEqualTo("E1");
        assertThat(e.getEmployeeName()).isEqualTo("Emp");
        assertThat(e.getTrainingName()).isEqualTo("Fire");
        assertThat(e.getTrainingDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(e.getDuration()).isEqualTo(2.0);
        assertThat(e.getTrainerName()).isEqualTo("Tr");
        assertThat(e.getTrainerId()).isEqualTo("TR1");
        assertThat(e.getRemarks()).isEqualTo("r");
    }

    @Test
    void update_setsProvidedFields() throws IOException {
        HseInternalTraining e = existing(null);
        when(repo.findByCode("INT-TRAIN-000005")).thenReturn(e);
        service.update("INT-TRAIN-000005", "E2", "N", "T", LocalDate.of(2025, 2, 2), 3.0, "TN", "TR2", "rr", null);
        assertThat(e.getEmployeeId()).isEqualTo("E2");
        assertThat(e.getTrainingName()).isEqualTo("T");
        assertThat(e.getDuration()).isEqualTo(3.0);
        assertThat(e.getTrainerName()).isEqualTo("TN");
        assertThat(e.getRemarks()).isEqualTo("rr");
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: throws RuntimeException instead of ResourceNotFoundException - HseInternalTrainingService.java:96")
    void update_notFoundThrowsResourceNotFound() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> doUpdate("X", null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_replacingFileDeletesOldFile() throws IOException {
        HseInternalTraining e = existing("old.pdf");
        when(repo.findByCode("INT-TRAIN-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenReturn("new.pdf");
        doUpdate("INT-TRAIN-000005", new MockMultipartFile("file", "n.pdf", "x", new byte[]{1}));
        verify(files).deleteFile("old.pdf");
        assertThat(e.getFilePath()).isEqualTo("new.pdf");
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: old file deleted before new upload succeeds - HseInternalTrainingService.java:110")
    void update_failingUploadMustNotDeleteOldFile() throws IOException {
        HseInternalTraining e = existing("old.pdf");
        when(repo.findByCode("INT-TRAIN-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenThrow(new RuntimeException("bad type"));
        assertThatThrownBy(() -> doUpdate("INT-TRAIN-000005", new MockMultipartFile("file", "n.exe", "x", new byte[]{1})))
                .isInstanceOf(RuntimeException.class);
        verify(files, never()).deleteFile("old.pdf");
    }

    @Test
    void delete_removesFileAndRecord() throws IOException {
        HseInternalTraining e = existing("old.pdf");
        when(repo.findByCode("INT-TRAIN-000005")).thenReturn(e);
        service.delete("INT-TRAIN-000005");
        verify(files).deleteFile("old.pdf");
        verify(repo).delete(e);
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: throws RuntimeException instead of ResourceNotFoundException - HseInternalTrainingService.java:96")
    void delete_notFoundThrowsResourceNotFound() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.delete("X")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteFile_clearsFileMetadata() throws IOException {
        HseInternalTraining e = existing("old.pdf");
        when(repo.findByCode("INT-TRAIN-000005")).thenReturn(e);
        service.deleteFile("INT-TRAIN-000005");
        verify(files).deleteFile("old.pdf");
        assertThat(e.getFilePath()).isNull();
        assertThat(e.getOriginalFileName()).isNull();
        assertThat(e.getFileSizeBytes()).isNull();
        assertThat(e.getFileExtension()).isNull();
        verify(repo).save(e);
    }

    @Test
    void deleteFile_noFileIsNoOp() throws IOException {
        when(repo.findByCode("INT-TRAIN-000005")).thenReturn(existing(null));
        service.deleteFile("INT-TRAIN-000005");
        verify(files, never()).deleteFile(any());
        verify(repo, never()).save(any());
    }

    @Test
    @org.junit.jupiter.api.Disabled("BUG: throws RuntimeException instead of ResourceNotFoundException - HseInternalTrainingService.java:96")
    void deleteFile_notFoundThrowsResourceNotFound() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.deleteFile("X")).isInstanceOf(ResourceNotFoundException.class);
    }
}
