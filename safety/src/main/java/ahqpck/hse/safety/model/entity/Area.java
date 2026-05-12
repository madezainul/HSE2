package ahqpck.hse.safety.model.entity;

import jakarta.persistence.*;
import lombok.*;
import ahqpck.hse.safety.util.Base62Utils;

@Entity
@Table(name = "areas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Area {

    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column
    private String description;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AreaStatus status;

    @PrePersist
    public void prePersist() {
        if (this.id == null || this.id.isEmpty()) {
            this.id = Base62Utils.generateBase62UUID();
        }
    }

    public enum AreaStatus {
        ACTIVE,
        INACTIVE,
        UNDER_MAINTENANCE
    }
}
