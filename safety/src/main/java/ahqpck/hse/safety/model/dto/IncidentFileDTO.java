package ahqpck.hse.safety.model.dto;

import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.IncidentFile;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentFileDTO {

    private String id;
    private String fileName;
    private String filePath;
    private String fileType;      // "IMAGE" or "DOCUMENT"
    private LocalDateTime uploadedAt;
    private String incidentCode;  // expose code, not internal id
    private String incidentId;    // keep id for internal use if needed

    public static IncidentFileDTO fromEntity(IncidentFile file) {
        return IncidentFileDTO.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .filePath(file.getFilePath())
                .fileType(file.getFileType().name())
                .uploadedAt(file.getUploadedAt())
                .incidentCode(file.getIncident().getCode())
                .incidentId(file.getIncident().getId())
                .build();
    }

    public IncidentFile toEntity(Incident incident) {
        return IncidentFile.builder()
                .id(this.id)
                .fileName(this.fileName)
                .filePath(this.filePath)
                .fileType(IncidentFile.FileType.valueOf(this.fileType))
                .uploadedAt(LocalDateTime.now())
                .incident(incident)
                .build();
    }
}