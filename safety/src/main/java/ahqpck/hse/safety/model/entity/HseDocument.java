package ahqpck.hse.safety.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hse_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HseDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 20)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 30)
    private Module module;

    @Column(name = "doc_name", nullable = false)
    private String docName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_extension", length = 10)
    private String fileExtension;

    @Builder.Default
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id")
    private User uploadedBy;

    // ── Presentation helpers ──────────────────────────────────────────────

    public String getFormattedFileSize() {
        if (fileSizeBytes == null) return "N/A";
        if (fileSizeBytes >= 1_048_576) {
            return String.format("%.2f MB", fileSizeBytes / 1_048_576.0);
        }
        return String.format("%.0f KB", fileSizeBytes / 1024.0);
    }

    public String getFileIconClass() {
        if (fileExtension == null) return "fa fa-file me-2 text-muted";
        return switch (fileExtension.toLowerCase()) {
            case ".pdf" -> "fa fa-file-pdf me-2 text-danger";
            case ".doc", ".docx" -> "fa fa-file-word me-2 text-primary";
            case ".xls", ".xlsx" -> "fa fa-file-excel me-2 text-success";
            case ".ppt", ".pptx" -> "fa fa-file-powerpoint me-2 text-warning";
            default -> "fa fa-file me-2 text-muted";
        };
    }

    // ── Module enum ───────────────────────────────────────────────────────

    public enum Module {
        RISK_ASSESSMENT, WORK_INSTRUCTION, WORK_PERMIT, HSE_FORM;

        public String toUrlPath() {
            return name().toLowerCase().replace("_", "-");
        }

        public static Module fromUrlPath(String path) {
            return Module.valueOf(path.toUpperCase().replace("-", "_"));
        }
    }
}
