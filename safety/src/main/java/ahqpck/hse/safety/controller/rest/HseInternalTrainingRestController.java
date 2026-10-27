package ahqpck.hse.safety.controller.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.model.entity.HseInternalTraining;
import ahqpck.hse.safety.service.HseInternalTrainingService;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hse-internal-training")
@Slf4j
@RequiredArgsConstructor
public class HseInternalTrainingRestController {

    private final HseInternalTrainingService internalTrainingService;

    /**
     * Create a new HSE Internal Training record with file upload
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createInternalTraining(
            @RequestParam String employeeId,
            @RequestParam(required = false) String employeeName,
            @RequestParam String trainingName,
            @RequestParam String trainingDate,
            @RequestParam(required = false) Double duration,
            @RequestParam String trainerName,
            @RequestParam(required = false) String trainerId,
            @RequestParam(required = false) String remarks,
            @RequestParam(value = "internalFile", required = false) MultipartFile internalFile) {
        try {
            log.info("Creating HSE Internal Training record for employee: {}", employeeId);

            LocalDate parsedDate = LocalDate.parse(trainingDate);

            HseInternalTraining createdTraining = internalTrainingService.create(
                    employeeId, employeeName, trainingName, parsedDate, duration,
                    trainerName, trainerId, remarks, internalFile);

            log.info("HSE Internal Training record created with code: {}", createdTraining.getCode());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "HSE Internal Training record created successfully");
            response.put("data", createdTraining);

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
            log.error("Error creating HSE Internal Training record", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error creating record: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get all HSE Internal Training records with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllInternalTraining(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<HseInternalTraining> trainingPage = internalTrainingService.findAll(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", trainingPage.getContent());
            response.put("page", trainingPage.getNumber());
            response.put("size", trainingPage.getSize());
            response.put("totalElements", trainingPage.getTotalElements());
            response.put("totalPages", trainingPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching HSE Internal Training records", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching records: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get a single HSE Internal Training record by code
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getInternalTrainingByCode(@PathVariable String code) {
        try {
            HseInternalTraining training = internalTrainingService.findByCode(code);
            if (training == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", training);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching HSE Internal Training record: {}", code, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error fetching record: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Update an HSE Internal Training record
     */
    @PutMapping("/{code}")
    public ResponseEntity<?> updateInternalTraining(
            @PathVariable String code,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String trainingName,
            @RequestParam(required = false) String trainingDate,
            @RequestParam(required = false) Double duration,
            @RequestParam(required = false) String trainerName,
            @RequestParam(required = false) String trainerId,
            @RequestParam(required = false) String remarks,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            log.info("Updating HSE Internal Training record: {}", code);

            LocalDate parsedDate = trainingDate != null ? LocalDate.parse(trainingDate) : null;
            HseInternalTraining updatedTraining = internalTrainingService.update(
                    code, employeeId, employeeName, trainingName, parsedDate, duration,
                    trainerName, trainerId, remarks, file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "HSE Internal Training record updated successfully");
            response.put("data", updatedTraining);

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
            log.error("Error updating HSE Internal Training record: {}", code, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error updating record: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Delete an HSE Internal Training record
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<?> deleteInternalTraining(@PathVariable String code) {
        try {
            log.info("Deleting HSE Internal Training record: {}", code);
            internalTrainingService.delete(code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "HSE Internal Training record deleted successfully");

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
            log.error("Error deleting HSE Internal Training record: {}", code, e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error deleting record: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Delete file from an HSE Internal Training record (without deleting the record)
     */
    @DeleteMapping("/{code}/file")
    public ResponseEntity<?> deleteFile(@PathVariable String code) {
        try {
            log.info("Deleting file from HSE Internal Training record: {}", code);
            internalTrainingService.deleteFile(code);

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
