package ahqpck.hse.safety.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;

import ahqpck.hse.safety.util.Base62Utils;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hse_certifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HseCertification {

    private static final int EXPIRING_SOON_DAYS = 30;

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "certificate_name", nullable = false)
    private String certificateName;

    @Column(name = "certificate_number")
    private String certificateNumber;

    @Column(name = "issuing_body")
    private String issuingBody;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

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
    @JsonIgnore
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = Base62Utils.generateBase62UUID();
        }
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

    /**
     * Status is calculated from the expiry date so it never goes stale.
     */
    public String getStatus() {
        if (expiryDate == null) return "No Expiry";
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
        if (daysLeft < 0) return "Expired";
        if (daysLeft <= EXPIRING_SOON_DAYS) return "Expiring Soon";
        return "Valid";
    }

    public String getStatusBadgeClass() {
        return switch (getStatus()) {
            case "Expired" -> "badge bg-danger";
            case "Expiring Soon" -> "badge bg-warning text-dark";
            case "Valid" -> "badge bg-success";
            default -> "badge bg-secondary";
        };
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
