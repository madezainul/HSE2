package ahqpck.hse.safety.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ahqpck.hse.safety.model.entity.ToolboxMeeting;
import ahqpck.hse.safety.model.entity.ToolboxMeetingFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolboxMeetingDTO {

    // Response-only fields
    private String id;
    private String code;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Request and response fields
    private String location;
    private LocalDate meetingDate;
    private LocalTime timeStarted;
    private LocalTime timeEnd;
    private String supervisor;
    private String supervisorEmployeeId;
    private String notes;
    private String status;

    // Files
    private List<ToolboxMeetingFileDTO> files;

    /**
     * Convert this DTO to a ToolboxMeeting entity (for create/update).
     */
    public ToolboxMeeting toEntity() {
        ToolboxMeeting.MeetingStatus meetingStatus = ToolboxMeeting.MeetingStatus.DRAFT;
        if (this.status != null) {
            try {
                meetingStatus = ToolboxMeeting.MeetingStatus.valueOf(this.status.toUpperCase());
            } catch (IllegalArgumentException ignored) { }
        }

        return ToolboxMeeting.builder()
                .id(this.id)
                .code(this.code)
                .location(this.location)
                .meetingDate(this.meetingDate)
                .timeStarted(this.timeStarted)
                .timeEnd(this.timeEnd)
                .supervisor(this.supervisor)
                .supervisorEmployeeId(this.supervisorEmployeeId)
                .notes(this.notes)
                .status(meetingStatus)
                .build();
    }

    /**
     * Build a DTO from a ToolboxMeeting entity (for API responses).
     */
    public static ToolboxMeetingDTO fromEntity(ToolboxMeeting entity) {
        ToolboxMeetingDTO dto = new ToolboxMeetingDTO();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setLocation(entity.getLocation());
        dto.setMeetingDate(entity.getMeetingDate());
        dto.setTimeStarted(entity.getTimeStarted());
        dto.setTimeEnd(entity.getTimeEnd());
        dto.setSupervisor(entity.getSupervisor());
        dto.setSupervisorEmployeeId(entity.getSupervisorEmployeeId());
        dto.setNotes(entity.getNotes());
        dto.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (entity.getFiles() != null) {
            dto.setFiles(entity.getFiles().stream()
                    .map(ToolboxMeetingFileDTO::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ToolboxMeetingFileDTO {
        private String id;
        private String filePath;
        private String fileName;
        private String fileType;
        private LocalDateTime uploadedAt;

        public static ToolboxMeetingFileDTO fromEntity(ToolboxMeetingFile file) {
            return ToolboxMeetingFileDTO.builder()
                    .id(file.getId())
                    .filePath(file.getFilePath())
                    .fileName(file.getFileName())
                    .fileType(file.getFileType() != null ? file.getFileType().name() : null)
                    .uploadedAt(file.getUploadedAt())
                    .build();
        }
    }
}
