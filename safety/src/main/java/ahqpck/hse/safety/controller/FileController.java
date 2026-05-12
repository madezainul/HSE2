package ahqpck.hse.safety.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ahqpck.hse.safety.service.FileStorageService;

@Controller
@RequestMapping("/file")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Serve files inline (for viewing in browser).
     * URL format: /file/incident/images/uuid_filename.jpg
     * Maps to: uploads/incident/images/uuid_filename.jpg
     */
    @GetMapping("/{category}/{type}/{fileName}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String category,
            @PathVariable String type,
            @PathVariable String fileName) {

        try {
            String relativePath = category + "/" + type + "/" + fileName;
            log.info("Serving file inline: {}", relativePath);

            Resource resource = fileStorageService.loadFile(relativePath);
            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error serving file", e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Download files as attachments (forces browser download).
     * URL format: /file/download/risk-assessment/documents/uuid_filename.pdf
     */
    @GetMapping("/download/{category}/{type}/{fileName}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String category,
            @PathVariable String type,
            @PathVariable String fileName) {

        try {
            String relativePath = category + "/" + type + "/" + fileName;
            log.info("Downloading file as attachment: {}", relativePath);

            Resource resource = fileStorageService.loadFile(relativePath);
            MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                    .orElse(MediaType.APPLICATION_OCTET_STREAM);

            // Strip the UUID prefix to give the user a clean original filename
            String cleanName = fileName.contains("_")
                    ? fileName.substring(fileName.indexOf("_") + 1)
                    : fileName;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cleanName + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("Error downloading file", e);
            return ResponseEntity.notFound().build();
        }
    }
}
