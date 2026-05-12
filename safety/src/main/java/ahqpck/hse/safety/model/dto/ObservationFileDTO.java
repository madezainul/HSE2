package ahqpck.hse.safety.model.dto;

import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.entity.ObservationFile;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for ObservationFile entity
 * Used for API request/response mapping for observation files
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationFileDTO {

    private String id;
    private String fileName;
    private String filePath;
    private String fileType;           // "IMAGE" or "DOCUMENT"
    private LocalDateTime uploadedAt;
    private String observationCode;    // expose code, not internal id
    private String observationId;      // keep id for internal use if needed

    /**
     * Convert ObservationFile entity to ObservationFileDTO
     */
    public static ObservationFileDTO fromEntity(ObservationFile file) {
        return ObservationFileDTO.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .filePath(file.getFilePath())
                .fileType(file.getFileType().name())
                .uploadedAt(file.getUploadedAt())
                .observationCode(file.getObservation().getCode())
                .observationId(file.getObservation().getId())
                .build();
    }

    public ObservationFile toEntity(Observation observation) {
        return ObservationFile.builder()
                .id(this.id)
                .fileName(this.fileName)
                .filePath(this.filePath)
                .fileType(ObservationFile.FileType.valueOf(this.fileType))
                .uploadedAt(LocalDateTime.now())
                .observation(observation)
                .build();
    }
}
