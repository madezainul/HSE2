package ahqpck.hse.safety.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class FileStoragePropertiesTrash {
    
    private String uploadDir = "./uploads"; // Default upload directory
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "txt"};
    private long maxFileSize = 10485760; // 10MB default

    //Getters and Setters
    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String[] getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(String[] allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
}
