package ahqpck.hse.safety.controller;

import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.dto.ObservationDTO;
import ahqpck.hse.safety.model.dto.ObservationFileDTO;
import ahqpck.hse.safety.model.dto.AreaDTO;
import ahqpck.hse.safety.service.ObservationServiceTrash;
import ahqpck.hse.safety.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing observations (near misses, unsafe conditions, unsafe acts)
 * 
 * Routes:
 * - GET /observation                    : Observation List -> observation/index.html
 * - GET /observation/{observationId}    : Observation Details -> observation/detail.html
 * - GET /observation/edit/{observationId} : Edit Observation -> observation/edit.html
 * - POST /observation                   : Create Observation (form submission)
 * - POST /observation/api               : Create Observation (REST API)
 * - POST /observation/{observationId}/images     : Upload image
 * - POST /observation/{observationId}/attachments : Upload document
 * - GET /observation/{observationId}/files        : Get all files
 * - GET /observation/{observationId}/images       : Get images only
 * - GET /observation/{observationId}/documents    : Get documents only
 * - DELETE /observation/files/{fileId}            : Delete file
 */
@Controller
@RequestMapping("/observation")
@RequiredArgsConstructor
@Slf4j
public class ObservationControllerTrash {

    private final ObservationServiceTrash observationService;
    private final UserService userService;

    // ── View Methods ──────────────────────────────────────────────────────────

