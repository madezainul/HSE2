package ahqpck.hse.safety.model.entity;

import jakarta.persistence.*;
import lombok.*;
import ahqpck.hse.safety.util.Base62Utils;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "incidents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "report_date", nullable = false)
    private LocalDateTime reportDate;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "severity", nullable = false)
    @Enumerated(EnumType.STRING)
    private SeverityLevel severity;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    private Area area;

    @Column(name = "reported_by", nullable = false)
    private String reportedBy;

    @Column(name = "involved_person_name")
    private String involvedPersonName;

    @Column(name = "involved_person_employee_id")
    private String involvedPersonEmployeeId;

    @Column(name = "involved_person_position")
    private String involvedPersonPosition;

    @Column(name = "witnesses", columnDefinition = "TEXT")
    private String witnesses;

    @Column(name = "witnesses_employee_id")
    private String witnessesEmployeeId;

    @Column(name = "immediate_action", columnDefinition = "TEXT")
    private String immediateAction;

    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    @Column(name = "medical_attention_required", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean medicalAttentionRequired = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id", nullable = true)
    private User updatedBy;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<IncidentFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<IncidentComment> comments = new ArrayList<>();

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

    public enum IncidentStatus {
        REPORTED,
        UNDER_INVESTIGATION,
        CLOSED,
        RESOLVED
    }

    public enum Type {
        ACCIDENT,
        NEAR_MISS,
        UNSAFE_ACT,
        UNSAFE_CONDITION,
        PROPERTY_DAMAGE,
        ENVIRONMENTAL_INCIDENT
    }
}
