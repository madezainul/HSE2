package ahqpck.hse.safety.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_for_user", columnList = "for_user_id"),
    @Index(name = "idx_notif_read", columnList = "is_read")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User ID this notification is targeted to.
     */
    @Column(name = "for_user_id", nullable = false)
    private String forUserId;

    /**
     * User ID who triggered the action (creator of the incident/observation).
     */
    @Column(name = "created_by_user_id")
    private String createdByUserId;

    /**
     * Short message, e.g. "New Incident INC-000001 reported by John"
     */
    @Column(nullable = false)
    private String message;

    /**
     * Type: INCIDENT or OBSERVATION
     */
    @Column(nullable = false, length = 20)
    private String type;

    /**
     * The code of the incident/observation, e.g. INC-000001
     */
    @Column(name = "reference_code")
    private String referenceCode;

    /**
     * URL to navigate to when notification is clicked.
     */
    @Column(name = "link_url")
    private String linkUrl;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = false;
        }
    }
}
