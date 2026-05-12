package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.entity.ObservationFile;
import ahqpck.hse.safety.model.dto.ObservationDTO;
import ahqpck.hse.safety.model.dto.ObservationFileDTO;
import ahqpck.hse.safety.repository.ObservationRepository;
import ahqpck.hse.safety.repository.ObservationFileRepository;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.util.FileCategory;
import ahqpck.hse.safety.util.ZeroPaddedCodeGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing observations (near misses, unsafe conditions, unsafe acts)
 * Handles CRUD operations, file management, and business logic for observations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ObservationServiceTrash {

    private final ObservationRepository observationRepository;
    private final ObservationFileRepository observationFileRepository;
    private final ZeroPaddedCodeGenerator codeGenerator;
    private final FileStorageService fileStorageService;
    private final AreaRepository areaRepository;

    // ── Read Methods ──────────────────────────────────────────────────────────

    /**
     * Get all observations
     * @return list of all observations
     */
    public List<Observation> getAllObservations() {
        log.info("Fetching all observations");
        // TODO: Replace with actual repository call
        // return observationRepository.findAll();
        return List.of();
    }

    /**
     * Get observations with pagination
     * @param pageable pagination parameters
     * @return page of observations
     */
    public Page<Observation> getObservationsPaginated(Pageable pageable) {
        log.info("Fetching observations with pagination: {}", pageable);
        // TODO: Replace with actual repository call
        // return observationRepository.findAll(pageable);
        return new PageImpl<>(List.of(), pageable, 0);
    }

    /**
     * Get observations by status
     * @param status the observation status
     * @return list of observations with the given status
     */
    public List<Observation> getObservationsByStatus(Observation.Status status) {
        log.info("Fetching observations with status: {}", status);
        // TODO: Replace with actual repository call
        // return observationRepository.findByStatus(status);
        return List.of();
    }

    /**
     * Get observations by status with pagination
     * @param status the observation status
     * @param pageable pagination parameters
     * @return page of observations with the given status
     */
    public Page<Observation> getObservationsByStatusPaginated(Observation.Status status, Pageable pageable) {
        log.info("Fetching observations with status: {} and pagination: {}", status, pageable);
        // TODO: Replace with actual repository call
        // return observationRepository.findByStatus(status, pageable);
        return new PageImpl<>(List.of(), pageable, 0);
    }

    /**
     * Get observations by category
     * @param category the observation category
     * @return list of observations with the given category
     */
    public List<Observation> getObservationsByCategory(String category) {
        log.info("Fetching observations with category: {}", category);
        // TODO: Replace with actual repository call
        // return observationRepository.findByCategory(category);
        return List.of();
    }

    /**
     * Get observations by category with pagination
     * @param category the observation category
     * @param pageable pagination parameters
     * @return page of observations with the given category
     */
    public Page<Observation> getObservationsByCategoryPaginated(String category, Pageable pageable) {
        log.info("Fetching observations with category: {} and pagination: {}", category, pageable);
        // TODO: Replace with actual repository call
        // return observationRepository.findByCategory(category, pageable);
        return new PageImpl<>(List.of(), pageable, 0);
    }

    /**
     * Get observations by type
     * @param type the observation type
     * @return list of observations with the given type
     */
    public List<Observation> getObservationsByType(Observation.Type type) {
        log.info("Fetching observations with type: {}", type);
        // TODO: Replace with actual repository call
        // return observationRepository.findByType(type);
        return List.of();
    }

    /**
     * Get observations by type with pagination
     * @param type the observation type
     * @param pageable pagination parameters
     * @return page of observations with the given type
     */
    public Page<Observation> getObservationsByTypePaginated(Observation.Type type, Pageable pageable) {
        log.info("Fetching observations with type: {} and pagination: {}", type, pageable);
        // TODO: Replace with actual repository call
        // return observationRepository.findByType(type, pageable);
        return new PageImpl<>(List.of(), pageable, 0);
    }

    /**
     * Get total observation count
     * @return total number of observations
     */
    public long getTotalObservationCount() {
        log.info("Getting total observation count");
        // TODO: Replace with actual repository call
        // return observationRepository.count();
        return 0;
    }

    /**
     * Get observation by ID
     * @param id the observation ID
     * @return the observation or null if not found
     */
    public Observation getObservationById(String id) {
        log.info("Fetching observation by id: {}", id);
        // TODO: Replace with actual repository call
        // return observationRepository.findById(id).orElse(null);
        return null;
    }

    /**
     * Get observation by business code
     * Used primarily by REST API and controllers for user-facing lookups
     * @param code the observation code (e.g., OBS-000001)
     * @return the observation or null if not found
     */
    public Observation getObservationByCode(String code) {
        log.info("Fetching observation by code: {}", code);
        // TODO: Replace with actual repository call
        // Observation observation = observationRepository.findByCode(code);
        // if (observation != null) {
        //     log.debug("Found observation with code: {}, internal id: {}", code, observation.getId());
        // }
        // return observation;
        return null;
    }

    // ── Write Methods ─────────────────────────────────────────────────────────

    /**
     * Create a new observation
     * @param observationDTO the observation DTO
     * @param currentUserId the ID of the user creating the observation
     * @return the created observation
     */
    @Transactional
    public Observation createObservation(ObservationDTO observationDTO, String currentUserId) {
        log.info("Creating new observation");

        Observation observation = new Observation();

        // Generate or use provided code
        if (observationDTO.getObservationCode() == null || observationDTO.getObservationCode().trim().isEmpty()) {
            String generatedCode = codeGenerator.generate(Observation.class, "code", "OBS");
            observation.setCode(generatedCode);
        } else {
            observation.setCode(observationDTO.getObservationCode().trim());
        }

        // Mandatory fields
        observation.setObservationDate(observationDTO.getObservationDate());
        observation.setDescription(observationDTO.getDescription());
        observation.setCategory(observationDTO.getCategory() != null ? Observation.Category.valueOf(observationDTO.getCategory()) : Observation.Category.OTHER);
        observation.setType(observationDTO.getType() != null ? Observation.Type.valueOf(observationDTO.getType()) : Observation.Type.NEAR_MISS);
        observation.setStatus(observationDTO.getStatus() != null ? Observation.Status.valueOf(observationDTO.getStatus()) : Observation.Status.OPEN);
        observation.setMitigation(observationDTO.getMitigation() != null ? Observation.Mitigation.valueOf(observationDTO.getMitigation()) : Observation.Mitigation.LOW);
        observation.setCorrectiveAction(observationDTO.getCorrectiveAction());
        observation.setRemarks(observationDTO.getRemarks());
        
        // Set observer information (optional)
        if (observationDTO.getObserverName() != null && !observationDTO.getObserverName().trim().isEmpty()) {
            observation.setObserver(observationDTO.getObserverName());
        }
        if (observationDTO.getObserverId() != null && !observationDTO.getObserverId().trim().isEmpty()) {
            observation.setObserverEmployeeId(observationDTO.getObserverId());
        }

        // Set responsible person information (optional)
        if (observationDTO.getResponsibleName() != null && !observationDTO.getResponsibleName().trim().isEmpty()) {
            observation.setResponsible(observationDTO.getResponsibleName());
        }
        if (observationDTO.getResponsibleId() != null && !observationDTO.getResponsibleId().trim().isEmpty()) {
            observation.setResponsibleEmployeeId(observationDTO.getResponsibleId());
        }
        
        // Map area by code - REQUIRED
        if (observationDTO.getArea() != null && observationDTO.getArea().getCode() != null && !observationDTO.getArea().getCode().trim().isEmpty()) {
            String areaCode = observationDTO.getArea().getCode().trim();
            var area = areaRepository.findByCode(areaCode);
            if (area == null) {
                throw new IllegalArgumentException("Area not found with code: " + areaCode);
            }
            observation.setArea(area);
            log.info("[createObservation] Area set: {}", area.getCode());
        } else {
            throw new IllegalArgumentException("Area is required and must have a valid code");
        }

        log.info("[createObservation] Saving observation with code: {}", observation.getCode());

        // Save to database
        Observation savedObservation = observationRepository.save(observation);
        log.info("[createObservation] Observation saved successfully with id: {}, code: {}",
                savedObservation.getId(), savedObservation.getCode());
        
        return savedObservation;
    }

    /**
     * Update an observation
     * @param observation the observation to update
     * @return the updated observation
     */
    @Transactional
    public Observation updateObservation(Observation observation) {
        log.info("Updating observation with id: {}", observation.getId());
        // TODO: Replace with actual repository call
        // return observationRepository.save(observation);
        return observation;
    }

    /**
     * Delete an observation
     * @param id the observation ID
     */
    @Transactional
    public void deleteObservation(String id) {
        log.info("Deleting observation with id: {}", id);
        // TODO: Replace with actual repository call
        // observationRepository.deleteById(id);
    }

    // ── File Methods ──────────────────────────────────────────────────────────

    /**
     * Add an image to an observation
     * @param observationCode the observation code
     * @param file the image file
     * @return the saved observation file DTO
     */
    @Transactional
    public ObservationFileDTO addObservationImage(String observationCode, MultipartFile file) {
        log.info("Adding image to observation: {}", observationCode);

        Observation observation = observationRepository.findByCode(observationCode);
        if (observation == null)
            throw new RuntimeException("Observation not found: " + observationCode);

        String path = fileStorageService.storeFile(file, FileCategory.OBSERVATION_IMAGE);

        ObservationFile observationFile = ObservationFile.builder()
                .observation(observation)
                .filePath(path)
                .fileName(file.getOriginalFilename())
                .fileType(ObservationFile.FileType.IMAGE)
                .build();

        return ObservationFileDTO.fromEntity(observationFileRepository.save(observationFile));
    }

    /**
     * Add a document to an observation
     * @param observationCode the observation code
     * @param file the document file
     * @return the saved observation file DTO
     */
    @Transactional
    public ObservationFileDTO addObservationDocument(String observationCode, MultipartFile file) {
        log.info("Adding document to observation: {}", observationCode);

        Observation observation = observationRepository.findByCode(observationCode);
        if (observation == null)
            throw new RuntimeException("Observation not found: " + observationCode);

        String path = fileStorageService.storeFile(file, FileCategory.OBSERVATION_DOCUMENT);

        ObservationFile observationFile = ObservationFile.builder()
                .observation(observation)
                .filePath(path)
                .fileName(file.getOriginalFilename())
                .fileType(ObservationFile.FileType.DOCUMENT)
                .build();

        return ObservationFileDTO.fromEntity(observationFileRepository.save(observationFile));
    }

    /**
     * Get all files for an observation
     * @param observationCode the observation code
     * @return list of observation file DTOs
     */
    public List<ObservationFileDTO> getObservationFiles(String observationCode) {
        // TODO: Replace with actual repository calls
        // Observation observation = observationRepository.findByCode(observationCode);
        // if (observation == null)
        //     throw new RuntimeException("Observation not found: " + observationCode);
        // return observationFileRepository.findByObservationId(observation.getId())
        //         .stream()
        //         .map(ObservationFileDTO::fromEntity)
        //         .collect(Collectors.toList());
        return List.of();
    }

    /**
     * Get all images for an observation
     * @param observationCode the observation code
     * @return list of observation image file DTOs
     */
    public List<ObservationFileDTO> getObservationImages(String observationCode) {
        // TODO: Replace with actual repository calls
        // Observation observation = observationRepository.findByCode(observationCode);
        // if (observation == null)
        //     throw new RuntimeException("Observation not found: " + observationCode);
        // return observationFileRepository.findByObservationIdAndFileType(observation.getId(), ObservationFile.FileType.IMAGE)
        //         .stream()
        //         .map(ObservationFileDTO::fromEntity)
        //         .collect(Collectors.toList());
        return List.of();
    }

    /**
     * Get all documents for an observation
     * @param observationCode the observation code
     * @return list of observation document file DTOs
     */
    public List<ObservationFileDTO> getObservationDocuments(String observationCode) {
        // TODO: Replace with actual repository calls
        // Observation observation = observationRepository.findByCode(observationCode);
        // if (observation == null)
        //     throw new RuntimeException("Observation not found: " + observationCode);
        // return observationFileRepository.findByObservationIdAndFileType(observation.getId(), ObservationFile.FileType.DOCUMENT)
        //         .stream()
        //         .map(ObservationFileDTO::fromEntity)
        //         .collect(Collectors.toList());
        return List.of();
    }

    /**
     * Delete an observation file
     * @param fileId the file ID
     * @throws IOException if file deletion fails
     */
    @Transactional
    public void deleteObservationFile(String fileId) throws IOException {
        // TODO: Replace with actual repository calls
        // ObservationFile file = observationFileRepository.findById(fileId)
        //         .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        // fileStorageService.deleteFile(file.getFilePath());
        // observationFileRepository.deleteById(fileId);
        log.info("Deleted observation file: {}", fileId);
    }
}
