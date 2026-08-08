package ahqpck.hse.safety.model.entity;

import java.time.LocalDateTime;

import ahqpck.hse.safety.util.Base62Utils;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hse_induction_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HseInductionFile {

    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hse_induction_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private HseInduction hseInduction;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType;  // IMAGE or DOCUMENT

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = Base62Utils.generateBase62UUID();
        }
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }

    public enum FileType {
        IMAGE,
        DOCUMENT
    }
}
