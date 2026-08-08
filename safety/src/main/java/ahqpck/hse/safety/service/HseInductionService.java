package ahqpck.hse.safety.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.model.entity.HseInduction;
import ahqpck.hse.safety.model.entity.HseInductionFile;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.HseInductionFileRepository;
import ahqpck.hse.safety.repository.HseInductionRepository;
import ahqpck.hse.safety.util.FileCategory;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class HseInductionService {

    private final HseInductionRepository inductionRepo;
    private final HseInductionFileRepository fileRepo;
    private final FileStorageService fileStorageService;

    /**
     * Create a new HSE Induction record
     */
    @Transactional
    public HseInduction create(HseInduction induction, User createdBy) {
        log.info("Creating HSE Induction record: {}", induction.getInductionName());
        induction.setCreatedBy(createdBy);
        return inductionRepo.save(induction);
    }

    /**
     * Get all HSE Induction records with pagination
     */
    public Page<HseInduction> findAll(Pageable pageable) {
        log.info("Fetching HSE Induction records - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return inductionRepo.findAll(pageable);
    }

    /**
     * Get a single HSE Induction record by code
     */
    public HseInduction findByCode(String code) {
        log.info("Fetching HSE Induction record by code: {}", code);
        return inductionRepo.findByCode(code);
    }

    /**
     * Update an existing HSE Induction record
     */
    @Transactional
    public HseInduction update(String code, HseInduction updateData) {
        log.info("Updating HSE Induction record: {}", code);
        HseInduction induction = inductionRepo.findByCode(code);
        if (induction == null) {
            throw new IllegalArgumentException("HSE Induction record not found: " + code);
        }

        if (updateData.getInductionName() != null) {
            induction.setInductionName(updateData.getInductionName());
        }
        if (updateData.getCategory() != null) {
            induction.setCategory(updateData.getCategory());
        }
        if (updateData.getInductionDate() != null) {
            induction.setInductionDate(updateData.getInductionDate());
        }
        if (updateData.getValue() != null) {
            induction.setValue(updateData.getValue());
        }
        if (updateData.getDescription() != null) {
            induction.setDescription(updateData.getDescription());
        }

        return inductionRepo.save(induction);
    }

    /**
     * Delete an HSE Induction record
     */
    @Transactional
    public void delete(String code) {
        log.info("Deleting HSE Induction record: {}", code);
        HseInduction induction = inductionRepo.findByCode(code);
        if (induction != null) {
            inductionRepo.delete(induction);
        }
    }

    /**
     * Save a file for an HSE Induction record
     *
     * @param inductionCode the induction code
     * @param file         the multipart file
     * @param fileType     the file type (IMAGE or DOCUMENT)
     * @return the saved file path
     */
    @Transactional
    public HseInductionFile saveFile(String inductionCode, MultipartFile file, String fileType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        HseInduction induction = inductionRepo.findByCode(inductionCode);
        if (induction == null) {
            throw new IllegalArgumentException("HSE Induction record not found: " + inductionCode);
        }

        log.info("Saving file for HSE Induction: {} - fileType: {}", inductionCode, fileType);

        // Determine file category and store using FileStorageService
        FileCategory category = "DOCUMENT".equalsIgnoreCase(fileType)
                ? FileCategory.HSE_INDUCTION_DOCUMENT
                : FileCategory.HSE_INDUCTION_IMAGE;

        String filePath = fileStorageService.storeFile(file, category);
        log.info("File stored at: {}", filePath);

        // Save file metadata to database
        HseInductionFile inductionFile = HseInductionFile.builder()
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .fileType(HseInductionFile.FileType.valueOf(fileType.toUpperCase()))
                .hseInduction(induction)
                .build();

        inductionFile = fileRepo.save(inductionFile);
        log.info("File metadata saved with ID: {}", inductionFile.getId());

        return inductionFile;
    }

    /**
     * Delete a file associated with an HSE Induction record
     */
    @Transactional
    public void deleteFile(String fileId) throws IOException {
        log.info("Deleting file with ID: {}", fileId);
        HseInductionFile file = fileRepo.findById(fileId).orElse(null);
        if (file != null) {
            fileStorageService.deleteFile(file.getFilePath());
            fileRepo.delete(file);
            log.info("File deleted successfully");
        }
    }

    /**
     * Get all files for an HSE Induction record
     */
    public java.util.List<HseInductionFile> getFilesByCode(String code) {
        log.info("Fetching files for HSE Induction: {}", code);
        return fileRepo.findByHseInductionCode(code);
    }
}
