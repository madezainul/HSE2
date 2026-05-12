package ahqpck.hse.safety.model.entity;

import java.time.LocalDateTime;

import ahqpck.hse.safety.util.Base62Utils;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "incident_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentComment {
    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String id;

    @ManyToOne
    @JoinColumn(name = "incident_id")
    private Incident incident;
    
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
