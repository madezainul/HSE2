package ahqpck.hse.safety.controller.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.model.entity.HseInduction;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.service.HseInductionService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hse-induction")
@Slf4j
@RequiredArgsConstructor
public class HseInductionRestController {

    private final HseInductionService inductionService;

    /**
     * Create a new HSE Induction record with optional file uploads
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createInduction(
            @RequestParam String inductionName,
            @RequestParam String category,
            @RequestParam String inductionDate,
            @RequestParam(required = false) String value,
            @RequestParam(required = false) String description,
            @RequestParam(value = "inductionImages", required = false) MultipartFile[] inductionImages,
            @RequestParam(value = "inductionDocuments", required = false) MultipartFile[] inductionDocuments,
            Authentication authentication) {
        try {
            log.info("Creating HSE Induction record: {}", inductionName);

            // Get current user
            User currentUser = (User) authentication.getPrincipal();

            // Parse induction date
            LocalDate parsedDate = LocalDate.parse(inductionDate);

            // Create induction record
            HseInduction induction = HseInduction.builder()
                    .inductionName(inductionName)
                    .category(category)
                    .inductionDate(parsedDate)
                    .value(value)
                    .description(description)
                    .build();

            HseInduction createdInduction = inductionService.create(induction, currentUser);
            log.info("HSE Induction record created with code: {}", createdInduction.getCode());

            // Upload images if provided
            if (inductionImages != null && inductionImages.length > 0) {
                for (MultipartFile imageFile : inductionImages) {
                    if (!imageFile.isEmpty()) {
                        inductionService.saveFile(createdInduction.getCode(), imageFile, "IMAGE");
                        log.info("Image uploaded for induction: {}", createdInduction.getCode());
                    }
                }
            }

            // Upload documents if provided
            if (inductionDocuments != null && inductionDocuments.length > 0) {
                for (MultipartFile docFile : inductionDocuments) {
                    if (!docFile.isEmpty()) {
                        inductionService.saveFile(createdInduction.getCode(), docFile, "DOCUMENT");
                        log.info("Document uploaded for induction: {}", createdInduction.getCode());
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "HSE Induction record created successfully");
            response.put("data", createdInduction);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error creating HSE Induction record", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating record: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get all HSE Induction records with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllInductions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<HseInduction> inductionPage = inductionService.findAll(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", inductionPage.getContent());
            response.put("page", inductionPage.getNumber());
            response.put("size", inductionPage.getSize());
            response.put("totalElements", inductionPage.getTotalElements());
            response.put("totalPages", inductionPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching HSE Induction records", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching records: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get a single HSE Induction record by code
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getInductionByCode(@PathVariable String code) {
        try {
            HseInduction induction = inductionService.findByCode(code);
            if (induction == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", induction);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching HSE Induction record: {}", code, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching record: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update an HSE Induction record
     */
    @PutMapping("/{code}")
    public ResponseEntity<?> updateInduction(
            @PathVariable String code,
            @RequestParam(required = false) String inductionName,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String inductionDate,
            @RequestParam(required = false) String value,
            @RequestParam(required = false) String description) {
        try {
            log.info("Updating HSE Induction record: {}", code);

            HseInduction updateData = new HseInduction();
            if (inductionName != null) updateData.setInductionName(inductionName);
            if (category != null) updateData.setCategory(category);
            if (inductionDate != null) updateData.setInductionDate(LocalDate.parse(inductionDate));
            if (value != null) updateData.setValue(value);
            if (description != null) updateData.setDescription(description);

            HseInduction updatedInduction = inductionService.update(code, updateData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "HSE Induction record updated successfully");
            response.put("data", updatedInduction);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error updating HSE Induction record: {}", code, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating record: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Delete an HSE Induction record
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<?> deleteInduction(@PathVariable String code) {
        try {
            log.info("Deleting HSE Induction record: {}", code);
            inductionService.delete(code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "HSE Induction record deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting HSE Induction record: {}", code, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting record: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Delete a file associated with an HSE Induction record
     */
    @DeleteMapping("/file/{fileId}")
    public ResponseEntity<?> deleteFile(@PathVariable String fileId) {
        try {
            log.info("Deleting HSE Induction file: {}", fileId);
            inductionService.deleteFile(fileId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File deleted successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error deleting file: {}", fileId, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
