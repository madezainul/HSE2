package ahqpck.hse.safety.training;

import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.model.entity.HseExternalTraining;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.HseExternalTrainingRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.service.FileStorageService;
import ahqpck.hse.safety.service.HseExternalTrainingService;
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
class HseExternalTrainingServiceTest {

    @Mock HseExternalTrainingRepository repo;
    @Mock FileStorageService files;
    @Mock UserRepository users;
    @InjectMocks HseExternalTrainingService service;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setUsername("admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "x"));
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(repo.save(any(HseExternalTraining.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private HseExternalTraining existing(String filePath) {
        return HseExternalTraining.builder().code("EXT-TRAIN-000005").employeeId("E1").employeeName("Emp")
                .trainingName("Crane").provider("ProvA").trainingDate(LocalDate.of(2024, 1, 1)).duration(8.0)
                .remarks("r").filePath(filePath).originalFileName(filePath == null ? null : "a.pdf")
                .fileSizeBytes(filePath == null ? null : 10L).fileExtension(filePath == null ? null : ".pdf").build();
    }

    private HseExternalTraining doCreate(MockMultipartFile f) throws IOException {
        return service.create("E1", "Emp", "Crane", "ProvA", LocalDate.of(2025, 1, 1), 8.0, "rem", f);
    }

    private HseExternalTraining doUpdate(String code, MockMultipartFile f) throws IOException {
        return service.update(code, null, null, null, null, null, null, null, f);
    }

    @Test
    void create_firstCodeWhenTableEmpty() throws IOException {
        when(repo.getMaxCodeNumber()).thenReturn(null);
        assertThat(doCreate(null).getCode()).isEqualTo("EXT-TRAIN-000001");
    }

    @Test
    void create_nextCodeIsHighestPlusOneWithPadding() throws IOException {
        when(repo.getMaxCodeNumber()).thenReturn(99);
        assertThat(doCreate(null).getCode()).isEqualTo("EXT-TRAIN-000100");
    }

    @Test
    void create_setsCreatedByAndFields() throws IOException {
        HseExternalTraining r = doCreate(null);
        assertThat(r.getCreatedBy()).isSameAs(admin);
        assertThat(r.getProvider()).isEqualTo("ProvA");
        assertThat(r.getDuration()).isEqualTo(8.0);
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
        HseExternalTraining d = doCreate(new MockMultipartFile("file", "a.XLSX", "x", new byte[]{1, 2}));
        assertThat(d.getFileExtension()).isEqualTo(".xlsx");
        assertThat(d.getFileSizeBytes()).isEqualTo(2L);
        assertThat(d.getOriginalFileName()).isEqualTo("a.XLSX");
        verify(files).storeFile(any(), eq(FileCategory.HSE_EXTERNAL_TRAINING_DOCUMENT));
        doCreate(new MockMultipartFile("file", "a.webp", "x", new byte[]{1}));
        verify(files).storeFile(any(), eq(FileCategory.HSE_EXTERNAL_TRAINING_IMAGE));
    }

    @Test
    void create_fileWithoutExtensionIsStoredAsDocument() throws IOException {
        doCreate(new MockMultipartFile("file", "noext", "x", new byte[]{1}));
        verify(files).storeFile(any(), eq(FileCategory.HSE_EXTERNAL_TRAINING_DOCUMENT));
    }

    @Test
    void create_storeFailureDoesNotSave() {
        when(files.storeFile(any(), any())).thenThrow(new RuntimeException("bad type"));
        assertThatThrownBy(() -> doCreate(new MockMultipartFile("file", "a.exe", "x", new byte[]{1})))
                .isInstanceOf(RuntimeException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void findByCode_notFoundThrows() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.findByCode("X"))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("X");
    }

    @Test
    void findByCode_found() {
        HseExternalTraining e = existing(null);
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        assertThat(service.findByCode("EXT-TRAIN-000005")).isSameAs(e);
    }

    @Test
    void update_nullParametersLeaveFieldsUnchanged() throws IOException {
        HseExternalTraining e = existing("keep.pdf");
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        doUpdate("EXT-TRAIN-000005", null);
        assertThat(e.getEmployeeId()).isEqualTo("E1");
        assertThat(e.getEmployeeName()).isEqualTo("Emp");
        assertThat(e.getTrainingName()).isEqualTo("Crane");
        assertThat(e.getProvider()).isEqualTo("ProvA");
        assertThat(e.getTrainingDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(e.getDuration()).isEqualTo(8.0);
        assertThat(e.getRemarks()).isEqualTo("r");
        assertThat(e.getFilePath()).isEqualTo("keep.pdf");
        verify(files, never()).deleteFile(any());
    }

    @Test
    void update_setsProvidedFields() throws IOException {
        HseExternalTraining e = existing(null);
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        service.update("EXT-TRAIN-000005", "E2", "N", "T", "P2", LocalDate.of(2025, 3, 3), 4.0, "rr", null);
        assertThat(e.getEmployeeId()).isEqualTo("E2");
        assertThat(e.getProvider()).isEqualTo("P2");
        assertThat(e.getDuration()).isEqualTo(4.0);
        assertThat(e.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_notFoundThrows() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> doUpdate("X", null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_replacingFileDeletesOldFileAfterStoringNew() throws IOException {
        HseExternalTraining e = existing("old.pdf");
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenReturn("new.pdf");
        doUpdate("EXT-TRAIN-000005", new MockMultipartFile("file", "n.pdf", "x", new byte[]{1}));
        var order = inOrder(files);
        order.verify(files).storeFile(any(), any());
        order.verify(files).deleteFile("old.pdf");
        assertThat(e.getFilePath()).isEqualTo("new.pdf");
    }

    @Test
    void update_firstFileUploadHasNothingToDelete() throws IOException {
        HseExternalTraining e = existing(null);
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenReturn("new.png");
        doUpdate("EXT-TRAIN-000005", new MockMultipartFile("file", "n.png", "x", new byte[]{1}));
        verify(files, never()).deleteFile(any());
        verify(files).storeFile(any(), eq(FileCategory.HSE_EXTERNAL_TRAINING_IMAGE));
    }

    @Test
    void update_failingUploadKeepsOldFileAndMetadata() throws IOException {
        HseExternalTraining e = existing("old.pdf");
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenThrow(new RuntimeException("bad type"));
        assertThatThrownBy(() -> doUpdate("EXT-TRAIN-000005", new MockMultipartFile("file", "n.exe", "x", new byte[]{1})))
                .isInstanceOf(RuntimeException.class);
        verify(files, never()).deleteFile(any());
        assertThat(e.getFilePath()).isEqualTo("old.pdf");
        verify(repo, never()).save(any());
    }

    @Test
    void update_oldFileDeleteIoErrorIsSwallowed() throws IOException {
        HseExternalTraining e = existing("old.pdf");
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenReturn("new.pdf");
        doThrow(new IOException("locked")).when(files).deleteFile("old.pdf");
        doUpdate("EXT-TRAIN-000005", new MockMultipartFile("file", "n.pdf", "x", new byte[]{1}));
        assertThat(e.getFilePath()).isEqualTo("new.pdf");
    }

    @Test
    void delete_removesFileAndRecord() {
        HseExternalTraining e = existing("old.pdf");
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        service.delete("EXT-TRAIN-000005");
        try {
            verify(files).deleteFile("old.pdf");
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
        verify(repo).delete(e);
    }

    @Test
    void delete_withoutFileOnlyRemovesRecord() throws IOException {
        HseExternalTraining e = existing(null);
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        service.delete("EXT-TRAIN-000005");
        verify(files, never()).deleteFile(any());
        verify(repo).delete(e);
    }

    @Test
    void delete_fileIoErrorStillDeletesRecord() throws IOException {
        HseExternalTraining e = existing("old.pdf");
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        doThrow(new IOException("x")).when(files).deleteFile("old.pdf");
        service.delete("EXT-TRAIN-000005");
        verify(repo).delete(e);
    }

    @Test
    void delete_notFoundThrows() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.delete("X")).isInstanceOf(ResourceNotFoundException.class);
        verify(repo, never()).delete(any());
    }

    @Test
    void deleteFile_clearsFileMetadata() throws IOException {
        HseExternalTraining e = existing("old.pdf");
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        service.deleteFile("EXT-TRAIN-000005");
        verify(files).deleteFile("old.pdf");
        assertThat(e.getFilePath()).isNull();
        assertThat(e.getOriginalFileName()).isNull();
        assertThat(e.getFileSizeBytes()).isNull();
        assertThat(e.getFileExtension()).isNull();
        verify(repo).save(e);
    }

    @Test
    void deleteFile_noFileIsNoOp() throws IOException {
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(existing(null));
        service.deleteFile("EXT-TRAIN-000005");
        verify(files, never()).deleteFile(any());
        verify(repo, never()).save(any());
    }

    @Test
    void deleteFile_ioErrorKeepsMetadata() throws IOException {
        HseExternalTraining e = existing("old.pdf");
        when(repo.findByCode("EXT-TRAIN-000005")).thenReturn(e);
        doThrow(new IOException("x")).when(files).deleteFile("old.pdf");
        assertThatThrownBy(() -> service.deleteFile("EXT-TRAIN-000005")).isInstanceOf(IOException.class);
        assertThat(e.getFilePath()).isEqualTo("old.pdf");
    }

    @Test
    void deleteFile_notFoundThrows() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.deleteFile("X")).isInstanceOf(ResourceNotFoundException.class);
    }
}
