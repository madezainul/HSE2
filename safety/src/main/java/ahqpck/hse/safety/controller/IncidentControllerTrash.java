package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.model.entity.IncidentComment;
import ahqpck.hse.safety.model.dto.IncidentDTO;
import ahqpck.hse.safety.model.dto.IncidentFileDTO;
import ahqpck.hse.safety.model.dto.AreaDTO;
import ahqpck.hse.safety.service.IncidentServiceTrash;
import ahqpck.hse.safety.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/incident-trash")
@RequiredArgsConstructor
@Slf4j
public class IncidentControllerTrash {

    private final IncidentServiceTrash incidentService;
    private final UserService userService;

    // ── View Methods ──────────────────────────────────────────────────────────

    @GetMapping
    public String listIncidents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String type,
            Model model) {

        log.info("Listing incidents - page: {}, size: {}, status: {}, severity: {}, type: {}",
                page, size, status, severity, type);

        // Create pageable with sorting by reportDate in ascending order
        Pageable pageable = PageRequest.of(page, size, Sort.by("reportDate").descending());
        Page<Incident> incidentsPage;

        if (status != null && !status.isEmpty()) {
            try {
                Incident.IncidentStatus statusEnum = Incident.IncidentStatus.valueOf(status);
                incidentsPage = incidentService.getIncidentsByStatusPaginated(statusEnum, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status filter: {}", status);
                incidentsPage = incidentService.getIncidentsPaginated(pageable);
            }
        } else if (severity != null && !severity.isEmpty()) {
            try {
                Incident.SeverityLevel severityEnum = Incident.SeverityLevel.valueOf(severity);
                incidentsPage = incidentService.getIncidentsBySeverityPaginated(severityEnum, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid severity filter: {}", severity);
                incidentsPage = incidentService.getIncidentsPaginated(pageable);
            }
        } else if (type != null && !type.isEmpty()) {
            try {
                Incident.Type typeEnum = Incident.Type.valueOf(type);
                incidentsPage = incidentService.getIncidentsByTypePaginated(typeEnum, pageable);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid type filter: {}", type);
                incidentsPage = incidentService.getIncidentsPaginated(pageable);
            }
        } else {
            incidentsPage = incidentService.getIncidentsPaginated(pageable);
        }

        model.addAttribute("incidents", incidentsPage.getContent());
        model.addAttribute("incidentsPage", incidentsPage);
        model.addAttribute("totalCount", incidentService.getTotalIncidentCount());
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalPages", incidentsPage.getTotalPages());
        model.addAttribute("title", "Incident Report");
        model.addAttribute("pageTitle", "Incidents Management");
        model.addAttribute("severityLevels", Incident.SeverityLevel.values());
        model.addAttribute("statuses", Incident.IncidentStatus.values());
        model.addAttribute("incidentTypes", Incident.Type.values());
        model.addAttribute("incident", new IncidentDTO());

        return "incident/index";
    }

    @GetMapping("/{incidentId}")
    public String viewIncident(@PathVariable String incidentId, Model model) {
        log.info("[Controller] Viewing incident with code: {}", incidentId);

        Incident incident = incidentService.getIncidentByCode(incidentId);
        if (incident == null) {
            log.warn("Incident not found: {}", incidentId);
            return "redirect:/incident";
        }

        model.addAttribute("incident", incident);
        model.addAttribute("title", "Incident Report");
        model.addAttribute("pageTitle", "Incident Details");

        return "incident/detail";
    }

    @GetMapping("/edit/{incidentId}")
    public String editIncident(@PathVariable String incidentId, Model model) {
        log.info("[Controller] Editing incident with code: {}", incidentId);

        Incident incident = incidentService.getIncidentByCode(incidentId);
        if (incident == null) {
            log.warn("Incident not found: {}", incidentId);
            return "redirect:/incident";
        }

        model.addAttribute("incident", incident);
        model.addAttribute("title", "Incident Report");
        model.addAttribute("pageTitle", "Edit Incident");
        model.addAttribute("severityLevels", Incident.SeverityLevel.values());
        model.addAttribute("statuses", Incident.IncidentStatus.values());
        model.addAttribute("incidentTypes", Incident.Type.values());

        return "incident/edit";
    }

    // ── REST API Methods ──────────────────────────────────────────────────────

    @PostMapping("/select-all")
    @ResponseBody
    public int selectAllIncidents() {
        log.info("Select all incidents request");
        return (int) incidentService.getTotalIncidentCount();
    }

    @PostMapping
    public String createIncidentForm(
            @RequestParam("reportDate") String reportDate,
            @RequestParam("areaCode") String areaCode,
            @RequestParam("severity") String severity,
            @RequestParam("type") String type,
            @RequestParam("description") String description,
            @RequestParam(value = "involvedPersonName", required = false) String involvedPersonName,
            @RequestParam(value = "involvedPersonEmployeeId", required = false) String involvedPersonEmployeeId,
            @RequestParam(value = "involvedPersonPosition", required = false) String involvedPersonPosition,
            @RequestParam(value = "witnesses", required = false) String witnesses,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "immediateAction", required = false) String immediateAction,
            @RequestParam(value = "correctiveAction", required = false) String correctiveAction,
            @RequestParam(value = "medicalAttentionRequired", required = false) Boolean medicalAttentionRequired,
            @RequestPart(value = "incidentImages", required = false) MultipartFile[] incidentImages,
            @RequestPart(value = "incidentDocuments", required = false) MultipartFile[] incidentDocuments,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("[Form Submission] Creating new incident for area: {}", areaCode);

            // Get current user ID from authentication
            String currentUserId = getCurrentUserId(authentication);
            if (currentUserId == null) {
                redirectAttributes.addFlashAttribute("error", "Authentication required");
                return "redirect:/incident";
            }

            // Build IncidentDTO from form parameters
            IncidentDTO incidentDTO = new IncidentDTO();
            incidentDTO.setReportDate(LocalDateTime.parse(reportDate.replace('T', ' ').replace(" ", "T")));
            incidentDTO.setDescription(description);
            incidentDTO.setSeverity(severity);
            incidentDTO.setType(type);
            incidentDTO.setStatus(status != null && !status.isEmpty() ? status : Incident.IncidentStatus.REPORTED.toString());
            incidentDTO.setInvolvedPersonName(involvedPersonName);
            incidentDTO.setInvolvedPersonEmployeeId(involvedPersonEmployeeId);
            incidentDTO.setInvolvedPersonPosition(involvedPersonPosition);
            incidentDTO.setWitnesses(witnesses);
            incidentDTO.setImmediateAction(immediateAction);
            incidentDTO.setCorrectiveAction(correctiveAction);
            incidentDTO.setMedicalAttentionRequired(medicalAttentionRequired != null ? medicalAttentionRequired : false);

            // Set area
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setCode(areaCode);
            incidentDTO.setArea(areaDTO);

            // Create the incident
            Incident createdIncident = incidentService.createIncident(incidentDTO, currentUserId);
            log.info("Incident created successfully - code: {}", createdIncident.getCode());

            // Upload images if provided
            if (incidentImages != null && incidentImages.length > 0) {
                for (MultipartFile file : incidentImages) {
                    if (!file.isEmpty()) {
                        try {
                            incidentService.addIncidentImage(createdIncident.getCode(), file);
                            log.info("Image uploaded for incident: {}", createdIncident.getCode());
                        } catch (Exception e) {
                            log.error("Error uploading image: {}", e.getMessage());
                        }
                    }
                }
            }

            // Upload documents if provided
            if (incidentDocuments != null && incidentDocuments.length > 0) {
                for (MultipartFile file : incidentDocuments) {
                    if (!file.isEmpty()) {
                        try {
                            incidentService.addIncidentDocument(createdIncident.getCode(), file);
                            log.info("Document uploaded for incident: {}", createdIncident.getCode());
                        } catch (Exception e) {
                            log.error("Error uploading document: {}", e.getMessage());
                        }
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success", "Incident " + createdIncident.getCode() + " reported successfully!");
            return "redirect:/incident";

        } catch (Exception e) {
            log.error("Error creating incident", e);
            redirectAttributes.addFlashAttribute("error", "Error creating incident: " + e.getMessage());
            return "redirect:/incident";
        }
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> createIncidentApi(
            @Valid @RequestBody IncidentDTO incidentDTO,
            Authentication authentication) {
        try {
            log.info("[REST API] Creating new incident for area: {}",
                    incidentDTO.getArea() != null ? incidentDTO.getArea().getCode() : "N/A");

            // Get current user ID from authentication
            String currentUserId = getCurrentUserId(authentication);
            if (currentUserId == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Authentication required"));
            }

            // Default status to REPORTED if not provided
            if (incidentDTO.getStatus() == null || incidentDTO.getStatus().trim().isEmpty()) {
                incidentDTO.setStatus(Incident.IncidentStatus.REPORTED.toString());
            }

            // Create the incident
            Incident createdIncident = incidentService.createIncident(incidentDTO, currentUserId);

            log.info("Incident created successfully - code: {}, id: {}",
                    createdIncident.getCode(), createdIncident.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Incident reported successfully",
                    "incidentCode", createdIncident.getCode()
            ));

        } catch (Exception e) {
            log.error("Error creating incident", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error creating incident: " + e.getMessage()
            ));
        }
    }

    // ── Validation Error Handler ──────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Validation failed");

        log.warn("Validation error: {}", errorMessage);

        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", errorMessage
        ));
    }

    // ── File Upload/Download Methods ──────────────────────────────────────────

    @PostMapping("/{incidentId}/images")
    @ResponseBody
    public ResponseEntity<?> uploadIncidentImage(
            @PathVariable String incidentId,
            @RequestParam("file") MultipartFile file) {
        try {
            IncidentFileDTO saved = incidentService.addIncidentImage(incidentId, file);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error uploading image for incident: {}", incidentId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Upload failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{incidentId}/attachments")
    @ResponseBody
    public ResponseEntity<?> uploadIncidentAttachment(
            @PathVariable String incidentId,
            @RequestParam("file") MultipartFile file) {
        try {
            IncidentFileDTO saved = incidentService.addIncidentDocument(incidentId, file);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error uploading attachment for incident: {}", incidentId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Upload failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{incidentId}/files")
    @ResponseBody
    public ResponseEntity<?> getIncidentFiles(@PathVariable String incidentId) {
        try {
            List<IncidentFileDTO> files = incidentService.getIncidentFiles(incidentId);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("Error fetching files for incident: {}", incidentId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{incidentId}/images")
    @ResponseBody
    public ResponseEntity<?> getIncidentImages(@PathVariable String incidentId) {
        try {
            List<IncidentFileDTO> images = incidentService.getIncidentImages(incidentId);
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            log.error("Error fetching images for incident: {}", incidentId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{incidentId}/documents")
    @ResponseBody
    public ResponseEntity<?> getIncidentDocuments(@PathVariable String incidentId) {
        try {
            List<IncidentFileDTO> documents = incidentService.getIncidentDocuments(incidentId);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error fetching documents for incident: {}", incidentId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/files/{fileId}")
    @ResponseBody
    public ResponseEntity<?> deleteIncidentFile(@PathVariable String fileId) {
        try {
            incidentService.deleteIncidentFile(fileId);
            return ResponseEntity.ok(Map.of("success", true, "message", "File deleted"));
        } catch (Exception e) {
            log.error("Error deleting file: {}", fileId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Delete failed: " + e.getMessage()
            ));
        }
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private String getCurrentUserId(Authentication authentication) {
        if (authentication == null) {
            log.warn("[getCurrentUserId] Authentication is null");
            return null;
        }

        if (!authentication.isAuthenticated()) {
            log.warn("[getCurrentUserId] User is not authenticated");
            return null;
        }

        Object principal = authentication.getPrincipal();
        String username;

        if (principal instanceof org.springframework.security.core.userdetails.User springUser) {
            username = springUser.getUsername();
            log.debug("[getCurrentUserId] Extracted username from spring user: {}", username);
        } else if (principal instanceof String) {
            username = (String) principal;
            log.debug("[getCurrentUserId] Principal is string username: {}", username);
        } else {
            username = authentication.getName();
            log.debug("[getCurrentUserId] Got username from authentication.getName(): {}", username);
        }

        if (username == null || username.isEmpty()) {
            log.warn("[getCurrentUserId] Could not extract username from authentication");
            return null;
        }

        User user = userService.findByUsername(username);
        if (user != null) {
            log.info("[getCurrentUserId] Found user in database: {}", user.getId());
            return user.getId();
        } else {
            log.warn("[getCurrentUserId] User not found in database: {}", username);
            return username;
        }
    }

    /**
     * Add a comment to an incident.
     * @param incidentId the incident ID
     * @param content the comment content
     * @param authentication the current user authentication
     * @return JSON response with success status
     */
    @PostMapping("/{incidentId}/comments")
    @ResponseBody
    public Map<String, Object> addComment(
            @PathVariable String incidentId,
            @RequestParam("content") String content,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            Incident incident = incidentService.getIncidentByCode(incidentId);
            if (incident == null) {
                response.put("success", false);
                response.put("message", "Incident not found");
                return response;
            }

            String currentUserId = getCurrentUserId(authentication);
            
            // Fetch user to get full name
            User currentUser = userService.findById(currentUserId);
            String userFullName = currentUser != null ? currentUser.getFullName() : currentUserId;
            
            // Create and save comment
            IncidentComment comment = new IncidentComment();
            comment.setIncident(incident);
            comment.setContent(content);
            comment.setCreatedByUser(currentUser);
            comment.setCreatedAt(LocalDateTime.now());
            
            incidentService.addComment(comment);
            
            // Format response
            response.put("success", true);
            response.put("content", content);
            response.put("createdBy", userFullName);
            response.put("formattedDate", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM, yyyy HH:mm")));
            
            log.info("Comment added to incident: {}", incidentId);
            return response;
        } catch (Exception e) {
            log.error("Error adding comment: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return response;
        }
    }
}