package ahqpck.hse.safety.model.dto;

public class FileUploadResponseTrash {
    private String fileName;
    private String originalFileName;
    private String contentType;
    private long size;
    private String errorMessage;

    public FileUploadResponseTrash(String fileName, String originalFileName, String contentType, long size) {
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.size = size;
    }

    public FileUploadResponseTrash(String fileName, String originalFileName, String contentType, long size, String errorMessage) {
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.size = size;
        this.errorMessage = errorMessage;
        
    }
}
