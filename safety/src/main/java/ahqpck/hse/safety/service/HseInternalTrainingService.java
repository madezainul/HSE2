package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.entity.HseInternalTraining;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.HseInternalTrainingRepository;
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
public class HseInternalTrainingService {

    private final HseInternalTrainingRepository internalTrainingRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    /**
     * Get all HSE Internal Training records with pagination
     */
    public Page<HseInternalTraining> findAll(Pageable pageable) {
        log.info("Fetching all HSE Internal Training records - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return internalTrainingRepository.findAll(pageable);
    }

    /**
     * Get HSE Internal Training record by code
     */
    public HseInternalTraining findByCode(String code) {
        log.info("Fetching HSE Internal Training record by code: {}", code);
        return internalTrainingRepository.findByCode(code);
    }

    /**
     * Create a new HSE Internal Training record with file upload
     */
    @Transactional
    public HseInternalTraining create(String employeeId, String employeeName, String trainingName,
                                     LocalDate trainingDate, Double duration, String trainerName,
                                     String trainerId, String remarks, MultipartFile file) throws IOException {
        log.info("Creating HSE Internal Training record for employee: {} ({})", employeeId, employeeName);

        String code = generateCode();
        User createdBy = getCurrentUser();

        HseInternalTraining training = HseInternalTraining.builder()
                .code(code)
                .employeeId(employeeId)
                .employeeName(employeeName)
                .trainingName(trainingName)
                .trainingDate(trainingDate)
                .duration(duration)
                .trainerName(trainerName)
                .trainerId(trainerId)
                .remarks(remarks)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Handle file upload if provided
        if (file != null && !file.isEmpty()) {
            training = uploadFile(training, file);
        }

        HseInternalTraining saved = internalTrainingRepository.save(training);
        log.info("HSE Internal Training record created with code: {}", saved.getCode());
        return saved;
    }

    /**
     * Update an existing HSE Internal Training record
     */
    @Transactional
    public HseInternalTraining update(String code, String employeeId, String employeeName,
                                     String trainingName, LocalDate trainingDate, Double duration,
                                     String trainerName, String trainerId, String remarks,
                                     MultipartFile file) throws IOException {
        log.info("Updating HSE Internal Training record: {}", code);

        HseInternalTraining training = internalTrainingRepository.findByCode(code);
        if (training == null) {
            throw new RuntimeException("HSE Internal Training record not found: " + code);
        }

        // Update basic fields
        if (employeeId != null) training.setEmployeeId(employeeId);
        if (employeeName != null) training.setEmployeeName(employeeName);
        if (trainingName != null) training.setTrainingName(trainingName);
        if (trainingDate != null) training.setTrainingDate(trainingDate);
        if (duration != null) training.setDuration(duration);
        if (trainerName != null) training.setTrainerName(trainerName);
        if (trainerId != null) training.setTrainerId(trainerId);
        if (remarks != null) training.setRemarks(remarks);

        // Handle file upload if provided
        if (file != null && !file.isEmpty()) {
            // Delete old file if exists
            if (training.getFilePath() != null) {
                try {
                    fileStorageService.deleteFile(training.getFilePath());
                    log.info("Old file deleted: {}", training.getFilePath());
                } catch (IOException e) {
                    log.warn("Could not delete old file: {}", training.getFilePath(), e);
                }
            }
            training = uploadFile(training, file);
        }

        training.setUpdatedAt(LocalDateTime.now());
        HseInternalTraining updated = internalTrainingRepository.save(training);
        log.info("HSE Internal Training record updated: {}", code);
        return updated;
    }

    /**
     * Delete an HSE Internal Training record and its file
     */
    @Transactional
    public void delete(String code) throws IOException {
        log.info("Deleting HSE Internal Training record: {}", code);

        HseInternalTraining training = internalTrainingRepository.findByCode(code);
        if (training == null) {
            throw new RuntimeException("HSE Internal Training record not found: " + code);
        }

        // Delete file if exists
        if (training.getFilePath() != null) {
            try {
                fileStorageService.deleteFile(training.getFilePath());
                log.info("File deleted: {}", training.getFilePath());
            } catch (IOException e) {
                log.warn("Could not delete file: {}", training.getFilePath(), e);
            }
        }

        internalTrainingRepository.delete(training);
        log.info("HSE Internal Training record deleted: {}", code);
    }

    /**
     * Delete a file from an HSE Internal Training record (without deleting the record)
     */
    @Transactional
    public void deleteFile(String code) throws IOException {
        log.info("Deleting file from HSE Internal Training record: {}", code);

        HseInternalTraining training = internalTrainingRepository.findByCode(code);
        if (training == null) {
            throw new RuntimeException("HSE Internal Training record not found: " + code);
        }

        if (training.getFilePath() != null) {
            fileStorageService.deleteFile(training.getFilePath());
            training.setFilePath(null);
            training.setOriginalFileName(null);
            training.setFileSizeBytes(null);
            training.setFileExtension(null);
            training.setUpdatedAt(LocalDateTime.now());
            internalTrainingRepository.save(training);
            log.info("File deleted from HSE Internal Training record: {}", code);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Upload file and set file properties on the training record
     */
    private HseInternalTraining uploadFile(HseInternalTraining training, MultipartFile file) throws IOException {
        // Determine file category based on file type
        String extension = getFileExtension(file.getOriginalFilename());
        FileCategory category = isImageFile(extension)
                ? FileCategory.HSE_INTERNAL_TRAINING_IMAGE
                : FileCategory.HSE_INTERNAL_TRAINING_DOCUMENT;

        String filePath = fileStorageService.storeFile(file, category);
        String originalName = Objects.requireNonNull(file.getOriginalFilename(), "File name must not be null");

        training.setFilePath(filePath);
        training.setOriginalFileName(originalName);
        training.setFileSizeBytes(file.getSize());
        training.setFileExtension(extension);

        log.info("File uploaded for HSE Internal Training: {} - path: {}", training.getCode(), filePath);
        return training;
    }

    /**
     * Generate sequential training code (INT-TRAIN-000001, INT-TRAIN-000002, etc.)
     */
    private String generateCode() {
        String prefix = "INT-TRAIN";
        Integer maxNumber = internalTrainingRepository.getMaxCodeNumber();
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;

        String code;
        int attempts = 0;
        do {
            code = String.format("%s-%06d", prefix, nextNumber + attempts);
            attempts++;
        } while (internalTrainingRepository.findByCode(code) != null && attempts < 10);

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
