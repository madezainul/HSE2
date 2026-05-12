package ahqpck.hse.safety.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.UUID;

/**
 * Utility class for handling file upload and deletion.
 * Supports common file types with configurable validation.
 * 
 * Supported Categories & Examples:
 * 
 * 1. IMAGE  -> .jpg, .jpeg, .png, .gif, .webp, .bmp
 * 2. VIDEO  -> .mp4, .avi, .mov, .wmv, .mkv
 * 3. AUDIO  -> .mp3, .wav, .ogg, .aac
 * 4. DOC    -> .pdf, .doc, .docx, .xls, .xlsx, .ppt, .pptx, .txt, .csv
 * 5. ANY    -> All file types (use with caution)
 */
@Component
@Slf4j
public class FileUploadUtil {

    /**
     * Saves uploaded file to the specified directory with type-based validation.
     *
     * @param uploadDir  Target directory (e.g., "uploads/parts")
     * @param file       The uploaded MultipartFile
     * @param fileType   Type category: "image", "video", "audio", "doc", or "any"
     * @return           Saved file name (e.g., "abc123.jpg")
     * @throws IOException if file is invalid, too large, or save fails
     */
    public String saveFile(String uploadDir, MultipartFile file, String fileType) throws IOException {
        log.info("[FileUploadUtil.saveFile] Starting file upload - uploadDir: {}, fileType: {}", uploadDir, fileType);
        
        if (file == null || file.isEmpty()) {
            log.warn("[FileUploadUtil.saveFile] File is null or empty");
            return null;
        }

        log.info("[FileUploadUtil.saveFile] File details - name: {}, size: {} bytes, contentType: {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // Validate file size: 5MB max (adjust as needed)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            log.error("[FileUploadUtil.saveFile] File size {} exceeds 5MB limit", file.getSize());
            throw new IOException("File size exceeds 5 MB limit.");
        }

        // Validate file type
        validateFileType(file, fileType);
        log.info("[FileUploadUtil.saveFile] File type validation passed");

        // Generate unique filename
        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + extension;
        log.info("[FileUploadUtil.saveFile] Generated unique filename: {}", fileName);

        // Resolve upload path - make it absolute or relative to project root
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
        log.info("[FileUploadUtil.saveFile] Resolved upload path: {}", uploadPath);

        // Create directory if it doesn't exist
        if (!Files.exists(uploadPath)) {
            log.info("[FileUploadUtil.saveFile] Creating directory: {}", uploadPath);
            Files.createDirectories(uploadPath);
            log.info("[FileUploadUtil.saveFile] Directory created successfully");
        } else {
            log.info("[FileUploadUtil.saveFile] Directory already exists: {}", uploadPath);
        }

        Path filePath = uploadPath.resolve(fileName);
        log.info("[FileUploadUtil.saveFile] Saving file to: {}", filePath);
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("[FileUploadUtil.saveFile] File saved successfully at: {}", filePath);

        return fileName;
    }

    /**
     * Validates the file based on allowed extensions for the given type.
     *
     * @param file      The uploaded file
     * @param fileType  "image", "video", "audio", "doc", or "any"
     * @throws IOException if file type is not allowed
     */
    private static void validateFileType(MultipartFile file, String fileType) throws IOException {
        String contentType = file.getContentType();
        String originalName = file.getOriginalFilename();

        if (originalName == null || !originalName.contains(".")) {
            throw new IOException("Invalid file: missing extension.");
        }

        String extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();

        // Define allowed extensions by type
        switch (fileType.toLowerCase()) {
            case "image":
                if (!Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp").contains(extension)) {
                    throw new IOException("Only image files (JPG, PNG, GIF, WEBP) are allowed.");
                }
                // Optionally check contentType for images
                if (contentType == null || !contentType.startsWith("image/")) {
                    throw new IOException("Invalid image content type.");
                }
                break;

            case "video":
                if (!Arrays.asList(".mp4", ".avi", ".mov", ".wmv", ".mkv").contains(extension)) {
                    throw new IOException("Only video files (MP4, AVI, MOV) are allowed.");
                }
                if (contentType == null || !contentType.startsWith("video/")) {
                    throw new IOException("Invalid video content type.");
                }
                break;

            case "audio":
                if (!Arrays.asList(".mp3", ".wav", ".ogg", ".aac").contains(extension)) {
                    throw new IOException("Only audio files (MP3, WAV, OGG) are allowed.");
                }
                if (contentType == null || !contentType.startsWith("audio/")) {
                    throw new IOException("Invalid audio content type.");
                }
                break;

            case "doc":
                if (!Arrays.asList(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".csv").contains(extension)) {
                    throw new IOException("Only document files (PDF, DOC, XLS, etc.) are allowed.");
                }
                // Optionally check contentType for docs (not always reliable)
                break;

            case "any":
                // Allow any (still block dangerous types like .exe in production)
                if (Arrays.asList(".exe", ".bat", ".sh", ".js", ".html").contains(extension)) {
                    throw new IOException("Executable files are not allowed.");
                }
                break;

            default:
                throw new IOException("Unsupported file type: '" + fileType + "'. Use 'image', 'video', 'audio', 'doc', or 'any'.");
        }
    }

    /**
     * Deletes a file from the filesystem if it exists.
     *
     * @param uploadDir   Directory where file is stored
     * @param fileName    Name of the file to delete (e.g., "abc123.jpg")
     */
    public void deleteFile(String uploadDir, String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return;
        }
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (Exception e) {
            // Log if needed, but don't break the flow
            System.err.println("Failed to delete file: " + fileName + " - " + e.getMessage());
        }
    }
}