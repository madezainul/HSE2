package ahqpck.hse.safety.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


import ahqpck.hse.safety.config.FileStorageProperties;
import ahqpck.hse.safety.util.FileCategory;

@Service
public class FileStorageService {

    private final Path baseUploadPath;

    private static final List<String> ALLOWED_IMAGE_TYPES =
            List.of(".jpg", ".jpeg", ".png", ".webp");

    private static final List<String> ALLOWED_DOCUMENT_TYPES =
            List.of(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx");

    public FileStorageService(FileStorageProperties properties) throws IOException {
        this.baseUploadPath = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();

        // Auto-create all category folders on startup
        for (FileCategory category : FileCategory.values()) {
            Files.createDirectories(baseUploadPath.resolve(category.getPath()));
        }
    }

    /**
     * Store a file under the given category folder.
     * @return relative path stored in DB (e.g. "incident/images/uuid_photo.jpg")
     */
    public String storeFile(MultipartFile file, FileCategory category) {
        String originalFileName = StringUtils.cleanPath(
                Objects.requireNonNull(file.getOriginalFilename())
        );

        validateExtension(originalFileName, category);

        String storedFileName = UUID.randomUUID() + "_" + originalFileName;
        Path targetPath = baseUploadPath.resolve(category.getPath()).resolve(storedFileName);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + storedFileName, e);
        }

        return category.getPath() + "/" + storedFileName;
    }

    /**
     * Load a file as a downloadable resource.
     * @param relativePath the path returned from storeFile()
     */
    public Resource loadFile(String relativePath) {
        try {
            Path filePath = baseUploadPath.resolve(relativePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            throw new RuntimeException("File not found or not readable: " + relativePath);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path: " + relativePath, e);
        }
    }

    /**
     * Delete a file from storage.
     */
    public void deleteFile(String relativePath) throws IOException {
        Path filePath = baseUploadPath.resolve(relativePath).normalize();
        Files.deleteIfExists(filePath);
    }

    // ── Private Helpers ──────────────────────────────────────────────────────

    private void validateExtension(String fileName, FileCategory category) {
        String lowerName = fileName.toLowerCase();
        List<String> allowed = category.getFileType() == FileCategory.FileType.IMAGE
                ? ALLOWED_IMAGE_TYPES
                : ALLOWED_DOCUMENT_TYPES;

        boolean valid = allowed.stream().anyMatch(lowerName::endsWith);
        if (!valid) {
            throw new RuntimeException(
                "Invalid file type for [" + category.name() + "]. Allowed: " + allowed
            );
        }
    }
}
