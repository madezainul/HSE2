package ahqpck.hse.safety.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ahqpck.hse.safety.model.dto.ObservationDTO;
import ahqpck.hse.safety.model.dto.AreaDTO;
import ahqpck.hse.safety.model.dto.ObservationFileDTO;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.service.NotificationService;
import ahqpck.hse.safety.service.ObservationService;
import ahqpck.hse.safety.service.UserService;
import ahqpck.hse.safety.repository.ObservationRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/observation")
public class ObservationRestController {

    @Autowired
    private ObservationService observationService;
    
    @Autowired
    private ObservationRepository observationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> saveObservation(
            @RequestParam String observationDate,
            @RequestParam String areaCode,
            @RequestParam String category,
            @RequestParam String type,
            @RequestParam String description,
            @RequestParam String status,
            @RequestParam String mitigation,
            @RequestParam(required = false) String observerId,
            @RequestParam(required = false) String observerName,
            @RequestParam(required = false) String responsibleId,
            @RequestParam(required = false) String responsibleName,
            @RequestParam(required = false) String correctiveAction,
            @RequestParam(required = false) String remarks,
            @RequestParam(value = "observationImages", required = false) MultipartFile[] observationImages,
            @RequestParam(value = "observationDocuments", required = false) MultipartFile[] observationDocuments,
            Authentication authentication) {
        try {
            // Build ObservationDTO from form parameters
            ObservationDTO dto = new ObservationDTO();
            
            // Parse observationDate from datetime-local format (yyyy-MM-dd'T'HH:mm)
            LocalDateTime parsedDate = LocalDateTime.parse(observationDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            dto.setObservationDate(parsedDate);
            
            dto.setCategory(category);
            dto.setType(type);
            dto.setDescription(description);
            dto.setStatus(status);
            dto.setMitigation(mitigation);
            dto.setObserverId(observerId);
            dto.setObserverName(observerName);
            dto.setResponsibleId(responsibleId);
            dto.setResponsibleName(responsibleName);
            dto.setCorrectiveAction(correctiveAction);
            dto.setRemarks(remarks);
            
            // Set area
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setCode(areaCode);
            dto.setArea(areaDTO);
            
            // Save observation
            Observation observation = observationService.save(dto);
            
            // Upload images if provided
            if (observationImages != null && observationImages.length > 0) {
                for (MultipartFile imageFile : observationImages) {
                    if (!imageFile.isEmpty()) {
                        observationService.saveFile(observation.getCode(), imageFile, "IMAGE");
                    }
                }
            }
            
            // Upload documents if provided
            if (observationDocuments != null && observationDocuments.length > 0) {
                for (MultipartFile docFile : observationDocuments) {
                    if (!docFile.isEmpty()) {
                        observationService.saveFile(observation.getCode(), docFile, "DOCUMENT");
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Observation and files saved successfully");
            response.put("code", observation.getCode());
            response.put("id", observation.getId());

            // Notify all other users about the new observation
            String currentUserId = null;
            String currentUserFullName = "Unknown";
            if (authentication != null && authentication.getPrincipal() instanceof User principal) {
                currentUserId = principal.getId();
                currentUserFullName = principal.getFullName() != null ? principal.getFullName() : principal.getUsername();
            } else if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails ud) {
                var cu = userService.findByUsername(ud.getUsername());
                if (cu != null) {
                    currentUserId = cu.getId();
                    currentUserFullName = cu.getFullName() != null ? cu.getFullName() : cu.getUsername();
                }
            }
            if (currentUserId != null) {
                notificationService.notifyAllExcept("OBSERVATION", observation.getCode(), currentUserId, currentUserFullName);
            }

            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error saving observation: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Invalid input: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error saving observation", e);
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @GetMapping("/{observationCode}")
    public ResponseEntity<?> getObservation(@PathVariable String observationCode) {
        try {
            Observation obs = observationRepository.findByCode(observationCode);
            if (obs == null) {
                Map<String, String> error = new HashMap<>();
                error.put("status", "error");
                error.put("message", "Observation not found: " + observationCode);
                return ResponseEntity.status(404).body(error);
            }
            
            // Convert to DTO for response using the fromEntity method
            ObservationDTO dto = ObservationDTO.fromEntity(obs);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Error fetching observation", e);
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PutMapping(value = "/{observationCode}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateObservation(
            @PathVariable String observationCode,
            @RequestParam String observationDate,
            @RequestParam String areaCode,
            @RequestParam String category,
            @RequestParam String type,
            @RequestParam String description,
            @RequestParam String status,
            @RequestParam String mitigation,
            @RequestParam(required = false) String observerId,
            @RequestParam(required = false) String observerName,
            @RequestParam(required = false) String responsibleId,
            @RequestParam(required = false) String responsibleName,
            @RequestParam(required = false) String correctiveAction,
            @RequestParam(required = false) String remarks,
            @RequestParam(value = "observationImages", required = false) MultipartFile[] observationImages,
            @RequestParam(value = "observationDocuments", required = false) MultipartFile[] observationDocuments) {
        try {
            // Build ObservationDTO from form parameters
            ObservationDTO dto = new ObservationDTO();
            
            // Parse observationDate from datetime-local format (yyyy-MM-dd'T'HH:mm)
            LocalDateTime parsedDate = LocalDateTime.parse(observationDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            dto.setObservationDate(parsedDate);
            
            dto.setCategory(category);
            dto.setType(type);
            dto.setDescription(description);
            dto.setStatus(status);
            dto.setMitigation(mitigation);
            dto.setObserverId(observerId);
            dto.setObserverName(observerName);
            dto.setResponsibleId(responsibleId);
            dto.setResponsibleName(responsibleName);
            dto.setCorrectiveAction(correctiveAction);
            dto.setRemarks(remarks);
            
            // Set area
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setCode(areaCode);
            dto.setArea(areaDTO);
            
            // Update observation
            Observation observation = observationService.update(observationCode, dto);
            
            // Upload new images if provided
            if (observationImages != null && observationImages.length > 0) {
                for (MultipartFile imageFile : observationImages) {
                    if (!imageFile.isEmpty()) {
                        observationService.saveFile(observation.getCode(), imageFile, "IMAGE");
                    }
                }
            }
            
            // Upload new documents if provided
            if (observationDocuments != null && observationDocuments.length > 0) {
                for (MultipartFile docFile : observationDocuments) {
                    if (!docFile.isEmpty()) {
                        observationService.saveFile(observation.getCode(), docFile, "DOCUMENT");
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Observation updated successfully");
            response.put("code", observation.getCode());
            response.put("id", observation.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating observation: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Invalid input: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error updating observation", e);
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @DeleteMapping("/{observationCode}")
    public ResponseEntity<?> deleteObservation(@PathVariable String observationCode) {
        try {
            observationService.delete(observationCode);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Observation deleted successfully");
            response.put("code", observationCode);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error deleting observation: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Invalid input: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error deleting observation", e);
            Map<String, String> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
}
