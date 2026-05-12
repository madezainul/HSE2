package ahqpck.hse.safety.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import ahqpck.hse.safety.util.Base62Utils;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "toolbox_meetings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolboxMeeting {

    @Id
    @Column(nullable = false, unique = true, length = 50)
    private String id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "location", nullable = false)
    private String location;

    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    @Column(name = "time_started", nullable = false)
    private LocalTime timeStarted;

    @Column(name = "time_end", nullable = true)
    private LocalTime timeEnd;

    @Column(name = "supervisor", nullable = true)
    private String supervisor;

    @Column(name = "supervisor_employee_id", nullable = true)
    private String supervisorEmployeeId;

    @Column(name = "notes", columnDefinition = "TEXT", nullable = true)
    private String notes;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private MeetingStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "toolboxMeeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ToolboxMeetingFile> files = new ArrayList<>();

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
        if (this.status == null) {
            this.status = MeetingStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public enum MeetingStatus {
        DRAFT, COMPLETED, CANCELLED;

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
}
