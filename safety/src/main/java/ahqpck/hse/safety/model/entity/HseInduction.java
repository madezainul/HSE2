package ahqpck.hse.safety.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ahqpck.hse.safety.util.Base62Utils;
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
    @Column(nullable = false, unique = true, length = 50)
    private String id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "induction_name", nullable = false)
    private String inductionName;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "induction_date", nullable = false)
    private LocalDate inductionDate;

    @Column(name = "value")
    private String value;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "hseInduction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<HseInductionFile> files = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = Base62Utils.generateBase62UUID();
        }
        if (this.code == null || this.code.isEmpty()) {
            this.code = generateInductionCode();
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
     * Generate a unique code for the induction record (e.g., IND-000001)
     */
    private String generateInductionCode() {
        return "IND-" + String.format("%06d", System.nanoTime() % 1000000);
    }
}
