package ahqpck.hse.safety.controller.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.exception.ResourceNotFoundException;
import ahqpck.hse.safety.model.entity.HseExternalTraining;
import ahqpck.hse.safety.service.HseExternalTrainingService;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hse-external-training")
@Slf4j
@RequiredArgsConstructor
public class HseExternalTrainingRestController {

    private final HseExternalTrainingService externalTrainingService;

    /**
     * Create a new HSE External Training record with file upload
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createExternalTraining(
            @RequestParam String employeeId,
            @RequestParam(required = false) String employeeName,
            @RequestParam String trainingName,
            @RequestParam String provider,
            @RequestParam String trainingDate,
            @RequestParam(required = false) Double duration,
            @RequestParam(required = false) String remarks,
            @RequestParam(value = "externalFile", required = false) MultipartFile externalFile) {
        try {
            log.info("Creating HSE External Training record for employee: {}", employeeId);

            HseExternalTraining created = externalTrainingService.create(
                    employeeId, employeeName, trainingName, provider,
                    LocalDate.parse(trainingDate), duration, remarks, externalFile);

            return ResponseEntity.ok(success("HSE External Training record created successfully", created));

        } catch (IllegalArgumentException | DateTimeParseException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(failure("Invalid input: " + e.getMessage()));
        } catch (IOException e) {
            log.error("File upload error", e);
            return ResponseEntity.badRequest().body(failure("File upload error"));
        } catch (Exception e) {
            log.error("Error creating HSE External Training record", e);
            return ResponseEntity.internalServerError().body(failure("Error creating record"));
        }
    }

    /**
     * Get all HSE External Training records with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllExternalTraining(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<HseExternalTraining> trainingPage = externalTrainingService.findAll(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", trainingPage.getContent());
            response.put("page", trainingPage.getNumber());
            response.put("size", trainingPage.getSize());
            response.put("totalElements", trainingPage.getTotalElements());
            response.put("totalPages", trainingPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching HSE External Training records", e);
            return ResponseEntity.internalServerError().body(failure("Error fetching records"));
        }
    }

    /**
     * Get a single HSE External Training record by code
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getExternalTrainingByCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(success(null, externalTrainingService.findByCode(code)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching HSE External Training record: {}", code, e);
            return ResponseEntity.internalServerError().body(failure("Error fetching record"));
        }
    }

    /**
     * Update an HSE External Training record
     */
    @PutMapping("/{code}")
    public ResponseEntity<?> updateExternalTraining(
            @PathVariable String code,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String trainingName,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String trainingDate,
            @RequestParam(required = false) Double duration,
            @RequestParam(required = false) String remarks,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            log.info("Updating HSE External Training record: {}", code);

            LocalDate parsedDate = trainingDate != null ? LocalDate.parse(trainingDate) : null;
            HseExternalTraining updated = externalTrainingService.update(
                    code, employeeId, employeeName, trainingName, provider, parsedDate, duration, remarks, file);

            return ResponseEntity.ok(success("HSE External Training record updated successfully", updated));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(failure(e.getMessage()));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(failure("Invalid input: " + e.getMessage()));
        } catch (IOException e) {
            log.error("File upload error", e);
            return ResponseEntity.badRequest().body(failure("File upload error"));
        } catch (Exception e) {
            log.error("Error updating HSE External Training record: {}", code, e);
            return ResponseEntity.internalServerError().body(failure("Error updating record"));
        }
    }

    /**
     * Delete an HSE External Training record
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<?> deleteExternalTraining(@PathVariable String code) {
        try {
            log.info("Deleting HSE External Training record: {}", code);
            externalTrainingService.delete(code);
            return ResponseEntity.ok(success("HSE External Training record deleted successfully", null));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting HSE External Training record: {}", code, e);
            return ResponseEntity.internalServerError().body(failure("Error deleting record"));
        }
    }

    /**
     * Delete file from an HSE External Training record (without deleting the record)
     */
    @DeleteMapping("/{code}/file")
    public ResponseEntity<?> deleteFile(@PathVariable String code) {
        try {
            log.info("Deleting file from HSE External Training record: {}", code);
            externalTrainingService.deleteFile(code);
            return ResponseEntity.ok(success("File deleted successfully", null));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(failure(e.getMessage()));
        } catch (IOException e) {
            log.error("File deletion error", e);
            return ResponseEntity.internalServerError().body(failure("File deletion error"));
        } catch (Exception e) {
            log.error("Error deleting file: {}", code, e);
            return ResponseEntity.internalServerError().body(failure("Error deleting file"));
        }
    }

    private Map<String, Object> success(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        if (message != null) {
            response.put("message", message);
        }
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }

    private Map<String, Object> failure(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return response;
    }
}
