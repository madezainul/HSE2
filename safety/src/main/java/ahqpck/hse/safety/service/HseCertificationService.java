package ahqpck.hse.safety.service;

import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.model.entity.HseCertification;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.HseCertificationRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.util.FileCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class HseCertificationService {

    private final HseCertificationRepository certificationRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    /**
     * Get all HSE Certification records with pagination
     */
    public Page<HseCertification> findAll(Pageable pageable) {
        log.info("Fetching all HSE Certification records - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return certificationRepository.findAll(pageable);
    }

    /**
     * Get HSE Certification record by code
     *
     * @throws ResourceNotFoundException if no record has this code
     */
    public HseCertification findByCode(String code) {
        log.info("Fetching HSE Certification record by code: {}", code);
        HseCertification certification = certificationRepository.findByCode(code);
        if (certification == null) {
            throw new ResourceNotFoundException("HSE Certification record not found: " + code);
        }
        return certification;
    }

    /**
     * Create a new HSE Certification record with file upload
     */
    @Transactional
    public HseCertification create(String employeeId, String employeeName, String certificateName,
                                   String certificateNumber, String issuingBody, LocalDate issuedDate,
                                   LocalDate expiryDate, String remarks, MultipartFile file) throws IOException {
        log.info("Creating HSE Certification record for employee: {} ({})", employeeId, employeeName);

        validateDates(issuedDate, expiryDate);

        HseCertification certification = HseCertification.builder()
                .code(generateCode())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .certificateName(certificateName)
                .certificateNumber(certificateNumber)
                .issuingBody(issuingBody)
                .issuedDate(issuedDate)
                .expiryDate(expiryDate)
                .remarks(remarks)
                .createdBy(getCurrentUser())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (file != null && !file.isEmpty()) {
            uploadFile(certification, file);
        }

        HseCertification saved = certificationRepository.save(certification);
        log.info("HSE Certification record created with code: {}", saved.getCode());
        return saved;
    }

    /**
     * Update an existing HSE Certification record
     */
    @Transactional
    public HseCertification update(String code, String employeeId, String employeeName,
                                   String certificateName, String certificateNumber, String issuingBody,
                                   LocalDate issuedDate, LocalDate expiryDate, String remarks,
                                   MultipartFile file) throws IOException {
        log.info("Updating HSE Certification record: {}", code);

        HseCertification certification = findByCode(code);

        if (employeeId != null) certification.setEmployeeId(employeeId);
        if (employeeName != null) certification.setEmployeeName(employeeName);
        if (certificateName != null) certification.setCertificateName(certificateName);
        if (certificateNumber != null) certification.setCertificateNumber(certificateNumber);
        if (issuingBody != null) certification.setIssuingBody(issuingBody);
        if (issuedDate != null) certification.setIssuedDate(issuedDate);
        if (expiryDate != null) certification.setExpiryDate(expiryDate);
        if (remarks != null) certification.setRemarks(remarks);

        validateDates(certification.getIssuedDate(), certification.getExpiryDate());

        if (file != null && !file.isEmpty()) {
            String oldFilePath = certification.getFilePath();
            uploadFile(certification, file);
            deleteStoredFile(oldFilePath);
        }

        certification.setUpdatedAt(LocalDateTime.now());
        HseCertification updated = certificationRepository.save(certification);
        log.info("HSE Certification record updated: {}", code);
        return updated;
    }

    /**
     * Delete an HSE Certification record and its file
     */
    @Transactional
    public void delete(String code) {
        log.info("Deleting HSE Certification record: {}", code);

        HseCertification certification = findByCode(code);
        deleteStoredFile(certification.getFilePath());

        certificationRepository.delete(certification);
        log.info("HSE Certification record deleted: {}", code);
    }

    /**
     * Delete a file from an HSE Certification record (without deleting the record)
     */
    @Transactional
    public void deleteFile(String code) throws IOException {
        log.info("Deleting file from HSE Certification record: {}", code);

        HseCertification certification = findByCode(code);

        if (certification.getFilePath() != null) {
            fileStorageService.deleteFile(certification.getFilePath());
            certification.setFilePath(null);
            certification.setOriginalFileName(null);
            certification.setFileSizeBytes(null);
            certification.setFileExtension(null);
            certification.setUpdatedAt(LocalDateTime.now());
            certificationRepository.save(certification);
            log.info("File deleted from HSE Certification record: {}", code);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void validateDates(LocalDate issuedDate, LocalDate expiryDate) {
        if (expiryDate != null && issuedDate != null && expiryDate.isBefore(issuedDate)) {
            throw new IllegalArgumentException("Expiry date cannot be before issued date");
        }
    }

    /**
     * Upload file and set file properties on the certification record
     */
    private void uploadFile(HseCertification certification, MultipartFile file) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());
        FileCategory category = isImageFile(extension)
                ? FileCategory.HSE_CERTIFICATION_IMAGE
                : FileCategory.HSE_CERTIFICATION_DOCUMENT;

        String filePath = fileStorageService.storeFile(file, category);
        String originalName = Objects.requireNonNull(file.getOriginalFilename(), "File name must not be null");

        certification.setFilePath(filePath);
        certification.setOriginalFileName(originalName);
        certification.setFileSizeBytes(file.getSize());
        certification.setFileExtension(extension);

        log.info("File uploaded for HSE Certification: {} - path: {}", certification.getCode(), filePath);
    }

    /**
     * Delete a stored file, logging instead of failing if it cannot be removed
     */
    private void deleteStoredFile(String filePath) {
        if (filePath == null) {
            return;
        }
        try {
            fileStorageService.deleteFile(filePath);
            log.info("File deleted: {}", filePath);
        } catch (IOException e) {
            log.warn("Could not delete file: {}", filePath, e);
        }
    }

    /**
     * Generate sequential certification code (CERT-000001, CERT-000002, etc.)
     */
    private String generateCode() {
        Integer maxNumber = certificationRepository.getMaxCodeNumber();
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("CERT-%06d", nextNumber);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return "." + filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private boolean isImageFile(String extension) {
        return extension.matches("\\.(?:jpg|jpeg|png|webp)");
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
