package ahqpck.hse.safety.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import ahqpck.hse.safety.model.dto.ObservationDTO;
import ahqpck.hse.safety.model.dto.ObservationFileDTO;
import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.entity.ObservationFile;
import ahqpck.hse.safety.model.entity.ObservationComment;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.repository.ObservationFileRepository;
import ahqpck.hse.safety.repository.ObservationRepository;
import ahqpck.hse.safety.repository.ObservationCommentRepository;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Slf4j
@Service
public class ObservationService {
    
    @Autowired private ObservationRepository observationRepo;
    @Autowired private ObservationFileRepository fileRepository;
    @Autowired private AreaRepository areaRepository;
    @Autowired private ObservationCommentRepository commentRepository;

    @Transactional
    public Observation save(ObservationDTO dto) {
        Observation obs = dto.toEntity();
        
        // Generate observation code if not provided
        if (obs.getCode() == null || obs.getCode().isEmpty()) {
            obs.setCode(generateObservationCode());
        }
        
        // Look up Area from database
        if (dto.getArea() != null && dto.getArea().getCode() != null) {
            Area area = areaRepository.findByCode(dto.getArea().getCode());
            if (area != null) {
                obs.setArea(area);
            } else {
                throw new IllegalArgumentException("Area with code '" + dto.getArea().getCode() + "' not found");
            }
        } else {
            throw new IllegalArgumentException("Area is required");
        }
        
        obs = observationRepo.save(obs);

        // Save files if provided
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            for (ObservationFileDTO fileDTO : dto.getFiles()) {
                ObservationFile file = fileDTO.toEntity(obs);
                fileRepository.save(file);
            }
        }
        return obs;
    }
    
    /**
     * Generate a unique observation code with auto-increment
     * Format: OBS-000001, OBS-000002, etc.
     */
    private String generateObservationCode() {
        long maxNumber = 0;
        String newCode = "";
        int attempts = 0;
        
        // Try up to 10 times to generate a unique code
        while (attempts < 10) {
            try {
                // Get all observations and find the maximum numeric value
                java.util.List<Observation> allObservations = observationRepo.findAll();
                
                for (Observation obs : allObservations) {
                    if (obs.getCode() != null && obs.getCode().startsWith("OBS-")) {
                        try {
                            // Extract numeric part (e.g., "000001" from "OBS-000001")
                            String numericPart = obs.getCode().substring(4);
                            long number = Long.parseLong(numericPart);
                            if (number > maxNumber) {
                                maxNumber = number;
                            }
                        } catch (NumberFormatException e) {
                            // Skip invalid codes
                        }
                    }
                }
                
                // Generate next code
                long nextNumber = maxNumber + 1;
                newCode = String.format("OBS-%06d", nextNumber);
                
                // Check if this code already exists
                Observation existing = observationRepo.findByCode(newCode);
                if (existing == null) {
                    log.debug("Generated observation code: {}", newCode);
                    return newCode;
                }
                
                // If code exists, increment and try again
                maxNumber = nextNumber;
                attempts++;
                
            } catch (Exception e) {
                log.error("Error generating observation code: {}", e.getMessage());
                attempts++;
            }
        }
        
        throw new RuntimeException("Failed to generate unique observation code after " + attempts + " attempts");
    }
    
    /**
     * Save a file for an observation
     * @param observationCode the observation code
     * @param file the multipart file
     * @param fileType the file type (IMAGE or DOCUMENT)
     * @return the saved observation file DTO
     */
    @Transactional
    public ObservationFileDTO saveFile(String observationCode, MultipartFile file, String fileType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        Observation obs = observationRepo.findByCode(observationCode);
        if (obs == null) {
            throw new IllegalArgumentException("Observation not found: " + observationCode);
        }
        
        // Determine file directory based on type
        String fileDir = "DOCUMENT".equalsIgnoreCase(fileType) ? "observation/documents" : "observation/images";
        Path uploadPath = Paths.get("uploads/" + fileDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);
        
        // Generate filename
        String originalFileName = file.getOriginalFilename();
        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path filePath = uploadPath.resolve(fileName);
        
        // Save file to disk
        Files.write(filePath, file.getBytes());
        
        // Save to database
        ObservationFile obsFile = ObservationFile.builder()
                .fileName(originalFileName)
                .filePath(fileDir + "/" + fileName)
                .fileType(ObservationFile.FileType.valueOf(fileType.toUpperCase()))
                .uploadedAt(LocalDateTime.now())
                .observation(obs)
                .build();
        
        obsFile = fileRepository.save(obsFile);
        return ObservationFileDTO.fromEntity(obsFile);
    }
    
    /**
     * Update an existing observation
     * @param observationCode the observation code to update
     * @param dto the updated observation data
     * @return the updated observation
     */
    @Transactional
    public Observation update(String observationCode, ObservationDTO dto) {
        Observation obs = observationRepo.findByCode(observationCode);
        if (obs == null) {
            throw new IllegalArgumentException("Observation not found: " + observationCode);
        }
        
        // Update fields
        if (dto.getObservationDate() != null) {
            obs.setObservationDate(dto.getObservationDate());
        }
        if (dto.getDescription() != null) {
            obs.setDescription(dto.getDescription());
        }
        if (dto.getCategory() != null) {
            obs.setCategory(Observation.Category.valueOf(dto.getCategory()));
        }
        if (dto.getType() != null) {
            obs.setType(Observation.Type.valueOf(dto.getType()));
        }
        if (dto.getStatus() != null) {
            obs.setStatus(Observation.Status.valueOf(dto.getStatus()));
        }
        if (dto.getMitigation() != null) {
            obs.setMitigation(Observation.Mitigation.valueOf(dto.getMitigation()));
        }
        if (dto.getObserverName() != null) {
            obs.setObserver(dto.getObserverName());
        }
        if (dto.getObserverId() != null) {
            obs.setObserverEmployeeId(dto.getObserverId());
        }
        if (dto.getResponsibleName() != null) {
            obs.setResponsible(dto.getResponsibleName());
        }
        if (dto.getResponsibleId() != null) {
            obs.setResponsibleEmployeeId(dto.getResponsibleId());
        }
        if (dto.getCorrectiveAction() != null) {
            obs.setCorrectiveAction(dto.getCorrectiveAction());
        }
        if (dto.getRemarks() != null) {
            obs.setRemarks(dto.getRemarks());
        }
        
        // Update area if provided
        if (dto.getArea() != null && dto.getArea().getCode() != null) {
            Area area = areaRepository.findByCode(dto.getArea().getCode());
            if (area != null) {
                obs.setArea(area);
            } else {
                throw new IllegalArgumentException("Area with code '" + dto.getArea().getCode() + "' not found");
            }
        }
        
        obs = observationRepo.save(obs);
        return obs;
    }

    /**
     * Delete an observation by code
     * @param observationCode the observation code
     */
    @Transactional
    public void delete(String observationCode) {
        Observation obs = observationRepo.findByCode(observationCode);
        if (obs == null) {
            throw new IllegalArgumentException("Observation with code '" + observationCode + "' not found");
        }
        
        // Delete associated files
        java.util.List<ObservationFile> files = fileRepository.findByObservationId(obs.getId());
        for (ObservationFile file : files) {
            try {
                // Reconstruct the full file path from the relative path stored in DB
                // DB stores: "observation/images/timestamp_filename" or "observation/documents/filename"
                // Full path should be: "uploads/observation/images/timestamp_filename"
                String fullFilePath = "uploads/" + file.getFilePath();
                Path filePath = Paths.get(fullFilePath).toAbsolutePath().normalize();
                
                log.info("Attempting to delete file: {}", filePath);
                
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("File deleted successfully: {}", filePath);
                } else {
                    log.warn("File not found on disk: {}", filePath);
                }
            } catch (IOException e) {
                log.warn("Failed to delete file from disk: {}", file.getFilePath(), e);
            }
            fileRepository.delete(file);
        }
        
        // Delete the observation
        observationRepo.delete(obs);
        log.info("Observation {} deleted successfully", observationCode);
    }

    /**
     * Add a comment to an observation
     * @param comment the observation comment to add
     * @return the saved comment
     */
    @Transactional
    public ObservationComment addComment(ObservationComment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("Comment cannot be null");
        }
        
        if (comment.getObservation() == null) {
            throw new IllegalArgumentException("Observation is required for comment");
        }
        
        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }
        
        if (comment.getCreatedAt() == null) {
            comment.setCreatedAt(LocalDateTime.now());
        }
        
        return commentRepository.save(comment);
    }

    // ── Dashboard Stats ───────────────────────────────────────────────────────

    public long getTotalCount() {
        return observationRepo.count();
    }

    public long countByStatus(Observation.Status status) {
        return observationRepo.countByStatus(status);
    }

    /**
     * Returns a 12-element array with monthly counts (index 0 = Jan, 11 = Dec)
     * for a given year and observation status.
     */
    public int[] getMonthlyCountByStatus(int year, Observation.Status status) {
        int[] months = new int[12];
        for (Object[] row : observationRepo.countByYearAndStatus(year, status)) {
            int month = ((Number) row[0]).intValue(); // 1-based
            int count = ((Number) row[1]).intValue();
            months[month - 1] = count;
        }
        return months;
    }
}
