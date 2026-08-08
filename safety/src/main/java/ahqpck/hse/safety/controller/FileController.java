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
     * Uses a catch-all path variable since category paths vary in depth
     * (e.g. "risk-assessment/documents" vs "inspection/fire-safety/documents").
     */
    @GetMapping("/{*path}")
    public ResponseEntity<Resource> serveFile(@PathVariable String path) {
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        String fileName = extractFileName(relativePath);

        try {
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
    @GetMapping("/download/{*path}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String path) {
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        String fileName = extractFileName(relativePath);

        try {
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

    private String extractFileName(String relativePath) {
        int lastSlash = relativePath.lastIndexOf('/');
        return lastSlash >= 0 ? relativePath.substring(lastSlash + 1) : relativePath;
    }
}
