package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.entity.Accident;
import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.model.dto.AccidentDTO;
import ahqpck.hse.safety.mapper.AccidentMapper;
import ahqpck.hse.safety.repository.AccidentRepository;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.util.FileUploadUtil;
import ahqpck.hse.safety.util.ZeroPaddedCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccidentService {

    @Value("${app.upload-accident-image.dir:src/main/resources/static/uploads/accident/images}")
    private String uploadAccidentImageDir;

    private final AccidentRepository accidentRepository;
    private final ZeroPaddedCodeGenerator codeGenerator;
    private final FileUploadUtil fileUploadUtil;
    private final UserService userService;
    private final AreaService areaService;
    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final AccidentMapper accidentMapper;


    public List<Accident> getAllAccidents() {
        log.info("Fetching all accidents");
        return accidentRepository.findAll();
    }

    public Page<Accident> getAccidentsPaginated(Pageable pageable) {
        log.info("Fetching accidents with pagination: {}", pageable);
        return accidentRepository.findAll(pageable);
    }

    public List<Accident> getAccidentsByStatus(Accident.AccidentStatus status) {
        log.info("Fetching accidents with status: {}", status);
        return accidentRepository.findByStatus(status);
    }

    public Page<Accident> getAccidentsByStatusPaginated(Accident.AccidentStatus status, Pageable pageable) {
        log.info("Fetching accidents with status: {} and pagination: {}", status, pageable);
        return accidentRepository.findByStatus(status, pageable);
    }

    public List<Accident> getAccidentsBySeverity(Accident.SeverityLevel severity) {
        log.info("Fetching accidents with severity: {}", severity);
        return accidentRepository.findBySeverity(severity);
    }

    public Page<Accident> getAccidentsBySeverityPaginated(Accident.SeverityLevel severity, Pageable pageable) {
        log.info("Fetching accidents with severity: {} and pagination: {}", severity, pageable);
        return accidentRepository.findBySeverity(severity, pageable);
    }

    public long getTotalAccidentCount() {
        log.info("Getting total accident count");
        return accidentRepository.count();
    }

    /**
     * Fetch accident by internal database id
     * Used for internal efficiency when id is already known
     */
    public Accident getAccidentById(String id) {
        log.info("Fetching accident by id: {}", id);
        return accidentRepository.findById(id).orElse(null);
    }

    /**
     * Fetch accident by business code (ACC-XXXXXX)
     * Used primarily by REST API and controllers for user-facing lookups
     * Internally converts code → id for efficient querying
     */
    public Accident getAccidentByCode(String code) {
        log.info("Fetching accident by code: {}", code);
        Accident accident = accidentRepository.findByCode(code);
        if (accident != null) {
            log.debug("Found accident with code: {}, internal id: {}", code, accident.getId());
        }
        return accident;
    }

    public Accident createAccident(AccidentDTO accidentDTO, MultipartFile accidentImages, String currentUserId) {

        Accident accident = new Accident();

        if (accidentDTO.getAccidentCode() == null || accidentDTO.getAccidentCode().trim().isEmpty()) {
            String generatedCode = codeGenerator.generate(Accident.class, "code", "CP");
            accident.setCode(generatedCode);
        } else {
            accident.setCode(accidentDTO.getAccidentCode().trim());
        }

        User currentUser = userRepository.findById(currentUserId).orElseThrow(() -> new RuntimeException("Current user not found with id: " + currentUserId));
        accident.setCreatedBy(currentUser);
        accident.setUpdatedBy(currentUser);
        
        // Set reportedBy from DTO or default to current user
        if (accidentDTO.getReportedBy() != null && !accidentDTO.getReportedBy().trim().isEmpty()) {
            accident.setReportedBy(accidentDTO.getReportedBy());
        } else {
            accident.setReportedBy(currentUser.getUsername());
        }

        accidentMapper.mapToEntity(
            accident,
            accidentDTO,
            areaRepository,
            userRepository);
        
        // Accident image filename is already set from DTO (uploaded in controller)
        // No need to re-upload here
        log.info("[createAccident] Saving accident with code: {}, reported by: {} and image: {}", accident.getCode(), accident.getReportedBy(), accident.getAccidentImages());
        
        Accident savedAccident = accidentRepository.save(accident);
        log.info("[createAccident] Accident saved successfully with id: {}, code: {}", savedAccident.getId(), savedAccident.getCode());
        
        return savedAccident;
    }

    public Accident updateAccident(Accident accident) {
        log.info("Updating accident with id: {}", accident.getId());
        return accidentRepository.save(accident);
    }

    public void deleteAccident(String id) {
        log.info("Deleting accident with id: {}", id);
        accidentRepository.deleteById(id);
    }
}
