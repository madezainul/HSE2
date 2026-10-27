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
import ahqpck.hse.safety.model.entity.HseCertification;
import ahqpck.hse.safety.service.HseCertificationService;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hse-certification")
@Slf4j
@RequiredArgsConstructor
public class HseCertificationRestController {

    private final HseCertificationService certificationService;

    /**
     * Create a new HSE Certification record with file upload
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createCertification(
            @RequestParam String employeeId,
            @RequestParam(required = false) String employeeName,
            @RequestParam String certificateName,
            @RequestParam(required = false) String certificateNumber,
            @RequestParam(required = false) String issuingBody,
            @RequestParam String issuedDate,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) String remarks,
            @RequestParam(value = "certificationFile", required = false) MultipartFile certificationFile) {
        try {
            log.info("Creating HSE Certification record for employee: {}", employeeId);

            HseCertification created = certificationService.create(
                    employeeId, employeeName, certificateName, certificateNumber, issuingBody,
                    LocalDate.parse(issuedDate), parseOptionalDate(expiryDate), remarks, certificationFile);

            return ResponseEntity.ok(success("HSE Certification record created successfully", created));

        } catch (IllegalArgumentException | DateTimeParseException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(failure("Invalid input: " + e.getMessage()));
        } catch (IOException e) {
            log.error("File upload error", e);
            return ResponseEntity.badRequest().body(failure("File upload error"));
        } catch (Exception e) {
            log.error("Error creating HSE Certification record", e);
            return ResponseEntity.internalServerError().body(failure("Error creating record"));
        }
    }

    /**
     * Get all HSE Certification records with pagination
     */
    @GetMapping
    public ResponseEntity<?> getAllCertifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<HseCertification> certificationPage = certificationService.findAll(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", certificationPage.getContent());
            response.put("page", certificationPage.getNumber());
            response.put("size", certificationPage.getSize());
            response.put("totalElements", certificationPage.getTotalElements());
            response.put("totalPages", certificationPage.getTotalPages());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching HSE Certification records", e);
            return ResponseEntity.internalServerError().body(failure("Error fetching records"));
        }
    }

    /**
     * Get a single HSE Certification record by code
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getCertificationByCode(@PathVariable String code) {
        try {
            return ResponseEntity.ok(success(null, certificationService.findByCode(code)));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching HSE Certification record: {}", code, e);
            return ResponseEntity.internalServerError().body(failure("Error fetching record"));
        }
    }

    /**
     * Update an HSE Certification record
     */
    @PutMapping("/{code}")
    public ResponseEntity<?> updateCertification(
            @PathVariable String code,
            @RequestParam(required = false) String employeeId,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String certificateName,
            @RequestParam(required = false) String certificateNumber,
            @RequestParam(required = false) String issuingBody,
            @RequestParam(required = false) String issuedDate,
            @RequestParam(required = false) String expiryDate,
            @RequestParam(required = false) String remarks,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            log.info("Updating HSE Certification record: {}", code);

            HseCertification updated = certificationService.update(
                    code, employeeId, employeeName, certificateName, certificateNumber, issuingBody,
                    parseOptionalDate(issuedDate), parseOptionalDate(expiryDate), remarks, file);

            return ResponseEntity.ok(success("HSE Certification record updated successfully", updated));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(failure(e.getMessage()));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            log.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(failure("Invalid input: " + e.getMessage()));
        } catch (IOException e) {
            log.error("File upload error", e);
            return ResponseEntity.badRequest().body(failure("File upload error"));
        } catch (Exception e) {
            log.error("Error updating HSE Certification record: {}", code, e);
            return ResponseEntity.internalServerError().body(failure("Error updating record"));
        }
    }

    /**
     * Delete an HSE Certification record
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<?> deleteCertification(@PathVariable String code) {
        try {
            log.info("Deleting HSE Certification record: {}", code);
            certificationService.delete(code);
            return ResponseEntity.ok(success("HSE Certification record deleted successfully", null));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting HSE Certification record: {}", code, e);
            return ResponseEntity.internalServerError().body(failure("Error deleting record"));
        }
    }

    /**
     * Delete file from an HSE Certification record (without deleting the record)
     */
    @DeleteMapping("/{code}/file")
    public ResponseEntity<?> deleteFile(@PathVariable String code) {
        try {
            log.info("Deleting file from HSE Certification record: {}", code);
            certificationService.deleteFile(code);
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

    private LocalDate parseOptionalDate(String value) {
        return (value == null || value.isBlank()) ? null : LocalDate.parse(value);
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
