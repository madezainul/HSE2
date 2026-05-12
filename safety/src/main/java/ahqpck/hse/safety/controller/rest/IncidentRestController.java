package ahqpck.hse.safety.controller.rest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.model.dto.AreaDTO;
import ahqpck.hse.safety.model.dto.IncidentDTO;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.IncidentRepository;
import ahqpck.hse.safety.service.IncidentService;
import ahqpck.hse.safety.service.NotificationService;
import ahqpck.hse.safety.service.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/incident")
public class IncidentRestController {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> saveIncident(
            @RequestParam String reportDate,
            @RequestParam String areaCode,
            @RequestParam String severity,
            @RequestParam String status,
            @RequestParam String type,
            @RequestParam String description,
            @RequestParam(required = false) String reportedBy,
            @RequestParam(required = false) String involvedPersonName,
            @RequestParam(required = false) String involvedPersonEmployeeId,
            @RequestParam(required = false) String involvedPersonPosition,
            @RequestParam(required = false) String witnesses,
            @RequestParam(required = false) String witnessesEmployeeId,
            @RequestParam(required = false) String immediateAction,
            @RequestParam(required = false) String correctiveAction,
            @RequestParam(required = false) Boolean medicalAttentionRequired,
            @RequestParam(value = "incidentImages", required = false) MultipartFile[] incidentImages,
            @RequestParam(value = "incidentDocuments", required = false) MultipartFile[] incidentDocuments,
            Authentication authentication) {
        try {
            // Build IncidentDTO from request parameters
            IncidentDTO dto = new IncidentDTO();

            // PArse incidentDate from datetime-local format
            LocalDateTime parseDate = LocalDateTime.parse(reportDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            dto.setReportDate(parseDate);

            dto.setSeverity(severity);
            dto.setStatus(status);
            dto.setType(type);
            dto.setDescription(description);
            dto.setReportedBy(reportedBy);
            dto.setInvolvedPersonName(involvedPersonName);
            dto.setInvolvedPersonEmployeeId(involvedPersonEmployeeId);
            dto.setInvolvedPersonPosition(involvedPersonPosition);
            dto.setWitnesses(witnesses);
            dto.setWitnessesEmployeeId(witnessesEmployeeId);
            dto.setImmediateAction(immediateAction);
            dto.setCorrectiveAction(correctiveAction);
            dto.setMedicalAttentionRequired(medicalAttentionRequired);

            // Set area
            AreaDTO areadDTO = new AreaDTO();
            areadDTO.setCode(areaCode);
            dto.setArea(areadDTO);

            // Extract current user from authentication
            String currentUserId = null;
            String currentUserFullName = "Unknown";
            if (authentication != null && authentication.getPrincipal() instanceof User principal) {
                currentUserId = principal.getId();
                currentUserFullName = principal.getFullName() != null ? principal.getFullName() : principal.getUsername();
            } else if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                var currentUser = userService.findByUsername(userDetails.getUsername());
                if (currentUser != null) {
                    currentUserId = currentUser.getId();
                    currentUserFullName = currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername();
                }
            }

            // Save Incident with current user ID
            Incident incident = incidentService.save(dto, currentUserId);

            // Upload images if provided
            if (incidentImages != null && incidentImages.length > 0) {
                for (MultipartFile imageFile : incidentImages) {
                    if (!imageFile.isEmpty()) {
                        incidentService.saveFile(incident.getCode(), imageFile, "IMAGE");
                    }
                }
            }

            // Upload documents if provided
            if (incidentDocuments != null && incidentDocuments.length > 0) {
                for (MultipartFile docFile : incidentDocuments) {
                    if (!docFile.isEmpty()) {
                        incidentService.saveFile(incident.getCode(), docFile, "DOCUMENT");
                    }
                }
            }

            // Notify all other users about the new incident
            if (currentUserId != null) {
                notificationService.notifyAllExcept("INCIDENT", incident.getCode(), currentUserId, currentUserFullName);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Incident and files saved successfully");
            response.put("code", incident.getCode());
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error saving incident: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error saving incident: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "An unexpected error occurred while saving the incident");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/{incidentCode}")
    public ResponseEntity<?> getIncident(@PathVariable String incidentCode) {
        try {
            Incident inc = incidentRepository.findByCode(incidentCode);
            if (inc == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", "Incident not found");
                return ResponseEntity.status(404).body(response);
            }
            
            // Convert to DTO for response using the fromEntity method
            IncidentDTO dto = IncidentDTO.fromEntity(inc);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Unexpected error retrieving incident: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "An unexpected error occurred while retrieving the incident");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PutMapping(value = "/{incidentCode}", consumes = "multipart/form-data")
    public ResponseEntity<?> updateIncident(
            @PathVariable String incidentCode,
            @RequestParam String reportDate,
            @RequestParam String areaCode,
            @RequestParam String severity,
            @RequestParam String status,
            @RequestParam String type,
            @RequestParam String description,
            @RequestParam(required = false) String reportedBy,
            @RequestParam(required = false) String involvedPersonName,
            @RequestParam(required = false) String involvedPersonEmployeeId,
            @RequestParam(required = false) String involvedPersonPosition,
            @RequestParam(required = false) String witnesses,
            @RequestParam(required = false) String witnessesEmployeeId,
            @RequestParam(required = false) String immediateAction,
            @RequestParam(required = false) String correctiveAction,
            @RequestParam(required = false) Boolean medicalAttentionRequired,
            @RequestParam(value = "incidentImages", required = false) MultipartFile[] incidentImages,
            @RequestParam(value = "incidentDocuments", required = false) MultipartFile[] incidentDocuments) {
        try {
            // Build IncidentDTO from request parameters
            IncidentDTO dto = new IncidentDTO();

            // PArse incidentDate from datetime-local format
            LocalDateTime parseDate = LocalDateTime.parse(reportDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            dto.setReportDate(parseDate);

            dto.setSeverity(severity);
            dto.setStatus(status);
            dto.setType(type);
            dto.setDescription(description);
            dto.setReportedBy(reportedBy);
            dto.setInvolvedPersonName(involvedPersonName);
            dto.setInvolvedPersonEmployeeId(involvedPersonEmployeeId);
            dto.setInvolvedPersonPosition(involvedPersonPosition);
            dto.setWitnesses(witnesses);
            dto.setWitnessesEmployeeId(witnessesEmployeeId);
            dto.setImmediateAction(immediateAction);
            dto.setCorrectiveAction(correctiveAction);
            dto.setMedicalAttentionRequired(medicalAttentionRequired);

            // Set area
            AreaDTO areadDTO = new AreaDTO();
            areadDTO.setCode(areaCode);
            dto.setArea(areadDTO);

            // Update Incident
            Incident updatedIncident = incidentService.update(incidentCode, dto);

            // Upload new images if provided
            if (incidentImages != null && incidentImages.length > 0) {
                for (MultipartFile imageFile : incidentImages) {
                    if (!imageFile.isEmpty()) {
                        incidentService.saveFile(updatedIncident.getCode(), imageFile, "IMAGE");
                    }
                }
            }

            // Upload new documents if provided
            if (incidentDocuments != null && incidentDocuments.length > 0) {
                for (MultipartFile docFile : incidentDocuments) {
                    if (!docFile.isEmpty()) {
                        incidentService.saveFile(updatedIncident.getCode(), docFile, "DOCUMENT");
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Incident updated successfully");
            response.put("code", updatedIncident.getCode());
            response.put("id", updatedIncident.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating incident: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error updating incident: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "An unexpected error occurred while updating the incident");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @DeleteMapping("/{incidentCode}")
    public ResponseEntity<Map<String, Object>> deleteIncident(@PathVariable String incidentCode) {
        try {
            incidentService.delete(incidentCode);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Incident deleted successfully");
            response.put("code", incidentCode);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error deleting incident: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Unexpected error deleting incident: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("status", "error");
            error.put("message", "An unexpected error occurred while deleting the incident");
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
