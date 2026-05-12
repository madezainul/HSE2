package ahqpck.hse.safety.model.entity;

import java.time.LocalDateTime;

import ahqpck.hse.safety.util.Base62Utils;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "observation_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservationComment {
    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "observation_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Observation observation;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdByUser;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = Base62Utils.generateBase62UUID();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
