package ahqpck.hse.safety.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ahqpck.hse.safety.model.entity.Observation;
import ahqpck.hse.safety.model.entity.Area;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Observation entity
 * Used for API request/response mapping
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObservationDTO {

    // Response only fields
    private String observationId;
    private String observationCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Request and Response fields
    @NotNull(message = "Observation date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm", shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
    private LocalDateTime observationDate;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotBlank(message = "Category is required")
    private String category;
    
    @NotBlank(message = "Type is required")
    private String type;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    @NotBlank(message = "Mitigation is required")
    private String mitigation;
    
    // Observer information
    private String observerId;  // Employee ID
    private String observerName;
    
    // Area information
    @NotNull(message = "Area is required")
    private AreaDTO area;
    
    // Responsible person information
    private String responsibleId;  // Employee ID
    private String responsibleName;
    
    // Observation details
    private String correctiveAction;
    private String remarks;

    // Files associated with the observation
    private List<ObservationFileDTO> files;

    /**
     * Convert ObservationDTO to Observation entity
     */
    public Observation toEntity() {
        Area areaEntity = null;
        if (this.area != null) {
            areaEntity = Area.builder()
                    .code(this.area.getCode())
                    .name(this.area.getName())
                    .build();
        }

        return Observation.builder()
                .id(this.observationId)
                .code(this.observationCode)
                .observationDate(this.observationDate)
                .description(this.description)
                .category(this.category != null ? Observation.Category.valueOf(this.category) : Observation.Category.OTHER)
                .type(this.type != null ? Observation.Type.valueOf(this.type) : Observation.Type.NEAR_MISS)
                .status(this.status != null ? Observation.Status.valueOf(this.status) : Observation.Status.OPEN)
                .mitigation(this.mitigation != null ? Observation.Mitigation.valueOf(this.mitigation) : Observation.Mitigation.LOW)
                .observer(this.observerName)
                .observerEmployeeId(this.observerId)
                .responsible(this.responsibleName)
                .responsibleEmployeeId(this.responsibleId)
                .correctiveAction(this.correctiveAction)
                .remarks(this.remarks)
                .area(areaEntity)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Convert Observation entity to ObservationDTO
     */
    public static ObservationDTO fromEntity(Observation observation) {
        if (observation == null) {
            return null;
        }
        
        AreaDTO areaDTO = null;
        if (observation.getArea() != null) {
            areaDTO = AreaDTO.builder()
                    .code(observation.getArea().getCode())
                    .name(observation.getArea().getName())
                    .build();
        }

        return ObservationDTO.builder()
                .observationId(observation.getId())
                .observationCode(observation.getCode())
                .observationDate(observation.getObservationDate())
                .description(observation.getDescription())
                .category(observation.getCategory() != null ? observation.getCategory().toString() : null)
                .type(observation.getType() != null ? observation.getType().toString() : null)
                .status(observation.getStatus() != null ? observation.getStatus().toString() : null)
                .mitigation(observation.getMitigation() != null ? observation.getMitigation().toString() : null)
                .observerName(observation.getObserver())
                .observerId(observation.getObserverEmployeeId())
                .responsibleName(observation.getResponsible())
                .responsibleId(observation.getResponsibleEmployeeId())
                .correctiveAction(observation.getCorrectiveAction())
                .remarks(observation.getRemarks())
                .area(areaDTO)
                .createdAt(observation.getCreatedAt())
                .updatedAt(observation.getUpdatedAt())
                .build();
    }
}
