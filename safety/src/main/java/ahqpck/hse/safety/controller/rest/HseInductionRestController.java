package ahqpck.hse.safety.controller.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.model.entity.HseInduction;
import ahqpck.hse.safety.service.HseTrainingService;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hse-induction")
@Slf4j
@RequiredArgsConstructor
public class HseInductionRestController {

    private final HseTrainingService trainingService;

    /**
     * Create a new HSE Induction record with file upload
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createInduction(
            @RequestParam String traineeId,
            @RequestParam(required = false) String traineeName,
            @RequestParam String inductionDate,
            @RequestParam String trainers,
            @RequestParam(required = false) String trainerId,
            @RequestParam(value = "inductionFile", required = false) MultipartFile inductionFile,
            @RequestParam(value = "inductionImages", required = false) MultipartFile[] inductionImages,
            @RequestParam(value = "inductionDocuments", required = false) MultipartFile[] inductionDocuments) {
        try {
            log.info("Creating HSE Induction record for trainee: {} ({})", traineeId, traineeName);

            LocalDate parsedDate = LocalDate.parse(inductionDate);

            // Use file from modal (inductionFile), or fallback to array-based upload from legacy form
            MultipartFile fileToStore = null;
            if (inductionFile != null && !inductionFile.isEmpty()) {
                fileToStore = inductionFile;
            } else if (inductionImages != null && inductionImages.length > 0 && !inductionImages[0].isEmpty()) {
                fileToStore = inductionImages[0];
            } else if (inductionDocuments != null && inductionDocuments.length > 0 && !inductionDocuments[0].isEmpty()) {
                fileToStore = inductionDocuments[0];
            }

            HseInduction createdInduction = trainingService.create(traineeId, traineeName, parsedDate, trainers, trainerId, fileToStore);
            log.info("HSE Induction record created with code: {}", createdInduction.getCode());

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
        } catch (IOException e) {
            log.error("File upload error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "File upload error: " + e.getMessage());
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
            Page<HseInduction> inductionPage = trainingService.findAll(pageable);

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
            HseInduction induction = trainingService.findByCode(code);
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
            @RequestParam(required = false) String traineeId,
            @RequestParam(required = false) String traineeName,
            @RequestParam(required = false) String inductionDate,
            @RequestParam(required = false) String trainers,
            @RequestParam(required = false) String trainerId,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            log.info("Updating HSE Induction record: {}", code);

            LocalDate parsedDate = inductionDate != null ? LocalDate.parse(inductionDate) : null;
            HseInduction updatedInduction = trainingService.update(code, traineeId, traineeName, parsedDate, trainers, trainerId, file);

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
        } catch (IOException e) {
            log.error("File upload error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "File upload error: " + e.getMessage());
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
            trainingService.delete(code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "HSE Induction record deleted successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Record not found: {}", code);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("File deletion error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "File deletion error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            log.error("Error deleting HSE Induction record: {}", code, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting record: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Delete file from an HSE Induction record (without deleting the record)
     */
    @DeleteMapping("/{code}/file")
    public ResponseEntity<?> deleteFile(@PathVariable String code) {
        try {
            log.info("Deleting file from HSE Induction record: {}", code);
            trainingService.deleteFile(code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File deleted successfully");

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Record not found: {}", code);
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("File deletion error", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "File deletion error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            log.error("Error deleting file: {}", code, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
