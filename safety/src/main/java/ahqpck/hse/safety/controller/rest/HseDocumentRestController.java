package ahqpck.hse.safety.controller.rest;

import ahqpck.hse.safety.model.entity.HseDocument;
import ahqpck.hse.safety.service.HseDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@Slf4j
public class HseDocumentRestController {

    @Autowired
    private HseDocumentService documentService;

    @PostMapping(value = "/{module}", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> upload(
            @PathVariable String module,
            @RequestParam("docName") String docName,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "isActive", required = false, defaultValue = "true") Boolean isActive,
            @RequestParam("docFile") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();
        try {
            HseDocument.Module mod = HseDocument.Module.fromUrlPath(module);
            HseDocument doc = documentService.upload(mod, docName, category, department, description, isActive, file);
            response.put("status", "success");
            response.put("code", doc.getCode());
            response.put("message", "Document uploaded successfully");
            return ResponseEntity.status(201).body(response);
        } catch (Exception e) {
            log.error("Error uploading document for module {}: {}", module, e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/{code}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable String code,
            @RequestParam String status) {
        Map<String, Object> response = new HashMap<>();
        try {
            HseDocument.DocumentStatus docStatus = HseDocument.DocumentStatus.valueOf(status.toUpperCase());
            documentService.updateStatus(code, docStatus);
            response.put("status", "success");
            response.put("message", "Status updated to " + docStatus.name().toLowerCase());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("status", "error");
            response.put("message", "Invalid status value: " + status);
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error updating status for document {}: {}", code, e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String code) {
        Map<String, Object> response = new HashMap<>();
        try {
            documentService.delete(code);
            response.put("status", "success");
            response.put("message", "Document deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting document {}: {}", code, e.getMessage());
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
