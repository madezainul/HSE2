package ahqpck.hse.safety.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hse_inductions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HseInduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "trainee_id", nullable = false)
    private String traineeId;

    @Column(name = "trainee_name")
    private String traineeName;

    @Column(name = "induction_date", nullable = false)
    private LocalDate inductionDate;

    @Column(name = "trainers")
    private String trainers;

    @Column(name = "trainer_id")
    private String trainerId;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_extension", length = 10)
    private String fileExtension;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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
            case ".jpg", ".jpeg", ".png", ".webp" -> "fa fa-file-image me-2 text-info";
            default -> "fa fa-file me-2 text-muted";
        };
    }
}
