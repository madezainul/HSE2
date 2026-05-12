package ahqpck.hse.safety.controller.rest;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ahqpck.hse.safety.model.dto.FileUploadResponse;
import ahqpck.hse.safety.service.FileStorageService;

@RestController
@RequestMapping("/api/files")
public class FileUploadControllerTrash {

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileName = fileStorageService.storeFile(file);

            FileUploadResponse response = new FileUploadResponse(
                    fileName,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new FileUploadResponse(null, null, null, 0, "Could not upload file: " + ex.getMessage()));
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<List<FileUploadResponse>> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        List<FileUploadResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                String fileName = fileStorageService.storeFile(file);
                responses.add(new FileUploadResponse(fileName, file.getOriginalFilename(),
                        file.getContentType(), file.getSize()));
            } catch (Exception ex) {
                responses.add(new FileUploadResponse(null, file.getOriginalFilename(),
                        null, 0L, ex.getMessage()));
            }
        }
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Resource resource = fileStorageService.loadFileAsResource(fileName);
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
