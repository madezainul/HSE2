package ahqpck.hse.safety.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

import ahqpck.hse.safety.config.FileStoragePropertiesTrash;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

@Service
@Slf4j
public class FileStorageServiceTrash {
    
    private final Path uploadDir;
    private final FileStoragePropertiesTrash fileStorageProperties;

    public FileStorageServiceTrash(FileStoragePropertiesTrash fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
        this.uploadDir = Path.of(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();

        log.info("[FileStorageService] Upload directory configured: {}", fileStorageProperties.getUploadDir());
        log.info("[FileStorageService] Absolute upload path: {}", this.uploadDir);

        try {
            Files.createDirectories(this.uploadDir);
            log.info("[FileStorageService] Upload directory created/exists at: {}", this.uploadDir);
        } catch (Exception e) {
            log.error("[FileStorageService] Could not create upload directory", e);
            throw new RuntimeException("Could not create upload directory", e); 
        }
    }

    public String storeFile(MultipartFile file) {
        validateFile(file);
        
        String fileName = generateUniqueFileName(file.getOriginalFilename());

        try {
            Path targetLocation = this.uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("[FileStorageService] File stored successfully: {}", targetLocation);
            return fileName;
        } catch (IOException ex) {
            log.error("[FileStorageService] Could not store file: {}", fileName, ex);
            throw new RuntimeException("Could not store file " + fileName, ex);
        }
    }

    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.uploadDir.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found " + fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not load file " + fileName, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

         if (file.getOriginalFilename() == null) {
            throw new RuntimeException("File name is missing");
        }

        String fileName = file.getOriginalFilename();
        String extension = getFileExtension(fileName).toLowerCase();
        
        if (!Arrays.asList(fileStorageProperties.getAllowedExtensions()).contains(extension)) {
            throw new RuntimeException("File type not allowed: " + extension);
        }

        if (file.getSize() > fileStorageProperties.getMaxFileSize()) {
            throw new RuntimeException("File size exceeds limit: " + file.getSize());
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String baseName = originalFileName.replace("." + extension, "");
        String uniqueId = java.util.UUID.randomUUID().toString();
        return baseName + "_" + uniqueId + "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex + 1);
    }

}
