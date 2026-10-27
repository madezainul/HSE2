package ahqpck.hse.safety.training;

import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.model.entity.HseCertification;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.HseCertificationRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.service.FileStorageService;
import ahqpck.hse.safety.service.HseCertificationService;
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
class HseCertificationServiceTest {

    @Mock HseCertificationRepository repo;
    @Mock FileStorageService files;
    @Mock UserRepository users;
    @InjectMocks HseCertificationService service;

    private User admin;
    private final LocalDate issued = LocalDate.of(2024, 6, 1);

    @BeforeEach
    void setUp() {
        admin = new User();
        admin.setUsername("admin");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "x"));
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(repo.save(any(HseCertification.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private HseCertification existing(String filePath) {
        return HseCertification.builder().code("CERT-000005").employeeId("E1").employeeName("Emp")
                .certificateName("NEBOSH").certificateNumber("N-1").issuingBody("Body")
                .issuedDate(issued).expiryDate(issued.plusYears(2)).remarks("r")
                .filePath(filePath).originalFileName(filePath == null ? null : "a.pdf")
                .fileSizeBytes(filePath == null ? null : 10L).fileExtension(filePath == null ? null : ".pdf").build();
    }

    private HseCertification doCreate(LocalDate issuedDate, LocalDate expiry, MockMultipartFile f) throws IOException {
        return service.create("E1", "Emp", "NEBOSH", "N-1", "Body", issuedDate, expiry, "rem", f);
    }

    private HseCertification doUpdate(String code, LocalDate issuedDate, LocalDate expiry, MockMultipartFile f) throws IOException {
        return service.update(code, null, null, null, null, null, issuedDate, expiry, null, f);
    }

    // ── create ───────────────────────────────────────────────────────────

    @Test
    void create_firstCodeWhenTableEmpty() throws IOException {
        when(repo.getMaxCodeNumber()).thenReturn(null);
        assertThat(doCreate(issued, null, null).getCode()).isEqualTo("CERT-000001");
    }

    @Test
    void create_nextCodeIsHighestPlusOneWithPadding() throws IOException {
        when(repo.getMaxCodeNumber()).thenReturn(7);
        assertThat(doCreate(issued, null, null).getCode()).isEqualTo("CERT-000008");
    }

    @Test
    void create_setsCreatedByAndFields() throws IOException {
        HseCertification r = doCreate(issued, issued.plusYears(1), null);
        assertThat(r.getCreatedBy()).isSameAs(admin);
        assertThat(r.getCertificateName()).isEqualTo("NEBOSH");
        assertThat(r.getCertificateNumber()).isEqualTo("N-1");
        assertThat(r.getIssuingBody()).isEqualTo("Body");
        assertThat(r.getExpiryDate()).isEqualTo(issued.plusYears(1));
    }

    @Test
    void create_expiryBeforeIssuedIsRejectedAndNothingSaved() {
        assertThatThrownBy(() -> doCreate(issued, issued.minusDays(1), null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void create_expiryEqualToIssuedIsAllowed() throws IOException {
        assertThat(doCreate(issued, issued, null).getExpiryDate()).isEqualTo(issued);
    }

    @Test
    void create_rejectedDatesDoNotStoreFile() {
        assertThatThrownBy(() -> doCreate(issued, issued.minusDays(1),
                new MockMultipartFile("file", "a.pdf", "x", new byte[]{1})))
                .isInstanceOf(IllegalArgumentException.class);
        verify(files, never()).storeFile(any(), any());
    }

    @Test
    void create_noFileOrEmptyFileDoesNotStore() throws IOException {
        doCreate(issued, null, null);
        doCreate(issued, null, new MockMultipartFile("file", "a.pdf", "x", new byte[0]));
        verify(files, never()).storeFile(any(), any());
    }

    @Test
    void create_documentAndImageCategories() throws IOException {
        when(files.storeFile(any(), any())).thenReturn("p/x");
        HseCertification d = doCreate(issued, null, new MockMultipartFile("file", "a.Pdf", "x", new byte[]{1, 2, 3}));
        assertThat(d.getFileExtension()).isEqualTo(".pdf");
        assertThat(d.getFileSizeBytes()).isEqualTo(3L);
        assertThat(d.getFilePath()).isEqualTo("p/x");
        verify(files).storeFile(any(), eq(FileCategory.HSE_CERTIFICATION_DOCUMENT));
        doCreate(issued, null, new MockMultipartFile("file", "a.jpeg", "x", new byte[]{1}));
        verify(files).storeFile(any(), eq(FileCategory.HSE_CERTIFICATION_IMAGE));
    }

    // ── findByCode ───────────────────────────────────────────────────────

    @Test
    void findByCode_notFoundThrows() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.findByCode("X")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByCode_found() {
        HseCertification e = existing(null);
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        assertThat(service.findByCode("CERT-000005")).isSameAs(e);
    }

    // ── update ───────────────────────────────────────────────────────────

    @Test
    void update_nullParametersLeaveFieldsUnchanged() throws IOException {
        HseCertification e = existing("keep.pdf");
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        doUpdate("CERT-000005", null, null, null);
        assertThat(e.getEmployeeId()).isEqualTo("E1");
        assertThat(e.getCertificateName()).isEqualTo("NEBOSH");
        assertThat(e.getCertificateNumber()).isEqualTo("N-1");
        assertThat(e.getIssuingBody()).isEqualTo("Body");
        assertThat(e.getIssuedDate()).isEqualTo(issued);
        assertThat(e.getExpiryDate()).isEqualTo(issued.plusYears(2));
        assertThat(e.getRemarks()).isEqualTo("r");
        assertThat(e.getFilePath()).isEqualTo("keep.pdf");
    }

    @Test
    void update_notFoundThrows() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> doUpdate("X", null, null, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_expiryBeforeExistingIssuedIsRejected() {
        HseCertification e = existing(null);
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        assertThatThrownBy(() -> doUpdate("CERT-000005", null, issued.minusDays(1), null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void update_issuedAfterExistingExpiryIsRejected() {
        HseCertification e = existing(null);
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        assertThatThrownBy(() -> doUpdate("CERT-000005", e.getExpiryDate().plusDays(1), null, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void update_rejectedDatesDoNotTouchFiles() {
        HseCertification e = existing("old.pdf");
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        assertThatThrownBy(() -> doUpdate("CERT-000005", null, issued.minusDays(1),
                new MockMultipartFile("file", "n.pdf", "x", new byte[]{1})))
                .isInstanceOf(IllegalArgumentException.class);
        verify(files, never()).storeFile(any(), any());
        try {
            verify(files, never()).deleteFile(any());
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    void update_validNewDatesApplied() throws IOException {
        HseCertification e = existing(null);
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        doUpdate("CERT-000005", issued.plusDays(1), issued.plusYears(5), null);
        assertThat(e.getIssuedDate()).isEqualTo(issued.plusDays(1));
        assertThat(e.getExpiryDate()).isEqualTo(issued.plusYears(5));
    }

    @Test
    void update_replacingFileDeletesOldFileAfterStoringNew() throws IOException {
        HseCertification e = existing("old.pdf");
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenReturn("new.pdf");
        doUpdate("CERT-000005", null, null, new MockMultipartFile("file", "n.pdf", "x", new byte[]{1}));
        var order = inOrder(files);
        order.verify(files).storeFile(any(), any());
        order.verify(files).deleteFile("old.pdf");
        assertThat(e.getFilePath()).isEqualTo("new.pdf");
    }

    @Test
    void update_failingUploadKeepsOldFile() throws IOException {
        HseCertification e = existing("old.pdf");
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenThrow(new RuntimeException("bad type"));
        assertThatThrownBy(() -> doUpdate("CERT-000005", null, null,
                new MockMultipartFile("file", "n.exe", "x", new byte[]{1})))
                .isInstanceOf(RuntimeException.class);
        verify(files, never()).deleteFile(any());
        assertThat(e.getFilePath()).isEqualTo("old.pdf");
        verify(repo, never()).save(any());
    }

    @Test
    void update_oldFileDeleteIoErrorIsSwallowed() throws IOException {
        HseCertification e = existing("old.pdf");
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        when(files.storeFile(any(), any())).thenReturn("new.pdf");
        doThrow(new IOException("locked")).when(files).deleteFile("old.pdf");
        doUpdate("CERT-000005", null, null, new MockMultipartFile("file", "n.pdf", "x", new byte[]{1}));
        assertThat(e.getFilePath()).isEqualTo("new.pdf");
    }

    // ── delete / deleteFile ──────────────────────────────────────────────

    @Test
    void delete_removesFileAndRecord() throws IOException {
        HseCertification e = existing("old.pdf");
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        service.delete("CERT-000005");
        verify(files).deleteFile("old.pdf");
        verify(repo).delete(e);
    }

    @Test
    void delete_withoutFileOnlyRemovesRecord() throws IOException {
        HseCertification e = existing(null);
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        service.delete("CERT-000005");
        verify(files, never()).deleteFile(any());
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
        HseCertification e = existing("old.pdf");
        when(repo.findByCode("CERT-000005")).thenReturn(e);
        service.deleteFile("CERT-000005");
        verify(files).deleteFile("old.pdf");
        assertThat(e.getFilePath()).isNull();
        assertThat(e.getOriginalFileName()).isNull();
        assertThat(e.getFileSizeBytes()).isNull();
        assertThat(e.getFileExtension()).isNull();
        verify(repo).save(e);
    }

    @Test
    void deleteFile_noFileIsNoOp() throws IOException {
        when(repo.findByCode("CERT-000005")).thenReturn(existing(null));
        service.deleteFile("CERT-000005");
        verify(files, never()).deleteFile(any());
        verify(repo, never()).save(any());
    }

    @Test
    void deleteFile_notFoundThrows() {
        when(repo.findByCode("X")).thenReturn(null);
        assertThatThrownBy(() -> service.deleteFile("X")).isInstanceOf(ResourceNotFoundException.class);
    }

    // ── status ───────────────────────────────────────────────────────────

    private HseCertification withExpiry(LocalDate d) {
        return HseCertification.builder().expiryDate(d).build();
    }

    @Test
    void status_noExpiry() {
        HseCertification c = withExpiry(null);
        assertThat(c.getStatus()).isEqualTo("No Expiry");
        assertThat(c.getStatusBadgeClass()).isEqualTo("badge bg-secondary");
    }

    @Test
    void status_yesterdayIsExpired() {
        HseCertification c = withExpiry(LocalDate.now().minusDays(1));
        assertThat(c.getStatus()).isEqualTo("Expired");
        assertThat(c.getStatusBadgeClass()).isEqualTo("badge bg-danger");
    }

    @Test
    void status_todayIsExpiringSoon() {
        HseCertification c = withExpiry(LocalDate.now());
        assertThat(c.getStatus()).isEqualTo("Expiring Soon");
        assertThat(c.getStatusBadgeClass()).isEqualTo("badge bg-warning text-dark");
    }

    @Test
    void status_plus30DaysIsExpiringSoon() {
        assertThat(withExpiry(LocalDate.now().plusDays(30)).getStatus()).isEqualTo("Expiring Soon");
    }

    @Test
    void status_plus31DaysIsValid() {
        HseCertification c = withExpiry(LocalDate.now().plusDays(31));
        assertThat(c.getStatus()).isEqualTo("Valid");
        assertThat(c.getStatusBadgeClass()).isEqualTo("badge bg-success");
    }

    @Test
    void status_farPastIsExpired() {
        assertThat(withExpiry(LocalDate.now().minusYears(10)).getStatus()).isEqualTo("Expired");
    }
}
