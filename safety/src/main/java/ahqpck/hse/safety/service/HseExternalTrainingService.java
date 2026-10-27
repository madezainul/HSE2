package ahqpck.hse.safety.service;

import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.model.entity.HseExternalTraining;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.HseExternalTrainingRepository;
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
public class HseExternalTrainingService {

    private final HseExternalTrainingRepository externalTrainingRepository;
    private final FileStorageService fileStorageService;
    private final UserRepository userRepository;

    /**
     * Get all HSE External Training records with pagination
     */
    public Page<HseExternalTraining> findAll(Pageable pageable) {
        log.info("Fetching all HSE External Training records - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return externalTrainingRepository.findAll(pageable);
    }

    /**
     * Get HSE External Training record by code
     *
     * @throws ResourceNotFoundException if no record has this code
     */
    public HseExternalTraining findByCode(String code) {
        log.info("Fetching HSE External Training record by code: {}", code);
        HseExternalTraining training = externalTrainingRepository.findByCode(code);
        if (training == null) {
            throw new ResourceNotFoundException("HSE External Training record not found: " + code);
        }
        return training;
    }

    /**
     * Create a new HSE External Training record with file upload
     */
    @Transactional
    public HseExternalTraining create(String employeeId, String employeeName, String trainingName,
                                      String provider, LocalDate trainingDate, Double duration,
                                      String remarks, MultipartFile file) throws IOException {
        log.info("Creating HSE External Training record for employee: {} ({})", employeeId, employeeName);

        HseExternalTraining training = HseExternalTraining.builder()
                .code(generateCode())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .trainingName(trainingName)
                .provider(provider)
                .trainingDate(trainingDate)
                .duration(duration)
                .remarks(remarks)
                .createdBy(getCurrentUser())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (file != null && !file.isEmpty()) {
            uploadFile(training, file);
        }

        HseExternalTraining saved = externalTrainingRepository.save(training);
        log.info("HSE External Training record created with code: {}", saved.getCode());
        return saved;
    }

    /**
     * Update an existing HSE External Training record
     */
    @Transactional
    public HseExternalTraining update(String code, String employeeId, String employeeName,
                                      String trainingName, String provider, LocalDate trainingDate,
                                      Double duration, String remarks, MultipartFile file) throws IOException {
        log.info("Updating HSE External Training record: {}", code);

        HseExternalTraining training = findByCode(code);

        if (employeeId != null) training.setEmployeeId(employeeId);
        if (employeeName != null) training.setEmployeeName(employeeName);
        if (trainingName != null) training.setTrainingName(trainingName);
        if (provider != null) training.setProvider(provider);
        if (trainingDate != null) training.setTrainingDate(trainingDate);
        if (duration != null) training.setDuration(duration);
        if (remarks != null) training.setRemarks(remarks);

        if (file != null && !file.isEmpty()) {
            String oldFilePath = training.getFilePath();
            uploadFile(training, file);
            deleteStoredFile(oldFilePath);
        }

        training.setUpdatedAt(LocalDateTime.now());
        HseExternalTraining updated = externalTrainingRepository.save(training);
        log.info("HSE External Training record updated: {}", code);
        return updated;
    }

    /**
     * Delete an HSE External Training record and its file
     */
    @Transactional
    public void delete(String code) {
        log.info("Deleting HSE External Training record: {}", code);

        HseExternalTraining training = findByCode(code);
        deleteStoredFile(training.getFilePath());

        externalTrainingRepository.delete(training);
        log.info("HSE External Training record deleted: {}", code);
    }

    /**
     * Delete a file from an HSE External Training record (without deleting the record)
     */
    @Transactional
    public void deleteFile(String code) throws IOException {
        log.info("Deleting file from HSE External Training record: {}", code);

        HseExternalTraining training = findByCode(code);

        if (training.getFilePath() != null) {
            fileStorageService.deleteFile(training.getFilePath());
            training.setFilePath(null);
            training.setOriginalFileName(null);
            training.setFileSizeBytes(null);
            training.setFileExtension(null);
            training.setUpdatedAt(LocalDateTime.now());
            externalTrainingRepository.save(training);
            log.info("File deleted from HSE External Training record: {}", code);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────

    /**
     * Upload file and set file properties on the training record
     */
    private void uploadFile(HseExternalTraining training, MultipartFile file) throws IOException {
        String extension = getFileExtension(file.getOriginalFilename());
        FileCategory category = isImageFile(extension)
                ? FileCategory.HSE_EXTERNAL_TRAINING_IMAGE
                : FileCategory.HSE_EXTERNAL_TRAINING_DOCUMENT;

        String filePath = fileStorageService.storeFile(file, category);
        String originalName = Objects.requireNonNull(file.getOriginalFilename(), "File name must not be null");

        training.setFilePath(filePath);
        training.setOriginalFileName(originalName);
        training.setFileSizeBytes(file.getSize());
        training.setFileExtension(extension);

        log.info("File uploaded for HSE External Training: {} - path: {}", training.getCode(), filePath);
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
     * Generate sequential training code (EXT-TRAIN-000001, EXT-TRAIN-000002, etc.)
     */
    private String generateCode() {
        Integer maxNumber = externalTrainingRepository.getMaxCodeNumber();
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;
        return String.format("EXT-TRAIN-%06d", nextNumber);
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
