package ahqpck.hse.safety.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ahqpck.hse.safety.model.entity.Accident;
import ahqpck.hse.safety.model.entity.Area;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Accident entity
 * Used for API request/response mapping
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccidentDTO {

    // Response only fields
    private String accidentCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Request and Response fields
    @NotNull(message = "Accident date is required")
    private LocalDateTime accidentDate;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotBlank(message = "Severity is required")
    private String severity;
    
    private String status;
    
    private String reportedBy;
    
    // Area information
    @NotNull(message = "Area is required")
    private AreaDTO area;
    
    // Affected person information
    private String affectedPersonName;
    private String affectedPersonEmployeeId;
    private String affectedPersonPosition;
    
    // Incident details
    private String witnesses;
    private String injuryType;
    private String causeOfAccident;
    private Boolean medicalAttentionRequired;
    
    // Image file name (transient, stored in separate @RequestParam)
    private String accidentImages;

    /**
     * Convert AccidentDTO to Accident entity
     */
    public Accident toEntity() {
        Area areaEntity = null;
        if (this.area != null) {
            areaEntity = Area.builder()
                    .code(this.area.getCode())
                    .name(this.area.getName())
                    .build();
        }

        return Accident.builder()
                .code(this.accidentCode)
                .accidentDate(this.accidentDate)
                .description(this.description)
                .severity(Accident.SeverityLevel.valueOf(this.severity != null ? this.severity : "MEDIUM"))
                .status(Accident.AccidentStatus.valueOf(this.status != null ? this.status : "REPORTED"))
                .area(areaEntity)
                .reportedBy(this.reportedBy)
                .affectedPersonName(this.affectedPersonName)
                .affectedPersonEmployeeId(this.affectedPersonEmployeeId)
                .affectedPersonPosition(this.affectedPersonPosition)
                .witnesses(this.witnesses)
                .injuryType(this.injuryType)
                .causeOfAccident(this.causeOfAccident)
                .medicalAttentionRequired(this.medicalAttentionRequired != null ? this.medicalAttentionRequired : false)
                .accidentImages(this.accidentImages)
                .build();
    }

    /**
     * Convert Accident entity to AccidentDTO
     */
    public static AccidentDTO fromEntity(Accident accident) {
        if (accident == null) {
            return null;
        }

        AreaDTO areaDTO = null;
        if (accident.getArea() != null) {
            areaDTO = AreaDTO.fromEntity(accident.getArea());
        }

        return AccidentDTO.builder()
                .accidentCode(accident.getCode())
                .accidentDate(accident.getAccidentDate())
                .description(accident.getDescription())
                .severity(accident.getSeverity() != null ? accident.getSeverity().toString() : null)
                .status(accident.getStatus() != null ? accident.getStatus().toString() : null)
                .reportedBy(accident.getReportedBy())
                .area(areaDTO)
                .affectedPersonName(accident.getAffectedPersonName())
                .affectedPersonEmployeeId(accident.getAffectedPersonEmployeeId())
                .affectedPersonPosition(accident.getAffectedPersonPosition())
                .witnesses(accident.getWitnesses())
                .injuryType(accident.getInjuryType())
                .causeOfAccident(accident.getCauseOfAccident())
                .medicalAttentionRequired(accident.getMedicalAttentionRequired())
                .accidentImages(accident.getAccidentImages())
                .createdAt(accident.getCreatedAt())
                .updatedAt(accident.getUpdatedAt())
                .build();
    }
}