    @GetMapping
    public String listObservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            Model model) {

        log.info("Listing observations - page: {}, size: {}, status: {}, category: {}, type: {}",
                page, size, status, category, type);

        try {
            // TODO: Replace with actual ObservationService calls
            Page<Observation> observationsPage = new PageImpl<>(new ArrayList<>(), 
                    PageRequest.of(page, size, Sort.by("observationDate").descending()), 0);

            model.addAttribute("observations", observationsPage.getContent());
            model.addAttribute("observationsPage", observationsPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("totalPages", observationsPage.getTotalPages());
            model.addAttribute("title", "Observations");
            model.addAttribute("pageTitle", "Observations Management");
            model.addAttribute("statuses", Observation.Status.values());
            model.addAttribute("categories", Observation.Category.values());
            model.addAttribute("types", Observation.Type.values());
            model.addAttribute("observation", new ObservationDTO());

            log.info("Observation list loaded successfully");
            return "observation/index";
        } catch (Exception e) {
            log.error("Error listing observations", e);
            model.addAttribute("error", "Error loading observations");
            return "redirect:/observation";
        }
    }

    @GetMapping("/{observationId}")
    public String viewObservation(@PathVariable String observationId, Model model) {
        log.info("[Controller] Viewing observation with code: {}", observationId);

        try {
            // TODO: Replace with actual ObservationService call
            // Observation observation = observationService.getObservationByCode(observationId);
            
            model.addAttribute("title", "Observations");
            model.addAttribute("pageTitle", "Observation Details");
            // model.addAttribute("observation", observation);

            return "observation/detail";
        } catch (Exception e) {
            log.error("Error viewing observation", e);
            return "redirect:/observation";
        }
    }

    @GetMapping("/edit/{observationId}")
    public String editObservation(@PathVariable String observationId, Model model) {
        log.info("[Controller] Editing observation with code: {}", observationId);

        try {
            // TODO: Replace with actual ObservationService call
            // Observation observation = observationService.getObservationByCode(observationId);

            model.addAttribute("title", "Observations");
            model.addAttribute("pageTitle", "Edit Observation");
            model.addAttribute("statuses", Observation.Status.values());
            model.addAttribute("categories", Observation.Category.values());
            model.addAttribute("types", Observation.Type.values());
            model.addAttribute("mitigations", Observation.Mitigation.values());
            // model.addAttribute("observation", observation);

            return "observation/edit";
        } catch (Exception e) {
            log.error("Error editing observation", e);
            return "redirect:/observation";
        }
    }

    // ── REST API Methods ──────────────────────────────────────────────────────

    @PostMapping("/select-all")
    @ResponseBody
    public int selectAllObservations() {
        log.info("Select all observations request");
        // TODO: Replace with actual ObservationService call
        return 0;
    }

    @PostMapping
    public String createObservationForm(
            @RequestParam("observationDate") String observationDate,
            @RequestParam("areaCode") String areaCode,
            @RequestParam("category") String category,
            @RequestParam("type") String type,
            @RequestParam("description") String description,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "mitigation", required = false) String mitigation,
            @RequestParam(value = "correctiveAction", required = false) String correctiveAction,
            @RequestParam(value = "remarks", required = false) String remarks,
            @RequestPart(value = "observationImages", required = false) MultipartFile[] observationImages,
            @RequestPart(value = "observationDocuments", required = false) MultipartFile[] observationDocuments,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            log.info("[Form Submission] Creating new observation for area: {}", areaCode);

            // Get current user ID from authentication
            String currentUserId = getCurrentUserId(authentication);
            if (currentUserId == null) {
                redirectAttributes.addFlashAttribute("error", "Authentication required");
                return "redirect:/observation";
            }

            // Build ObservationDTO from form parameters
            ObservationDTO observationDTO = new ObservationDTO();
            observationDTO.setObservationDate(LocalDateTime.parse(observationDate.replace('T', ' ').replace(" ", "T")));
            observationDTO.setDescription(description);
            observationDTO.setCategory(category);
            observationDTO.setType(type);
            observationDTO.setStatus(status != null && !status.isEmpty() ? status : Observation.Status.OPEN.toString());
            observationDTO.setMitigation(mitigation);
            observationDTO.setCorrectiveAction(correctiveAction);
            observationDTO.setRemarks(remarks);

            // Set area
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setCode(areaCode);
            observationDTO.setArea(areaDTO);

            // Create the observation - TODO: Inject ObservationService when available
            // For now, return success message
            // Create the incident
            Observation createdObservation = observationService.createObservation(observationDTO, currentUserId);
            log.info("Observation created successfully - code: {}", createdObservation.getCode());

            // Upload images if provided
            if (observationImages != null && observationImages.length > 0) {
                for (MultipartFile file : observationImages) {
                    if (!file.isEmpty()) {
                        try {
                            observationService.addObservationImage(createdObservation.getCode(), file);
                            log.info("Image uploaded for observation: {}", file.getOriginalFilename());
                        } catch (Exception e) {
                            log.error("Error uploading image: {}", e.getMessage());
                        }
                    }
                }
            }

            // Upload documents if provided
            if (observationDocuments != null && observationDocuments.length > 0) {
                for (MultipartFile file : observationDocuments) {
                    if (!file.isEmpty()) {
                        try {
                            observationService.addObservationDocument(createdObservation.getCode(), file);
                            log.info("Document uploaded for observation: {}", file.getOriginalFilename());
                        } catch (Exception e) {
                            log.error("Error uploading document: {}", e.getMessage());
                        }
                    }
                }
            }

            redirectAttributes.addFlashAttribute("success", "Observation recorded successfully!");
            return "redirect:/observation";

        } catch (Exception e) {
            log.error("Error creating observation", e);
            redirectAttributes.addFlashAttribute("error", "Error creating observation: " + e.getMessage());
            return "redirect:/observation";
        }
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<?> createObservationApi(
            @Valid @RequestBody ObservationDTO observationDTO,
            Authentication authentication) {
        try {
            log.info("[REST API] Creating new observation for area: {}",
                    observationDTO.getArea() != null ? observationDTO.getArea().getCode() : "N/A");

            // Get current user ID from authentication
            String currentUserId = getCurrentUserId(authentication);
            if (currentUserId == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "Authentication required"));
            }

            // Set default status if not provided
            if (observationDTO.getStatus() == null || observationDTO.getStatus().trim().isEmpty()) {
                observationDTO.setStatus(Observation.Status.OPEN.toString());
            }

            // Create the observation using service
            Observation createdObservation = observationService.createObservation(observationDTO, currentUserId);

            log.info("Observation created successfully - code: {}", createdObservation.getCode());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Observation recorded successfully",
                    "observationCode", createdObservation.getCode()
            ));

        } catch (Exception e) {
            log.error("Error creating observation", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error creating observation: " + e.getMessage()
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

    @PostMapping("/{observationId}/images")
    @ResponseBody
    public ResponseEntity<?> uploadObservationImage(
            @PathVariable String observationId,
            @RequestParam("file") MultipartFile file) {
        try {
            ObservationFileDTO saved = observationService.addObservationImage(observationId, file);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error uploading image for observation: {}", observationId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Upload failed: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/{observationId}/attachments")
    @ResponseBody
    public ResponseEntity<?> uploadObservationAttachment(
            @PathVariable String observationId,
            @RequestParam("file") MultipartFile file) {
        try {
            ObservationFileDTO saved = observationService.addObservationDocument(observationId, file);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error uploading attachment for observation: {}", observationId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Upload failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{observationId}/files")
    @ResponseBody
    public ResponseEntity<?> getObservationFiles(@PathVariable String observationId) {
        try {
            // TODO: Implement using ObservationService
            List<ObservationFileDTO> files = new ArrayList<>();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("Error fetching files for observation: {}", observationId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{observationId}/images")
    @ResponseBody
    public ResponseEntity<?> getObservationImages(@PathVariable String observationId) {
        try {
            // TODO: Implement using ObservationService
            List<ObservationFileDTO> images = new ArrayList<>();
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            log.error("Error fetching images for observation: {}", observationId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/{observationId}/documents")
    @ResponseBody
    public ResponseEntity<?> getObservationDocuments(@PathVariable String observationId) {
        try {
            // TODO: Implement using ObservationService
            List<ObservationFileDTO> documents = new ArrayList<>();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error fetching documents for observation: {}", observationId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Error: " + e.getMessage()
            ));
        }
    }

    @DeleteMapping("/files/{fileId}")
    @ResponseBody
    public ResponseEntity<?> deleteObservationFile(@PathVariable String fileId) {
        try {
            // TODO: Implement using ObservationService
            log.info("Deleting file: {}", fileId);
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

        // Return username as user ID (TODO: Replace with UserService call when available)
        log.info("[getCurrentUserId] Returning username as user ID: {}", username);
        return username;
    }
}
