package ahqpck.hse.safety.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.model.dto.IncidentDTO;
import ahqpck.hse.safety.model.dto.IncidentFileDTO;
import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.IncidentComment;
import ahqpck.hse.safety.model.entity.IncidentFile;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.repository.IncidentCommentRepository;
import ahqpck.hse.safety.repository.IncidentFileRepository;
import ahqpck.hse.safety.repository.IncidentRepository;
import ahqpck.hse.safety.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepo;
    @Autowired
    IncidentFileRepository incidentFileRepo;
    @Autowired
    AreaRepository areaRepo;
    @Autowired
    IncidentCommentRepository incidentCommentRepo;
    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Incident save(IncidentDTO dto, String currentUserId) {
        Incident inc = dto.toEntity();

        //Generate incident Code if not provided
        if (inc.getCode() == null || inc.getCode().isEmpty()) {
            inc.setCode(generateIncidentCode());
        }

        // Resolve current user and set updatedBy
        if (currentUserId != null && !currentUserId.isEmpty()) {
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("Current user not found with id: " + currentUserId));
            inc.setUpdatedBy(currentUser);
        }

        // Look up Area from database
        if (dto.getArea() != null && dto.getArea().getCode() != null) {
            Area area = areaRepo.findByCode(dto.getArea().getCode());
            if (area != null) {
                inc.setArea(area);
            } else {
                throw new IllegalArgumentException("Area with code " + dto.getArea().getCode() + " not found");
            }
        } else {
            throw new IllegalArgumentException("Area code is required");
        }

        inc = incidentRepo.save(inc);

        // Save files if provided
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            for (IncidentFileDTO fileDto : dto.getFiles()) {
                IncidentFile file = fileDto.toEntity(inc);
                incidentFileRepo.save(file);
            }
        }
        return inc;
    }

    /**
     * Generate a unique observation code with auto-increment
     * Format: INC-000001, INC-000002, etc.
     */
    private String generateIncidentCode() {
        long maxNumber = 0;
        String newCode = "";
        int attempts = 0;
        
        // Try up to 10 times to generate a unique code
        while (attempts < 10) {
            try {
                // Get all incidents and find the maximum numeric value
                java.util.List<Incident> allIncidents = incidentRepo.findAll();
                
                for (Incident inc : allIncidents) {
                    if (inc.getCode() != null && inc.getCode().startsWith("INC-")) {
                        try {
                            // Extract numeric part (e.g., "000001" from "INC-000001")
                            String numericPart = inc.getCode().substring(4);
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
                newCode = String.format("INC-%06d", nextNumber);
                
                // Check if this code already exists
                Incident existing = incidentRepo.findByCode(newCode);
                if (existing == null) {
                    log.debug("Generated incident code: {}", newCode);
                    return newCode;
                }
                
                // If code exists, increment and try again
                maxNumber = nextNumber;
                attempts++;
                
            } catch (Exception e) {
                log.error("Error generating incident code: {}", e.getMessage());
                attempts++;
            }
        }
        
        throw new RuntimeException("Failed to generate unique incident code after " + attempts + " attempts");
    }

    /**
     * Save a file for an incident
     * 
     * @param incidentCode the incident code
     * @param file         the multipart file
     * @param fileType     the file type (IMAGE or DOCUMENT)
     * @return the saved incident file DTO
     */
    @Transactional
    public IncidentFileDTO saveFile(String incidentCode, MultipartFile file, String fileType) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        Incident inc = incidentRepo.findByCode(incidentCode);
        if (inc == null) {
            throw new IllegalArgumentException("Incident not found: " + incidentCode);
        }

        // Determine file directory based on type
        String fileDir = "DOCUMENT".equalsIgnoreCase(fileType) ? "incident/documents" : "incident/images";
        Path uploadPath = Paths.get("uploads/" + fileDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        // Generate filename
        String originalFileName = file.getOriginalFilename();
        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path filePath = uploadPath.resolve(fileName);

        // Save file to disk
        Files.write(filePath, file.getBytes());

        // Save to database
        IncidentFile incFile = IncidentFile.builder()
                .fileName(originalFileName)
                .filePath(fileDir + "/" + fileName)
                .fileType(IncidentFile.FileType.valueOf(fileType.toUpperCase()))
                .uploadedAt(LocalDateTime.now())
                .incident(inc)
                .build();

        incFile = incidentFileRepo.save(incFile);
        return IncidentFileDTO.fromEntity(incFile);
    }

    /**
     * Update an existing incident
     * 
     * @param incidentCode the incident code to update
     * @param dto          the updated incident data
     * @return the updated incident
     */
    @Transactional
    public Incident update(String incidentCode, IncidentDTO dto) {
        Incident inc = incidentRepo.findByCode(incidentCode);
        if (inc == null) {
            throw new IllegalArgumentException("Incident with code " + incidentCode + " not found");
        }

        // Update fields
        if (dto.getReportDate() != null) {
            inc.setReportDate(dto.getReportDate());
        }
        if (dto.getDescription() != null) {
            inc.setDescription(dto.getDescription());
        }
        if (dto.getSeverity() != null) {
            inc.setSeverity(Incident.SeverityLevel.valueOf(dto.getSeverity()));
        }
        if (dto.getType() != null) {
            inc.setType(Incident.Type.valueOf(dto.getType()));
        }
        if (dto.getInvolvedPersonName() != null) {
            inc.setInvolvedPersonName(dto.getInvolvedPersonName());
        }
        if (dto.getInvolvedPersonEmployeeId() != null) {
            inc.setInvolvedPersonEmployeeId(dto.getInvolvedPersonEmployeeId());
        }
        if (dto.getInvolvedPersonPosition() != null) {
            inc.setInvolvedPersonPosition(dto.getInvolvedPersonPosition());
        }
        if (dto.getWitnesses() != null) {
            inc.setWitnesses(dto.getWitnesses());
        }
        if (dto.getWitnessesEmployeeId() != null) {
            inc.setWitnessesEmployeeId(dto.getWitnessesEmployeeId());
        }
        if (dto.getStatus() != null) {
            inc.setStatus(Incident.IncidentStatus.valueOf(dto.getStatus()));
        }
        if (dto.getImmediateAction() != null) {
            inc.setImmediateAction(dto.getImmediateAction());
        }
        if (dto.getCorrectiveAction() != null) {
            inc.setCorrectiveAction(dto.getCorrectiveAction());
        }
        if (dto.getMedicalAttentionRequired() != null) {
            inc.setMedicalAttentionRequired(dto.getMedicalAttentionRequired());
        }

        // Update area if provided
        if (dto.getArea() != null && dto.getArea().getCode() != null) {
            Area area = areaRepo.findByCode(dto.getArea().getCode());
            if (area != null) {
                inc.setArea(area);
            } else {
                throw new IllegalArgumentException("Area with code " + dto.getArea().getCode() + " not found");
            }
        }

        inc = incidentRepo.save(inc);
        return inc;
    }

    /**
     * Delete an incident by code
     * @param incidentCode the incident code to delete
     */
    @Transactional
    public void delete(String incidentCode) {
        Incident inc = incidentRepo.findByCode(incidentCode);
        if (inc == null) {
            throw new IllegalArgumentException("Incident with code " + incidentCode + " not found");
        }

        // Delete associated files from disk and database
        java.util.List<IncidentFile> files = incidentFileRepo.findByIncidentId(inc.getId());
        for (IncidentFile file : files) {
            try {
                String fullFilePath = "uploads/" + file.getFilePath();
                Path filePath = Paths.get(fullFilePath).toAbsolutePath().normalize();

                log.info("Attempting to delete dile: {}", fullFilePath);

                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    log.info("File deleted successfully: {}", fullFilePath);
                } else {
                    log.warn("File not found, cannot delete: {}", fullFilePath);
                }
            } catch (IOException e) {
                log.warn("Error deleting file: {}", e.getMessage());
            }
            incidentFileRepo.delete(file);
        }

        // Delete the incident
        incidentRepo.delete(inc);
        log.info("Incident deleted: {}", incidentCode);
    }

    /**
     * Add a comment to an observation
     * @param comment the observation comment to add
     * @return the saved comment
     */
    @Transactional
    public IncidentComment addComment(IncidentComment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("Comment cannot be null");
        }

        if (comment.getIncident() == null) {
            throw new IllegalArgumentException("Comment must be associated with an incident");
        }

        if (comment.getContent() == null || comment.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be empty");
        }

        if (comment.getCreatedAt() == null) {
            comment.setCreatedAt(LocalDateTime.now());
        }

        return incidentCommentRepo.save(comment);
    }

    // ── Dashboard Stats ───────────────────────────────────────────────────────

    public long getTotalCount() {
        return incidentRepo.count();
    }

    public long countByStatus(Incident.IncidentStatus status) {
        return incidentRepo.countByStatus(status);
    }

    /**
     * Returns a 12-element array with monthly counts (index 0 = Jan, 11 = Dec)
     * for a given year and incident type.
     */
    public int[] getMonthlyCountByType(int year, Incident.Type type) {
        int[] months = new int[12];
        for (Object[] row : incidentRepo.countByYearAndType(year, type)) {
            int month = ((Number) row[0]).intValue(); // 1-based
            int count = ((Number) row[1]).intValue();
            months[month - 1] = count;
        }
        return months;
    }
}
