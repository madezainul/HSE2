package ahqpck.hse.safety.service;

import ahqpck.hse.safety.mapper.IncidentMapper;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.IncidentFile;
import ahqpck.hse.safety.model.entity.IncidentComment;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.model.dto.IncidentDTO;
import ahqpck.hse.safety.model.dto.IncidentFileDTO;
import ahqpck.hse.safety.repository.IncidentRepository;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.repository.IncidentFileRepository;
import ahqpck.hse.safety.repository.IncidentCommentRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.util.FileCategory;
import ahqpck.hse.safety.util.ZeroPaddedCodeGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentServiceTrash {

    private final IncidentRepository incidentRepository;
    private final ZeroPaddedCodeGenerator codeGenerator;
    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final IncidentMapper incidentMapper;
    private final IncidentFileRepository incidentFileRepository;
    private final IncidentCommentRepository incidentCommentRepository;
    private final FileStorageService fileStorageService;

    // ── Read Methods ──────────────────────────────────────────────────────────

    public List<Incident> getAllIncidents() {
        log.info("Fetching all incidents");
        return incidentRepository.findAll();
    }

    public Page<Incident> getIncidentsPaginated(Pageable pageable) {
        log.info("Fetching incidents with pagination: {}", pageable);
        return incidentRepository.findAll(pageable);
    }

    public List<Incident> getIncidentsByStatus(Incident.IncidentStatus status) {
        log.info("Fetching incidents with status: {}", status);
        return incidentRepository.findByStatus(status);
    }

    public Page<Incident> getIncidentsByStatusPaginated(Incident.IncidentStatus status, Pageable pageable) {
        log.info("Fetching incidents with status: {} and pagination: {}", status, pageable);
        return incidentRepository.findByStatus(status, pageable);
    }

    public List<Incident> getIncidentsBySeverity(Incident.SeverityLevel severity) {
        log.info("Fetching incidents with severity: {}", severity);
        return incidentRepository.findBySeverity(severity);
    }

    public Page<Incident> getIncidentsBySeverityPaginated(Incident.SeverityLevel severity, Pageable pageable) {
        log.info("Fetching incidents with severity: {} and pagination: {}", severity, pageable);
        return incidentRepository.findBySeverity(severity, pageable);
    }

    public List<Incident> getIncidentsByType(Incident.Type type) {
        log.info("Fetching incidents with type: {}", type);
        return incidentRepository.findByType(type);
    }

    public Page<Incident> getIncidentsByTypePaginated(Incident.Type type, Pageable pageable) {
        log.info("Fetching incidents with type: {} and pagination: {}", type, pageable);
        return incidentRepository.findByType(type, pageable);
    }

    public long getTotalIncidentCount() {
        log.info("Getting total incident count");
        return incidentRepository.count();
    }

    public Incident getIncidentById(String id) {
        log.info("Fetching incident by id: {}", id);
        return incidentRepository.findById(id).orElse(null);
    }

    /**
     * Fetch incident by business code (INC-XXXXXX)
     * Used primarily by REST API and controllers for user-facing lookups
     */
    public Incident getIncidentByCode(String code) {
        log.info("Fetching incident by code: {}", code);
        Incident incident = incidentRepository.findByCodeWithFiles(code);
        if (incident != null) {
            log.debug("Found incident with code: {}, internal id: {}", code, incident.getId());
        }
        return incident;
    }

    // ── Write Methods ─────────────────────────────────────────────────────────

    @Transactional
    public Incident createIncident(IncidentDTO incidentDTO, String currentUserId) {
        log.info("Creating new incident");

        Incident incident = new Incident();

        // Generate or use provided code
        if (incidentDTO.getIncidentCode() == null || incidentDTO.getIncidentCode().trim().isEmpty()) {
            String generatedCode = codeGenerator.generate(Incident.class, "code", "INC");
            incident.setCode(generatedCode);
        } else {
            incident.setCode(incidentDTO.getIncidentCode().trim());
        }

        // Resolve current user
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Current user not found with id: " + currentUserId));
        incident.setUpdatedBy(currentUser);

        // Set reportedBy from DTO or default to current user
        if (incidentDTO.getReportedBy() != null && !incidentDTO.getReportedBy().trim().isEmpty()) {
            incident.setReportedBy(incidentDTO.getReportedBy());
        } else {
            incident.setReportedBy(currentUser.getUsername());
        }

        // Use mapper to map all remaining fields from DTO to entity
        // (handles: dates, enums, area lookup, involved person, witnesses, actions)
        incidentMapper.mapToEntity(incident, incidentDTO, areaRepository, userRepository);

        log.info("[createIncident] Saving incident with code: {}, reported by: {}",
                incident.getCode(), incident.getReportedBy());

        Incident savedIncident = incidentRepository.save(incident);
        log.info("[createIncident] Incident saved successfully with id: {}, code: {}",
                savedIncident.getId(), savedIncident.getCode());

        return savedIncident;
    }

    @Transactional
    public Incident updateIncident(Incident incident) {
        log.info("Updating incident with id: {}", incident.getId());
        return incidentRepository.save(incident);
    }

    @Transactional
    public void deleteIncident(String id) {
        log.info("Deleting incident with id: {}", id);
        incidentRepository.deleteById(id);
    }

    // ── File Methods ──────────────────────────────────────────────────────────

    @Transactional
    public IncidentFileDTO addIncidentImage(String incidentCode, MultipartFile file) {
        log.info("Adding image to incident: {}", incidentCode);

        Incident incident = incidentRepository.findByCode(incidentCode);
        if (incident == null)
            throw new RuntimeException("Incident not found: " + incidentCode);

        String path = fileStorageService.storeFile(file, FileCategory.INCIDENT_IMAGE);

        IncidentFile incidentFile = IncidentFile.builder()
                .incident(incident)
                .filePath(path)
                .fileName(file.getOriginalFilename())
                .fileType(IncidentFile.FileType.IMAGE)
                .build();

        return IncidentFileDTO.fromEntity(incidentFileRepository.save(incidentFile));
    }

    @Transactional
    public IncidentFileDTO addIncidentDocument(String incidentCode, MultipartFile file) {
        log.info("Adding document to incident: {}", incidentCode);

        Incident incident = incidentRepository.findByCode(incidentCode);
        if (incident == null)
            throw new RuntimeException("Incident not found: " + incidentCode);

        String path = fileStorageService.storeFile(file, FileCategory.INCIDENT_DOCUMENT);

        IncidentFile incidentFile = IncidentFile.builder()
                .incident(incident)
                .filePath(path)
                .fileName(file.getOriginalFilename())
                .fileType(IncidentFile.FileType.DOCUMENT)
                .build();

        return IncidentFileDTO.fromEntity(incidentFileRepository.save(incidentFile));
    }

    public List<IncidentFileDTO> getIncidentFiles(String incidentCode) {
        Incident incident = incidentRepository.findByCode(incidentCode);
        if (incident == null)
            throw new RuntimeException("Incident not found: " + incidentCode);
        return incidentFileRepository.findByIncidentId(incident.getId())
                .stream()
                .map(IncidentFileDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<IncidentFileDTO> getIncidentImages(String incidentCode) {
        Incident incident = incidentRepository.findByCode(incidentCode);
        if (incident == null)
            throw new RuntimeException("Incident not found: " + incidentCode);
        return incidentFileRepository.findByIncidentIdAndFileType(incident.getId(), IncidentFile.FileType.IMAGE)
                .stream()
                .map(IncidentFileDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<IncidentFileDTO> getIncidentDocuments(String incidentCode) {
        Incident incident = incidentRepository.findByCode(incidentCode);
        if (incident == null)
            throw new RuntimeException("Incident not found: " + incidentCode);
        return incidentFileRepository.findByIncidentIdAndFileType(incident.getId(), IncidentFile.FileType.DOCUMENT)
                .stream()
                .map(IncidentFileDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteIncidentFile(String fileId) throws IOException {
        IncidentFile file = incidentFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found: " + fileId));
        fileStorageService.deleteFile(file.getFilePath());
        incidentFileRepository.deleteById(fileId);
        log.info("Deleted incident file: {}", fileId);
    }

    /**
     * Add a comment to an incident.
     * @param comment the incident comment to add
     */
    @Transactional
    public void addComment(IncidentComment comment) {
        log.info("Adding comment to incident: {}", comment.getIncident().getCode());
        incidentCommentRepository.save(comment);
        log.info("Comment added successfully");
    }
}