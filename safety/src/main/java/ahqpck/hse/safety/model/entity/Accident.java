package ahqpck.hse.safety.model.entity;

import jakarta.persistence.*;
import lombok.*;
import ahqpck.hse.safety.util.Base62Utils;
import java.time.LocalDateTime;

@Entity
@Table(name = "accidents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Accident {

    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "accident_date", nullable = false)
    private LocalDateTime accidentDate;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private SeverityLevel severity;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccidentStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(name = "reported_by", nullable = false)
    private String reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", nullable = true)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id", nullable = true)
    private User updatedBy;

    @Column(name = "affected_person_name")
    private String affectedPersonName;

    @Column(name = "affected_person_employee_id")
    private String affectedPersonEmployeeId;

    @Column(name = "affected_person_position")
    private String affectedPersonPosition;

    @Column(name = "injury_type")
    private String injuryType;

    @Column(name = "medical_attention_required", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean medicalAttentionRequired = false;

    @Column(name = "cause_of_accident", columnDefinition = "TEXT", nullable = false)
    private String causeOfAccident;

    @Column(name = "witnesses", columnDefinition = "TEXT", nullable = true)
    private String witnesses;

    @Column(name = "preventive_measures", columnDefinition = "TEXT", nullable = true)
    private String preventiveMeasures;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "accident_images", nullable = true)
    private String accidentImages;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isEmpty()) {
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

    public enum SeverityLevel {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    public enum AccidentStatus {
        REPORTED,
        UNDER_INVESTIGATION,
        CLOSED,
        RESOLVED
    }
}
