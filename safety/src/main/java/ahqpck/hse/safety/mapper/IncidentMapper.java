package ahqpck.hse.safety.mapper;

import ahqpck.hse.safety.model.dto.IncidentDTO;
import ahqpck.hse.safety.model.dto.AreaDTO;
import ahqpck.hse.safety.model.entity.Incident;
import ahqpck.hse.safety.model.entity.Area;
import ahqpck.hse.safety.repository.AreaRepository;
import ahqpck.hse.safety.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting Incident entity to/from IncidentDTO
 * Follows the pattern established in Accident service for consistency
 */
@Component
@RequiredArgsConstructor
public class IncidentMapper {

    /**
     * Convert Incident entity to IncidentDTO
     * Maps all fields including user and area relationships
     */
    public IncidentDTO toDTO(Incident incident) {
        IncidentDTO dto = new IncidentDTO();

        // Mandatory fields
        dto.setIncidentId(incident.getId());
        dto.setIncidentCode(incident.getCode());
        dto.setReportDate(incident.getReportDate());
        dto.setDescription(incident.getDescription());
        dto.setSeverity(incident.getSeverity() != null ? incident.getSeverity().toString() : null);
        dto.setStatus(incident.getStatus() != null ? incident.getStatus().toString() : null);
        dto.setType(incident.getType() != null ? incident.getType().toString() : null);
        dto.setCreatedAt(incident.getCreatedAt());
        dto.setUpdatedAt(incident.getUpdatedAt());

        // Involved person details
        dto.setInvolvedPersonName(incident.getInvolvedPersonName());
        dto.setInvolvedPersonEmployeeId(incident.getInvolvedPersonEmployeeId());
        dto.setInvolvedPersonPosition(incident.getInvolvedPersonPosition());

        // Incident details
        dto.setImmediateAction(incident.getImmediateAction());
        dto.setCorrectiveAction(incident.getCorrectiveAction());
        dto.setMedicalAttentionRequired(incident.getMedicalAttentionRequired());
        dto.setWitnesses(incident.getWitnesses());

        // Reported by
        dto.setReportedBy(incident.getReportedBy());

        // Area mapping
        if (incident.getArea() != null) {
            AreaDTO areaDTO = new AreaDTO();
            areaDTO.setId(incident.getArea().getId());
            areaDTO.setCode(incident.getArea().getCode());
            areaDTO.setName(incident.getArea().getName());
            areaDTO.setDescription(incident.getArea().getDescription());
            areaDTO.setStatus(incident.getArea().getStatus() != null ? incident.getArea().getStatus().toString() : null);
            dto.setArea(areaDTO);
        }

        return dto;
    }

    /**
     * Convert IncidentDTO to Incident entity
     * Maps all fields including looking up referenced entities from repositories
     */
    public void mapToEntity(Incident incident, IncidentDTO dto,
            AreaRepository areaRepository, UserRepository userRepository) {

        // Mandatory fields
        incident.setReportDate(dto.getReportDate());
        incident.setDescription(dto.getDescription());
        incident.setSeverity(Incident.SeverityLevel.valueOf(dto.getSeverity() != null ? dto.getSeverity() : "MEDIUM"));
        incident.setStatus(Incident.IncidentStatus.valueOf(dto.getStatus() != null ? dto.getStatus() : "REPORTED"));
        incident.setType(Incident.Type.valueOf(dto.getType() != null ? dto.getType() : "UNSAFE_ACT"));

        // Involved person details
        incident.setInvolvedPersonName(dto.getInvolvedPersonName());
        incident.setInvolvedPersonEmployeeId(dto.getInvolvedPersonEmployeeId());
        incident.setInvolvedPersonPosition(dto.getInvolvedPersonPosition());

        // Incident details
        incident.setImmediateAction(dto.getImmediateAction());
        incident.setCorrectiveAction(dto.getCorrectiveAction());
        incident.setMedicalAttentionRequired(
            dto.getMedicalAttentionRequired() != null ? dto.getMedicalAttentionRequired() : false
        );
        incident.setWitnesses(dto.getWitnesses());

        // Map area by code
        if (dto.getArea() != null && dto.getArea().getCode() != null && !dto.getArea().getCode().trim().isEmpty()) {
            String areaCode = dto.getArea().getCode().trim();
            Area area = areaRepository.findByCode(areaCode);
            if (area == null) {
                throw new IllegalArgumentException("Area not found with code: " + areaCode);
            }
            incident.setArea(area);
        } else {
            incident.setArea(null);
        }

        // Map involved person details
        if (dto.getInvolvedPersonName() == null && dto.getInvolvedPersonEmployeeId() != null) {
            incident.setInvolvedPersonName("Employee ID: " + dto.getInvolvedPersonEmployeeId());
        }

        // Map witnesses
        if (dto.getWitnesses() != null && !dto.getWitnesses().trim().isEmpty()) {
            incident.setWitnesses(dto.getWitnesses());
        } else {
            incident.setWitnesses(null);
        }

        // Note: reportedBy should be set from the authenticated user context in the service/controller
        // and not from the DTO for security reasons
    }
}
