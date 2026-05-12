package ahqpck.hse.safety.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.Area;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Incident entity
 * Used for API request/response mapping
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncidentDTO {

    // Response only fields
    private String incidentId;
    private String incidentCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Request and Response fields
    @NotNull(message = "Report date is required")
    private LocalDateTime reportDate;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotBlank(message = "Severity is required")
    private String severity;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    @NotBlank(message = "Type is required")
    private String type;
    
    private String reportedBy;
    
    // Area information
    @NotNull(message = "Area is required")
    private AreaDTO area;
    
    // Involved person information
    private String involvedPersonName;
    private String involvedPersonEmployeeId;
    private String involvedPersonPosition;
    
    // Incident details
    private String witnesses;
    private String witnessesEmployeeId;
    private String immediateAction;
    private String correctiveAction;
    private Boolean medicalAttentionRequired;

    // Files associated with the incident
    private List<IncidentFileDTO> files;

    /**
     * Convert IncidentDTO to Incident entity
     */
    public Incident toEntity() {
        Area areaEntity = null;
        if (this.area != null) {
            areaEntity = Area.builder()
                    .code(this.area.getCode())
                    .name(this.area.getName())
                    .build();
        }

        return Incident.builder()
                .id(this.incidentId)
                .code(this.incidentCode)
                .reportDate(this.reportDate)
                .description(this.description)
                .severity(this.severity != null ? Incident.SeverityLevel.valueOf(this.severity) : Incident.SeverityLevel.MEDIUM)
                .status(this.status != null ? Incident.IncidentStatus.valueOf(this.status) : Incident.IncidentStatus.REPORTED)
                .type(this.type != null ? Incident.Type.valueOf(this.type) : Incident.Type.ACCIDENT)
                .area(areaEntity)
                .reportedBy(this.reportedBy)
                .involvedPersonName(this.involvedPersonName)
                .involvedPersonEmployeeId(this.involvedPersonEmployeeId)
                .involvedPersonPosition(this.involvedPersonPosition)
                .witnesses(this.witnesses)
                .witnessesEmployeeId(this.witnessesEmployeeId)
                .immediateAction(this.immediateAction)
                .correctiveAction(this.correctiveAction)
                .medicalAttentionRequired(this.medicalAttentionRequired != null ? this.medicalAttentionRequired : false)
                .build();
    }

    /**
     * Convert Incident entity to IncidentDTO
     */
    public static IncidentDTO fromEntity(Incident incident) {
        if (incident == null) {
            return null;
        }

        AreaDTO areaDTO = null;
        if (incident.getArea() != null) {
            areaDTO = AreaDTO.builder()
                    .code(incident.getArea().getCode())
                    .name(incident.getArea().getName())
                    .build();
        }

        return IncidentDTO.builder()
                .incidentId(incident.getId())
                .incidentCode(incident.getCode())
                .reportDate(incident.getReportDate())
                .description(incident.getDescription())
                .severity(incident.getSeverity() != null ? incident.getSeverity().toString() : null)
                .status(incident.getStatus() != null ? incident.getStatus().toString() : null)
                .type(incident.getType() != null ? incident.getType().toString() : null)
                .reportedBy(incident.getReportedBy())
                .area(areaDTO)
                .involvedPersonName(incident.getInvolvedPersonName())
                .involvedPersonEmployeeId(incident.getInvolvedPersonEmployeeId())
                .involvedPersonPosition(incident.getInvolvedPersonPosition())
                .witnesses(incident.getWitnesses())
                .witnessesEmployeeId(incident.getWitnessesEmployeeId())
                .immediateAction(incident.getImmediateAction())
                .correctiveAction(incident.getCorrectiveAction())
                .medicalAttentionRequired(incident.getMedicalAttentionRequired())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}
