package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.entity.HseInduction;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.HseInductionRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.util.FileCategory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class HseTrainingService {

    private final HseInductionRepository inductionRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    /**
     * Get all HSE Induction records with pagination
     */
    public Page<HseInduction> findAll(Pageable pageable) {
        log.info("Fetching all HSE Induction records - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return inductionRepository.findAll(pageable);
    }

    /**
     * Get HSE Induction record by code
     */
    public HseInduction findByCode(String code) {
        log.info("Fetching HSE Induction record by code: {}", code);
        return inductionRepository.findByCode(code);
    }

    /**
     * Create a new HSE Induction record with file upload
     */
    @Transactional
    public HseInduction create(String traineeId, String traineeName, LocalDate inductionDate,
                               String trainers, String trainerId, MultipartFile file) throws IOException {
        log.info("Creating HSE Induction record for trainee: {} ({})", traineeId, traineeName);

        String code = generateCode();
        User createdBy = getCurrentUser();

        HseInduction induction = HseInduction.builder()
                .code(code)
                .traineeId(traineeId)
                .traineeName(traineeName)
                .inductionDate(inductionDate)
                .trainers(trainers)
                .trainerId(trainerId)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Handle file upload if provided
        if (file != null && !file.isEmpty()) {
            induction = uploadFile(induction, file);
        }

        HseInduction saved = inductionRepository.save(induction);
        log.info("HSE Induction record created with code: {}", saved.getCode());
        return saved;
    }

    /**
     * Update an existing HSE Induction record
     */
    @Transactional
    public HseInduction update(String code, String traineeId, String traineeName, LocalDate inductionDate,
                               String trainers, String trainerId, MultipartFile file) throws IOException {
        log.info("Updating HSE Induction record: {}", code);

        HseInduction induction = inductionRepository.findByCode(code);
        if (induction == null) {
            throw new RuntimeException("HSE Induction record not found: " + code);
        }

        // Update basic fields
        if (traineeId != null) induction.setTraineeId(traineeId);
        if (traineeName != null) induction.setTraineeName(traineeName);
        if (inductionDate != null) induction.setInductionDate(inductionDate);
        if (trainers != null) induction.setTrainers(trainers);
        if (trainerId != null) induction.setTrainerId(trainerId);

        // Handle file upload if provided
        if (file != null && !file.isEmpty()) {
            // Delete old file if exists
            if (induction.getFilePath() != null) {
                try {
                    fileStorageService.deleteFile(induction.getFilePath());
                    log.info("Old file deleted: {}", induction.getFilePath());
                } catch (IOException e) {
                    log.warn("Could not delete old file: {}", induction.getFilePath(), e);
                }
            }
            induction = uploadFile(induction, file);
        }

        induction.setUpdatedAt(LocalDateTime.now());
        HseInduction updated = inductionRepository.save(induction);
        log.info("HSE Induction record updated: {}", code);
        return updated;
    }

    /**
     * Delete an HSE Induction record and its file
     */
    @Transactional
    public void delete(String code) throws IOException {
        log.info("Deleting HSE Induction record: {}", code);

        HseInduction induction = inductionRepository.findByCode(code);
        if (induction == null) {
            throw new RuntimeException("HSE Induction record not found: " + code);
        }

        // Delete file if exists
        if (induction.getFilePath() != null) {
            try {
                fileStorageService.deleteFile(induction.getFilePath());
                log.info("File deleted: {}", induction.getFilePath());
            } catch (IOException e) {
                log.warn("Could not delete file: {}", induction.getFilePath(), e);
            }
        }

        inductionRepository.delete(induction);
        log.info("HSE Induction record deleted: {}", code);
    }

    /**
     * Delete a file from an HSE Induction record (without deleting the record)
     */
    @Transactional
    public void deleteFile(String code) throws IOException {
        log.info("Deleting file from HSE Induction record: {}", code);

        HseInduction induction = inductionRepository.findByCode(code);
        if (induction == null) {
            throw new RuntimeException("HSE Induction record not found: " + code);
        }

        if (induction.getFilePath() != null) {
            fileStorageService.deleteFile(induction.getFilePath());
            induction.setFilePath(null);
            induction.setOriginalFileName(null);
            induction.setFileSizeBytes(null);
            induction.setFileExtension(null);
            induction.setUpdatedAt(LocalDateTime.now());
            inductionRepository.save(induction);
            log.info("File deleted from HSE Induction record: {}", code);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Upload file and set file properties on the induction record
     */
    private HseInduction uploadFile(HseInduction induction, MultipartFile file) throws IOException {
        // Determine file category based on file type
        String extension = getFileExtension(file.getOriginalFilename());
        FileCategory category = isImageFile(extension)
                ? FileCategory.HSE_INDUCTION_IMAGE
                : FileCategory.HSE_INDUCTION_DOCUMENT;

        String filePath = fileStorageService.storeFile(file, category);
        String originalName = Objects.requireNonNull(file.getOriginalFilename(), "File name must not be null");

        induction.setFilePath(filePath);
        induction.setOriginalFileName(originalName);
        induction.setFileSizeBytes(file.getSize());
        induction.setFileExtension(extension);

        log.info("File uploaded for HSE Induction: {} - path: {}", induction.getCode(), filePath);
        return induction;
    }

    /**
     * Generate sequential induction code (IND-000001, IND-000002, etc.)
     */
    private String generateCode() {
        String prefix = "IND";
        Integer maxNumber = inductionRepository.getMaxCodeNumber();
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;

        String code;
        int attempts = 0;
        do {
            code = String.format("%s-%06d", prefix, nextNumber + attempts);
            attempts++;
        } while (inductionRepository.findByCode(code) != null && attempts < 10);

        return code;
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return "." + filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Check if file is an image based on extension
     */
    private boolean isImageFile(String extension) {
        return extension.matches("\\.(?:jpg|jpeg|png|webp)");
    }

    /**
     * Get current authenticated user
     */
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
