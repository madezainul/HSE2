package ahqpck.hse.safety.service;

import ahqpck.hse.safety.model.entity.HseDocument;
import ahqpck.hse.safety.model.entity.User;
import ahqpck.hse.safety.repository.HseDocumentRepository;
import ahqpck.hse.safety.repository.UserRepository;
import ahqpck.hse.safety.util.FileCategory;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class HseDocumentService {

    @Autowired
    private HseDocumentRepository documentRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    public List<HseDocument> findByModule(HseDocument.Module module) {
        return documentRepository.findByModuleOrderByUploadedAtDesc(module);
    }

    @Transactional
    public HseDocument upload(HseDocument.Module module, String docName, String category,
                               String department, String description, Boolean isActive, MultipartFile file) {

        FileCategory fileCategory = toFileCategory(module);
        String filePath = fileStorageService.storeFile(file, fileCategory);

        String originalName = Objects.requireNonNull(file.getOriginalFilename(), "File name must not be null");
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
                : "";

        String code = generateCode(module);
        User uploader = getCurrentUser();

        HseDocument doc = HseDocument.builder()
                .code(code)
                .module(module)
                .docName(docName)
                .category(category)
                .department(department)
                .description(description)
                .filePath(filePath)
                .originalFileName(originalName)
                .fileSizeBytes(file.getSize())
                .fileExtension(ext)
                .isActive(isActive != null ? isActive : true)
                .uploadedAt(LocalDateTime.now())
                .uploadedBy(uploader)
                .build();

        return documentRepository.save(doc);
    }

    @Transactional
    public HseDocument updateStatus(String code, HseDocument.DocumentStatus status) {
        HseDocument doc = documentRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Document not found: " + code));
        doc.setStatus(status);
        return documentRepository.save(doc);
    }

    @Transactional
    public void delete(String code) throws IOException {
        HseDocument doc = documentRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Document not found: " + code));
        if (doc.getFilePath() != null) {
            fileStorageService.deleteFile(doc.getFilePath());
        }
        documentRepository.delete(doc);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String generateCode(HseDocument.Module module) {
        long count = documentRepository.countByModule(module) + 1;
        String prefix = switch (module) {
            case RISK_ASSESSMENT -> "RA";
            case WORK_INSTRUCTION -> "WI";
            case WORK_PERMIT -> "WP";
            case HSE_FORM -> "HF";
        };
        return String.format("%s%06d", prefix, count);
    }

    private FileCategory toFileCategory(HseDocument.Module module) {
        return switch (module) {
            case RISK_ASSESSMENT -> FileCategory.RISK_ASSESSMENT_DOCUMENT;
            case WORK_INSTRUCTION -> FileCategory.WORK_INSTRUCTION_DOCUMENT;
            case WORK_PERMIT -> FileCategory.WORK_PERMIT_DOCUMENT;
            case HSE_FORM -> FileCategory.HSE_FORM_DOCUMENT;
        };
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElse(null);
    }
}
