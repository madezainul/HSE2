package ahqpck.hse.safety.model.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ahqpck.hse.safety.util.Base62Utils;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "observations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Observation {
    
    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "observation_date", nullable = false)
    private LocalDateTime observationDate;

    @Column(name = "observer", nullable = true)
    private String observer;

    @Column(name = "observer_employee_id", nullable = true)
    private String observerEmployeeId;

    @Column(name = "responsible", nullable = true)
    private String responsible;

    @Column(name = "responsible_employee_id", nullable = true)
    private String responsibleEmployeeId;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "mitigation", nullable = false)
    @Enumerated(EnumType.STRING)
    private Mitigation mitigation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", referencedColumnName = "id", nullable = false)
    private Area area;

    @Column(name = "corrective_action", columnDefinition = "TEXT")
    private String correctiveAction;

    @Column(name = "remarks", columnDefinition = "TEXT", nullable = true)
    private String remarks;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "observation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ObservationFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "observation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ObservationComment> comments = new ArrayList<>();

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

    public enum Category {
        BEHAVIOR, FALL_PROTECTION, ELECTRICAL, NOISE, WRONG_WORK_PROCEDURE, HOUSEKEEPING,
        MATERIAL_HANDLING, ENVIRONMENT, ACCESS_AND_BARRICADE, HEALTH_AND_WELFARE_AND_WELLBEING,
        EQUIPMENT_OPERATION, PPE, SAFETY_SIGNS, FIRE_PREVENTION_AND_FIRE_PROTECTION, HAND_AND_POWER_TOOLS,
        RESPIRATORY_PROTECTION, SUSPENDED_WORK, LIFTING_AND_RIGGING, ROAD_AND_TRANSPORTION_SAFETY, PERMIT_TO_WORK,
        PEOPLE_AND_VEHICLE_MOVEMENT, ENTRY_INTO_RESTRICTED_AND_RED_ZONE, OTHER;

        public String getDisplayName() {
            String[] words = this.name().split("_");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
            }
            return sb.toString();
        }
    }

    public enum Type {
        UNSAFE_ACT, UNSAFE_CONDITION, NEAR_MISS;

        public String getDisplayName() {
            String[] words = this.name().split("_");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
            }
            return sb.toString();
        }
    }

    public enum Status {
        OPEN, CLOSED, MITIGATED;

        public String getDisplayName() {
            String[] words = this.name().split("_");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
            }
            return sb.toString();
        }
    }

    public enum Mitigation {
        HIGH, MEDIUM, LOW, CONTROL, MITIGATE;

        public String getDisplayName() {
            String[] words = this.name().split("_");
            StringBuilder sb = new StringBuilder();
            for (String word : words) {
                if (sb.length() > 0) sb.append(" ");
                sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
            }
            return sb.toString();
        }
    }

    @Transient
    public String getObserverName() {
        return observer;
    }

    @Transient
    public String getObserverId() {
        return observerEmployeeId;
    }

    @Transient
    public String getResponsibleName() {
        return responsible;
    }

    @Transient
    public String getResponsibleId() {
        return responsibleEmployeeId;
    }
}
